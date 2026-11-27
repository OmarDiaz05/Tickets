package fes.proyecto.tickets.ptickets.businessLayer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.PaginaDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.dtos.TicketFiltroDto;
import fes.proyecto.tickets.ptickets.entities.CategoriaFalla;
import fes.proyecto.tickets.ptickets.entities.EstadoTicket;
import fes.proyecto.tickets.ptickets.entities.Prioridad;
import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.NoAutenticadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.helpers.FolioGenerator;
import fes.proyecto.tickets.ptickets.helpers.Mappers;
import fes.proyecto.tickets.ptickets.repositories.ICategoriaFallaRepository;
import fes.proyecto.tickets.ptickets.repositories.IEstadoTicketRepository;
import fes.proyecto.tickets.ptickets.repositories.IPrioridadRepository;
import fes.proyecto.tickets.ptickets.repositories.ITicketRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import fes.proyecto.tickets.ptickets.repositories.TicketFiltro;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;

@Service
public class TicketBusinessLayer {

    /**
     * Resultado de un registro. {@code registroPrevio} es true cuando el mismo
     * cliente ya había enviado un ticket con ese guid (idempotencia): no se crea
     * nada nuevo y se devuelve el id existente.
     */
    public record ResultadoRegistro(IdDto idDto, boolean registroPrevio) {
    }

    static final String ESTADO_INICIAL = "Abierto";
    static final String ROL_ADMIN = "ADMIN";
    static final int TAMANIO_MAXIMO_PAGINA = 50;

    private static final Logger log = LoggerFactory.getLogger(TicketBusinessLayer.class);

    private final ITicketRepository ticketRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ICategoriaFallaRepository categoriaRepository;
    private final IPrioridadRepository prioridadRepository;
    private final IEstadoTicketRepository estadoRepository;
    private final TransactionTemplate transactionTemplate;

    public TicketBusinessLayer(
            ITicketRepository ticketRepository,
            IUsuarioRepository usuarioRepository,
            ICategoriaFallaRepository categoriaRepository,
            IPrioridadRepository prioridadRepository,
            IEstadoTicketRepository estadoRepository,
            PlatformTransactionManager transactionManager) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.estadoRepository = estadoRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Crea un ticket para el cliente de la sesión.
     *
     * Cada intento corre completo en su propia transacción (equivale a
     * {@code @Transactional}). Si el INSERT choca con una clave única porque
     * otro ticket tomó el mismo folio al mismo tiempo, se reintenta una vez en
     * una transacción nueva: PostgreSQL aborta la transacción tras el error y
     * no permite seguir usándola. El reintento recalcula el folio y vuelve a
     * revisar el guid, así un doble envío simultáneo termina en 208.
     *
     * @param idUsuarioSesion id del UsuarioAutenticado; nunca viene del cuerpo
     */
    public ResultadoRegistro agregar(TicketDtoIn dtoIn, int idUsuarioSesion) {
        try {
            return transactionTemplate.execute(status -> registrar(dtoIn, idUsuarioSesion));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Colisión de clave única al crear ticket; se reintenta una vez");
            return transactionTemplate.execute(status -> registrar(dtoIn, idUsuarioSesion));
        }
    }

