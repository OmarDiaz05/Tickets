package fes.proyecto.tickets.ptickets.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "roles")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // "Cliente", "Administrador"
}