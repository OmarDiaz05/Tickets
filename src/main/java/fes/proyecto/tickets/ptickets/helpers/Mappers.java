package fes.proyecto.tickets.ptickets.helpers;

import java.time.LocalDateTime;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
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
                entity.getRol(),
                entity.getNumeroEmpleado(),
                entity.isVerificado(),
                entity.getFechaRegistro(),
                entity.isActivo()
        );
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

        Usuario entity = new Usuario();
        entity.setNombre(dtoIn.getNombre().trim());
        entity.setEmail(dtoIn.getEmail().trim().toLowerCase());
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