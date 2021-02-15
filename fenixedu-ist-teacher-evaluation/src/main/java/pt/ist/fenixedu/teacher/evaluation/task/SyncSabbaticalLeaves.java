package pt.ist.fenixedu.teacher.evaluation.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;

import pt.ist.fenixedu.teacher.evaluation.domain.contracts.NonExerciseSituation;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.OtherServiceExemption;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.Sabbatical;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.ServiceExemptionSituation;
import pt.ist.sap.client.SapStaff;
import pt.ist.sap.group.integration.domain.ColaboratorNonExerciseSituation;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Task(englishTitle = "SyncSabbaticalLeaves", readOnly = false)
public class SyncSabbaticalLeaves extends CronTask {
    int deletedLeaves;
    Map<User, Set<ColaboratorNonExerciseSituation>> colaboratorNonExerciseSituationsMap;

    @Override
    public void runTask() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        int newLeaves = 0;
        deletedLeaves = 0;
        final SapStaff sapStaff = new SapStaff();
        Set<NonExerciseSituation> userNonExerciseSituation = new HashSet<NonExerciseSituation>();

        final JsonObject params = new JsonObject();
        params.addProperty("institution", SapSdkConfiguration.getConfiguration().sapServiceInstitutionCode());

        colaboratorNonExerciseSituationsMap = new HashMap<>();
        sapStaff.listPersonSabaticals(params).forEach(e -> {
            setColaboratorNonExerciseSituationsMap(e);
        });
        newLeaves = updateNonExerciseSituations(colaboratorNonExerciseSituationsMap, userNonExerciseSituation, Sabbatical.class);

        colaboratorNonExerciseSituationsMap = new HashMap<>();
        sapStaff.listServiceExcemptions(params).forEach(e -> {
            setColaboratorNonExerciseSituationsMap(e);
        });
        newLeaves =
                newLeaves
                        + updateNonExerciseSituations(colaboratorNonExerciseSituationsMap, userNonExerciseSituation, ServiceExemptionSituation.class);

        Bennu.getInstance().getNonExerciseSituationSet().forEach(nonExerciseSituation -> {
            if (!(nonExerciseSituation instanceof OtherServiceExemption || userNonExerciseSituation.contains(nonExerciseSituation))) {
                nonExerciseSituation.delete();
                deletedLeaves++;
            }
        });

        taskLog("\nNew: " + newLeaves);
        taskLog("\nDeleted: " + deletedLeaves);
        taskLog("\nTotal: " + Bennu.getInstance().getNonExerciseSituationSet().size());
    }

    private void setColaboratorNonExerciseSituationsMap(JsonElement e) {
        final ColaboratorNonExerciseSituation colaboratorNonExerciseSituation = new ColaboratorNonExerciseSituation(e.getAsJsonObject());
        final User user = User.findByUsername(colaboratorNonExerciseSituation.username().toLowerCase());
        if (user == null) {
            taskLog("\nError: No valid user found for " + colaboratorNonExerciseSituation.username());
        } else {

            Set<ColaboratorNonExerciseSituation> colaboratorNonExerciseSituations = colaboratorNonExerciseSituationsMap.get(user);
            if (colaboratorNonExerciseSituations == null) {
                colaboratorNonExerciseSituations = new HashSet<>();
            }
            colaboratorNonExerciseSituations.add(colaboratorNonExerciseSituation);
            colaboratorNonExerciseSituationsMap.put(user, colaboratorNonExerciseSituations);
        }
    }

    private int updateNonExerciseSituations(Map<User, Set<ColaboratorNonExerciseSituation>> colaboratorNonExerciseSituationsMap,
            Set<NonExerciseSituation> userNonExerciseSituation, Class<?> clazz) throws NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        int newLeaves = 0;
        for (User user : colaboratorNonExerciseSituationsMap.keySet()) {
            Teacher teacher = user.getPerson().getTeacher();
            if (teacher != null) {
                Set<ColaboratorNonExerciseSituation> colaboratorNonExerciseSituationSet = colaboratorNonExerciseSituationsMap.get(user);
                for (ColaboratorNonExerciseSituation colaboratorNonExerciseSituation : colaboratorNonExerciseSituationSet) {
                    LocalDate beginDate = getJsonDate(colaboratorNonExerciseSituation.beginDate());
                    LocalDate endDate = getJsonDate(colaboratorNonExerciseSituation.endDate());

                    NonExerciseSituation nonExerciseSituation = null;
                    nonExerciseSituation =
                            user.getPerson()
                                    .getNonExerciseSituationSet()
                                    .stream()
                                    .filter(se -> clazz.isAssignableFrom(se.getClass())
                                            && se.getBeginDate().equals(beginDate)
                                            && (se instanceof Sabbatical || (StringUtils.isEmpty(((ServiceExemptionSituation) se).getDescription()) ? StringUtils
                                                    .isEmpty(colaboratorNonExerciseSituation.name()) : ((ServiceExemptionSituation) se)
                                                    .getDescription().equals(colaboratorNonExerciseSituation.name())))
                                            && (endDate == null ? se.getEndDate() == null : endDate.equals(se.getEndDate()))).findFirst()
                                    .orElse(null);
                    if (nonExerciseSituation == null) {
                        String description = colaboratorNonExerciseSituation.name();
                        if (clazz.isAssignableFrom(Sabbatical.class)) {
                            description = "Licença Sabática";
                        }

                        Constructor<?> constructor = clazz.getConstructor(Person.class, LocalDate.class, LocalDate.class, String.class);
                        nonExerciseSituation =
                                (NonExerciseSituation) constructor.newInstance(new Object[] { user.getPerson(), beginDate, endDate, description });
                        newLeaves++;
                    }

                    userNonExerciseSituation.add(nonExerciseSituation);
                }
            }
        }
        return newLeaves;
    }

    private LocalDate getJsonDate(String dateString) {
        return Strings.isNullOrEmpty(dateString) ? null : LocalDate.parse(dateString, ISODateTimeFormat.date());
    }

}
