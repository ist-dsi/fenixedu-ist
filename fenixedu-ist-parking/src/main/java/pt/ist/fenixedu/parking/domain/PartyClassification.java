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

import java.util.function.Predicate;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;

import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.Invitation;

public enum PartyClassification {
    TEACHER, EMPLOYEE, RESEARCHER, GRANT_OWNER, MASTER_DEGREE(DegreeType::isPreBolonhaMasterDegree),
    DEGREE(DegreeType::isPreBolonhaDegree), BOLONHA_SPECIALIZATION_DEGREE(DegreeType::isSpecializationDegree),
    BOLONHA_ADVANCED_FORMATION_DIPLOMA(DegreeType::isAdvancedFormationDiploma),
    BOLONHA_MASTER_DEGREE(DegreeType::isBolonhaMasterDegree),
    BOLONHA_INTEGRATED_MASTER_DEGREE(DegreeType::isIntegratedMasterDegree),
    BOLONHA_ADVANCED_SPECIALIZATION_DIPLOMA(DegreeType::isAdvancedSpecializationDiploma),
    BOLONHA_DEGREE(DegreeType::isBolonhaDegree), INVITATION, PERSON, UNIT;

    private final Predicate<DegreeType> mapping;

    private PartyClassification() {
        this.mapping = type -> false;
    }

    private PartyClassification(Predicate<DegreeType> mapping) {
        this.mapping = mapping;
    }

    public static DegreeType degreeTypeFor(PartyClassification classification) {
        return DegreeType.matching(classification.mapping).orElse(null);
    }

    public static PartyClassification getClassificationByDegreeType(DegreeType degreeType) {
        for (PartyClassification classification : values()) {
            if (classification.mapping.test(degreeType)) {
                return classification;
            }
        }
        return null;
    }

    public static PartyClassification getPartyClassification(Party party) {
        if (party instanceof Unit) {
            return PartyClassification.UNIT;
        }
        if (party instanceof Person) {
            Person person = (Person) party;
            final Teacher teacher = person.getTeacher();
            if (teacher != null) {
                if (new ActiveTeachersGroup().isMember(person.getUser())) {
                    return PartyClassification.TEACHER;
                }
            }
            if (new ActiveEmployees().isMember(person.getUser())) {
                return PartyClassification.EMPLOYEE;
            }
            if (new ActiveResearchers().isMember(person.getUser())) {
                return PartyClassification.RESEARCHER;
            }
            if (new ActiveGrantOwner().isMember(person.getUser())) {
                return PartyClassification.GRANT_OWNER;
            }
            if (person.getStudent() != null) {
                final DegreeType degreeType = ParkingParty.mostSignificantDegreeType(person.getStudent());
                if (degreeType != null) {
                    return PartyClassification.getClassificationByDegreeType(degreeType);
                }
            }
            if (!Invitation.getActiveInvitations(person).isEmpty()) {
                return PartyClassification.INVITATION;
            }

            return PartyClassification.PERSON;
        }
        return PartyClassification.UNIT;
    }

    public static Integer getMostSignificantNumber(Person person) {
        PartyClassification classification = getPartyClassification(person);
        if (classification.equals(PartyClassification.TEACHER)) {
            if (person.getEmployee() != null) {
                return person.getEmployee().getEmployeeNumber();
            }
        }
        if (classification.equals(PartyClassification.EMPLOYEE)) {
            return person.getEmployee().getEmployeeNumber();
        }
        if (classification.equals(PartyClassification.RESEARCHER) && person.getEmployee() != null) {
            return person.getEmployee().getEmployeeNumber();
        }
        if (person.getStudent() != null) {
            return person.getStudent().getNumber();
        }
        if (classification.equals(PartyClassification.GRANT_OWNER) && person.getEmployee() != null) {
            return person.getEmployee().getEmployeeNumber();
        }
        return 0;
    }

}
