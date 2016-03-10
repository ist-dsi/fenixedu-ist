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
package pt.ist.fenixedu.integration.task.exportData.humanResources;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;

@Task(englishTitle = "Export working relations to shared json file with other applications.")
public class ExportEmployeeInfo extends CronTask {

    @Override
    public void runTask() throws Exception {
        // Pre-load all necessary data
        preloadData();

        // Chew everything into a json furrball
        JsonArray result = new JsonArray();
        for (final User user : Bennu.getInstance().getUserSet()) {
            final Person person = user.getPerson();
            if (person != null && user.getProfile() != null) {
                final Employee employee = person.getEmployee();
                if (employee != null && getCurrentContractedContractSituation(person, CategoryType.TEACHER) != null) {
                    registerContractSituation(result, user, person, employee, CategoryType.TEACHER);
                }
                if (employee != null && getCurrentContractedContractSituation(person, CategoryType.RESEARCHER) != null) {
                    registerContractSituation(result, user, person, employee, CategoryType.RESEARCHER);
                }
                if (employee != null && employee.isActive()) {
                    registerContractSituation(result, user, person, employee, CategoryType.EMPLOYEE);
                }
                if (employee != null && isGrantOwner(employee)) {
                    registerContractSituation(result, user, person, employee, CategoryType.GRANT_OWNER);
                }
            }
        }

        // Spit it out
        final byte[] bytes = result.toString().getBytes();
        output("employeeInfo.json", bytes);
        try (final FileOutputStream fos = new FileOutputStream("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/employeeInfo.json")) {
            fos.write(bytes);
        }
    }

    public PersonContractSituation getCurrentContractedContractSituation(final Person person, final CategoryType categoryType) {
        final PersonProfessionalData data = person.getPersonProfessionalData();
        return data != null ? data.getCurrentPersonContractSituationByCategoryType(categoryType) : null;
    }

    private void preloadData() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Bennu.getInstance().getUserSet();
        loadProfiles(Bennu.getInstance());
        Bennu.getInstance().getTeachersSet();
        Bennu.getInstance().getResearchersSet();
        Bennu.getInstance().getEmployeesSet();
        Bennu.getInstance().getPartyTypesSet();
    }

    private boolean isGrantOwner(final Employee employee) {
        final PersonProfessionalData data = employee.getPerson().getPersonProfessionalData();
        if (data != null) {
            final GiafProfessionalData giafProfessionalData =
                    data.getGiafProfessionalDataByCategoryType(CategoryType.GRANT_OWNER);
            return giafProfessionalData != null && giafProfessionalData.isActive();
        }
        return false;
    }

    private void registerContractSituation(final JsonArray result, final User user, final Person person, final Employee employee,
            final CategoryType type) {
        final String employer = determineEmployer(person, type);
        if (employer != null) {
            final Unit workingPlace = employee.getCurrentWorkingPlace();
            if (workingPlace != null) {
                register(result, user, type, workingPlace.getCostCenterCode().toString(), employer);
            }
        }
    }

    private void register(final JsonArray result, final User user, final CategoryType type, final String wp, final String employer) {
        final JsonObject object = new JsonObject();
        object.addProperty("user", user.getUsername());
        object.addProperty("role", type.name());
        object.addProperty("wp", Strings.padStart(wp, 4, '0'));
        object.addProperty("employer", employer);
        result.add(object);
    }

    private String determineEmployer(final Person person, final CategoryType type) {
        final PersonProfessionalData data = person.getPersonProfessionalData();
        if (data != null) {
            final GiafProfessionalData giafProfessionalData =
                    type == null ? null : data.getGiafProfessionalDataByCategoryType(type);
            return giafProfessionalData == null ? null : giafProfessionalData.getEmployer();

        }
        return null;
    }

    private void loadProfiles(Bennu b) throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        final Method method = b.getClass().getMethod("getProfileSet");
        method.setAccessible(true);
        method.invoke(b);
    }

}
