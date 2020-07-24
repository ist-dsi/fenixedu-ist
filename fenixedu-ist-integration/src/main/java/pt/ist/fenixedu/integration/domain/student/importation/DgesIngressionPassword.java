package pt.ist.fenixedu.integration.domain.student.importation;

import org.fenixedu.bennu.core.domain.Bennu;

public class DgesIngressionPassword extends DgesIngressionPassword_Base {
    
    public DgesIngressionPassword(String dgesPassword) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setDgesPassword(dgesPassword);
    }
    
}
