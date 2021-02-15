package pt.ist.fenixedu.teacher.evaluation.domain.contracts;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

public class ServiceExemptionSituation extends ServiceExemptionSituation_Base {
    
    public ServiceExemptionSituation(final Person person, final LocalDate beginDate, final LocalDate endDate, final String description) {
        super();
        setPerson(person);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setDescription(description);
        setRootDomainObject(Bennu.getInstance());
    }
    
}
