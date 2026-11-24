package fes.proyecto.tickets.ptickets.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "prioridades")
public class Prioridad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // Baja, Media, Alta, Crítica

    @Column(nullable = false)
    private int nivel; // 1 a 4
}
