package pt.ist.fenixedu.domain;

public enum SapRequestType {

    ADVANCEMENT(true), CREDIT(true), DEBT(false), DEBT_CREDIT(false), INVOICE(true), INVOICE_INTEREST(true), PAYMENT(true),
    PAYMENT_INTEREST(true), REIMBURSEMENT(true), CLOSE_INVOICE(false);

    private boolean isToGetDocument;

    private SapRequestType(boolean isToGetDocument) {
        this.isToGetDocument = isToGetDocument;
    }

    public boolean isToGetDocument() {
        return isToGetDocument;
    }
}
