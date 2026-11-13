package fes.proyecto.tickets.ptickets.dtos;

import fes.proyecto.tickets.ptickets.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuariooDtoIn {
    @NotBlank
    private String nombre;
    
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
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