package fes.proyecto.tickets.ptickets.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "estados_ticket")
public class Estadoticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre; // Abierto, Registrado, En proceso, En pausa, Resuelto, Cerrado

    @Column(nullable = false)
    private int orden; // 1 a 6

}