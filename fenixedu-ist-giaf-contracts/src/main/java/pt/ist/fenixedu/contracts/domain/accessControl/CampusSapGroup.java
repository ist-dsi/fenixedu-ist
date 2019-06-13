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
import java.util.Objects;
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

import com.google.common.collect.Iterables;

import pt.ist.sap.group.integration.domain.SapGroup;
import pt.ist.sap.group.integration.domain.SapWrapper;

public abstract class CampusSapGroup extends FenixGroup {
    private static final long serialVersionUID = 4185082898828533195L;


    public abstract String[] getSapGroups();

//    private static final String[] SAP_GROUPS = new String[] { " Não Docente", " Dirigentes", " Técnicos e Administ." };

    @GroupArgument
    protected Space campus;
    protected String sapCampusCode;

    protected CampusSapGroup() {
        super();
    }

    protected CampusSapGroup(Space campus) {
        this.campus = campus;
    }

    private String getSapCampusCode() {
        if (this.sapCampusCode == null) {
            if (campus != null) {
                switch (campus.getExternalId()) {
                case "2448131360897" : this.sapCampusCode = "Alameda"; break;
                case "2448131360898" : this.sapCampusCode = "Tagus Park"; break;
                case "2448131392438" : this.sapCampusCode = "CTN"; break;
                // Other unknown values N201, N203, N301, N603
                }
            }
        }
        return this.sapCampusCode;
    }

    @Override
    public String[] getPresentationNameKeyArgs() {
        return new String[] { campus.getName() };
    }

    @Override
    public Stream<User> getMembers() {
        if (getSapCampusCode() == null) {
            return Stream.empty();
        }
        final SapGroup sapGroup = new SapGroup();
        Iterable<String> result = Collections.emptySet();
        for (final String institution : SapWrapper.institutions) {
            final String institutionCode = SapWrapper.institutionCode.apply(institution);
            for (String sapGroupName : getSapGroups()) {
                sapGroup.setGroup(institutionCode + sapGroupName);
                sapGroup.setCampus(institutionCode + " " + getSapCampusCode());
                result = Iterables.concat(result, sapGroup.list());
            }
        }
        return StreamSupport.stream(result.spliterator(), false).map(User::findByUsername);
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        throw new RuntimeException("information.not.available");
    }

    @Override
    public boolean isMember(User user) {
        if (getSapCampusCode() == null) {
            return false;
        }
        final SapGroup sapGroup = new SapGroup();
        if (user != null && user.getPerson() != null) {
            for (final String institution : SapWrapper.institutions) {
                final String institutionCode = SapWrapper.institutionCode.apply(institution);
                for (String sapGroupName : getSapGroups()) {
                    sapGroup.setGroup(institutionCode + sapGroupName);
                    sapGroup.setCampus(institutionCode + " " + getSapCampusCode());
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CampusSapGroup that = (CampusSapGroup) o;

        if (!Objects.equals(campus, that.campus)) {
            return false;
        }
        if (!Objects.equals(sapCampusCode, that.sapCampusCode)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = campus != null ? campus.hashCode() : 0;
        result = 31 * result + (sapCampusCode != null ? sapCampusCode.hashCode() : 0);
        return result;
    }
}
