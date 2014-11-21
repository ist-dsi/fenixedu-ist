package pt.ist.fenixedu.libraryattendance.space;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.space.SpaceUtils;
import org.joda.time.DateTime;

public class SpaceAttendances extends SpaceAttendances_Base {

    public SpaceAttendances(String personUsername, String responsibleUsername, DateTime entranceTime) {
        this.setPersonUsername(personUsername);
        this.setResponsibleForEntranceUsername(responsibleUsername);
        this.setEntranceTime(entranceTime);
    }

    public String getOccupationDesctiption() {
        if (getOccupiedLibraryPlace() != null && SpaceUtils.isRoomSubdivision(getOccupiedLibraryPlace())) {
            return getOccupiedLibraryPlace().getName();
        }
        return "-";
    }

    public Person getPerson() {
        return Person.readPersonByUsername(getPersonUsername());
    }

    public void exit(String responsibleUsername) {
        if (getOccupiedLibraryPlace() != null) {
            setResponsibleForExitUsername(responsibleUsername);
            setExitTime(new DateTime());
            setOccupiedLibraryPlace(null);
        }
    }

    public void delete() {
        setOccupiedLibraryPlace(null);
        setVisitedLibraryPlace(null);
        deleteDomainObject();
    }

}
