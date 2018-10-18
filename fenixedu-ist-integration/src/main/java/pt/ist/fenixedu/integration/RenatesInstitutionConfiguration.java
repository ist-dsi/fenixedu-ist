package pt.ist.fenixedu.integration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.bennu.RenatesIntegrationConfiguration;

import pt.ist.renates.domain.InstitutionCodeProvider;

@WebListener
public class RenatesInstitutionConfiguration implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initializeInstitutionCodeProvider();
    }

    public void initializeInstitutionCodeProvider() {

        RenatesIntegrationConfiguration.setInstitutionCodeProvider(new InstitutionCodeProvider() {
            @Override
            public String getOrganicUnitCode(DegreeCurricularPlan degreeCurricularPlan) {

                switch (degreeCurricularPlan.getLastCampus().getName()) {

                case "Alameda":
                    return "1518";

                case "Taguspark":
                    return "1519";

                default:
                    return "1518";
                }
            }

            @Override
            public String getEstablishmentCode() {
                return "1500";
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

}
