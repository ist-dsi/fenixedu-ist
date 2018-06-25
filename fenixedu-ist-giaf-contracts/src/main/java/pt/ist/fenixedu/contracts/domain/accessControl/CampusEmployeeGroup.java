/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.domain.accessControl;

import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.FenixGroup;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

import pt.ist.sap.group.integration.domain.SapGroup;
import pt.ist.sap.group.integration.domain.SapWrapper;

@GroupOperator("campusEmployee")
public class CampusEmployeeGroup extends FenixGroup {
    private static final long serialVersionUID = 4185082898828533195L;

    private static final String[] SAP_GROUPS = new String[] { " Não Docente", " Dirigentes", " Técnicos e Administ." };

    @GroupArgument
    private Space campus;
    private String sapCampusCode;

    private CampusEmployeeGroup() {
        super();
    }

    private CampusEmployeeGroup(Space campus) {
        this();
        this.campus = campus;
        sapCampusCode = campus == null ? null
                : "Alameda".equals(campus.getName()) ? "Alameda"
                : "Taguspark".equals(campus.getName()) ? "Tagus Park"
                //: "Tecnológico e Nuclear".equals(campus.getName()) ? "N603"
                : null
                ;
        // Other unknown values N201, N203, N301, N603
    }

    public static CampusEmployeeGroup get(Space campus) {
        return new CampusEmployeeGroup(campus);
    }

    @Override
    public String[] getPresentationNameKeyArgs() {
        return new String[] { campus.getName() };
    }

    @Override
    public Stream<User> getMembers() {
        if (sapCampusCode == null) {
            return Stream.empty();
        }
        final SapGroup sapGroup = new SapGroup();
        Iterable<String> result = Collections.emptySet();
        for (final String institution : SapWrapper.institutions) {
            final String institutionCode = SapWrapper.institutionCode.apply(institution);
            for (String sapGroupName : SAP_GROUPS) {
                sapGroup.setGroup(institutionCode + sapGroupName);
                sapGroup.setCampus(institutionCode + " " + sapCampusCode);
                result = Iterables.concat(result, sapGroup.list());
            }
        }
        return StreamSupport.stream(result.spliterator(), false).map(username -> User.findByUsername(username));
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        throw new RuntimeException("information.not.available");
    }

    @Override
    public boolean isMember(User user) {
        if (sapCampusCode == null) {
            return false;
        }
        final SapGroup sapGroup = new SapGroup();
        if (user != null && user.getPerson() != null) {
            for (final String institution : SapWrapper.institutions) {
                final String institutionCode = SapWrapper.institutionCode.apply(institution);
                for (String sapGroupName : SAP_GROUPS) {
                    sapGroup.setGroup(institutionCode + sapGroupName);
                    sapGroup.setCampus(institutionCode + " " + sapCampusCode);
                    if (sapGroup.isMember(user.getUsername())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        throw new RuntimeException("information.not.available");
    }

    public boolean isMember(final Person person, final Space campus, DateTime when) {
        throw new RuntimeException("information.not.available");
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentCampusEmployeeGroup.getInstance(campus);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof CampusEmployeeGroup && Objects.equal(campus, ((CampusEmployeeGroup) object).campus);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(campus);
    }
}
