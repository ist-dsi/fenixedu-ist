package pt.ist.fenixedu.quc.domain.exceptions;

import org.fenixedu.bennu.core.domain.exceptions.DomainException;

public class FenixEduQucDomainException extends DomainException {

    private static final long serialVersionUID = 4379796158079580297L;

    protected static final String BUNDLE = "resources.FenixEduQucResources";

    protected FenixEduQucDomainException(String key, String... args) {
        super(BUNDLE, key, args);
    }

    protected FenixEduQucDomainException(Throwable cause, String key, String... args) {
        super(cause, BUNDLE, key, args);
    }

    public static FenixEduQucDomainException inquiriesNotAnswered() {
        return new FenixEduQucDomainException("message.student.cannotEnroll.inquiriesNotAnswered");
    }
}
