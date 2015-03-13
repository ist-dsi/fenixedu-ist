package pt.ist.fenix.jasper.reports;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.util.report.ReportsUtils;

@WebListener
public class JasperReportInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ReportsUtils.setPrinter(new JasperReportPrinter());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
