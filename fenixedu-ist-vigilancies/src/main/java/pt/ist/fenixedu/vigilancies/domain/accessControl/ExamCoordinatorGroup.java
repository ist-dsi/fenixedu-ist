/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.domain.accessControl;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.GroupStrategy;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

@GroupOperator("examCoordinator")
public class ExamCoordinatorGroup extends GroupStrategy {

    private static final long serialVersionUID = 9028259183901043270L;

    @Override
    public String getPresentationName() {
        return BundleUtil.getString("resources.VigilancyResources", "label.group.name.ExamCoordinatorGroup");
    }

    @Override
    public Set<User> getMembers() {
        return Bennu.getInstance().getExamCoordinatorsSet().stream()
                .filter(coordinator -> coordinator.getExecutionYear().equals(ExecutionYear.readCurrentExecutionYear()))
                .map(coordinator -> coordinator.getPerson().getUser()).collect(Collectors.toSet());
    }

    @Override
    public Set<User> getMembers(DateTime when) {
        return getMembers();
    }

    @Override
    public boolean isMember(User user) {
        return user != null
                && user.getPerson() != null
                && user.getPerson().getExamCoordinatorsSet().stream()
                        .filter(coordinator -> coordinator.getExecutionYear().equals(ExecutionYear.readCurrentExecutionYear()))
                        .findAny().isPresent();
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return isMember(user);
    }

}
