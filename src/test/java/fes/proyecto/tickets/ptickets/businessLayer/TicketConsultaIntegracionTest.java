package fes.proyecto.tickets.ptickets.businessLayer;

import fes.proyecto.tickets.ptickets.dtos.PaginaDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDto;
import fes.proyecto.tickets.ptickets.dtos.TicketFiltroDto;
import fes.proyecto.tickets.ptickets.entities.CategoriaFalla;
import fes.proyecto.tickets.ptickets.entities.EstadoTicket;
import fes.proyecto.tickets.ptickets.entities.Prioridad;
import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas de tickets (Punto 5.3) contra una base H2 en memoria: capa de
 * negocio + repositorio + Specifications reales.
 *
 * Datos (orden por fechaRegistro descendente: t6, t5, t4, t3, t2, t1):
 * <pre>
 *  ticket  cliente  admin  estado      prioridad  categoría     fechaRegistro
 *  t1      A        X      Abierto     Alta       DNS           2026-11-01 10:00
 *  t2      A        -      En proceso  Baja       Conectividad  2026-11-05 23:59:59
 *  t3      A        Y      Abierto     Baja       DNS           2026-11-10 00:00
 *  t4      B        X      En proceso  Alta       Conectividad  2026-11-15 12:00
 *  t5      B        -      Abierto     Alta       DNS           2026-11-20 08:00
 *  t6      B        Y      Abierto     Baja       Conectividad  2026-11-25 18:30
 * </pre>
 */
@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import(TicketBusinessLayer.class)
class TicketConsultaIntegracionTest {

    @Autowired private TicketBusinessLayer businessLayer;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    private Usuario clienteA, clienteB, adminX, adminY;
    private EstadoTicket abierto, enProceso;
    private Prioridad baja, alta;
    private CategoriaFalla dns, conectividad;
    private Ticket t1, t2, t3, t4, t5, t6;

    @BeforeEach
    void cargarDatos() {
        clienteA = persistirUsuario("Ana Cliente", "CLIENTE");
        clienteB = persistirUsuario("Beto Cliente", "CLIENTE");
        adminX = persistirUsuario("Xavier Admin", "ADMIN");
        adminY = persistirUsuario("Yolanda Admin", "ADMIN");

        abierto = persistir(new EstadoTicket(0, "Abierto", 1));
        enProceso = persistir(new EstadoTicket(0, "En proceso", 3));
        baja = persistir(new Prioridad(0, "Baja", 1));
        alta = persistir(new Prioridad(0, "Alta", 3));
        dns = persistir(new CategoriaFalla(0, "DNS"));
        conectividad = persistir(new CategoriaFalla(0, "Conectividad"));

        t1 = persistirTicket(1, clienteA, adminX, abierto, alta, dns, LocalDateTime.of(2026, 11, 1, 10, 0));
        t2 = persistirTicket(2, clienteA, null, enProceso, baja, conectividad, LocalDateTime.of(2026, 11, 5, 23, 59, 59));
        t3 = persistirTicket(3, clienteA, adminY, abierto, baja, dns, LocalDateTime.of(2026, 11, 10, 0, 0));
        t4 = persistirTicket(4, clienteB, adminX, enProceso, alta, conectividad, LocalDateTime.of(2026, 11, 15, 12, 0));
        t5 = persistirTicket(5, clienteB, null, abierto, alta, dns, LocalDateTime.of(2026, 11, 20, 8, 0));
        t6 = persistirTicket(6, clienteB, adminY, abierto, baja, conectividad, LocalDateTime.of(2026, 11, 25, 18, 30));

        em.flush();
        em.clear(); // las consultas deben ir a la base, no al contexto de persistencia
    }

    // ---------- helpers ----------

    private <T> T persistir(T entidad) {
        em.persist(entidad);
        return entidad;
    }

    private Usuario persistirUsuario(String nombre, String rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(nombre.toLowerCase().replace(' ', '.') + "@example.com");
        u.setPassword("$2a$04$hash");
        u.setRol(rol);
        u.setVerificado(true);
        u.setActivo(true);
        u.setFechaRegistro(LocalDateTime.of(2026, 10, 1, 0, 0));
        u.setGuid("guid-" + nombre);
        return persistir(u);
    }

