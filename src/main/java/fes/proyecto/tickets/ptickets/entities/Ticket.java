package fes.proyecto.tickets.ptickets.entities;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 20)
    private String folio; // FESA-<año>-<consecutivo base 36>, ver FolioGenerator

    @Column(nullable = false, unique = true, length = 64)
    private String guid; // identificador de idempotencia

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // cliente que reporta

    @ManyToOne
    @JoinColumn(name = "administrador_asignado_id")
    private Usuario administradorAsignado; // null hasta que se asigne

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaFalla categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prioridad_id", nullable = false)
    private Prioridad prioridad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoTicket estado;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "desde_cuando", nullable = false)
    private LocalDateTime desdeCuando;

    @Column(name = "area_afectada", nullable = false, length = 100)
    private String areaAfectada;

    @Column(name = "servicio_impactado", nullable = false, length = 100)
    private String servicioImpactado;

    @Column(name = "medio_transmision", length = 100)
    private String medioTransmision; // opcional

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // ----- Campos opcionales para fases posteriores (cierre y análisis) -----

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "causa_raiz", columnDefinition = "TEXT")
    private String causaRaiz;

    @Column(name = "acciones_correctivas", columnDefinition = "TEXT")
    private String accionesCorrectivas;

    @Column(name = "pruebas_posteriores", columnDefinition = "TEXT")
    private String pruebasPosteriores;

    @Column(name = "lecciones_aprendidas", columnDefinition = "TEXT")
    private String leccionesAprendidas;
}
