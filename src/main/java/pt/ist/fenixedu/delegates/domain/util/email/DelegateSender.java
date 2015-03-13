/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Delegates.
 *
 * FenixEdu Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.util.email;

import org.fenixedu.academic.domain.util.email.CurrentUserReplyTo;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.groups.NobodyGroup;
import org.fenixedu.bennu.core.groups.UserGroup;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateSender extends DelegateSender_Base {

    public DelegateSender() {
        super();
    }

    public DelegateSender(Delegate delegate) {
        setFromName(delegate.getUser().getPerson().getName() + " (" + delegate.getTitle() + ")");
        setFromAddress(getNoreplyMail());
        setMembers(UserGroup.of(delegate.getUser()));
        setRootDomainObject(Bennu.getInstance());
        addReplyTos(new CurrentUserReplyTo());
    }

    @Override
    public Group getMembers() {
        if (getDelegateSet().iterator().next().isActive()) {
            return super.getMembers();
        }
        return NobodyGroup.get();
    }

}
