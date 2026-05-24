package com.leveluparcade.service;

import com.leveluparcade.config.FacturasProperties;
import com.leveluparcade.entity.Factura;
import com.leveluparcade.repository.FacturaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Genera numeros de factura con formato {prefijo}-{anio}-{secuencial}.
 *
 * El secuencial se reinicia cada anio. Se usa SERIALIZABLE para evitar
 * que dos transacciones concurrentes calculen el mismo MAX y generen
 * numeros duplicados. La UNIQUE en numero_factura es defensa de BD.
 */
@Component
public class NumeradorFacturas {

    private final FacturaRepository facturaRepository;
    private final FacturasProperties props;

    public NumeradorFacturas(FacturaRepository facturaRepository, FacturasProperties props) {
        this.facturaRepository = facturaRepository;
        this.props = props;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String generarSiguiente() {
        int anio = LocalDateTime.now().getYear();
        String prefijoYAnio = props.getPrefijoNumero() + "-" + anio + "-";

        Pageable top1 = PageRequest.of(0, 1);
        List<Factura> ultima = facturaRepository.buscarUltimaPorPrefijoAnio(prefijoYAnio, top1);

        int siguiente = 1;
        if (!ultima.isEmpty()) {
            String numero = ultima.get(0).getNumeroFactura();
            String secuencialStr = numero.substring(prefijoYAnio.length());
            siguiente = Integer.parseInt(secuencialStr) + 1;
        }

        return String.format("%s%05d", prefijoYAnio, siguiente);
    }
}