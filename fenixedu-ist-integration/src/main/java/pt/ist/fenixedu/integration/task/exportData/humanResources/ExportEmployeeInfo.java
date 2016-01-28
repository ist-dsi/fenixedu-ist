package pt.ist.fenixedu.integration.task.exportData.humanResources;

import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
import pt.ist.fenixedu.contracts.domain.research.Researcher;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Task(englishTitle = "Export working relations to shared json file with other applications.")
public class ExportEmployeeInfo extends CustomTask {

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
                final Researcher researcher = person.getResearcher();
                if (employee != null && isActiveContractedTeacher(person)) {
                    registerContractSituation(result, user, person, employee, CategoryType.TEACHER);
                }
                if (researcher != null && researcher.isActiveContractedResearcher()) {
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

    public boolean isActiveContractedTeacher(final Person person) {
        return getCurrentContractedTeacherContractSituation(person) != null;
    }

    public PersonContractSituation getCurrentContractedTeacherContractSituation(final Person person) {
        final PersonProfessionalData data = person.getPersonProfessionalData();
        return data != null ? data.getCurrentPersonContractSituationByCategoryType(CategoryType.TEACHER) : null;
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
        object.addProperty("wp", StringUtils.leftPad(wp, 4, '0'));
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
