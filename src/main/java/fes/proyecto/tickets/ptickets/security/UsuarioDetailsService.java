package fes.proyecto.tickets.ptickets.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

/**
 * Carga el usuario por email para el login. La autoridad es "ROLE_" + rol
 * (ROLE_CLIENTE / ROLE_ADMIN). Un usuario desactivado (activo = false) se marca
 * como deshabilitado y Spring Security rechaza su autenticación.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final IUsuarioRepository repository;

    public UsuarioDetailsService(IUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // El registro guarda los correos en minúsculas y sin espacios
        String email = username == null ? "" : username.trim().toLowerCase();

        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getRol(),
                usuario.isActivo());
    }
}
