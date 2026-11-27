package fes.proyecto.tickets.ptickets.helpers;

import java.time.LocalDateTime;
import fes.proyecto.tickets.ptickets.dtos.TicketDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.entities.Usuario;

public class Mappers {

    public static UsuarioDto toDto(Usuario entity) {
        if (entity == null) {
            return null;
        }
        return new UsuarioDto(
                entity.getId(),
                entity.getNombre(),
                entity.getEmail(),
                entity.getTelefono(),
                entity.getRol(),
                entity.getNumeroEmpleado(),
                entity.isVerificado(),
                entity.getFechaRegistro(),
                entity.isActivo()
        );
    }

    public static TicketDto toDto(Ticket entity) {
        if (entity == null) {
            return null;
        }
        TicketDto dto = new TicketDto();
        dto.setId(entity.getId());
        dto.setFolio(entity.getFolio());

        Usuario cliente = entity.getUsuario();
        if (cliente != null) {
            dto.setNombreCliente(cliente.getNombre());
            dto.setCorreoCliente(cliente.getEmail());
        }
        Usuario admin = entity.getAdministradorAsignado();
        dto.setAdministradorAsignado(admin != null ? admin.getNombre() : null);

        dto.setCategoria(entity.getCategoria() != null ? entity.getCategoria().getNombre() : null);
        dto.setPrioridad(entity.getPrioridad() != null ? entity.getPrioridad().getNombre() : null);
        dto.setEstado(entity.getEstado() != null ? entity.getEstado().getNombre() : null);

        dto.setDescripcion(entity.getDescripcion());
        dto.setDesdeCuando(entity.getDesdeCuando());
        dto.setAreaAfectada(entity.getAreaAfectada());
        dto.setServicioImpactado(entity.getServicioImpactado());
        dto.setMedioTransmision(entity.getMedioTransmision());
        dto.setFechaRegistro(entity.getFechaRegistro());

        dto.setFechaCierre(entity.getFechaCierre());
        dto.setCausaRaiz(entity.getCausaRaiz());
        dto.setAccionesCorrectivas(entity.getAccionesCorrectivas());
        dto.setPruebasPosteriores(entity.getPruebasPosteriores());
        dto.setLeccionesAprendidas(entity.getLeccionesAprendidas());
        return dto;
    }

    /**
     * Copia solo los datos que captura el cliente. Folio, estado, usuario,
     * administrador, catálogos y fechaRegistro los asigna TicketBusinessLayer.
     */
    public static Ticket toEntity(TicketDtoIn dtoIn) {
        if (dtoIn == null) {
            return null;
        }
        String medio = (dtoIn.getMedioTransmision() != null && !dtoIn.getMedioTransmision().isBlank())
                ? dtoIn.getMedioTransmision().trim()
                : null;

        Ticket entity = new Ticket();
        entity.setDescripcion(dtoIn.getDescripcion().trim());
        entity.setDesdeCuando(dtoIn.getDesdeCuando());
        entity.setAreaAfectada(dtoIn.getAreaAfectada().trim());
        entity.setServicioImpactado(dtoIn.getServicioImpactado().trim());
        entity.setMedioTransmision(medio);
        entity.setGuid(dtoIn.getGuid().trim());
        return entity;
    }

    /**
     * @param passwordHash contraseña ya codificada con el PasswordEncoder;
     *                     la contraseña en texto plano del dtoIn no se copia.
     */
    public static Usuario toEntity(UsuarioDtoIn dtoIn, String passwordHash) {
        if (dtoIn == null) {
            return null;
        }
        String rolNormalizado = dtoIn.getRol().trim().toUpperCase();
        String numEmpleado = "ADMIN".equals(rolNormalizado) ? dtoIn.getNumeroEmpleado() : null;
        String tel = (dtoIn.getTelefono() != null && !dtoIn.getTelefono().isBlank())
                ? dtoIn.getTelefono().trim()
                : null;

        Usuario entity = new Usuario();
        entity.setNombre(dtoIn.getNombre().trim());
        entity.setEmail(dtoIn.getEmail().trim().toLowerCase());
        entity.setTelefono(tel);
        entity.setPassword(passwordHash);
        entity.setRol(rolNormalizado);
        entity.setNumeroEmpleado(numEmpleado);
        entity.setVerificado(false);
        entity.setFechaRegistro(LocalDateTime.now());
        entity.setActivo(true);
        entity.setGuid(dtoIn.getGuid().trim());
        return entity;
    }
}