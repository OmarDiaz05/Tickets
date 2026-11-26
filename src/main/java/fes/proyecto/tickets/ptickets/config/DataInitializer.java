package fes.proyecto.tickets.ptickets.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import fes.proyecto.tickets.ptickets.entities.NumeroEmpleado;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.repositories.INumeroEmpleadoRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

/**
 * Apartado 4.6 — Carga de datos de prueba al arrancar la aplicación:
 * - Lista de números de empleado autorizados para el registro de administradores (de un solo uso).
 * - Usuarios de prueba fijos (cliente y administrador) para reproducibilidad de reportes y pruebas.
 * - Restringido a entornos que no sean de producción (!prod).
 */
@Component
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final INumeroEmpleadoRepository numeroEmpleadoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            INumeroEmpleadoRepository numeroEmpleadoRepository,
            IUsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.numeroEmpleadoRepository = numeroEmpleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            cargarNumerosEmpleadoPrueba();
            cargarUsuariosPruebaFijos();
        } catch (Exception ex) {
            log.warn("No fue posible inicializar los datos de prueba en la base de datos: {}", ex.getMessage());
        }
    }

    private void cargarNumerosEmpleadoPrueba() {
        if (numeroEmpleadoRepository.count() == 0) {
            log.info("Inicializando números de empleado autorizados para pruebas (Apartado 4.6)...");

            List<NumeroEmpleado> lista = List.of(
                crearNumero("EMP-2026-01", "Ing. Carlos Ruiz", false, true),
                crearNumero("EMP-2026-02", "Ing. Laura Mendoza", false, true),
                crearNumero("EMP-2026-03", "Ing. Fernando Álvarez", false, true),
                crearNumero("EMP-2026-04", "Técnico Soporte Redes", false, true),
                crearNumero("EMP-2026-05", "Coordinador Infraestructura", false, true),
                // Registro para validar rechazo por número ya utilizado (409 Conflict)
                crearNumero("EMP-USADO-01", "Empleado Ya Registrado Previamente", true, true),
                // Registro para validar rechazo por número no autorizado o inactivo (403 Forbidden)
                crearNumero("EMP-INACTIVO-01", "Empleado Inactivo o Dado de Baja", false, false)
            );

            numeroEmpleadoRepository.saveAll(lista);
            log.info("Se cargaron {} números de empleado de prueba con éxito.", lista.size());
        }
    }

    private NumeroEmpleado crearNumero(String numero, String nombreRef, boolean usado, boolean activo) {
        NumeroEmpleado n = new NumeroEmpleado();
        n.setNumeroEmpleado(numero);
        n.setNombreReferencia(nombreRef);
        n.setUsado(usado);
        n.setActivo(activo);
        n.setFechaRegistro(LocalDateTime.now());
        return n;
    }

    private void cargarUsuariosPruebaFijos() {
        // Usuario CLIENTE fijo de prueba (Fase 4, página 5 del plan de trabajo)
        String emailCliente = "cliente.fijo@aragon.unam.mx";
        if (!usuarioRepository.existsByEmail(emailCliente)) {
            Usuario cliente = new Usuario();
            cliente.setNombre("Cliente Fijo de Prueba");
            cliente.setEmail(emailCliente);
            cliente.setPassword(passwordEncoder.encode("Password123"));
            cliente.setRol("CLIENTE");
            cliente.setNumeroEmpleado(null);
            cliente.setVerificado(true);
            cliente.setActivo(true);
            cliente.setFechaRegistro(LocalDateTime.now());
            cliente.setGuid("guid-cliente-fijo-001");
            usuarioRepository.save(cliente);
            log.info("Usuario cliente fijo de prueba creado: {}", emailCliente);
        }

        // Usuario ADMIN fijo de prueba (Fase 4, página 5 del plan de trabajo)
        String emailAdmin = "admin.fijo@aragon.unam.mx";
        if (!usuarioRepository.existsByEmail(emailAdmin)) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador Fijo de Prueba");
            admin.setEmail(emailAdmin);
            admin.setPassword(passwordEncoder.encode("AdminPass123"));
            admin.setRol("ADMIN");
            admin.setNumeroEmpleado("EMP-ADMIN-FIJO");
            admin.setVerificado(true);
            admin.setActivo(true);
            admin.setFechaRegistro(LocalDateTime.now());
            admin.setGuid("guid-admin-fijo-001");
            usuarioRepository.save(admin);
            log.info("Usuario administrador fijo de prueba creado: {}", emailAdmin);
        }
    }
}
