package pt.ist.fenixedu.teacher.evaluation.domain.contracts;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class OtherServiceExemption extends OtherServiceExemption_Base {

    public OtherServiceExemption(final Person person, final LocalDate beginDate, final LocalDate endDate, final String description) {
        super();
        setPerson(person);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setDescription(description);
        setRootDomainObject(Bennu.getInstance());
    }

    public static OtherServiceExemption create(Person person, LocalDate beginDate, LocalDate endDate, String description) {
        if (person == null || beginDate == null || Strings.isNullOrEmpty(description)) {
            throw new DomainException("message.otherServiceExemption.emptyMandatoryFields");
        }
        NonExerciseSituation existingSituation =
                person.getNonExerciseSituationSet()
                        .stream()
                        .filter(se -> se instanceof OtherServiceExemption && se.getBeginDate().equals(beginDate)
                                && se.getDescription().equals(description)).findFirst().orElse(null);
        if (existingSituation != null) {
            existingSituation.setEndDate(endDate);
            return (OtherServiceExemption) existingSituation;
        }

        return new OtherServiceExemption(person, beginDate, endDate, description);
    }

}
