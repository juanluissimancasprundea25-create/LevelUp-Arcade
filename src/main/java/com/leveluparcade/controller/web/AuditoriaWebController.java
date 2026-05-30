package com.leveluparcade.controller.web;

import com.leveluparcade.entity.AuditoriaLog;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller web para consultar el log de auditoria desde el panel admin.
 *
 * <p>Vista read-only paginada con filtros opcionales por usuario, accion
 * o entidad. La escritura del log la hace {@code AuditoriaListener}
 * automaticamente; aqui solo se consulta.
 *
 * <p>Llama directamente al AuditoriaService (Opcion A).
 *
 * <p>Tras la mejora, ademas del log enriquece la vista con un mapa
 * id -> Usuario de los usuarios referenciados en la pagina actual,
 * para mostrar nombre completo + email en lugar del id crudo.
 */
@Controller
@RequestMapping("/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaWebController {

    private static final String SECCION = "auditoria";

    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaWebController(AuditoriaService auditoriaService,
                                  UsuarioRepository usuarioRepository) {
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Numero minimo de caracteres del prefijo para que la busqueda por
     * nombre/email de usuario sea efectiva. Por debajo se ignora el
     * filtro (evita listar medio sistema con 1-2 letras).
     */
    private static final int MIN_PREFIJO_USUARIO = 3;

    /**
     * Listado paginado con filtros opcionales.
     * Solo se aplica un filtro a la vez (igual que el API): si hay
     * usuario se ignoran los demas, etc.
     */
    @GetMapping
    public String listar(
            @RequestParam(value = "usuario", required = false) String usuario,
            @RequestParam(value = "accion", required = false) String accion,
            @RequestParam(value = "entidad", required = false) String entidad,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            Model model) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "fecha"));

        // Aviso si el prefijo de usuario es demasiado corto: lo ignoramos
        // como filtro pero lo enseñamos en pantalla.
        String avisoUsuario = null;
        String usuarioNorm = (usuario != null) ? usuario.trim() : null;
        boolean filtraPorUsuario = usuarioNorm != null && !usuarioNorm.isBlank();

        Page<AuditoriaLog> pagina;
        if (filtraPorUsuario) {
            if (usuarioNorm.length() < MIN_PREFIJO_USUARIO) {
                avisoUsuario = "Escribe al menos " + MIN_PREFIJO_USUARIO
                        + " caracteres para buscar por usuario.";
                pagina = auditoriaService.listar(pageable);
            } else {
                List<Usuario> coincidencias =
                        usuarioRepository.buscarPorPrefijoNombreOEmail(usuarioNorm);
                if (coincidencias.isEmpty()) {
                    avisoUsuario = "Ningun usuario coincide con \""
                            + usuarioNorm + "\".";
                    pagina = Page.empty(pageable);
                } else {
                    Set<Long> ids = coincidencias.stream()
                            .map(Usuario::getId)
                            .collect(Collectors.toSet());
                    pagina = auditoriaService.filtrarPorUsuarios(ids, pageable);
                }
            }
        } else if (accion != null && !accion.isBlank()) {
            pagina = auditoriaService.filtrarPorAccion(accion.trim(), pageable);
        } else if (entidad != null && !entidad.isBlank()) {
            pagina = auditoriaService.filtrarPorEntidad(entidad.trim(), pageable);
        } else {
            pagina = auditoriaService.listar(pageable);
        }

        // Carga en una sola consulta los usuarios referenciados en la
        // pagina actual. Asi la vista puede mostrar nombre + email en
        // lugar del id crudo, sin caer en N+1 (un select por fila).
        Set<Long> userIds = pagina.getContent().stream()
                .map(AuditoriaLog::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Usuario> usuariosMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Usuario> usuarios = usuarioRepository.findAllById(userIds);
            for (Usuario u : usuarios) {
                usuariosMap.put(u.getId(), u);
            }
        }

        model.addAttribute("pagina", pagina);
        model.addAttribute("logs", pagina.getContent());
        model.addAttribute("usuariosMap", usuariosMap);
        model.addAttribute("usuarioFiltro", usuarioNorm);
        model.addAttribute("avisoUsuario", avisoUsuario);
        model.addAttribute("accionFiltro", accion);
        model.addAttribute("entidadFiltro", entidad);
        model.addAttribute("seccionActiva", SECCION);
        return "auditoria/list";
    }
}