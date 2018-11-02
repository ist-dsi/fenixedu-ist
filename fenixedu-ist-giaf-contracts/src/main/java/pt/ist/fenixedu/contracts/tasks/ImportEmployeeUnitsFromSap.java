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
import pt.ist.sap.client.SapStructure;
import pt.ist.sap.group.integration.domain.Colaborator;
import pt.ist.sap.group.integration.domain.ColaboratorSituation;
import pt.ist.sap.group.integration.domain.Situation;

@Task(englishTitle = "ImportEmployeeUnitsFromSap")
public class ImportEmployeeUnitsFromSap extends CronTask {

    private static final List<String> SAP_GROUPS = Arrays.asList("Docentes", "Não Docente", "Dirigentes", "Técnicos e Administ.",
            "Bolseiros", "Bols. Investigação", "Investigadores");

    @Override
    public void runTask() {
        final SapStructure sapStructure = new SapStructure();
        final SapStaff sapStaff = new SapStaff();

        final JsonObject input = new JsonObject();
        input.addProperty("institution", SapSdkConfiguration.getConfiguration().sapServiceInstitutionCode());

        Map<User, ColaboratorSituation> colaboratorSituationsMap = getColaboratorSituationsMap(sapStaff, input);

        Set<EmployeeContract> activeWorkingContracts = new HashSet<EmployeeContract>();
        YearMonthDay today = new YearMonthDay();

        sapStructure.listPeople(input).forEach(je ->

        {
            final Colaborator colaborator = new Colaborator(je.getAsJsonObject());
            final User user = User.findByUsername(SapSdkConfiguration.usernameProvider().toUsername(colaborator.sapId()));
            final Person person = user == null ? null : user.getPerson();
            if (person == null) {
                taskLog("Skipping colaborator %s because there is no user.%n", colaborator.sapId());
            } else {
                if (!SAP_GROUPS.contains(colaborator.group())) {
                    taskLog("Skipping colaborator %s because there is no group: %s.%n", colaborator.sapId(), colaborator.group());
                } else {
                    Unit unit = getUnit(colaborator.costCenter());
                    if (unit == null) {
                        taskLog("Skipping colaborator %s because there is no cost center: %s.%n", colaborator.sapId(),
                                colaborator.costCenter());
                    } else {
                        Contract workingContractOnDate =
                                getLastContractByContractType(person, AccountabilityTypeEnum.WORKING_CONTRACT, today);
                        ColaboratorSituation colaboratorSituation = colaboratorSituationsMap.get(user);
                        if (workingContractOnDate != null) {
                            if (colaboratorSituation != null) {
                                if (!workingContractOnDate.getUnit().equals(unit)) {
                                    activeWorkingContracts.add(createEmployeeContract(person, today, unit,
                                            AccountabilityTypeEnum.WORKING_CONTRACT, workingContractOnDate));
                                } else {
                                    activeWorkingContracts.add((EmployeeContract) workingContractOnDate);
                                }
                            } else {
                                closeCurrentContract(AccountabilityTypeEnum.WORKING_CONTRACT, workingContractOnDate,
                                        today.minusDays(1));
                            }
                        } else if (colaboratorSituation != null) {
                            activeWorkingContracts.add(
                                    createEmployeeContract(person, today, unit, AccountabilityTypeEnum.WORKING_CONTRACT, null));
                        }

                    }
                }
            }
        });

        for (Accountability accountability : Bennu.getInstance().getAccountabilitysSet()) {
            if (accountability.getAccountabilityType().getType() == AccountabilityTypeEnum.WORKING_CONTRACT
                    && accountability.isActive() && !activeWorkingContracts.contains(accountability)) {
                closeCurrentContract(AccountabilityTypeEnum.WORKING_CONTRACT, (Contract) accountability, today.minusDays(1));
            }
        }
    }

    private Map<User, ColaboratorSituation> getColaboratorSituationsMap(SapStaff sapStaff, JsonObject input) {
        Set<Situation> situations = new HashSet<Situation>();
        sapStaff.listProfessionalSituation(input).forEach(e -> {
            situations.add(new Situation(e.getAsJsonObject()));
        });

        Map<User, ColaboratorSituation> colaboratorSituationsMap = new HashMap<User, ColaboratorSituation>();
        sapStaff.listPersonProfessionalInformation(input).forEach(e -> {
            final ColaboratorSituation colaboratorSituation = new ColaboratorSituation(e.getAsJsonObject());
            final User user = User.findByUsername(colaboratorSituation.username().toLowerCase());
            if (isActive(colaboratorSituation)) {
                if (user == null) {
                    taskLog("\nError: No valid user found for " + colaboratorSituation.username());
                } else {
                    colaboratorSituationsMap.put(user, colaboratorSituation);
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

    private Contract getLastContractByContractType(Person person, AccountabilityTypeEnum contractType, YearMonthDay begin) {
        YearMonthDay date = null;
        Contract contractToReturn = null;
        for (final Contract accountability : (Collection<Contract>) person.getParentAccountabilities(contractType,
                EmployeeContract.class)) {
            if (accountability.belongsToPeriod(begin, null)) {
                if (date == null || accountability.getBeginDate().isAfter(date)) {
                    date = accountability.getBeginDate();
                    contractToReturn = accountability;
                }
            }
        }
        return contractToReturn;
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
            final AccountabilityTypeEnum accountabilityTypeEnum, final Contract currentWorkingContract) {
        if (currentWorkingContract != null) {
            closeCurrentContract(accountabilityTypeEnum, currentWorkingContract, today.minusDays(1));
        }
        taskLog(accountabilityTypeEnum.getName() + ". NEW contract - unit: " + unit.getPresentationName() + " user:"
                + person.getUsername() + " begin date:" + today);
        return new EmployeeContract(person, today, null, unit, accountabilityTypeEnum, false);
    }

}