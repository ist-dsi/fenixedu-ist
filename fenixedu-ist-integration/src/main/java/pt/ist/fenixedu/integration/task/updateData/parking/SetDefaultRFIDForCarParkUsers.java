package pt.ist.fenixedu.integration.task.updateData.parking;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixedu.parking.domain.ParkingParty;

@Task(englishTitle = "Set default RFID for car park users")
public class SetDefaultRFIDForCarParkUsers extends CronTask {

    @Override
    public void runTask() throws Exception {
     /*   for (final User user : Bennu.getInstance().getUserSet()) {
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
        }*/
    }

    /*private boolean isOutDatedValue(final Person person, final Long cardNumber) {
        final String rfid = getLastMifareSerialNumber(person);
        if (rfid ==  null) {
            return false;
        }
        final Long lastValue = Long.valueOf(rfid.trim());
        final Stream<SantanderCardInformation> infos = person.getSantanderCardsInformationSet().stream();
        return !cardNumber.equals(lastValue) && infos.map(i -> i.getDchpRegisteLine()).filter(l -> l != null)
                .map(l -> Long.valueOf(getMifareSerialNumber(l).trim())).anyMatch(l -> l.equals(cardNumber));
    }

    private static String getLastMifareSerialNumber(final Person person) {
        final Stream<SantanderCardInformation> infos = person.getSantanderCardsInformationSet().stream();
        final String line =
                infos.map(i -> i.getDchpRegisteLine()).max(SetDefaultRFIDForCarParkUsers::compareDHCPLines).orElse(null);
        return line == null ? null : getMifareSerialNumber(line);
    }

    private static String getMifareSerialNumber(String line) {
        final int offset = line.length() - 550 - 1;
        return line.substring(offset - 10, offset);
    }

    private static int compareDHCPLines(final String l1, String l2) {
        return l1.substring(1, 9).compareTo(l2.substring(1, 9));
    }*/

}
