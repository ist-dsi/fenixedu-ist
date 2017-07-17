package pt.ist.registration.process.ui.service;

import org.springframework.stereotype.Service;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public interface QRCodeGenerator {

    byte[] generate(String identifier, int width, int height) throws QRCodeGenerationException;

    default byte[] generate(String identifier) throws QRCodeGenerationException {
        return generate(identifier, 250, 250);
    }
}
