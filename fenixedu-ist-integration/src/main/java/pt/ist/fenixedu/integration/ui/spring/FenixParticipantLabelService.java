/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.spring;

import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.thesis.domain.ThesisProposalParticipant;
import org.fenixedu.academic.thesis.ui.service.ParticipantLabelService;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.springframework.stereotype.Service;

import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;

@Service
public class FenixParticipantLabelService implements ParticipantLabelService {

    @Override
    public String getInstitutionRole(ThesisProposalParticipant participant) {
        User user = participant.getUser();
        Teacher teacher = user.getPerson().getTeacher();

        if (teacher != null && teacher.getTeacherAuthorization().isPresent()) {
            return BundleUtil.getString("resources.FenixEduThesisProposalsResources", "label.participant.active.teacher");
        }

        if (new ActiveResearchers().isMember(user)) {
            return BundleUtil.getString("resources.FenixEduThesisProposalsResources", "label.participant.contracted.researcher");
        }

        return null;
    }

}
