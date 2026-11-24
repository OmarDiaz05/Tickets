
package fes.proyecto.tickets.ptickets.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "empleados_autorizados")
public class EmpleadoAutorizado {

    @Id
    @Column(name = "numero_empleado", length = 9)
    private String numeroEmpleado;

    @Column(name = "nombre_completo", nullable = false, length = 50)
    private String nombreCompleto;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private boolean registrado;
}