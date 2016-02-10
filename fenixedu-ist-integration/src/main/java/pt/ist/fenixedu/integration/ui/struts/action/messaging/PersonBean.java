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
package pt.ist.fenixedu.integration.ui.struts.action.messaging;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.contacts.WebAddress;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.domain.occupation.SharedOccupation;

import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageSite;
import pt.ist.fenixedu.contracts.domain.Employee;

public class PersonBean {
    private Person person;

    public List<WebAddress> getWebAddresses() {
        return person.getWebAddresses();
    }

    public List<MobilePhone> getMobilePhones() {
        return person.getMobilePhones();
    }

    public List<EmailAddress> getEmailAddresses() {
        return person.getEmailAddresses();
    }

    public List<Phone> getPhones() {
        return person.getPhones();
    }

    public String getUsername() {
        return person.getUsername();
    }

    public String getName() {
        return person.getName();
    }

    public PersonBean(Person p) {
        person = p;
    }

    public Site getSite() { return person.getHomepage(); }

    public HomepageSite getHomepage() {
        return person.getHomepage().getHomepageSite();
    }

    public Teacher getTeacher() {
        return person.getTeacher();
    }

    public Employee getEmployee() {
        return person.getEmployee();
    }

    public Student getStudent() {
        return person.getStudent();
    }

    public User getUser() {
        return person.getUser();
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getExternalId() {
        return person.getExternalId();
    }

    public Set<Space> getActivePersonSpaces() {
        Set<Space> toRet = new HashSet<Space>();
        for (SharedOccupation so : person.getUser().getSharedOccupationSet()) {
            if (so.isActive() && so.getSpaces().iterator().hasNext()) {
                toRet.add(so.getSpaces().iterator().next());
            }
        }
        return toRet;
    }

}
