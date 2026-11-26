package fes.proyecto.tickets.ptickets.helpers;

import fes.proyecto.tickets.ptickets.dtos.TicketDto;
import fes.proyecto.tickets.ptickets.entities.CategoriaFalla;
import fes.proyecto.tickets.ptickets.entities.EstadoTicket;
import fes.proyecto.tickets.ptickets.entities.Prioridad;
import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MappersTicketTest {

    private static Usuario usuario(int id, String nombre, String email, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        u.setPassword("$2a$10$hashQueNoDebeSalir");
        return u;
    }

    private static Ticket ticket() {
        Ticket t = new Ticket();
        t.setId(5);
        t.setFolio("FESA-2026-0B1");
        t.setGuid("guid-ticket-001");
        t.setUsuario(usuario(1, "Ana Lopez", "ana@example.com", "CLIENTE"));
        t.setCategoria(new CategoriaFalla(2, "DNS"));
        t.setPrioridad(new Prioridad(3, "Alta", 3));
        t.setEstado(new EstadoTicket(1, "Abierto", 1));
        t.setDescripcion("No resuelven los nombres de dominio internos");
        t.setDesdeCuando(LocalDateTime.of(2026, 11, 20, 9, 30));
        t.setAreaAfectada("Laboratorio de cómputo");
        t.setServicioImpactado("Correo institucional");
        t.setMedioTransmision("Fibra óptica");
        t.setFechaRegistro(LocalDateTime.of(2026, 11, 26, 10, 0));
        return t;
    }

    @Test
    void toDto_TicketSinAdministrador_MapeaCamposYDejaNulosLosOpcionales() {
        TicketDto dto = Mappers.toDto(ticket());

        assertEquals(5, dto.getId());
        assertEquals("FESA-2026-0B1", dto.getFolio());
        assertEquals("Ana Lopez", dto.getNombreCliente());
        assertEquals("ana@example.com", dto.getCorreoCliente());
        assertNull(dto.getAdministradorAsignado());
        assertEquals("DNS", dto.getCategoria());
        assertEquals("Alta", dto.getPrioridad());
        assertEquals("Abierto", dto.getEstado());
        assertEquals("No resuelven los nombres de dominio internos", dto.getDescripcion());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 30), dto.getDesdeCuando());
        assertEquals("Laboratorio de cómputo", dto.getAreaAfectada());
        assertEquals("Correo institucional", dto.getServicioImpactado());
        assertEquals("Fibra óptica", dto.getMedioTransmision());
        assertEquals(LocalDateTime.of(2026, 11, 26, 10, 0), dto.getFechaRegistro());
        assertNull(dto.getFechaCierre());
        assertNull(dto.getCausaRaiz());
        assertNull(dto.getAccionesCorrectivas());
        assertNull(dto.getPruebasPosteriores());
        assertNull(dto.getLeccionesAprendidas());
    }

    @Test
    void toDto_TicketConAdministradorYCierre_MapeaNombreDelAdminYCamposDeCierre() {
        Ticket t = ticket();
        t.setAdministradorAsignado(usuario(9, "Ing. Carlos Ruiz", "carlos@example.com", "ADMIN"));
        t.setFechaCierre(LocalDateTime.of(2026, 11, 27, 18, 0));
        t.setCausaRaiz("Zona DNS mal configurada");
        t.setAccionesCorrectivas("Se corrigió el registro A");
        t.setPruebasPosteriores("nslookup exitoso");
        t.setLeccionesAprendidas("Validar cambios de DNS en staging");

        TicketDto dto = Mappers.toDto(t);

        assertEquals("Ing. Carlos Ruiz", dto.getAdministradorAsignado());
        assertEquals(LocalDateTime.of(2026, 11, 27, 18, 0), dto.getFechaCierre());
        assertEquals("Zona DNS mal configurada", dto.getCausaRaiz());
        assertEquals("Se corrigió el registro A", dto.getAccionesCorrectivas());
        assertEquals("nslookup exitoso", dto.getPruebasPosteriores());
        assertEquals("Validar cambios de DNS en staging", dto.getLeccionesAprendidas());
    }

    @Test
    void toDto_TicketNulo_RetornaNulo() {
        assertNull(Mappers.toDto((Ticket) null));
    }
}
