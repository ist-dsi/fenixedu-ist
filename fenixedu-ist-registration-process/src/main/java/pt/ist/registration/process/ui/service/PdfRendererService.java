package pt.ist.registration.process.ui.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public interface PdfRendererService {

    InputStream render(InputStream template, JsonObject payload);

    default byte[] renderToByteArray(InputStream template, JsonObject payload) throws PdfRenderingException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ByteStreams.copy(render(template, payload), baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new PdfRenderingException("Error while rendering pdf", e);
        }
    }
}
