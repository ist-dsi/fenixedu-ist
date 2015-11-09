package pt.ist.fenixedu.giaf.invoices;

@FunctionalInterface
public interface ErrorConsumer<T> {

    void accept(final T t, final String erro, final String arg);

}
