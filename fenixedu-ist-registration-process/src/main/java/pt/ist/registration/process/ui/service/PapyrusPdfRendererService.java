package pt.ist.registration.process.ui.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;

import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusSettings;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@Service
public class PapyrusPdfRendererService implements PdfRendererService {

    private PapyrusClient papyrusClient;
    private PapyrusSettings defaultSettings;

    @Autowired
    public PapyrusPdfRendererService(PapyrusClient papyrusClient, PapyrusSettings defaultSettings) {
        this.papyrusClient = papyrusClient;
        this.defaultSettings = defaultSettings;
    }

    @Override
    public InputStream render(InputStream template, JsonObject payload) {
        return papyrusClient.liveRender(template, payload, defaultSettings);
    }
}
