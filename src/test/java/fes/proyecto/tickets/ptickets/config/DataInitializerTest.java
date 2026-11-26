package fes.proyecto.tickets.ptickets.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.repositories.ICategoriaFallaRepository;
import fes.proyecto.tickets.ptickets.repositories.IEstadoTicketRepository;
import fes.proyecto.tickets.ptickets.repositories.INumeroEmpleadoRepository;
import fes.proyecto.tickets.ptickets.repositories.IPrioridadRepository;
import fes.proyecto.tickets.ptickets.repositories.IRolRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock
    private IRolRepository rolRepository;

    @Mock
    private IEstadoTicketRepository estadoTicketRepository;

    @Mock
    private IPrioridadRepository prioridadRepository;

    @Mock
    private ICategoriaFallaRepository categoriaFallaRepository;
    @Mock
    private INumeroEmpleadoRepository numeroEmpleadoRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(
            rolRepository, 
            estadoTicketRepository, 
            prioridadRepository, 
            categoriaFallaRepository, 
            numeroEmpleadoRepository, 
            usuarioRepository,
            passwordEncoder);
    }

    @Test
    void run_TablaVacia_CargaNumerosYUsuariosDePrueba() {
        when(rolRepository.count()).thenReturn(0L);
        when(estadoTicketRepository.count()).thenReturn(0L);
        when(prioridadRepository.count()).thenReturn(0L);
        when(categoriaFallaRepository.count()).thenReturn(0L);
        when(numeroEmpleadoRepository.count()).thenReturn(0L);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        dataInitializer.run();

        verify(rolRepository, times(1)).saveAll(anyList());
        verify(estadoTicketRepository, times(1)).saveAll(anyList());
        verify(prioridadRepository, times(1)).saveAll(anyList());
        verify(categoriaFallaRepository, times(1)).saveAll(anyList());
        verify(numeroEmpleadoRepository, times(1)).saveAll(anyList());
        verify(usuarioRepository, times(2)).save(any(Usuario.class));
    }

    @Test
    void run_DatosExistentes_NoDuplicaRegistros() {
        when(rolRepository.count()).thenReturn(2L);
        when(estadoTicketRepository.count()).thenReturn(6L);
        when(prioridadRepository.count()).thenReturn(4L);
        when(categoriaFallaRepository.count()).thenReturn(5L);
        when(numeroEmpleadoRepository.count()).thenReturn(5L);
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        dataInitializer.run();
        verify(rolRepository, never()).saveAll(anyList());
        verify(estadoTicketRepository, never()).saveAll(anyList());
        verify(prioridadRepository, never()).saveAll(anyList());
        verify(categoriaFallaRepository, never()).saveAll(anyList());
        verify(numeroEmpleadoRepository, never()).saveAll(anyList());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void run_ExcepcionBaseDeDatos_NoLanzaExcepcion() {
        when(rolRepository.count()).thenThrow(new RuntimeException("Error simulado de conexión a BD"));
        //when(numeroEmpleadoRepository.count()).thenThrow(new RuntimeException("Error simulado de conexión a BD"));

        // No debe propagar la excepción hacia el arranque de la app
        dataInitializer.run();
        verify(rolRepository, never()).saveAll(anyList());
        verify(numeroEmpleadoRepository, never()).saveAll(anyList());
    }
}
