package com.leveluparcade.controller.web;

import com.leveluparcade.entity.EstadoDevolucion;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.DevolucionRepository;
import com.leveluparcade.repository.FacturaRepository;
import com.leveluparcade.repository.MensajeChatRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.repository.ProveedorRepository;
import com.leveluparcade.service.PedidoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de paginas web no especificas de una entidad concreta:
 * landing publica, login, dashboard con KPIs y endpoints utilitarios.
 *
 * <p>El dashboard agrega contadores y metricas de todo el sistema
 * llamando directamente a los repositorios (lectura ligera, sin
 * pasar por los Services para no acoplar ni anadirles metodos solo
 * para esto).
 */
@Controller
public class HomeController {

    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final FacturaRepository facturaRepository;
    private final DevolucionRepository devolucionRepository;
    private final MensajeChatRepository mensajeRepository;
    private final PedidoService pedidoService;

    public HomeController(ClienteRepository clienteRepository,
                          ProveedorRepository proveedorRepository,
                          ProductoRepository productoRepository,
                          PedidoRepository pedidoRepository,
                          FacturaRepository facturaRepository,
                          DevolucionRepository devolucionRepository,
                          MensajeChatRepository mensajeRepository,
                          PedidoService pedidoService) {
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.facturaRepository = facturaRepository;
        this.devolucionRepository = devolucionRepository;
        this.mensajeRepository = mensajeRepository;
        this.pedidoService = pedidoService;
    }

    /** Landing publica. */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("titulo", "LevelUp Arcade");
        model.addAttribute("mensaje", "Sistema de gestion funcionando correctamente");
        return "home";
    }

    /** Pagina de login. Spring Security gestiona el POST. */
    @GetMapping("/admin/login")
    public String adminLoginRedirect() {
        // Compatibilidad: si alguien viene a /admin/login lo mandamos al login publico
        return "redirect:/login";
    }

    /**
     * Dashboard del panel admin con KPIs en vivo.
     *
     * <p>Carga contadores agregados de todas las entidades principales
     * y los pedidos mas recientes para mostrar como tabla resumen.
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String dashboard(Model model) {

        Map<String, Object> kpis = new HashMap<>();

        // Contadores generales. Excluimos entidades soft-deleted para que
        // el numero coincida con lo que el admin ve en cada listado:
        //   - Cliente eliminado: usuario.email termina en "@borrado.local".
        //   - Producto eliminado: sku empieza por "BORRADO-".
        kpis.put("totalClientes", contarClientesActivosSafe());
        kpis.put("totalProveedores", proveedorRepository.count());
        kpis.put("totalProductos", contarProductosActivosSafe());
        kpis.put("totalPedidos", pedidoRepository.count());
        kpis.put("totalFacturas", facturaRepository.count());

        // Pedidos por estado
        long pedidosPendientes = countPedidosEstadoSafe(EstadoPedido.PENDIENTE);
        long pedidosPagados = countPedidosEstadoSafe(EstadoPedido.PAGADO);
        long pedidosEnviados = countPedidosEstadoSafe(EstadoPedido.ENVIADO);
        long pedidosEntregados = countPedidosEstadoSafe(EstadoPedido.ENTREGADO);
        long pedidosCancelados = countPedidosEstadoSafe(EstadoPedido.CANCELADO);

        kpis.put("pedidosPendientes", pedidosPendientes);
        kpis.put("pedidosPagados", pedidosPagados);
        kpis.put("pedidosEnviados", pedidosEnviados);
        kpis.put("pedidosEntregados", pedidosEntregados);
        kpis.put("pedidosCancelados", pedidosCancelados);

        // Devoluciones por estado
        kpis.put("devolucionesSolicitadas", countDevolucionesEstadoSafe(EstadoDevolucion.SOLICITADA));
        kpis.put("devolucionesAprobadas", countDevolucionesEstadoSafe(EstadoDevolucion.APROBADA));

        // Stock alerts
        kpis.put("productosBajoStock", contarProductosBajoStockSafe());

        // Chat no leidos en buzon admin
        kpis.put("mensajesSinLeer",
                mensajeRepository.countByDestinatarioIsNullAndLeidoFalse());

        // Ultimos 5 pedidos
        var ultimosPedidos = pedidoService.listarTodos().stream()
                .limit(5)
                .toList();

        // Total facturado (suma del total de todos los pedidos con factura)
        BigDecimal totalFacturado = calcularTotalFacturadoSafe();
        kpis.put("totalFacturado", totalFacturado);

        model.addAttribute("kpis", kpis);
        model.addAttribute("ultimosPedidos", ultimosPedidos);
        model.addAttribute("seccionActiva", "dashboard");
        return "dashboard";
    }

    // ---------- helpers defensivos ----------

    private long countPedidosEstadoSafe(EstadoPedido estado) {
        try {
            return pedidoService.listarPorEstado(estado).size();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long countDevolucionesEstadoSafe(EstadoDevolucion estado) {
        try {
            // Sin filtro de cliente, pagina 0 con tamano alto
            return devolucionRepository.findAll().stream()
                    .filter(d -> estado.equals(d.getEstado()))
                    .count();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long contarProductosBajoStockSafe() {
        try {
            return productoRepository.findAll().stream()
                    .filter(p -> p.getStock() != null
                              && p.getStockMinimo() != null
                              && p.getStock() <= p.getStockMinimo())
                    .count();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private BigDecimal calcularTotalFacturadoSafe() {
        // El "total facturado" es el dinero realmente ingresado: la suma
        // de los totales de todos los pedidos que NO esten cancelados.
        // Antes se sumaban facturas (PDF) emitidas, pero podia quedarse
        // a 0 si los pedidos aun no habian generado factura, dando una
        // cifra enganosa para el admin.
        try {
            return pedidoRepository.findAll().stream()
                    .filter(p -> p.getEstado() != null
                            && p.getEstado() != EstadoPedido.CANCELADO)
                    .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private long contarClientesActivosSafe() {
        try {
            return clienteRepository.findAll().stream()
                    .filter(c -> {
                        if (c.getUsuario() == null) return true;
                        String e = c.getUsuario().getEmail();
                        return e == null || !e.endsWith("@borrado.local");
                    })
                    .count();
        } catch (Exception ex) {
            return clienteRepository.count();
        }
    }

    private long contarProductosActivosSafe() {
        try {
            return productoRepository.findAll().stream()
                    .filter(p -> p.getSku() == null
                            || !p.getSku().startsWith("BORRADO-"))
                    .count();
        } catch (Exception ex) {
            return productoRepository.count();
        }
    }
}