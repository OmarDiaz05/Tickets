package fes.proyecto.tickets.ptickets.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDtoIn {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Email(message = "El formato de correo no es válido")
    @NotBlank(message = "El correo es obligatorio")
    private String email;

    @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El rol es obligatorio (CLIENTE o ADMIN)")
    private String rol;

    // Requerido si rol = ADMIN; null permitido para clientes
    @Pattern(regexp = "\\d{9}", message = "El número de empleado debe tener 9 dígitos")
    private String numeroEmpleado;

    @NotBlank(message = "El guid es obligatorio")
    private String guid;
}