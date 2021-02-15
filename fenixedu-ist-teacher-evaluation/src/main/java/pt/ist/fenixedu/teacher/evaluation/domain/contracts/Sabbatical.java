package pt.ist.fenixedu.teacher.evaluation.domain.contracts;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

public class Sabbatical extends Sabbatical_Base {

    public Sabbatical(final Person person, final LocalDate beginDate, final LocalDate endDate, final String description) {
        super();
        setPerson(person);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setDescription(description);
        setRootDomainObject(Bennu.getInstance());
    }
}
