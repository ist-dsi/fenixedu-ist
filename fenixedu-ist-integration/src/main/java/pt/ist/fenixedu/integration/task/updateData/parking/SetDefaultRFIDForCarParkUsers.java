package pt.ist.fenixedu.integration.task.updateData.parking;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.idcards.domain.SantanderCardInfo;

import org.fenixedu.idcards.domain.SantanderEntry;
import pt.ist.fenixedu.parking.domain.ParkingParty;

@Task(englishTitle = "Set default RFID for car park users")
public class SetDefaultRFIDForCarParkUsers extends CronTask {

    @Override
    public void runTask() throws Exception {
        for (final User user : Bennu.getInstance().getUserSet()) {
            final Person person = user.getPerson();
            if (person != null) {
                final ParkingParty parkingParty = person.getParkingParty();
                if (parkingParty != null
                        && (parkingParty.getCardNumber() == null || parkingParty.getCardNumber().longValue() == 0l
                                || isOutDatedValue(person, parkingParty.getCardNumber()))) {
                    final String rfid = getLastMifareSerialNumber(person);
                    if (rfid != null) {
                        parkingParty.setCardNumber(Long.valueOf(rfid.trim()));
                    }
                }
            }
        }
    }

    private boolean isOutDatedValue(final Person person, final Long cardNumber) {
        final String rfid = getLastMifareSerialNumber(person);
        if (rfid ==  null) {
            return false;
        }
        final Long lastValue = Long.valueOf(rfid);
        final Stream<SantanderCardInfo> infos = SantanderEntry.getSantanderEntryHistory(person.getUser()).stream()
                .map(e -> e.getSantanderCardInfo());
        return !cardNumber.equals(lastValue) && infos.filter(c -> c.getMifareNumber() != null)
                .map(c -> Long.valueOf(c.getMifareNumber())).anyMatch(l -> l.equals(cardNumber));
    }

    private static String getLastMifareSerialNumber(final Person person) {
        return SantanderEntry.getLastMifareNumber(person.getUser());
    }
}
