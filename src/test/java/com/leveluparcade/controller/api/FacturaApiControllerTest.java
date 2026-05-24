package com.leveluparcade.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leveluparcade.config.SecurityConfig;
import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.security.JwtFilter;
import com.leveluparcade.security.JwtService;
import com.leveluparcade.service.FacturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = FacturaApiController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, JwtFilter.class, JwtService.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
@DisplayName("FacturaApiController - tests web mvc")
class FacturaApiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean FacturaService facturaService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST facturas (ADMIN) -> 200")
    void emitirOk() throws Exception {
        when(facturaService.emitirFactura(any(EmitirFacturaRequest.class))).thenReturn(facturaDemo());

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(7L);

        mvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroFactura").value("FAC-2026-00001"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST facturas con rol CLIENTE -> 403")
    void emitirSinPermiso() throws Exception {
        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(7L);

        mvc.perform(post("/api/facturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST desde-pedido (ADMIN) -> 200")
    void emitirDesdePedido() throws Exception {
        when(facturaService.emitirFactura(any(EmitirFacturaRequest.class))).thenReturn(facturaDemo());

        mvc.perform(post("/api/facturas/desde-pedido/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pedidoId").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET facturas (ADMIN) -> 200 con Page")
    void listarTodasAdmin() throws Exception {
        Page<FacturaResponse> page = new PageImpl<>(List.of(facturaDemo()));
        when(facturaService.listarTodas(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/facturas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].numeroFactura").value("FAC-2026-00001"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET facturas como CLIENTE -> 403")
    void listarTodasComoClienteDenegado() throws Exception {
        mvc.perform(get("/api/facturas"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET mias (CLIENTE) -> 200")
    void listarMias() throws Exception {
        Page<FacturaResponse> page = new PageImpl<>(List.of(facturaDemo()));
        when(facturaService.listarMias(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/facturas/mias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET id (CLIENTE no dueno) -> 403")
    void obtenerSinPermiso() throws Exception {
        when(facturaService.obtenerPorId(eq(5L)))
            .thenThrow(new AccessDeniedException("no"));

        mvc.perform(get("/api/facturas/5"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET id inexistente -> 404")
    void obtenerNoEncontrado() throws Exception {
        when(facturaService.obtenerPorId(eq(404L)))
            .thenThrow(new ResourceNotFoundException("no"));

        mvc.perform(get("/api/facturas/404"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET pdf (ADMIN) -> 200 con content-type application/pdf")
    void descargarPdf() throws Exception {
        byte[] bytes = "PDF".getBytes();
        when(facturaService.descargarPdf(1L)).thenReturn(bytes);

        mvc.perform(get("/api/facturas/1/pdf"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString("factura-1.pdf")));
    }

    @Test
    @DisplayName("GET verificar sin auth -> 200 (endpoint publico)")
    void verificarPublico() throws Exception {
        when(facturaService.verificar("FAC-2026-00001")).thenReturn(facturaDemo());

        mvc.perform(get("/api/facturas/verificar/FAC-2026-00001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroFactura").value("FAC-2026-00001"));
    }

    private FacturaResponse facturaDemo() {
        FacturaResponse r = new FacturaResponse();
        r.setId(1L);
        r.setNumeroFactura("FAC-2026-00001");
        r.setFechaEmision(LocalDateTime.now());
        r.setPedidoId(7L);
        r.setClienteId(10L);
        r.setClienteNombre("Ana Lopez");
        r.setTotal(new BigDecimal("99.99"));
        r.setUrlVerificacion("http://localhost:8080/api/facturas/verificar/FAC-2026-00001");
        return r;
    }
}