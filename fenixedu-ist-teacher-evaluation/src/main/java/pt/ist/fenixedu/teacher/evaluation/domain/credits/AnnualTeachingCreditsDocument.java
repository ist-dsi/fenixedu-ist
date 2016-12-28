/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.domain.credits;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.bennu.core.groups.Group;
import pt.ist.fenixedu.teacher.evaluation.domain.CreditsManagerGroup;

public class AnnualTeachingCreditsDocument extends AnnualTeachingCreditsDocument_Base {

    public AnnualTeachingCreditsDocument(AnnualTeachingCredits annualTeachingCredits, byte[] content,
            boolean hasConfidencialInformation) {
        super();
        String filename = getFilename(annualTeachingCredits);

        Group group = RoleType.SCIENTIFIC_COUNCIL.actualGroup();
        final Teacher teacher = annualTeachingCredits.getTeacher();
        if (teacher != null) {
            final Person person = teacher.getPerson();
            if (person != null) {
                group = group.grant(person.getUser());
            }
        }
        if (!hasConfidencialInformation) {
            group = group.or(new CreditsManagerGroup());
        }

        init(filename, filename, content, group);
        setAnnualTeachingCredits(annualTeachingCredits);
        setHasConfidencialInformation(hasConfidencialInformation);
    }

    private String getFilename(AnnualTeachingCredits annualTeachingCredits) {
        return (annualTeachingCredits.getTeacher().getPerson().getUsername() + "_"
                + annualTeachingCredits.getAnnualCreditsState().getExecutionYear().getYear() + ".pdf").replaceAll(" ", "_")
                .replaceAll("/", "_");
    }

}
