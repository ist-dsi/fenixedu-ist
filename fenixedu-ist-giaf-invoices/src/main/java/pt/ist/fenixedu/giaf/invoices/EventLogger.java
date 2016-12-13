package pt.ist.fenixedu.giaf.invoices;

@FunctionalInterface
public interface EventLogger {

    void log(final String msg, final Object... args);

}
