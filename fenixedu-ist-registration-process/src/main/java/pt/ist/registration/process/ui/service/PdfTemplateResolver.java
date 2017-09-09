package pt.ist.registration.process.ui.service;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public interface PdfTemplateResolver {

    InputStream resolve(String templateName) throws UnresolvableTemplateException;
}
