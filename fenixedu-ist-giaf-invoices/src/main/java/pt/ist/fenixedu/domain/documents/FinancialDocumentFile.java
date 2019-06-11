package pt.ist.fenixedu.domain.documents;

import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;

public class FinancialDocumentFile extends FinancialDocumentFile_Base {
    
    public FinancialDocumentFile(final String displayName, final String fileName, final byte[] content) {
        super();
        init(displayName, fileName, content);
    }

    @Override
    public boolean isAccessible(final User user) {
        final Event event = getFinancialDocument().getEvent();
        return (user != null && event != null && event.getPerson() == user.getPerson())
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_ACCOUNTING_EVENTS).isMember(user);
    }

}
