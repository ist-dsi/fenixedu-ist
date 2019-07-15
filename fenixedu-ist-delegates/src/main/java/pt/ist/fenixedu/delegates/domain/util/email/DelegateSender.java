/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.util.email;

import org.fenixedu.academic.domain.Installation;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.messaging.core.domain.MessageStoragePolicy;
import pt.ist.fenixedu.delegates.domain.student.Delegate;

import java.util.stream.Collectors;

public class DelegateSender extends DelegateSender_Base {

    public DelegateSender() {
        super();
    }

    public DelegateSender(Delegate delegate) {
        super();
        setName(delegate.getUser().getPerson().getName() + " (" + delegate.getTitle() + ")");
        setAddress(Installation.getInstance().getInstituitionalEmailAddress("noreply"));
        setMembers(delegate.getUser().groupOf());
        setReplyTo(delegate.getUser().getPerson().getDefaultEmailAddressValue());
        setPolicy(MessageStoragePolicy.keepAll());
    }

    @Override
    public Group getMembers() {
        if (getDelegateSet().iterator().next().isActive()) {
            return super.getMembers();
        }
        return Group.nobody();
    }

}
