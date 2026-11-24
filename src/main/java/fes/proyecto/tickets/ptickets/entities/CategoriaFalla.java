package fes.proyecto.tickets.ptickets.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
@Entity
@Table(name = "categorias_falla")
public class CategoriaFalla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre; // Conectividad, Enrutamiento, Servicio web, DNS, Red inalámbrica
}