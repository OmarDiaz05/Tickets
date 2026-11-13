package fes.proyecto.tickets.ptickets.entities;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean activo = true;

    public UsuarioDto toDto() {
        return new UsuarioDto(this.id, this.nombre, this.email, this.fechaRegistro, this.activo);
    }
}