package fes.proyecto.tickets.ptickets.dtos;

import fes.proyecto.tickets.ptickets.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuarioDtoIn {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Email(message = "El correo debe tener un formato válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Conversión inversa: de DTO a Entidad
    public Usuario toEntity() {
        Usuario entity = new Usuario();
        entity.setNombre(this.nombre);
        entity.setEmail(this.email);
        entity.setPassword(this.password);
        entity.setActivo(true);
        return entity;
    }
}