    private ResultadoRegistro registrar(TicketDtoIn dtoIn, int idUsuarioSesion) {
        String guid = dtoIn.getGuid().trim();

        // Idempotencia por GUID: misma solicitud repetida devuelve el ticket existente
        Optional<Ticket> previo = ticketRepository.findByGuid(guid);
        if (previo.isPresent()) {
            Ticket existente = previo.get();
            if (existente.getUsuario() == null || existente.getUsuario().getId() != idUsuarioSesion) {
                throw new RecursoDuplicadoException("El guid ya fue utilizado en otro ticket");
            }
            return new ResultadoRegistro(
                    new IdDto(existente.getId(),
                            "Ticket " + existente.getFolio() + " registrado previamente (GUID duplicado)"),
                    true);
        }

        CategoriaFalla categoria = categoriaRepository.findById(dtoIn.getCategoriaId())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "La categoría con id " + dtoIn.getCategoriaId() + " no existe"));

        Prioridad prioridad = prioridadRepository.findById(dtoIn.getPrioridadId())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "La prioridad con id " + dtoIn.getPrioridadId() + " no existe"));

        EstadoTicket estadoInicial = estadoRepository.findByNombreIgnoreCase(ESTADO_INICIAL)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado '" + ESTADO_INICIAL + "' en el catálogo estados_ticket"));

        Usuario cliente = usuarioRepository.findById(idUsuarioSesion)
                .orElseThrow(() -> new NoAutenticadoException("La sesión no corresponde a un usuario registrado"));

        LocalDateTime ahora = LocalDateTime.now();
        int anio = ahora.getYear();
        String folio = FolioGenerator.siguiente(anio, ticketRepository.obtenerConsecutivoMaximo(anio));

        Ticket ticket = Mappers.toEntity(dtoIn);
        ticket.setFolio(folio);
        ticket.setUsuario(cliente);
        ticket.setAdministradorAsignado(elegirAdministradorAlAzar());
        ticket.setCategoria(categoria);
        ticket.setPrioridad(prioridad);
        ticket.setEstado(estadoInicial);
        ticket.setFechaRegistro(ahora);

        Ticket guardado = ticketRepository.saveAndFlush(ticket);

        return new ResultadoRegistro(new IdDto(guardado.getId(), "Ticket " + folio + " creado"), false);
    }

    // ---------- Consultas (Punto 5.3) ----------

    /**
     * Lista paginada de tickets, del más reciente al más antiguo.
     *
     * Un CLIENTE solo ve los suyos: el filtro por usuario se fuerza con el id de
     * la sesión y se ignoran administradorId y soloMios. Un ADMIN ve todos y
     * puede combinar los filtros; soloMios limita a los asignados a él.
     */
    @Transactional(readOnly = true)
    public PaginaDto<TicketDto> obtenerTodos(TicketFiltroDto filtroDto, int page, int size,
            UsuarioAutenticado sesion) {
        TicketFiltroDto f = filtroDto != null ? filtroDto : new TicketFiltroDto();

        if (page < 0) {
            throw new SolicitudInvalidaException("El parámetro page no puede ser negativo");
        }
        if (size < 1) {
            throw new SolicitudInvalidaException("El parámetro size debe ser mayor o igual a 1");
        }
        LocalDate desde = f.getFechaDesde();
        LocalDate hasta = f.getFechaHasta();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                    "La fechaDesde (" + desde + ") no puede ser posterior a la fechaHasta (" + hasta + ")");
        }

        boolean esAdmin = esAdmin(sesion);
        TicketFiltro filtro = new TicketFiltro(
                esAdmin ? null : sesion.getId(),
                esAdmin ? f.getAdministradorId() : null,
                esAdmin && Boolean.TRUE.equals(f.getSoloMios()) ? sesion.getId() : null,
                f.getEstadoId(),
                f.getPrioridadId(),
                f.getCategoriaId(),
                Boolean.TRUE.equals(f.getSinAsignar()),
                desde != null ? desde.atStartOfDay() : null,
                hasta != null ? hasta.plusDays(1).atStartOfDay() : null);

        Pageable pageable = PageRequest.of(page, Math.min(size, TAMANIO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "fechaRegistro").and(Sort.by(Sort.Direction.DESC, "id")));

        return Mappers.toPaginaDto(ticketRepository.buscar(filtro, pageable));
    }

    /** Detalle de un ticket. Un CLIENTE solo puede ver los suyos. */
    @Transactional(readOnly = true)
    public TicketDto obtenerPorId(int id, UsuarioAutenticado sesion) {
        Ticket ticket = ticketRepository.buscarDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado con id " + id));

        boolean esDueno = ticket.getUsuario() != null && ticket.getUsuario().getId() == sesion.getId();
        if (!esAdmin(sesion) && !esDueno) {
            throw new AccesoDenegadoException("No tienes permiso para ver este ticket");
        }
        return Mappers.toDto(ticket);
    }

    /** Cualquier rol distinto de ADMIN se trata como cliente (el caso más restrictivo). */
    private static boolean esAdmin(UsuarioAutenticado sesion) {
        return sesion.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + ROL_ADMIN).equals(a.getAuthority()));
    }

    /** Un administrador activo al azar, o null si no hay ninguno. */
    private Usuario elegirAdministradorAlAzar() {
        List<Usuario> admins = usuarioRepository.findByRolAndActivoTrue(ROL_ADMIN);
        if (admins.isEmpty()) {
            return null;
        }
        return admins.get(ThreadLocalRandom.current().nextInt(admins.size()));
    }
}
