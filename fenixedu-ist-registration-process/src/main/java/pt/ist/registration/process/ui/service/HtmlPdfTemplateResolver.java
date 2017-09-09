package pt.ist.registration.process.ui.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public class HtmlPdfTemplateResolver implements PdfTemplateResolver {

    private WebApplicationContext webApplicationContext;

    @Autowired
    public HtmlPdfTemplateResolver(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    @Override
    public InputStream resolve(String templateName) throws UnresolvableTemplateException {
        try {
            return webApplicationContext
                    .getResource("classpath:/templates/" + templateName)
                    .getInputStream();
        } catch (IOException e) {
            throw new UnresolvableTemplateException("Unable to resolve template " + templateName, e);
        }
    }
}
