package com.leveluparcade.service;

import com.leveluparcade.config.FacturasProperties;
import com.leveluparcade.entity.Factura;
import com.leveluparcade.repository.FacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NumeradorFacturas - tests unitarios")
class NumeradorFacturasTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private FacturasProperties props;

    @InjectMocks private NumeradorFacturas numerador;

    private int anioActual;
    private String prefijoYAnio;

    @BeforeEach
    void setUp() {
        anioActual = LocalDateTime.now().getYear();
        when(props.getPrefijoNumero()).thenReturn("FAC");
        prefijoYAnio = "FAC-" + anioActual + "-";
    }

    @Test
    @DisplayName("generarSiguiente: sin facturas previas devuelve 00001")
    void generarSiguiente_primerNumero() {
        when(facturaRepository.buscarUltimaPorPrefijoAnio(anyString(), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        String numero = numerador.generarSiguiente();

        assertThat(numero).isEqualTo(prefijoYAnio + "00001");
    }

    @Test
    @DisplayName("generarSiguiente: con factura 00007 devuelve 00008")
    void generarSiguiente_incrementa() {
        Factura ultima = new Factura();
        ultima.setNumeroFactura(prefijoYAnio + "00007");

        when(facturaRepository.buscarUltimaPorPrefijoAnio(anyString(), any(Pageable.class)))
            .thenReturn(List.of(ultima));

        String numero = numerador.generarSiguiente();

        assertThat(numero).isEqualTo(prefijoYAnio + "00008");
    }

    @Test
    @DisplayName("generarSiguiente: padea ceros a 5 digitos")
    void generarSiguiente_padding() {
        Factura ultima = new Factura();
        ultima.setNumeroFactura(prefijoYAnio + "00099");

        when(facturaRepository.buscarUltimaPorPrefijoAnio(anyString(), any(Pageable.class)))
            .thenReturn(List.of(ultima));

        String numero = numerador.generarSiguiente();

        assertThat(numero).isEqualTo(prefijoYAnio + "00100");
    }
}