package com.leveluparcade.service;

import com.leveluparcade.config.FacturasProperties;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Factura;
import com.leveluparcade.entity.LineaPedido;
import com.leveluparcade.entity.Pedido;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF de una factura usando OpenPDF (fork libre de iText 4).
 * El PDF NO se persiste en disco, se devuelve como byte[] para descarga al vuelo.
 */
@Service
public class PdfFacturaService {

    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FacturasProperties props;

    public PdfFacturaService(FacturasProperties props) {
        this.props = props;
    }

    public byte[] generar(Factura factura) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            anadirCabeceraEmisor(doc);
            anadirDatosFactura(doc, factura);
            anadirDatosCliente(doc, factura.getPedido().getCliente());
            anadirTablaLineas(doc, factura.getPedido());
            anadirTotales(doc, factura.getPedido());

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error generando PDF de factura: " + e.getMessage(), e);
        }
    }

    private void anadirCabeceraEmisor(Document doc) throws DocumentException {
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(40, 40, 120));
        Paragraph titulo = new Paragraph(props.getEmisor().getNombre(), fontTitulo);
        titulo.setAlignment(Element.ALIGN_LEFT);
        doc.add(titulo);

        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 10);
        doc.add(new Paragraph("CIF: " + props.getEmisor().getCif(), fontSub));
        doc.add(new Paragraph(props.getEmisor().getDireccion(), fontSub));
        doc.add(new Paragraph(props.getEmisor().getEmail(), fontSub));
        doc.add(new Paragraph(" "));
    }

    private void anadirDatosFactura(Document doc, Factura f) throws DocumentException {
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph titulo = new Paragraph("FACTURA " + f.getNumeroFactura(), fontTitulo);
        doc.add(titulo);

        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 10);
        doc.add(new Paragraph("Fecha emision: " + f.getFechaEmision().format(FMT_FECHA), fontTexto));
        doc.add(new Paragraph("Pedido n.: " + f.getPedido().getId(), fontTexto));
        doc.add(new Paragraph(" "));
    }

    private void anadirDatosCliente(Document doc, Cliente cliente) throws DocumentException {
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        doc.add(new Paragraph("Datos del cliente", fontTitulo));

        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String nombreCompleto = cliente.getUsuario().getNombre() +
            (cliente.getUsuario().getApellidos() != null ? " " + cliente.getUsuario().getApellidos() : "");
        doc.add(new Paragraph("Nombre: " + nombreCompleto, fontTexto));
        if (cliente.getNif() != null) {
            doc.add(new Paragraph("NIF: " + cliente.getNif(), fontTexto));
        }
        if (cliente.getDireccion() != null) {
            doc.add(new Paragraph("Direccion: " + cliente.getDireccion(), fontTexto));
        }
        doc.add(new Paragraph("Email: " + cliente.getUsuario().getEmail(), fontTexto));
        doc.add(new Paragraph(" "));
    }

    private void anadirTablaLineas(Document doc, Pedido pedido) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{4f, 1f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);

        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Color colorHeader = new Color(40, 40, 120);

        for (String h : new String[]{"Producto", "Cant.", "Precio ud.", "Subtotal"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
            c.setBackgroundColor(colorHeader);
            c.setPadding(6);
            tabla.addCell(c);
        }

        Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (LineaPedido linea : pedido.getLineas()) {
            tabla.addCell(new PdfPCell(new Phrase(linea.getProducto().getNombre(), fontFila)));
            tabla.addCell(celdaDerecha(String.valueOf(linea.getCantidad()), fontFila));
            tabla.addCell(celdaDerecha(linea.getPrecioUnitario().toPlainString() + " EUR", fontFila));
            BigDecimal subtotal = linea.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(linea.getCantidad()));
            tabla.addCell(celdaDerecha(subtotal.toPlainString() + " EUR", fontFila));
        }

        doc.add(tabla);
        doc.add(new Paragraph(" "));
    }

    private PdfPCell celdaDerecha(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(5);
        return c;
    }

    private void anadirTotales(Document doc, Pedido pedido) throws DocumentException {
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Paragraph total = new Paragraph("TOTAL: " + pedido.getTotal().toPlainString() + " EUR", fontTotal);
        total.setAlignment(Element.ALIGN_RIGHT);
        doc.add(total);
        doc.add(new Paragraph(" "));
    }
}