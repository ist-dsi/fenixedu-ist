/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Pre Bolonha.
 *
 * FenixEdu IST Pre Bolonha is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Pre Bolonha is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Pre Bolonha.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.service;

import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.serviceAgreements.DegreeCurricularPlanServiceAgreement;
import org.fenixedu.academic.domain.candidacy.Candidacy;
import org.fenixedu.academic.domain.candidacy.DFACandidacy;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.phd.candidacy.PHDProgramCandidacy;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserLoginPeriod;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;

public class CreateCandidacy {

    @Atomic
    public static Candidacy run(ExecutionDegree executionDegree, DegreeType degreeType, String givenNames, String familyNames,
            String identificationDocumentNumber, IDDocumentType identificationDocumentType, String contributorNumber,
            YearMonthDay startDate) {

        Person person = Person.readByDocumentIdNumberAndIdDocumentType(identificationDocumentNumber, identificationDocumentType);
        if (person == null) {
            UserProfile profile = new UserProfile(givenNames, familyNames, null, null, Locale.getDefault());
            new User(profile);
            person = new Person(profile);
            person.setGender(Gender.MALE);
            person.setIdentification(identificationDocumentNumber, identificationDocumentType);
        }

        person.setSocialSecurityNumber(contributorNumber);

        if (person.getStudent() == null) {
            new Student(person);
        }

        UserLoginPeriod.createOpenPeriod(person.getUser());

        Candidacy candidacy = newCandidacy(degreeType, person, executionDegree, startDate);

        new DegreeCurricularPlanServiceAgreement(person, executionDegree.getDegreeCurricularPlan().getServiceAgreementTemplate());

        return candidacy;

    }

    public static Candidacy newCandidacy(DegreeType degreeType, Person person, ExecutionDegree executionDegree,
            YearMonthDay startDate) throws DomainException {

        switch (degreeType) {
        case BOLONHA_ADVANCED_SPECIALIZATION_DIPLOMA:
            // TODO: remove this after PHD Program candidacy is completed and
            // data migrated
            return new PHDProgramCandidacy(person, executionDegree);
        case BOLONHA_ADVANCED_FORMATION_DIPLOMA:
            return new DFACandidacy(person, executionDegree, startDate);

        default:
            throw new DomainException("error.candidacyFactory.invalid.degree.type");
        }
    }

}