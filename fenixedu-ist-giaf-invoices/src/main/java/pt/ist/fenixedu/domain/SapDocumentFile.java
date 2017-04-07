package pt.ist.fenixedu.domain;

import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.bennu.core.domain.User;

public class SapDocumentFile extends SapDocumentFile_Base {

    public SapDocumentFile(String filename, byte[] content) {
        super();
        init(filename, filename, content);
    }

    @Override
    public boolean isAccessible(User user) {
        User personDocuments =
                getSapRequest().getEvent().getPerson() != null ? getSapRequest().getEvent().getPerson().getUser() : null;
        return (personDocuments != null && personDocuments == user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV).isMember(user);
    }

    @Override
    public void delete() {
        setSapRequest(null);
        super.delete();
    }
}
