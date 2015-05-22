/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Parking.
 *
 * FenixEdu IST Parking is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Parking is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.parking.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.StringNormalizer;

import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ProfessionalCategory;

public class ParkingRequestSearch implements Serializable {
    private ParkingRequestState parkingRequestState;

    private PartyClassification partyClassification;

    private String personName;

    private String carPlateNumber;

    private List<ParkingRequest> searchResult;

    public PartyClassification getPartyClassification() {
        return partyClassification;
    }

    public void setPartyClassification(PartyClassification partyClassification) {
        this.partyClassification = partyClassification;
    }

    public ParkingRequestState getParkingRequestState() {
        return parkingRequestState;
    }

    public void setParkingRequestState(ParkingRequestState parkingRequestState) {
        this.parkingRequestState = parkingRequestState;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getCarPlateNumber() {
        return carPlateNumber;
    }

    public void setCarPlateNumber(String carPlateNumber) {
        this.carPlateNumber = carPlateNumber;
    }

    public void doSearch() {
        final List<ParkingRequest> parkingRequests = new ArrayList<ParkingRequest>();

        if (personName != null && !personName.isEmpty()) {
            final Collection<Person> people = Person.findPerson(personName);
            for (final Person person : people) {
                final ParkingParty parkingParty = person.getParkingParty();
                if (parkingParty != null) {
                    for (final ParkingRequest parkingRequest : parkingParty.getParkingRequestsSet()) {
                        if (satisfiedPersonClassification(parkingRequest) && satisfiedRequestState(parkingRequest)
                                && satisfiedCarPlateNumber(parkingRequest)) {
                            parkingRequests.add(parkingRequest);
                        }
                    }
                }
            }
        } else if (parkingRequestState != null || partyClassification != null || carPlateNumber != null) {
            for (ParkingRequest request : Bennu.getInstance().getParkingRequestsSet()) {
                if (satisfiedPersonClassification(request) && satisfiedPersonName(request) && satisfiedRequestState(request)
                        && satisfiedCarPlateNumber(request)) {
                    parkingRequests.add(request);
                }
            }
        }

        setSearchResult(parkingRequests);
    }

    private boolean satisfiedCarPlateNumber(ParkingRequest request) {
        if (org.apache.commons.lang.StringUtils.isEmpty(getCarPlateNumber())) {
            return true;
        }
        return request.hasVehicleContainingPlateNumber(getCarPlateNumber());
    }

    private boolean satisfiedRequestState(ParkingRequest request) {
        return getParkingRequestState() == null || request.getParkingRequestState() == getParkingRequestState();
    }

    private boolean satisfiedPersonClassification(ParkingRequest request) {
        final ParkingParty parkingParty = request.getParkingParty();
        if (getPartyClassification() != null) {
            DegreeType degreeType = PartyClassification.degreeTypeFor(getPartyClassification());
            if (degreeType != null && request.getRequestedAs() != null && request.getRequestedAs().equals(RoleType.STUDENT)) {
                final Student student = ((Person) parkingParty.getParty()).getStudent();
                if (degreeType.isAdvancedSpecializationDiploma()) {
                    for (PhdIndividualProgramProcess phdIndividualProgramProcess : student.getPerson()
                            .getPhdIndividualProgramProcessesSet()) {
                        if (phdIndividualProgramProcess.getActiveState().isPhdActive()) {
                            return true;
                        }
                    }
                }
                return student.getActiveRegistrations().stream().anyMatch(reg -> reg.getDegreeType().equals(degreeType));
            } else if (PartyClassification.getPartyClassification(parkingParty.getParty()) == getPartyClassification()) {
                if (getPartyClassification() == PartyClassification.TEACHER) {
                    final Teacher teacher = ((Person) parkingParty.getParty()).getTeacher();
                    return teacher == null
                            || !ProfessionalCategory.isMonitor(teacher, ExecutionSemester.readActualExecutionSemester());
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean satisfiedPersonName(ParkingRequest request) {
        return org.apache.commons.lang.StringUtils.isEmpty(getPersonName())
                || verifyContainsWithEquality(request.getParkingParty().getParty().getName(), getPersonName());
    }

    public static boolean verifyContainsWithEquality(String originalString, String stringToCompare) {
        if (originalString == null || stringToCompare == null) {
            return false;
        }
        String[] stringOriginalArray = getStrings(originalString);
        String[] stringToCompareArray = getStrings(stringToCompare);

        if (stringToCompareArray == null) {
            return true;
        }

        if (stringOriginalArray != null) {
            int j, i;
            for (i = 0; i < stringToCompareArray.length; i++) {
                if (!stringToCompareArray[i].equals("")) {
                    for (j = 0; j < stringOriginalArray.length; j++) {
                        if (stringOriginalArray[j].equals(stringToCompareArray[i])) {
                            break;
                        }
                    }
                    if (j == stringOriginalArray.length) {
                        return false;
                    }
                }
            }
            if (i == stringToCompareArray.length) {
                return true;
            }
        }
        return false;
    }

    private static String[] getStrings(String string) {
        String[] strings = null;
        if (string != null && !string.trim().equals("")) {
            strings = string.trim().split(" ");
            return Arrays.stream(strings).map(StringNormalizer::normalize).toArray(String[]::new);
        }
        return strings;
    }

    public List<ParkingRequest> getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(List<ParkingRequest> result) {
        this.searchResult = result;
    }
}
