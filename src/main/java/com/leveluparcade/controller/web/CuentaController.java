package com.leveluparcade.controller.web;

import com.leveluparcade.config.DevolucionesProperties;
import com.leveluparcade.dto.request.ActualizarPerfilRequest;
import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.LineaDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Devolucion;
import com.leveluparcade.entity.EstadoDevolucion;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.LineaPedido;
import com.leveluparcade.entity.Pedido;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.DevolucionRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.DevolucionService;
import com.leveluparcade.service.FacturaService;
import com.leveluparcade.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Area privada del cliente: inicio, mis pedidos, mis facturas,
 * mis devoluciones, mi perfil.
 *
 * <p>Todas las rutas estan protegidas para {@code ROLE_CLIENTE} por la
 * cadena 3 de SecurityConfig (ya definido en PR #23). Anonimo -> /login.
 * CSRF deshabilitado en esa cadena (los forms POST no llevan token).
 *
 * <p>Seguridad por endpoint:
 * <ul>
 *   <li>Listados ({@code /cuenta/pedidos}, {@code /cuenta/facturas},
 *       {@code /cuenta/devoluciones}): los services {@code listarMias}/
 *       {@code listarMisDevoluciones}/{@code listarDeCliente} ya filtran
 *       por el cliente logueado.</li>
 *   <li>Detalle de pedido: aqui SI validamos propiedad porque
 *       {@code PedidoService.obtenerPorId} no la valida (lo usa el admin).</li>
 *   <li>Descarga de factura PDF: {@code FacturaService.descargarPdf} ya
 *       valida propiedad internamente.</li>
 *   <li>Devoluciones del cliente: solicitar / detalle: validacion de
 *       propiedad explicita; el servicio tambien la valida internamente
 *       (defensa en profundidad).</li>
 * </ul>
 */
@Controller
@RequestMapping("/cuenta")
public class CuentaController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final DevolucionRepository devolucionRepository;
    private final PedidoService pedidoService;
    private final FacturaService facturaService;
    private final DevolucionService devolucionService;
    private final DevolucionesProperties devolucionesProperties;
    private final SecurityHelper securityHelper;

    public CuentaController(UsuarioRepository usuarioRepository,
                            ClienteRepository clienteRepository,
                            PedidoRepository pedidoRepository,
                            DevolucionRepository devolucionRepository,
                            PedidoService pedidoService,
                            FacturaService facturaService,
                            DevolucionService devolucionService,
                            DevolucionesProperties devolucionesProperties,
                            SecurityHelper securityHelper) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.devolucionRepository = devolucionRepository;
        this.pedidoService = pedidoService;
        this.facturaService = facturaService;
        this.devolucionService = devolucionService;
        this.devolucionesProperties = devolucionesProperties;
        this.securityHelper = securityHelper;
    }

    // ---------- INICIO ----------

    @GetMapping
    public String inicio(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Long clienteId = securityHelper.getClienteActualId();
        long numPedidos = (clienteId != null)
                ? pedidoService.listarDeCliente(clienteId).size()
                : 0L;
        long numFacturas = facturaService.listarMias(PageRequest.of(0, 1)).getTotalElements();
        long numDevoluciones = devolucionService.listarMisDevoluciones(PageRequest.of(0, 1))
                .getTotalElements();

        model.addAttribute("usuario", usuario);
        model.addAttribute("numPedidos", numPedidos);
        model.addAttribute("numFacturas", numFacturas);
        model.addAttribute("numDevoluciones", numDevoluciones);
        model.addAttribute("seccionCuenta", "inicio");
        return "tienda/cuenta/inicio";
    }

    // ---------- PEDIDOS ----------

    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        Long clienteId = clienteIdActual();
        List<PedidoResponse> pedidos = pedidoService.listarDeCliente(clienteId);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("seccionCuenta", "pedidos");
        return "tienda/cuenta/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    @Transactional(readOnly = true)
    public String pedidoDetalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            flash.addFlashAttribute("error", "No se ha encontrado el pedido.");
            return "redirect:/cuenta/pedidos";
        }
        Long clienteId = clienteIdActual();
        if (!clienteId.equals(pedido.getCliente().getId())) {
            flash.addFlashAttribute("error", "No tienes acceso a ese pedido.");
            return "redirect:/cuenta/pedidos";
        }

        // Materializa lineas + producto dentro de la transaccion (open-in-view=false)
        pedido.getLineas().forEach(l -> l.getProducto().getNombre());

        PedidoResponse pedidoDto = PedidoResponse.from(pedido);

        // Reglas de elegibilidad para solicitar devolucion:
        // 1) Pedido en estado ENTREGADO
        // 2) Dentro de la ventana temporal (dias-limite desde fechaPedido)
        // 3) Al menos una linea aun devolvible
        // 4) Sin devolucion activa (SOLICITADA o APROBADA) sobre este pedido
        boolean estaEntregado = pedido.getEstado() == EstadoPedido.ENTREGADO;
        boolean dentroDeVentana = pedido.getFechaPedido()
                .plusDays(devolucionesProperties.getDiasLimite())
                .isAfter(LocalDateTime.now());
        boolean hayDevolvibles = pedido.getLineas().stream()
                .anyMatch(l -> l.getCantidadDevolvible() > 0);
        boolean tieneActiva = devolucionRepository.findByPedidoId(id).stream()
                .anyMatch(d -> d.getEstado() == EstadoDevolucion.SOLICITADA
                            || d.getEstado() == EstadoDevolucion.APROBADA);

        boolean puedeDevolver = estaEntregado && dentroDeVentana && hayDevolvibles && !tieneActiva;

        model.addAttribute("pedido", pedidoDto);
        model.addAttribute("puedeDevolver", puedeDevolver);
        model.addAttribute("motivoNoDevolver",
                motivoNoDevolver(estaEntregado, dentroDeVentana, hayDevolvibles, tieneActiva));
        model.addAttribute("seccionCuenta", "pedidos");
        return "tienda/cuenta/pedido-detalle";
    }

    private String motivoNoDevolver(boolean entregado, boolean dentroVentana,
                                    boolean hayDevolvibles, boolean tieneActiva) {
        if (!entregado)      return "Solo puedes devolver pedidos que ya han sido entregados.";
        if (!dentroVentana)  return "Ha expirado el plazo de devolucion ("
                + devolucionesProperties.getDiasLimite() + " dias).";
        if (!hayDevolvibles) return "Todas las unidades de este pedido ya han sido devueltas.";
        if (tieneActiva)     return "Ya tienes una devolucion activa sobre este pedido.";
        return null;
    }

    // ---------- FACTURAS ----------

    @GetMapping("/facturas")
    public String facturas(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                           Model model) {
        int pagina = Math.max(0, page);
        Page<FacturaResponse> facturas = facturaService.listarMias(
                PageRequest.of(pagina, 10, Sort.by(Sort.Direction.DESC, "fechaEmision")));
        model.addAttribute("facturas", facturas);
        model.addAttribute("paginaActual", facturas.getNumber());
        model.addAttribute("totalPaginas", facturas.getTotalPages());
        model.addAttribute("seccionCuenta", "facturas");
        return "tienda/cuenta/facturas";
    }

    @GetMapping("/facturas/{id}/pdf")
    public ResponseEntity<byte[]> facturaPdf(@PathVariable Long id) {
        byte[] pdf = facturaService.descargarPdf(id);
        FacturaResponse f = facturaService.obtenerPorId(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "factura-" + f.getNumeroFactura() + ".pdf");
        return new ResponseEntity<>(pdf, headers, 200);
    }

    // ---------- DEVOLUCIONES ----------

    @GetMapping("/devoluciones")
    public String devoluciones(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                               Model model) {
        int pagina = Math.max(0, page);
        Page<DevolucionResponse> devs = devolucionService.listarMisDevoluciones(
                PageRequest.of(pagina, 10, Sort.by(Sort.Direction.DESC, "fechaSolicitud")));
        model.addAttribute("devoluciones", devs);
        model.addAttribute("paginaActual", devs.getNumber());
        model.addAttribute("totalPaginas", devs.getTotalPages());
        model.addAttribute("seccionCuenta", "devoluciones");
        return "tienda/cuenta/devoluciones";
    }

    /**
     * Detalle de una devolucion concreta del cliente.
     * El service valida propiedad internamente.
     */
    @GetMapping("/devoluciones/{id}")
    public String devolucionDetalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        DevolucionResponse devolucion;
        try {
            devolucion = devolucionService.obtenerDevolucion(id);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "No se ha encontrado la devolucion.");
            return "redirect:/cuenta/devoluciones";
        }
        model.addAttribute("devolucion", devolucion);
        model.addAttribute("seccionCuenta", "devoluciones");
        return "tienda/cuenta/devolucion-detalle";
    }

    /**
     * Formulario para solicitar una devolucion sobre un pedido concreto.
     * Solo accesible si el pedido cumple las reglas de elegibilidad.
     */
    @GetMapping("/pedidos/{id}/devolver")
    @Transactional(readOnly = true)
    public String devolverForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            flash.addFlashAttribute("error", "No se ha encontrado el pedido.");
            return "redirect:/cuenta/pedidos";
        }
        Long clienteId = clienteIdActual();
        if (!clienteId.equals(pedido.getCliente().getId())) {
            flash.addFlashAttribute("error", "No tienes acceso a ese pedido.");
            return "redirect:/cuenta/pedidos";
        }

        // Reaplicamos las MISMAS reglas del detalle de pedido. No fiarse de
        // que el cliente venga por el boton: alguien podria escribir la URL.
        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            flash.addFlashAttribute("error",
                    "Solo se pueden devolver pedidos en estado ENTREGADO.");
            return "redirect:/cuenta/pedidos/" + id;
        }
        LocalDateTime fechaLimite = pedido.getFechaPedido()
                .plusDays(devolucionesProperties.getDiasLimite());
        if (LocalDateTime.now().isAfter(fechaLimite)) {
            flash.addFlashAttribute("error",
                    "Ha expirado el plazo de devolucion ("
                    + devolucionesProperties.getDiasLimite() + " dias).");
            return "redirect:/cuenta/pedidos/" + id;
        }
        boolean tieneActiva = devolucionRepository.findByPedidoId(id).stream()
                .anyMatch(d -> d.getEstado() == EstadoDevolucion.SOLICITADA
                            || d.getEstado() == EstadoDevolucion.APROBADA);
        if (tieneActiva) {
            flash.addFlashAttribute("error",
                    "Ya tienes una devolucion activa sobre este pedido.");
            return "redirect:/cuenta/pedidos/" + id;
        }

        // Materializa lineas + producto dentro de la transaccion.
        pedido.getLineas().forEach(l -> l.getProducto().getNombre());

        List<LineaPedido> lineasDevolvibles = pedido.getLineas().stream()
                .filter(l -> l.getCantidadDevolvible() > 0)
                .toList();
        if (lineasDevolvibles.isEmpty()) {
            flash.addFlashAttribute("error",
                    "Todas las unidades de este pedido ya han sido devueltas.");
            return "redirect:/cuenta/pedidos/" + id;
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("lineasDevolvibles", lineasDevolvibles);
        model.addAttribute("diasLimite", devolucionesProperties.getDiasLimite());
        model.addAttribute("seccionCuenta", "devoluciones");
        return "tienda/cuenta/devolucion-solicitar";
    }

    /**
     * Procesa la solicitud de devolucion. Recibe:
     * <ul>
     *   <li>{@code pedidoId} — el pedido</li>
     *   <li>{@code motivo} — texto libre obligatorio</li>
     *   <li>{@code incluir} — lista de ids de lineas marcadas (checkbox)</li>
     *   <li>{@code cantidad_<lineaId>} — cantidad para cada linea</li>
     * </ul>
     *
     * <p>Construye el DTO y delega en el servicio. El servicio valida
     * propiedad, estado del pedido, ventana temporal y cantidades.
     */
    @PostMapping("/devoluciones")
    public String solicitarDevolucion(
            @RequestParam("pedidoId") Long pedidoId,
            @RequestParam(value = "motivo", required = false) String motivo,
            @RequestParam(value = "incluir", required = false) List<Long> incluir,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes flash) {

        if (motivo == null || motivo.isBlank()) {
            flash.addFlashAttribute("error", "El motivo de la devolucion es obligatorio.");
            return "redirect:/cuenta/pedidos/" + pedidoId + "/devolver";
        }
        if (incluir == null || incluir.isEmpty()) {
            flash.addFlashAttribute("error",
                    "Selecciona al menos un articulo a devolver.");
            return "redirect:/cuenta/pedidos/" + pedidoId + "/devolver";
        }

        List<LineaDevolucionRequest> lineas = new ArrayList<>();
        for (Long lineaId : incluir) {
            String raw = allParams.get("cantidad_" + lineaId);
            int cantidad;
            try {
                cantidad = (raw == null || raw.isBlank()) ? 0 : Integer.parseInt(raw.trim());
            } catch (NumberFormatException ex) {
                flash.addFlashAttribute("error",
                        "Cantidad invalida para uno de los articulos.");
                return "redirect:/cuenta/pedidos/" + pedidoId + "/devolver";
            }
            if (cantidad < 1) {
                continue; // marcada pero sin cantidad -> la ignoramos
            }
            LineaDevolucionRequest l = new LineaDevolucionRequest();
            l.setLineaPedidoId(lineaId);
            l.setCantidad(cantidad);
            lineas.add(l);
        }

        if (lineas.isEmpty()) {
            flash.addFlashAttribute("error",
                    "Indica al menos una cantidad mayor que cero.");
            return "redirect:/cuenta/pedidos/" + pedidoId + "/devolver";
        }

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(pedidoId);
        req.setMotivo(motivo.trim());
        req.setLineas(lineas);

        try {
            DevolucionResponse creada = devolucionService.crearDevolucion(req);
            flash.addFlashAttribute("info",
                    "Solicitud de devolucion #" + creada.getId() + " enviada. " +
                    "Recibiras una respuesta del equipo de soporte en breve.");
            return "redirect:/cuenta/devoluciones/" + creada.getId();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cuenta/pedidos/" + pedidoId + "/devolver";
        }
    }

    // ---------- PERFIL ----------

    @GetMapping("/perfil")
    @Transactional(readOnly = true)
    public String perfil(Model model) {
        Cliente cliente = clienteActual();
        Usuario usuario = cliente.getUsuario();

        if (!model.containsAttribute("perfil")) {
            ActualizarPerfilRequest req = new ActualizarPerfilRequest();
            req.setNombre(usuario.getNombre());
            req.setApellidos(usuario.getApellidos());
            req.setTelefono(cliente.getTelefono());
            req.setDireccion(cliente.getDireccion());
            req.setCiudad(cliente.getCiudad());
            req.setCodigoPostal(cliente.getCodigoPostal());
            req.setPais(cliente.getPais());
            model.addAttribute("perfil", req);
        }
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("seccionCuenta", "perfil");
        return "tienda/cuenta/perfil";
    }

    @PostMapping("/perfil")
    @Transactional
    public String actualizarPerfil(@Valid @ModelAttribute("perfil") ActualizarPerfilRequest req,
                                   BindingResult binding,
                                   RedirectAttributes flash,
                                   Model model) {
        Cliente cliente = clienteActual();
        Usuario usuario = cliente.getUsuario();

        if (binding.hasErrors()) {
            model.addAttribute("email", usuario.getEmail());
            model.addAttribute("seccionCuenta", "perfil");
            return "tienda/cuenta/perfil";
        }

        usuario.setNombre(req.getNombre());
        usuario.setApellidos(req.getApellidos());
        usuarioRepository.save(usuario);

        cliente.setTelefono(req.getTelefono());
        cliente.setDireccion(req.getDireccion());
        cliente.setCiudad(req.getCiudad());
        cliente.setCodigoPostal(req.getCodigoPostal());
        cliente.setPais(req.getPais());
        clienteRepository.save(cliente);

        flash.addFlashAttribute("info", "Perfil actualizado correctamente.");
        return "redirect:/cuenta/perfil";
    }

    // ---------- helpers ----------

    private Long clienteIdActual() {
        Long id = securityHelper.getClienteActualId();
        if (id == null) {
            throw new IllegalStateException("No hay cliente autenticado.");
        }
        return id;
    }

    private Cliente clienteActual() {
        return clienteRepository.findById(clienteIdActual())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }
}
