package pt.ist.registration.process.ui.service;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.itextpdf.text.pdf.BarcodeQRCode;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@Service
public class ITextQRCodeGenerator implements QRCodeGenerator {
    
    @Override
    public byte[] generate(String identifier, int width, int height) throws QRCodeGenerationException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            BarcodeQRCode qrcode = new BarcodeQRCode(identifier, width, height, null);
            Image awtImage = qrcode.createAwtImage(Color.BLACK, Color.WHITE);
            BufferedImage buffer =
                    new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
            buffer.getGraphics().drawImage(awtImage, 0, 0, null);
            ImageIO.write(buffer, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new QRCodeGenerationException("Error while generating qr code for identifier " + identifier, e);
        }
    }

}
