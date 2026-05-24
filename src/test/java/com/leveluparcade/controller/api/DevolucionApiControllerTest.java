package com.leveluparcade.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leveluparcade.config.SecurityConfig;
import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.LineaDevolucionRequest;
import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.security.JwtFilter;
import com.leveluparcade.security.JwtService;
import com.leveluparcade.service.DevolucionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = DevolucionApiController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, JwtFilter.class, JwtService.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
@DisplayName("DevolucionApiController - tests web mvc")
class DevolucionApiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean DevolucionService devolucionService;

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST crear (CLIENTE) -> 201")
    void crearOk() throws Exception {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(1L);
        when(devolucionService.crearDevolucion(any(CrearDevolucionRequest.class))).thenReturn(r);

        mvc.perform(post("/api/devoluciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(crearRequestValido())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("POST crear (EMPLEADO) -> 403")
    void crearSinPermiso() throws Exception {
        mvc.perform(post("/api/devoluciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(crearRequestValido())))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET listar (ADMIN) -> 200 con Page")
    void listarTodas() throws Exception {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(1L);
        Page<DevolucionResponse> page = new PageImpl<>(List.of(r));
        when(devolucionService.listarDevoluciones(any(), any(), any(Pageable.class)))
            .thenReturn(page);

        mvc.perform(get("/api/devoluciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET listar como CLIENTE -> 403")
    void listarComoClienteDenegado() throws Exception {
        mvc.perform(get("/api/devoluciones"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET mias (CLIENTE) -> 200")
    void listarMias() throws Exception {
        Page<DevolucionResponse> page = new PageImpl<>(List.of(new DevolucionResponse()));
        when(devolucionService.listarMisDevoluciones(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/devoluciones/mias"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET id no dueno -> 403")
    void obtenerSinPermiso() throws Exception {
        when(devolucionService.obtenerDevolucion(eq(5L)))
            .thenThrow(new AccessDeniedException("no"));

        mvc.perform(get("/api/devoluciones/5"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET id inexistente -> 404")
    void obtenerNoEncontrado() throws Exception {
        when(devolucionService.obtenerDevolucion(eq(404L)))
            .thenThrow(new ResourceNotFoundException("no"));

        mvc.perform(get("/api/devoluciones/404"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST aprobar (ADMIN) -> 200")
    void aprobarOk() throws Exception {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(1L);
        when(devolucionService.aprobarDevolucion(1L)).thenReturn(r);

        mvc.perform(post("/api/devoluciones/1/aprobar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST aprobar (CLIENTE) -> 403")
    void aprobarSinPermiso() throws Exception {
        mvc.perform(post("/api/devoluciones/1/aprobar"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST rechazar (ADMIN) -> 200")
    void rechazarOk() throws Exception {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(1L);
        when(devolucionService.rechazarDevolucion(eq(1L), any(RechazarDevolucionRequest.class)))
            .thenReturn(r);

        RechazarDevolucionRequest req = new RechazarDevolucionRequest();
        req.setMotivoRechazo("Fuera de plazo");

        mvc.perform(post("/api/devoluciones/1/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST completar (ADMIN) -> 200")
    void completarOk() throws Exception {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(1L);
        when(devolucionService.completarDevolucion(1L)).thenReturn(r);

        mvc.perform(post("/api/devoluciones/1/completar"))
            .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private CrearDevolucionRequest crearRequestValido() {
        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(7L);
        req.setMotivo("Producto defectuoso");

        LineaDevolucionRequest linea = new LineaDevolucionRequest();
        try {
            LineaDevolucionRequest.class
                .getMethod("setLineaPedidoId", Long.class)
                .invoke(linea, 1L);
        } catch (NoSuchMethodException e) {
            try {
                LineaDevolucionRequest.class
                    .getMethod("setLineaId", Long.class)
                    .invoke(linea, 1L);
            } catch (Exception ex) {
                throw new RuntimeException("No encuentro setter de id de linea", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            LineaDevolucionRequest.class
                .getMethod("setCantidad", Integer.class)
                .invoke(linea, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        req.setLineas(List.of(linea));
        return req;
    }
}