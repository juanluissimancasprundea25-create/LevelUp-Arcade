package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.ActualizarPerfilRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
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

import java.util.List;

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
 * </ul>
 */
@Controller
@RequestMapping("/cuenta")
public class CuentaController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;
    private final FacturaService facturaService;
    private final DevolucionService devolucionService;
    private final SecurityHelper securityHelper;

    public CuentaController(UsuarioRepository usuarioRepository,
                            ClienteRepository clienteRepository,
                            PedidoService pedidoService,
                            FacturaService facturaService,
                            DevolucionService devolucionService,
                            SecurityHelper securityHelper) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
        this.facturaService = facturaService;
        this.devolucionService = devolucionService;
        this.securityHelper = securityHelper;
    }

    // ---------- INICIO ----------

    @GetMapping
    public String inicio(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // KPIs en vivo para los cards del dashboard del cliente.
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
    public String pedidoDetalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        PedidoResponse pedido;
        try {
            pedido = pedidoService.obtenerPorId(id);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "No se ha encontrado el pedido.");
            return "redirect:/cuenta/pedidos";
        }
        // Seguridad: pedidoService.obtenerPorId NO valida propiedad
        // (lo usa tambien el admin). Aqui hay que comprobarlo.
        Long clienteId = clienteIdActual();
        if (!clienteId.equals(pedido.clienteId())) {
            flash.addFlashAttribute("error", "No tienes acceso a ese pedido.");
            return "redirect:/cuenta/pedidos";
        }
        model.addAttribute("pedido", pedido);
        model.addAttribute("seccionCuenta", "pedidos");
        return "tienda/cuenta/pedido-detalle";
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

    /** Descarga el PDF de una factura. El service valida propiedad. */
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
        model.addAttribute("email", usuario.getEmail()); // solo lectura
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
