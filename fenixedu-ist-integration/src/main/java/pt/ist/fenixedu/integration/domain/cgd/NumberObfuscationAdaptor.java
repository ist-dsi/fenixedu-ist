package pt.ist.fenixedu.integration.domain.cgd;

import java.time.Year;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;

import com.qubit.solution.fenixedu.integration.cgd.webservices.resolver.memberid.IMemberIDAdapter;

public class NumberObfuscationAdaptor implements IMemberIDAdapter {

    @Override
    public String retrieveMemberID(final Person person) {
        final User user = person.getUser();
        return CgdCardCounter.getNextSerialNumber(user);
    }

    @Override
    public Person readPerson(final String memberId) {
        final CgdCardCounter counter = CgdCardCounter.findCounterForYear(CgdCard.yearFor(memberId));
        final int serialNumber = CgdCard.serialNumberFor(memberId);
        return counter == null ? null : counter.getCgdCardSet().stream().filter(c -> c.getSerialNumber() == serialNumber)
                .map(c -> c.getUser().getPerson()).findAny().orElse(null);
    }

    @Override
    public boolean isAllowedAccessToMember(final Person person) {
        final User user = person.getUser();
        if (user != null) {
            final int year = Year.now().getValue();
            for (final CgdCard card : user.getCgdCardSet()) {
                if (card.getAllowSendDetails() && card.getCgdCardCounter().getYear() == year) {
                    return true;
                }
            }
        }
        return false;
    }

}
