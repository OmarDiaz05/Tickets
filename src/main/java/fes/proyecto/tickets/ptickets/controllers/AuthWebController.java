package fes.proyecto.tickets.ptickets.controllers;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.ReglaNegocioException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;
import jakarta.validation.Valid;

@Controller
public class AuthWebController {

    private final UsuarioBusinessLayer usuarioBusinessLayer;

    public AuthWebController(UsuarioBusinessLayer usuarioBusinessLayer) {
        this.usuarioBusinessLayer = usuarioBusinessLayer;
    }

    @GetMapping("/")
    public String inicio(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UsuarioAutenticado) {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            return esAdmin ? "redirect:/admin/panel" : "redirect:/cliente/tickets";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registrado", required = false) String registrado,
            Model model) {
        if (error != null) {
            model.addAttribute("mensajeError", "Correo o contraseña incorrectos, o cuenta deshabilitada.");
        }
        if (logout != null) {
            model.addAttribute("mensajeInfo", "Has cerrado sesión correctamente.");
        }
        if (registrado != null) {
            model.addAttribute("mensajeExito", "Cuenta creada correctamente. Ya puedes iniciar sesión.");
        }
        return "login";
    }

    //registro de Cliente (Puntos 4.4 y 4.9)

    @GetMapping("/registro/cliente")
    public String mostrarRegistroCliente(Model model) {
        UsuarioDtoIn dto = new UsuarioDtoIn();
        dto.setRol("CLIENTE");
        dto.setGuid(UUID.randomUUID().toString());
        model.addAttribute("usuario", dto);
        return "registro-cliente";
    }

    @PostMapping("/registro/cliente")
    public String procesarRegistroCliente(
            @Valid @ModelAttribute("usuario") UsuarioDtoIn dtoIn,
            BindingResult result,
            Model model) {
        dtoIn.setRol("CLIENTE");
        if (dtoIn.getGuid() == null || dtoIn.getGuid().isBlank()) {
            dtoIn.setGuid(UUID.randomUUID().toString());
        }

        if (result.hasErrors()) {
            return "registro-cliente";
        }

        try {
            usuarioBusinessLayer.agregar(dtoIn);
            return "redirect:/login?registrado=cliente";
        } catch (ReglaNegocioException | SolicitudInvalidaException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
            return "registro-cliente";
        }
    }

    // registro de Administrador (Puntos 4.5 y 4.9)

    @GetMapping("/registro/admin")
    public String mostrarRegistroAdmin(Model model) {
        UsuarioDtoIn dto = new UsuarioDtoIn();
        dto.setRol("ADMIN");
        dto.setGuid(UUID.randomUUID().toString());
        model.addAttribute("usuario", dto);
        return "registro-admin";
    }

    @PostMapping("/registro/admin")
    public String procesarRegistroAdmin(
            @Valid @ModelAttribute("usuario") UsuarioDtoIn dtoIn,
            BindingResult result,
            Model model) {
        dtoIn.setRol("ADMIN");
        if (dtoIn.getGuid() == null || dtoIn.getGuid().isBlank()) {
            dtoIn.setGuid(UUID.randomUUID().toString());
        }

        if (result.hasErrors()) {
            return "registro-admin";
        }

        try {
            usuarioBusinessLayer.agregar(dtoIn);
            return "redirect:/login?registrado=admin";
        } catch (ReglaNegocioException | AccesoDenegadoException | SolicitudInvalidaException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
            return "registro-admin";
        }
    }

    //vistas destino tras iniciar sesión según rol (Punto 4.10)

    @GetMapping("/cliente/tickets")
    public String vistaTicketsCliente(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        UsuarioDto dto = usuarioBusinessLayer.obtenerPorId(usuario.getId());
        model.addAttribute("nombreUsuario", dto.getNombre());
        model.addAttribute("rolUsuario", "Cliente");
        return "cliente/tickets";
    }

    @GetMapping("/admin/panel")
    public String vistaPanelAdmin(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        UsuarioDto dto = usuarioBusinessLayer.obtenerPorId(usuario.getId());
        model.addAttribute("nombreUsuario", dto.getNombre());
        model.addAttribute("rolUsuario", "Administrador");
        return "admin/panel";
    }
}