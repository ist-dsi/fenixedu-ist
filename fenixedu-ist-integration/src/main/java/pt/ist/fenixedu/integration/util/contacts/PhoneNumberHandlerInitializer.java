package pt.ist.fenixedu.integration.util.contacts;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.util.PhoneUtil;

@WebListener
public class PhoneNumberHandlerInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        PartyContact.setResolver(Phone.class, (pc) -> ISTPhoneNumberHandler.getPresentationValue(((Phone) pc).getNumber()));
        PhoneUtil.setPhoneNumberHandler(new ISTPhoneNumberHandler());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
