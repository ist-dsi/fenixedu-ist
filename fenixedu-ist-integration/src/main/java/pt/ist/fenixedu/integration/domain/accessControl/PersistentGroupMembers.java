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
package pt.ist.fenixedu.integration.domain.accessControl;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.messaging.core.domain.Sender;
import pt.ist.fenixedu.integration.domain.UnitFile;
import pt.ist.fenixframework.Atomic;

import java.util.Collection;

public class PersistentGroupMembers extends PersistentGroupMembers_Base {

    public PersistentGroupMembers(String name, PersistentGroupMembersType type) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setName(name);
        setType(type);
        checkIfPersistenGroupAlreadyExists(name, type);
    }

    public void edit(String name, PersistentGroupMembersType type) {
        setName(name);
        setType(type);
        checkIfPersistenGroupAlreadyExists(name, type);
    }

    @Override
    public void setUnit(Unit unit) {
        super.setUnit(unit);
        updateUnitSenders();
    }

    public void delete() {
        if (getMembersLinkGroup() != null) {
            throw new DomainException("error.persistentGroupMembers.cannotDeletePersistentGroupMembersUsedInAccessControl");
        }
        getPersonsSet().clear();
        if (getUnit() != null) {
            MembersLinkGroup group = MembersLinkGroup.get(this);
            for (UnitFile file : getUnit().getFilesSet()) {
                file.updatePermissions(group);
            }
        }
        setUnit(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public void setNewPersonToMembersList(Person person) {
        if (person == null) {
            throw new DomainException("error.PersistentGroupMembers.empty.person");
        }
        addPersons(person);
    }

    @Override
    public void removePersons(Person person) {
        super.removePersons(person);
    }

    // This method is only used for Renderers
    public Person getNewPersonToMembersList() {
        return null;
    }

    @Override
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new DomainException("error.PersistentGroupMembers.empty.name");
        }
        super.setName(name);
    }

    @Override
    public void setType(PersistentGroupMembersType type) {
        if (type == null) {
            throw new DomainException("error.PersistentGroupMembers.empty.type");
        }
        super.setType(type);
    }

    private void checkIfPersistenGroupAlreadyExists(String name, PersistentGroupMembersType type) {
        Collection<PersistentGroupMembers> persistentGroupMembers = Bennu.getInstance().getPersistentGroupMembersSet();
        for (PersistentGroupMembers persistentGroup : persistentGroupMembers) {
            if (!persistentGroup.equals(this) && persistentGroup.getName().equalsIgnoreCase(name)
                    && persistentGroup.getType().equals(type)) {
                throw new DomainException("error.PersistentGroupMembers.group.already.exists");
            }
        }
    }

    @Atomic
    private void updateUnitSenders() {
        Sender sender = getUnit().getSender();
        for (PersistentGroupMembers group : getUnit().getPersistentGroupsSet()) {
            if (!hasRecipientWithToName(sender, group.getName())) {
                sender.addRecipient(MembersLinkGroup.get(group));
            }
        }
    }

    private boolean hasRecipientWithToName(Sender sender, final String toName) {
        return sender.getRecipients().stream().anyMatch(r -> r.getPresentationName().equals(toName));
    }
}
