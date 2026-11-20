package fes.proyecto.tickets.ptickets.businessLayer;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioBusinessLayerTest {

    @Mock
    private IUsuarioRepository repository;

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

        usuarioExistente = new Usuario();
        usuarioExistente.setId(1L);
        usuarioExistente.setNombre("Juan Perez");
        usuarioExistente.setEmail("juan@example.com");
        usuarioExistente.setPassword("123456");
        usuarioExistente.setActivo(true);
    }

    @Test
    void agregar_CorreoNuevo_GuardaUsuario() {
        when(repository.existsByEmail("juan@example.com")).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        IdDto respuesta = businessLayer.agregar(dtoIn);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.getId());
        verify(repository, times(1)).save(any(Usuario.class));
    }

    @Test
    void agregar_CorreoDuplicado_LanzaExcepcion() {
        when(repository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> businessLayer.agregar(dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizar_MismoCorreo_ActualizaExitosamente() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(repository.existsByEmailAndIdNot("juan@example.com", 1L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        IdDto respuesta = businessLayer.actualizar(1L, dtoIn);

        assertNotNull(respuesta);
        assertEquals(1L, respuesta.getId());
        verify(repository, times(1)).save(usuarioExistente);
    }

    @Test
    void actualizar_CorreoDeOtroUsuario_LanzaExcepcion() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(repository.existsByEmailAndIdNot("juan@example.com", 1L)).thenReturn(true);

        assertThrows(RecursoDuplicadoException.class, () -> businessLayer.actualizar(1L, dtoIn));
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizar_UsuarioNoEncontrado_LanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> businessLayer.actualizar(99L, dtoIn));
    }

    @Test
    void deshabilitar_UsuarioExistente_MarcaInactivo() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioExistente));
        when(repository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        IdDto respuesta = businessLayer.deshabilitar(1L);

        assertNotNull(respuesta);
        assertFalse(usuarioExistente.getActivo());
        verify(repository, times(1)).save(usuarioExistente);
    }
}
