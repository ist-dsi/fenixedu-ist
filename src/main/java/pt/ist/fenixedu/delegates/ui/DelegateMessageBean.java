/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.util.email.Recipient;
import org.fenixedu.bennu.core.groups.Group;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateMessageBean {

    List<Group> selectedAdditionalGroups;
    List<Group> availableAdditionalGroups;
    Delegate selectedSender;
    List<Recipient> recipients;

    public DelegateMessageBean(Delegate delegate) {
        selectedSender = delegate;
        recipients = new ArrayList<Recipient>();
        selectedAdditionalGroups = new ArrayList<Group>();
        availableAdditionalGroups = new ArrayList<Group>();
    }

    public DelegateMessageBean(DelegateStudentSelectBean delegateStudentSelectBean) {
        selectedSender = delegateStudentSelectBean.getSelectedPosition();
        recipients = delegateStudentSelectBean.getRecipients();
        selectedAdditionalGroups = new ArrayList<Group>();
        availableAdditionalGroups = new ArrayList<Group>();
    }

    public DelegateMessageBean(Set<Delegate> delegates) {
        recipients = new ArrayList<Recipient>();
        selectedAdditionalGroups = new ArrayList<Group>();
        availableAdditionalGroups = new ArrayList<Group>();
    }

    public DelegateMessageBean() {
        recipients = new ArrayList<Recipient>();
        selectedAdditionalGroups = new ArrayList<Group>();
        availableAdditionalGroups = new ArrayList<Group>();
    }

    public Delegate getSelectedSender() {
        return selectedSender;
    }

    public void setSelectedSender(Delegate selectedSender) {
        this.selectedSender = selectedSender;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

}
