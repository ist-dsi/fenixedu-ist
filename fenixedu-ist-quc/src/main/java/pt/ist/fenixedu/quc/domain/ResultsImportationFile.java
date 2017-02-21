package pt.ist.fenixedu.quc.domain;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.DynamicGroup;

public class ResultsImportationFile extends ResultsImportationFile_Base {
    
    public ResultsImportationFile(String filename, byte[] content) {
        super();
        init(filename, filename, content);
    }

    @Override
    public boolean isAccessible(User user) {
        return DynamicGroup.get("#gep").isMember(user);
    }
}
