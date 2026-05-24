package com.leveluparcade.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Genera codigos QR como PNG en memoria.
 * Sin estado, sin dependencias externas. Reutilizable por cualquier modulo.
 */
@Service
public class QrService {

    private static final int DEFAULT_SIZE = 200;

    public byte[] generarPng(String contenido) {
        return generarPng(contenido, DEFAULT_SIZE);
    }

    public byte[] generarPng(String contenido, int tamano) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                contenido,
                BarcodeFormat.QR_CODE,
                tamano,
                tamano,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                       EncodeHintType.MARGIN, 1)
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Error generando QR: " + e.getMessage(), e);
        }
    }
}