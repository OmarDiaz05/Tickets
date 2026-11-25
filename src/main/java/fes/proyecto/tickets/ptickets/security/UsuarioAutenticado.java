package fes.proyecto.tickets.ptickets.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Usuario guardado en la sesión del servidor tras el login. Extiende el User de
 * Spring Security para conservar también el id del usuario.
 * El hash de la contraseña se borra de la sesión al terminar la autenticación.
 */
public class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;

    private final int id;

    public UsuarioAutenticado(int id, String email, String passwordHash, String rol, boolean activo) {
        super(email, passwordHash, activo, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
