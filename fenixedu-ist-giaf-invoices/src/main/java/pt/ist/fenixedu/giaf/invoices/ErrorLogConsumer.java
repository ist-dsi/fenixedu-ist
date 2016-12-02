package pt.ist.fenixedu.giaf.invoices;

@FunctionalInterface
public interface ErrorLogConsumer {

    void accept(final String oid,
            final String user,
            final String name,
            final String amount,
            final String cycleType,
            final String error,
            final String args,
            final String type,
            final String countryOfVatNumber,
            final String vatNumber,
            final String address,
            final String locality,
            final String postCode,
            final String countryOfAddress,
            final String paymentMethod);

}