    private Ticket persistirTicket(int n, Usuario cliente, Usuario admin, EstadoTicket estado,
            Prioridad prioridad, CategoriaFalla categoria, LocalDateTime fechaRegistro) {
        Ticket t = new Ticket();
        t.setFolio("FESA-2026-00" + n);
        t.setGuid("guid-ticket-" + n);
        t.setUsuario(cliente);
        t.setAdministradorAsignado(admin);
        t.setEstado(estado);
        t.setPrioridad(prioridad);
        t.setCategoria(categoria);
        t.setDescripcion("Falla " + n);
        t.setDesdeCuando(fechaRegistro.minusHours(1));
        t.setAreaAfectada("Área " + n);
        t.setServicioImpactado("Servicio " + n);
        t.setFechaRegistro(fechaRegistro);
        return persistir(t);
    }

    private static UsuarioAutenticado sesion(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getEmail(), "x", u.getRol(), true);
    }

    private PaginaDto<TicketDto> listar(Usuario usuarioSesion, TicketFiltroDto filtro) {
        return businessLayer.obtenerTodos(filtro, 0, 10, sesion(usuarioSesion));
    }

    private static TicketFiltroDto filtro() {
        return new TicketFiltroDto();
    }

    private static List<String> folios(PaginaDto<TicketDto> pagina) {
        return pagina.getContenido().stream().map(TicketDto::getFolio).toList();
    }

    private static List<String> folios(Ticket... tickets) {
        return java.util.Arrays.stream(tickets).map(Ticket::getFolio).toList();
    }

    // ---------- CLIENTE ----------

    @Test
    void cliente_SinFiltros_SoloVeLosSuyosDelMasRecienteAlMasAntiguo() {
        PaginaDto<TicketDto> pagina = listar(clienteA, filtro());

        assertEquals(folios(t3, t2, t1), folios(pagina));
        assertTrue(pagina.getContenido().stream().allMatch(t -> "ana.cliente@example.com".equals(t.getCorreoCliente())));
    }

    @Test
    void cliente_ConFiltrosAjenos_SeIgnoranYSigueViendoSoloLosSuyos() {
        TicketFiltroDto f = filtro();
        f.setAdministradorId(adminX.getId()); // en sus tickets solo t1 tiene a X
        f.setSoloMios(true);

        assertEquals(folios(t3, t2, t1), folios(listar(clienteA, f)));
        assertEquals(folios(t6, t5, t4), folios(listar(clienteB, f)));
    }

    @Test
    void cliente_FiltroPorEstado_SeAplicaSoloSobreSusTickets() {
        TicketFiltroDto f = filtro();
        f.setEstadoId(abierto.getId());

        assertEquals(folios(t3, t1), folios(listar(clienteA, f)));
    }

    @Test
    void cliente_SinAsignar_SoloSusTicketsSinAdministrador() {
        TicketFiltroDto f = filtro();
        f.setSinAsignar(true);

        assertEquals(folios(t2), folios(listar(clienteA, f)));
    }

    // ---------- ADMIN ----------

    @Test
    void admin_SinFiltros_VeTodosDelMasRecienteAlMasAntiguo() {
        PaginaDto<TicketDto> pagina = listar(adminX, filtro());

        assertEquals(folios(t6, t5, t4, t3, t2, t1), folios(pagina));
        assertEquals(6, pagina.getTotalElementos());
    }

    @Test
    void admin_FiltroPorEstado() {
        TicketFiltroDto f = filtro();
        f.setEstadoId(enProceso.getId());

        assertEquals(folios(t4, t2), folios(listar(adminX, f)));
    }

    @Test
    void admin_FiltroPorPrioridad() {
        TicketFiltroDto f = filtro();
        f.setPrioridadId(alta.getId());

        assertEquals(folios(t5, t4, t1), folios(listar(adminX, f)));
    }

    @Test
    void admin_FiltroPorCategoria() {
        TicketFiltroDto f = filtro();
        f.setCategoriaId(dns.getId());

        assertEquals(folios(t5, t3, t1), folios(listar(adminX, f)));
    }

    @Test
    void admin_FiltroPorAdministrador() {
        TicketFiltroDto f = filtro();
        f.setAdministradorId(adminY.getId());

        assertEquals(folios(t6, t3), folios(listar(adminX, f)));
    }

    @Test
    void admin_SinAsignar() {
        TicketFiltroDto f = filtro();
        f.setSinAsignar(true);

        PaginaDto<TicketDto> pagina = listar(adminX, f);

        assertEquals(folios(t5, t2), folios(pagina));
        assertTrue(pagina.getContenido().stream().allMatch(t -> t.getAdministradorAsignado() == null));
    }

    @Test
    void admin_SoloMios_SoloLosAsignadosAEl() {
        TicketFiltroDto f = filtro();
        f.setSoloMios(true);

        assertEquals(folios(t4, t1), folios(listar(adminX, f)));
        assertEquals(folios(t6, t3), folios(listar(adminY, f)));
    }

    @Test
    void admin_SoloMiosFalse_NoFiltra() {
        TicketFiltroDto f = filtro();
        f.setSoloMios(false);

        assertEquals(6, listar(adminX, f).getTotalElementos());
    }

    @Test
    void admin_FiltrosCombinados() {
        TicketFiltroDto f = filtro();
        f.setEstadoId(abierto.getId());
        f.setPrioridadId(alta.getId());
        f.setCategoriaId(dns.getId());
        assertEquals(folios(t5, t1), folios(listar(adminX, f)));

        f.setFechaDesde(LocalDate.of(2026, 11, 2));
        assertEquals(folios(t5), folios(listar(adminX, f)));

        f.setFechaDesde(null);
        f.setSoloMios(true);
        assertEquals(folios(t1), folios(listar(adminX, f)));
    }

    @Test
    void admin_SoloMiosConAdministradorIdDeOtro_DevuelveVacio() {
        TicketFiltroDto f = filtro();
        f.setSoloMios(true);
        f.setAdministradorId(adminY.getId());

        assertTrue(listar(adminX, f).getContenido().isEmpty());
    }

    @Test
    void admin_IdDeFiltroInexistente_DevuelveListaVacia() {
        TicketFiltroDto f = filtro();
        f.setEstadoId(9999);

        PaginaDto<TicketDto> pagina = listar(adminX, f);

        assertTrue(pagina.getContenido().isEmpty());
        assertEquals(0, pagina.getTotalElementos());
    }

    // ---------- fechas ----------

    @Test
    void rangoDeFechas_AmbosExtremosInclusivos() {
        TicketFiltroDto f = filtro();
        f.setFechaDesde(LocalDate.of(2026, 11, 5));  // t2: 05 a las 23:59:59
        f.setFechaHasta(LocalDate.of(2026, 11, 10)); // t3: 10 a las 00:00

        assertEquals(folios(t3, t2), folios(listar(adminX, f)));
    }

    @Test
    void soloFechaDesde_Y_SoloFechaHasta() {
        TicketFiltroDto desde = filtro();
        desde.setFechaDesde(LocalDate.of(2026, 11, 20));
        assertEquals(folios(t6, t5), folios(listar(adminX, desde)));

        TicketFiltroDto hasta = filtro();
        hasta.setFechaHasta(LocalDate.of(2026, 11, 5));
        assertEquals(folios(t2, t1), folios(listar(adminX, hasta)));
    }

    @Test
    void mismoDiaEnAmbasFechas_DevuelveLosDeEseDia() {
        TicketFiltroDto f = filtro();
        f.setFechaDesde(LocalDate.of(2026, 11, 15));
        f.setFechaHasta(LocalDate.of(2026, 11, 15));

        assertEquals(folios(t4), folios(listar(adminX, f)));
    }

    @Test
    void fechaDesdePosteriorAFechaHasta_LanzaSolicitudInvalida() {
        TicketFiltroDto f = filtro();
        f.setFechaDesde(LocalDate.of(2026, 11, 20));
        f.setFechaHasta(LocalDate.of(2026, 11, 1));

        SolicitudInvalidaException ex =
                assertThrows(SolicitudInvalidaException.class, () -> listar(adminX, f));
        assertEquals("La fechaDesde (2026-11-20) no puede ser posterior a la fechaHasta (2026-11-01)", ex.getMessage());
    }

    // ---------- paginación ----------

    @Test
    void paginacion_DevuelvePaginaYTotales() {
        PaginaDto<TicketDto> pagina = businessLayer.obtenerTodos(filtro(), 1, 2, sesion(adminX));

        assertEquals(folios(t4, t3), folios(pagina));
        assertEquals(1, pagina.getPagina());
        assertEquals(2, pagina.getTamanio());
        assertEquals(6, pagina.getTotalElementos());
        assertEquals(3, pagina.getTotalPaginas());
    }

    @Test
    void paginacion_SizeMayorA50_SeLimitaA50() {
        PaginaDto<TicketDto> pagina = businessLayer.obtenerTodos(filtro(), 0, 500, sesion(adminX));

        assertEquals(50, pagina.getTamanio());
        assertEquals(6, pagina.getContenido().size());
    }

    @Test
    void paginacion_PaginaFueraDeRango_DevuelveContenidoVacio() {
        PaginaDto<TicketDto> pagina = businessLayer.obtenerTodos(filtro(), 5, 10, sesion(adminX));

        assertTrue(pagina.getContenido().isEmpty());
        assertEquals(6, pagina.getTotalElementos());
    }

    @Test
    void paginacion_ValoresInvalidos_LanzanSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class,
                () -> businessLayer.obtenerTodos(filtro(), -1, 10, sesion(adminX)));
        assertThrows(SolicitudInvalidaException.class,
                () -> businessLayer.obtenerTodos(filtro(), 0, 0, sesion(adminX)));
    }

    // ---------- N+1 ----------

    @Test
    void listar_CargaRelacionesEnUnaSolaConsulta() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        PaginaDto<TicketDto> pagina = listar(adminX, filtro());

        // 6 tickets con 4 usuarios, 2 estados, 2 prioridades y 2 categorías distintos:
        // sin el EntityGraph serían varias consultas extra. La página cabe completa,
        // así que Spring Data no necesita la consulta de conteo.
        assertEquals(6, pagina.getContenido().size());
        assertEquals("Ana Cliente", pagina.getContenido().get(5).getNombreCliente());
        assertEquals("Xavier Admin", pagina.getContenido().get(5).getAdministradorAsignado());
        assertEquals(1, stats.getPrepareStatementCount(), "Debe ejecutarse una sola consulta SQL");
    }

    @Test
    void listarPaginado_UsaSoloConsultaDeDatosYDeConteo() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        businessLayer.obtenerTodos(filtro(), 0, 2, sesion(adminX));

        assertEquals(2, stats.getPrepareStatementCount(), "Una consulta de datos y una de conteo");
    }

    // ---------- detalle ----------

    @Test
    void detalle_ClienteDueno_DevuelveElTicketCompleto() {
        TicketDto dto = businessLayer.obtenerPorId(t1.getId(), sesion(clienteA));

        assertEquals(t1.getFolio(), dto.getFolio());
        assertEquals("Ana Cliente", dto.getNombreCliente());
        assertEquals("Xavier Admin", dto.getAdministradorAsignado());
        assertEquals("Abierto", dto.getEstado());
        assertEquals("Alta", dto.getPrioridad());
        assertEquals("DNS", dto.getCategoria());
    }

    @Test
    void detalle_TicketDeOtroCliente_LanzaAccesoDenegado() {
        assertThrows(AccesoDenegadoException.class,
                () -> businessLayer.obtenerPorId(t4.getId(), sesion(clienteA)));
    }

    @Test
    void detalle_Admin_PuedeVerCualquiera() {
        assertEquals(t4.getFolio(), businessLayer.obtenerPorId(t4.getId(), sesion(adminY)).getFolio());
        assertEquals(t2.getFolio(), businessLayer.obtenerPorId(t2.getId(), sesion(adminX)).getFolio());
    }

    @Test
    void detalle_Inexistente_LanzaRecursoNoEncontrado() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> businessLayer.obtenerPorId(999999, sesion(adminX)));
        assertEquals("Ticket no encontrado con id 999999", ex.getMessage());
    }
}
