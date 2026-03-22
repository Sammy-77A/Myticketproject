package com.myticket.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class QrCodeService {
    
    private final Path qrCodesLocation = Paths.get("qr-codes");

    public QrCodeService() {
        try {
            if (!Files.exists(qrCodesLocation)) {
                Files.createDirectories(qrCodesLocation);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create QR codes directory", e);
        }
    }

    public String generateQr(String ticketCode) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(ticketCode, BarcodeFormat.QR_CODE, 250, 250);
            
            String fileName = ticketCode + ".png";
            Path path = qrCodesLocation.resolve(fileName);
            
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            
            return path.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR Code", e);
        }
    }
}
