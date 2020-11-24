package pt.ist.fenixedu.contracts.tasks;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Accountability;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.Contract;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.EmployeeContract;
import pt.ist.sap.client.SapStaff;
import pt.ist.sap.group.integration.domain.ColaboratorSituation;

@Task(englishTitle = "ImportEmployeeUnitsFromSap")
public class ImportEmployeeUnitsFromSap extends CronTask {

    private static final List<String> SAP_GROUPS = Arrays.asList("Docentes", "Não Docente", "Dirigentes", "Técnicos e Administ.",
            "Bolseiros", "Bols. Investigação", "Investigadores");

    @Override
    public void runTask() {
        final SapStaff sapStaff = new SapStaff();

        final JsonObject input = new JsonObject();
        input.addProperty("institution", SapSdkConfiguration.getConfiguration().sapServiceInstitutionCode());

        Map<User, Set<ColaboratorSituation>> colaboratorSituationsMap = getColaboratorSituationsMap(sapStaff, input);

        Set<EmployeeContract> activeWorkingContracts = new HashSet<EmployeeContract>();
        YearMonthDay today = new YearMonthDay();

        for (User user : colaboratorSituationsMap.keySet()) {
            for (ColaboratorSituation colaboratorSituation : colaboratorSituationsMap.get(user)) {
                final Person person = user == null ? null : user.getPerson();
                if (person == null) {
                    taskLog("Skipping colaborator %s because there is no user.%n", colaboratorSituation.username());
                } else {
                    if (!SAP_GROUPS.contains(colaboratorSituation.categoryTypeName())) {
                        taskLog("Skipping colaborator %s because there is no group: %s.%n", colaboratorSituation.username(),
                                colaboratorSituation.categoryTypeName());
                    } else {
                        Unit unit = getUnit(colaboratorSituation.costcenter());
                        if (unit == null) {
                            taskLog("Skipping colaborator %s because there is no cost center: %s.%n",
                                    colaboratorSituation.username(), colaboratorSituation.costcenter());
                        } else {
                            Set<Contract> workingContractOnDateSet =
                                    getLastContractByContractType(person, AccountabilityTypeEnum.WORKING_CONTRACT, today);

                            EmployeeContract existingContractOnUnit = null;
                            for (Contract workingContractOnDate : workingContractOnDateSet) {
                                if (workingContractOnDate.getUnit().equals(unit)) {
                                    existingContractOnUnit = (EmployeeContract) workingContractOnDate;
                                }
                            }
                            if (existingContractOnUnit == null) {
                                activeWorkingContracts.add(
                                        createEmployeeContract(person, today, unit, AccountabilityTypeEnum.WORKING_CONTRACT));
                            } else {
                                activeWorkingContracts.add(existingContractOnUnit);
                            }
                        }
                    }
                }
            }
        }

        for (Accountability accountability : Bennu.getInstance().getAccountabilitysSet()) {
            if (EmployeeContract.class.isAssignableFrom(accountability.getClass())
                    && accountability.getAccountabilityType().getType() == AccountabilityTypeEnum.WORKING_CONTRACT
                    && accountability.isActive() && !activeWorkingContracts.contains(accountability)) {
                closeCurrentContract(AccountabilityTypeEnum.WORKING_CONTRACT, (Contract) accountability, today.minusDays(1));
            }
        }
    }

    private Map<User, Set<ColaboratorSituation>> getColaboratorSituationsMap(SapStaff sapStaff, JsonObject input) {
        Map<User, Set<ColaboratorSituation>> colaboratorSituationsMap = new HashMap<User, Set<ColaboratorSituation>>();
        sapStaff.listPersonProfessionalInformation(input).forEach(e -> {
            final ColaboratorSituation colaboratorSituation = new ColaboratorSituation(e.getAsJsonObject());
            final User user = User.findByUsername(colaboratorSituation.username().toLowerCase());
            if (colaboratorSituation.inExercise() && isActive(colaboratorSituation)) {
                if (user == null) {
                    taskLog("\nError: No valid user found for " + colaboratorSituation.username());
                } else {
                    Set<ColaboratorSituation> colaboratorSituationSet = colaboratorSituationsMap.get(user);
                    if (colaboratorSituationSet == null) {
                        colaboratorSituationSet = new HashSet<ColaboratorSituation>();
                    }
                    colaboratorSituationSet.add(colaboratorSituation);
                    colaboratorSituationsMap.put(user, colaboratorSituationSet);
                }
            }
        });
        return colaboratorSituationsMap;
    }

    private boolean isActive(ColaboratorSituation colaboratorSituation) {
        if (!Strings.isNullOrEmpty(colaboratorSituation.beginDate()) && !Strings.isNullOrEmpty(colaboratorSituation.endDate())) {
            return new Interval(LocalDate.parse(colaboratorSituation.beginDate()).toDateTimeAtStartOfDay(),
                    LocalDate.parse(colaboratorSituation.endDate()).toDateTimeAtStartOfDay()).containsNow();
        }
        return Strings.isNullOrEmpty(colaboratorSituation.beginDate())
                || LocalDate.parse(colaboratorSituation.beginDate()).toDateTimeAtStartOfDay().isBeforeNow();
    }

    private Unit getUnit(String costCenter) {
        Integer costCenterCode = null;
        if (!Strings.isNullOrEmpty(costCenter)) {
            try {
                costCenterCode = Integer.parseInt(costCenter.substring(2));
            } catch (NumberFormatException e) {
            }
        }
        return Unit.readByCostCenterCode(costCenterCode);
    }

    private Set<Contract> getLastContractByContractType(Person person, AccountabilityTypeEnum contractType, YearMonthDay begin) {
        Set<Contract> result = new HashSet<Contract>();
        for (final Contract accountability : (Collection<Contract>) person.getParentAccountabilities(contractType,
                EmployeeContract.class)) {
            if (accountability.belongsToPeriod(begin, null)) {
                result.add(accountability);
            }
        }
        return result;
    }

    private void closeCurrentContract(final AccountabilityTypeEnum accountabilityTypeEnum, final Contract currentWorkingContract,
            final YearMonthDay endDate) {
        if (currentWorkingContract.getBeginDate().isAfter(endDate)) {
            taskLog(accountabilityTypeEnum.getName() + ". DELETED contract - unit:"
                    + currentWorkingContract.getUnit().getPresentationName() + " user:"
                    + currentWorkingContract.getPerson().getUsername());
            currentWorkingContract.delete();
        } else {
            currentWorkingContract.setEndDate(endDate);
            taskLog(accountabilityTypeEnum.getName() + ". CLOSED contract - unit: "
                    + currentWorkingContract.getUnit().getPresentationName() + "  user:"
                    + currentWorkingContract.getPerson().getUsername() + " end date :" + endDate);
        }
    }

    private EmployeeContract createEmployeeContract(final Person person, final YearMonthDay today, final Unit unit,
            final AccountabilityTypeEnum accountabilityTypeEnum) {
        taskLog(accountabilityTypeEnum.getName() + ". NEW contract - unit: " + unit.getPresentationName() + " user:"
                + person.getUsername() + " begin date:" + today);
        return new EmployeeContract(person, today, null, unit, accountabilityTypeEnum, false);
    }
}