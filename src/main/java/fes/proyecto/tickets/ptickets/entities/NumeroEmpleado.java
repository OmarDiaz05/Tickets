package fes.proyecto.tickets.ptickets.entities;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "numeros_empleado")
public class NumeroEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "numero_empleado", nullable = false, unique = true, length = 9)
    private String numeroEmpleado;

    @Column(name = "nombre_referencia", nullable = true, length = 100)
    private String nombreReferencia;

    @Column(nullable = false)
    private boolean usado;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}