package fes.proyecto.tickets.ptickets.businessLayer;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.NumeroEmpleado;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.exceptions.ReglaNegocioException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.repositories.INumeroEmpleadoRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioBusinessLayerTest {

    private static final String GUID = "a1b2c3d4-0000-0000-0000-000000000001";

    @Mock
    private IUsuarioRepository repository;

    @Mock
    private INumeroEmpleadoRepository numeroEmpleadoRepository;

    @InjectMocks
    private UsuarioBusinessLayer businessLayer;

    private UsuarioDtoIn dtoIn;
    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Juan Perez");
        dtoIn.setEmail("juan@example.com");
        dtoIn.setPassword("123456");
        dtoIn.setRol("CLIENTE");
        dtoIn.setGuid(GUID);

        usuarioExistente = new Usuario();
        usuarioExistente.setId(1);
        usuarioExistente.setNombre("Juan Perez");
        usuarioExistente.setEmail("juan@example.com");
        usuarioExistente.setPassword("123456");
        usuarioExistente.setRol("CLIENTE");
        usuarioExistente.setGuid(GUID);
        usuarioExistente.setFechaRegistro(LocalDateTime.now());
        usuarioExistente.setActivo(true);
    }

    private NumeroEmpleado numeroEmpleado(boolean usado) {
        NumeroEmpleado num = new NumeroEmpleado();
        num.setId(7);
        num.setNumeroEmpleado("EMP001");
        num.setNombreReferencia("Ana Admin");
        num.setActivo(true);
        num.setUsado(usado);
        num.setFechaRegistro(LocalDateTime.now());
        return num;
    }

    private void configurarAdmin() {
        dtoIn.setRol("ADMIN");
        dtoIn.setNumeroEmpleado("EMP001");
    }

    // ---------- agregar ----------

    @Test
    void agregar_CorreoNuevo_GuardaUsuario() {
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1);
            return u;
        });

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn);

        assertFalse(resultado.registroPrevio());
        assertEquals(1, resultado.idDto().getId());
        verify(repository, times(1)).save(any(Usuario.class));
    }

    @Test
    void agregar_GuidRepetido_DevuelveIdExistenteSinGuardar() {
        when(repository.findByGuid(GUID)).thenReturn(Optional.of(usuarioExistente));

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn);

        assertTrue(resultado.registroPrevio());
        assertEquals(1, resultado.idDto().getId());
        verify(repository, never()).existsByEmail(any());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void agregar_CorreoRepetidoConOtroGuid_LanzaRecursoDuplicado() {
        dtoIn.setGuid("otro-guid");
        dtoIn.setEmail("  Juan@Example.com ");
        when(repository.findByGuid("otro-guid")).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> businessLayer.agregar(dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void agregar_RolInvalido_LanzaSolicitudInvalida() {
        dtoIn.setRol("SUPERUSUARIO");
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);

        assertThrows(SolicitudInvalidaException.class, () -> businessLayer.agregar(dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void agregar_AdminSinNumeroEmpleado_LanzaSolicitudInvalida() {
        dtoIn.setRol("ADMIN");
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);

        assertThrows(SolicitudInvalidaException.class, () -> businessLayer.agregar(dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void agregar_AdminNumeroEmpleadoNoAutorizado_LanzaAccesoDenegado() {
        configurarAdmin();
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue("EMP001")).thenReturn(Optional.empty());

        assertThrows(AccesoDenegadoException.class, () -> businessLayer.agregar(dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void agregar_AdminNumeroEmpleadoYaUsado_LanzaReglaNegocio() {
        configurarAdmin();
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue("EMP001"))
                .thenReturn(Optional.of(numeroEmpleado(true)));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> businessLayer.agregar(dtoIn));
        assertFalse(ex instanceof RecursoDuplicadoException);
        verify(repository, never()).save(any(Usuario.class));
        verify(numeroEmpleadoRepository, never()).save(any(NumeroEmpleado.class));
    }

    @Test
    void agregar_AdminNumeroEmpleadoValido_GuardaYMarcaUsado() {
        configurarAdmin();
        NumeroEmpleado num = numeroEmpleado(false);
        when(repository.findByGuid(GUID)).thenReturn(Optional.empty());
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue("EMP001")).thenReturn(Optional.of(num));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2);
            return u;
        });

        ResultadoRegistro resultado = businessLayer.agregar(dtoIn);

        assertFalse(resultado.registroPrevio());
        assertEquals(2, resultado.idDto().getId());
        assertTrue(num.isUsado());
        verify(numeroEmpleadoRepository, times(1)).save(num);
    }

    // ---------- obtenerPorId ----------

    @Test
    void obtenerPorId_Existente_DevuelveDto() {
        when(repository.findById(1)).thenReturn(Optional.of(usuarioExistente));

        UsuarioDto dto = businessLayer.obtenerPorId(1);

        assertEquals(1, dto.getId());
        assertEquals("juan@example.com", dto.getEmail());
    }

    @Test
    void obtenerPorId_Inexistente_LanzaExcepcion() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> businessLayer.obtenerPorId(99));
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_MismoCorreo_ActualizaExitosamente() {
        when(repository.findById(1)).thenReturn(Optional.of(usuarioExistente));
        when(repository.existsByEmailAndIdNot("juan@example.com", 1)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        IdDto respuesta = businessLayer.actualizar(1, dtoIn);

        assertNotNull(respuesta);
        assertEquals(1, respuesta.getId());
        verify(repository, times(1)).save(usuarioExistente);
    }

    @Test
    void actualizar_CorreoDeOtroUsuario_LanzaExcepcion() {
        when(repository.findById(1)).thenReturn(Optional.of(usuarioExistente));
        when(repository.existsByEmailAndIdNot("juan@example.com", 1)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> businessLayer.actualizar(1, dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizar_UsuarioNoEncontrado_LanzaExcepcion() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> businessLayer.actualizar(99, dtoIn));
    }

    // ---------- borrar / activar ----------

    @Test
    void borrar_UsuarioExistente_MarcaInactivo() {
        when(repository.findById(1)).thenReturn(Optional.of(usuarioExistente));
        when(repository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        IdDto respuesta = businessLayer.borrar(1);

        assertNotNull(respuesta);
        assertFalse(usuarioExistente.isActivo());
        verify(repository, times(1)).save(usuarioExistente);
    }

    @Test
    void borrar_UsuarioNoEncontrado_LanzaExcepcion() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> businessLayer.borrar(99));
    }

    @Test
    void activar_UsuarioExistente_MarcaActivo() {
        usuarioExistente.setActivo(false);
        when(repository.findById(1)).thenReturn(Optional.of(usuarioExistente));
        when(repository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        IdDto respuesta = businessLayer.activar(1);

        assertEquals(1, respuesta.getId());
        assertTrue(usuarioExistente.isActivo());
    }

    @Test
    void activar_UsuarioNoEncontrado_LanzaExcepcion() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> businessLayer.activar(99));
    }
}
