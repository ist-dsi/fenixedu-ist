package pt.ist.fenixedu.contracts.tasks;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;

@Task(englishTitle = "Update Retired Teachers Group", readOnly = false)
public class UpdateRetiredTeachersGroup extends CronTask {

    @Override
    public void runTask() throws Exception {
        Set<ContractSituation> retirementSituations = Bennu.getInstance().getContractSituationsSet().stream()
                .filter(this::isRetiredSituation).collect(Collectors.toSet());
        LocalDate today = new LocalDate();

        Set<User> retiredTeachersUsers = Bennu.getInstance().getGiafProfessionalDataSet().stream()
                .filter(giafData -> giafData.getPersonProfessionalData().getPerson() != null
                        && isUsersWithRetirementTeacherSituations(giafData, retirementSituations, today))
                .map(giafData -> giafData.getPersonProfessionalData().getPerson().getUser()).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Group actualRetiredTeachersGroup = Group.users(retiredTeachersUsers.stream());
        DynamicGroup.get("retiredTeachers").mutator().changeGroup(actualRetiredTeachersGroup);

    }

    public boolean isUsersWithRetirementTeacherSituations(GiafProfessionalData giafProfessionalData,
            Set<ContractSituation> retirementSituations, LocalDate today) {
        for (PersonContractSituation personContractSituation : giafProfessionalData.getPersonContractSituationsSet()) {
            if (personContractSituation.isValid() && personContractSituation.getAnulationDate() == null
                    && personContractSituation.isActive(today)
                    && (personContractSituation.getProfessionalCategory() != null
                            && personContractSituation.getProfessionalCategory().getCategoryType().equals(CategoryType.TEACHER))
                    && retirementSituations.contains(personContractSituation.getContractSituation())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRetiredSituation(final ContractSituation cs) {
        final String name = cs.getName().getContent();
        return name.matches(".*pose.*") || name.matches(".*eform.*") || name.matches(".*ubil.*") || name.matches(".*mérito.*");
    }
}
