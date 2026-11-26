package fes.proyecto.tickets.ptickets.businessLayer;

import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.entities.CategoriaFalla;
import fes.proyecto.tickets.ptickets.entities.EstadoTicket;
import fes.proyecto.tickets.ptickets.entities.Prioridad;
import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.repositories.ICategoriaFallaRepository;
import fes.proyecto.tickets.ptickets.repositories.IEstadoTicketRepository;
import fes.proyecto.tickets.ptickets.repositories.IPrioridadRepository;
import fes.proyecto.tickets.ptickets.repositories.ITicketRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketBusinessLayerTest {

    private static final int ID_CLIENTE = 10;
    private static final String GUID = "guid-ticket-001";
    private static final int ANIO = LocalDateTime.now().getYear();

    @Mock private ITicketRepository ticketRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private ICategoriaFallaRepository categoriaRepository;
    @Mock private IPrioridadRepository prioridadRepository;
    @Mock private IEstadoTicketRepository estadoRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private TicketBusinessLayer businessLayer;
    private TicketDtoIn dtoIn;
    private Usuario cliente;
    private final EstadoTicket abierto = new EstadoTicket(1, "Abierto", 1);

    @BeforeEach
    void setUp() {
        businessLayer = new TicketBusinessLayer(ticketRepository, usuarioRepository, categoriaRepository,
                prioridadRepository, estadoRepository, transactionManager);

        dtoIn = new TicketDtoIn(1, 2, "  Sin internet en el edificio A  ",
                LocalDateTime.now().minusHours(3), " Edificio A ", " Internet ", "   ", "  " + GUID + "  ");

        cliente = usuario(ID_CLIENTE, "Ana Lopez", "CLIENTE");
    }

    private static Usuario usuario(int id, String nombre, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase().replace(' ', '.') + "@example.com");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    /** Catálogos, cliente y guardado listos para un registro exitoso. */
    private void prepararRegistro(List<Usuario> adminsActivos) {
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(new CategoriaFalla(1, "Conectividad")));
        when(prioridadRepository.findById(2)).thenReturn(Optional.of(new Prioridad(2, "Media", 2)));
        when(estadoRepository.findByNombreIgnoreCase("Abierto")).thenReturn(Optional.of(abierto));
        when(usuarioRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByRolAndActivoTrue("ADMIN")).thenReturn(adminsActivos);
        when(ticketRepository.obtenerConsecutivoMaximo(ANIO)).thenReturn(0);
        // lenient: las pruebas de colisión reemplazan este comportamiento
        lenient().when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setId(50);
            return t;
        });
    }

    private Ticket ticketGuardado() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository, atLeastOnce()).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    // ---------- creación exitosa ----------

    @Test
    void agregar_DatosValidos_CreaTicketAbiertoConFolioYUsuarioDeLaSesion() {
        prepararRegistro(List.of(usuario(20, "Carlos Admin", "ADMIN")));

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn, ID_CLIENTE);

        String folioEsperado = "FESA-" + ANIO + "-001";
        assertFalse(resultado.registroPrevio());
        assertEquals(50, resultado.idDto().getId());
        assertEquals("Ticket " + folioEsperado + " creado", resultado.idDto().getMensaje());

        Ticket t = ticketGuardado();
        assertEquals(folioEsperado, t.getFolio());
        assertTrue(t.getFolio().matches("FESA-\\d{4}-[0-9A-Z]{3,}"));
        assertSame(abierto, t.getEstado());
        assertSame(cliente, t.getUsuario());
        assertEquals("Conectividad", t.getCategoria().getNombre());
        assertEquals("Media", t.getPrioridad().getNombre());
        assertNotNull(t.getFechaRegistro());
        assertTrue(ChronoUnit.SECONDS.between(t.getFechaRegistro(), LocalDateTime.now()) < 5);
        // Textos recortados y medio de transmisión vacío guardado como null
        assertEquals("Sin internet en el edificio A", t.getDescripcion());
        assertEquals("Edificio A", t.getAreaAfectada());
        assertEquals("Internet", t.getServicioImpactado());
        assertNull(t.getMedioTransmision());
        assertEquals(GUID, t.getGuid());
        assertNull(t.getFechaCierre());
    }

    @Test
    void agregar_ConsecutivoPrevio_GeneraElSiguienteFolio() {
        prepararRegistro(List.of());
        when(ticketRepository.obtenerConsecutivoMaximo(ANIO)).thenReturn(396);

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn, ID_CLIENTE);

        assertEquals("FESA-" + ANIO + "-0B1", ticketGuardado().getFolio());
        assertEquals("Ticket FESA-" + ANIO + "-0B1 creado", resultado.idDto().getMensaje());
    }

    // ---------- asignación de administrador ----------

    @Test
    void agregar_UnAdminActivo_LoAsigna() {
        Usuario admin = usuario(20, "Carlos Admin", "ADMIN");
        prepararRegistro(List.of(admin));

        businessLayer.agregar(dtoIn, ID_CLIENTE);

        assertSame(admin, ticketGuardado().getAdministradorAsignado());
    }

    @Test
    void agregar_VariosAdminsActivos_AsignaUnoAlAzar() {
        List<Usuario> admins = List.of(
                usuario(20, "Carlos Admin", "ADMIN"),
                usuario(21, "Laura Admin", "ADMIN"),
                usuario(22, "Fernando Admin", "ADMIN"));
        prepararRegistro(admins);

        Set<Integer> asignados = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            businessLayer.agregar(dtoIn, ID_CLIENTE);
            Usuario asignado = ticketGuardado().getAdministradorAsignado();
            assertTrue(admins.contains(asignado), "Debe asignarse uno de los admins activos");
            asignados.add(asignado.getId());
        }
        // Con 100 tickets y 3 admins, la probabilidad de no ver a alguno es despreciable
        assertEquals(Set.of(20, 21, 22), asignados);
    }

    @Test
    void agregar_SinAdminsActivos_QuedaSinAsignar() {
        prepararRegistro(List.of());

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn, ID_CLIENTE);

        assertFalse(resultado.registroPrevio());
        assertNull(ticketGuardado().getAdministradorAsignado());
    }

    // ---------- idempotencia ----------

    @Test
    void agregar_GuidRepetidoDelMismoCliente_DevuelveExistenteSinCrear() {
        Ticket existente = new Ticket();
        existente.setId(7);
        existente.setFolio("FESA-" + ANIO + "-005");
        existente.setUsuario(cliente);
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.of(existente));

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn, ID_CLIENTE);

        assertTrue(resultado.registroPrevio());
        assertEquals(7, resultado.idDto().getId());
        assertEquals("Ticket FESA-" + ANIO + "-005 registrado previamente (GUID duplicado)",
                resultado.idDto().getMensaje());
        verify(ticketRepository, never()).saveAndFlush(any());
        verifyNoInteractions(categoriaRepository, prioridadRepository, estadoRepository, usuarioRepository);
    }

    @Test
    void agregar_GuidUsadoPorOtroCliente_LanzaRecursoDuplicado() {
        Ticket deOtro = new Ticket();
        deOtro.setId(8);
        deOtro.setUsuario(usuario(99, "Otro Cliente", "CLIENTE"));
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.of(deOtro));

        assertThrows(RecursoDuplicadoException.class, () -> businessLayer.agregar(dtoIn, ID_CLIENTE));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    // ---------- catálogos inexistentes ----------

    @Test
    void agregar_CategoriaInexistente_LanzaSolicitudInvalida() {
        dtoIn.setCategoriaId(99);
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        SolicitudInvalidaException ex =
                assertThrows(SolicitudInvalidaException.class, () -> businessLayer.agregar(dtoIn, ID_CLIENTE));
        assertEquals("La categoría con id 99 no existe", ex.getMessage());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void agregar_PrioridadInexistente_LanzaSolicitudInvalida() {
        dtoIn.setPrioridadId(99);
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(new CategoriaFalla(1, "Conectividad")));
        when(prioridadRepository.findById(99)).thenReturn(Optional.empty());

        SolicitudInvalidaException ex =
                assertThrows(SolicitudInvalidaException.class, () -> businessLayer.agregar(dtoIn, ID_CLIENTE));
        assertEquals("La prioridad con id 99 no existe", ex.getMessage());
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void agregar_SinEstadoAbiertoEnCatalogo_LanzaIllegalState() {
        when(ticketRepository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(new CategoriaFalla(1, "Conectividad")));
        when(prioridadRepository.findById(2)).thenReturn(Optional.of(new Prioridad(2, "Media", 2)));
        when(estadoRepository.findByNombreIgnoreCase("Abierto")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> businessLayer.agregar(dtoIn, ID_CLIENTE));
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    // ---------- colisión de folio ----------

    @Test
    void agregar_ColisionDeFolio_ReintentaUnaVezConElSiguienteFolio() {
        prepararRegistro(List.of());
        // Otro ticket tomó el 001 entre el cálculo y el INSERT
        when(ticketRepository.obtenerConsecutivoMaximo(ANIO)).thenReturn(0, 1);
        doThrow(new DataIntegrityViolationException("duplicate key folio"))
                .doAnswer(invocation -> {
                    Ticket t = invocation.getArgument(0);
                    t.setId(51);
                    return t;
                })
                .when(ticketRepository).saveAndFlush(any(Ticket.class));

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn, ID_CLIENTE);

        assertEquals(51, resultado.idDto().getId());
        assertEquals("Ticket FESA-" + ANIO + "-002 creado", resultado.idDto().getMensaje());
        verify(ticketRepository, times(2)).saveAndFlush(any(Ticket.class));
        // Cada intento corre en su propia transacción: el primero se revierte
        verify(transactionManager, times(2)).getTransaction(any());
        verify(transactionManager, times(1)).rollback(any());
    }

    @Test
    void agregar_ColisionEnAmbosIntentos_PropagaLaExcepcion() {
        prepararRegistro(List.of());
        doThrow(new DataIntegrityViolationException("duplicate key folio"))
                .when(ticketRepository).saveAndFlush(any(Ticket.class));

        assertThrows(DataIntegrityViolationException.class, () -> businessLayer.agregar(dtoIn, ID_CLIENTE));
        verify(ticketRepository, times(2)).saveAndFlush(any(Ticket.class));
    }
}
