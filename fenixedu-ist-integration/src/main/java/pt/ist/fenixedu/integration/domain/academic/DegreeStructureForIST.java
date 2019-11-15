package pt.ist.fenixedu.integration.domain.academic;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularPeriod.CurricularPeriod;
import org.fenixedu.academic.domain.curricularRules.CurricularRuleType;
import org.fenixedu.academic.domain.curricularRules.CurricularRulesManager;
import org.fenixedu.academic.domain.degreeStructure.*;
import org.fenixedu.academic.dto.bolonhaManager.CurricularRuleParametersDTO;
import org.fenixedu.academic.service.services.bolonhaManager.CreateCurricularCourse;

import java.util.HashSet;
import java.util.Set;

public class DegreeStructureForIST {

    public static void resetDegreeCurricularPlan(final DegreeCurricularPlan degreeCurricularPlan) {
        final Set<DegreeModule> toDelete = new HashSet<>();
        final RootCourseGroup rootCourseGroup = degreeCurricularPlan.getRoot();
        rootCourseGroup.getChildContextsSet().stream().forEach(c -> delete(c, toDelete));
        toDelete.forEach(DegreeModule::delete);
        rootCourseGroup.delete();
        final RootCourseGroup newRoot = RootCourseGroup.createRoot(degreeCurricularPlan, degreeCurricularPlan.getName(), degreeCurricularPlan.getName());

        newRoot.getChildContextsSet().stream()
                .map(c -> c.getChildDegreeModule())
                .filter(dm -> dm instanceof CycleCourseGroup)
                .map(dm -> (CycleCourseGroup) dm)
                .filter(ccg -> ccg.getCycleType() == CycleType.FIRST_CYCLE)
                .forEach(ccg -> initFirstCycleDegreeStructure(ccg));
    }

    private static void delete(final CourseGroup courseGroup, final Set<DegreeModule> toDelete) {
        courseGroup.getChildContextsSet().stream().forEach(c -> delete(c, toDelete));
        courseGroup.delete();
        courseGroup.setProgramConclusion(null);
    }

    private static void delete(final Context context, final Set<DegreeModule> toDelete) {
        final DegreeModule degreeModule = context.getChildDegreeModule();
        context.delete();
        if (degreeModule == null) {
        } else if (degreeModule.isCourseGroup()) {
            delete((CourseGroup) degreeModule, toDelete);
        } else {
            toDelete.add(degreeModule);
        }
    }

    public static void initFirstCycleDegreeStructure(final CourseGroup courseGroup) {
        if (courseGroup instanceof CycleCourseGroup) {
            final CycleCourseGroup cycleCourseGroup = (CycleCourseGroup) courseGroup;
            if (cycleCourseGroup.getCycleType() == CycleType.FIRST_CYCLE) {
                final ExecutionSemester semester = tryNext(ExecutionSemester.readActualExecutionSemester());
                final CourseGroup hassGroup = new CourseGroup(courseGroup, "HASS", "HASS", semester, null);
                CurricularRulesManager.createCurricularRule(hassGroup, semester, null, CurricularRuleType.CREDITS_LIMIT, limitDTO(0d, 9d));

                final DegreeCurricularPlan degreeCurricularPlan = courseGroup.getParentDegreeCurricularPlan();

                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 1a", "Option 1a", new int[] { 1, 1 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 1b", "Option 1b", new int[] { 1, 1 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 1c", "Option 1c", new int[] { 1, 1 }, new int[] { 1, 2 });

                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção Ecónomia a", "Option Economy a", new int[] { 2, 2 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção Ecónomia b", "Option Economy b", new int[] { 2, 2 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção Ecónomia c", "Option Economy c", new int[] { 2, 2 }, new int[] { 1, 2 });

                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 2a", "Option 2a", new int[] { 2, 2 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 2b", "Option 2b", new int[] { 2, 2 }, new int[] { 1, 2 });
                createOptionalCurricularCourse(degreeCurricularPlan, hassGroup, semester,
                        "Opção 2c", "Option 2c", new int[] { 2, 2 }, new int[] { 1, 2 });
            }
        }
    }

    private static CurricularCourse createOptionalCurricularCourse(final DegreeCurricularPlan degreeCurricularPlan,
                                                                   final CourseGroup hassGroup, final ExecutionSemester semester,
                                                                   final String namePT, final String nameEN,
                                                                   final int[] curricularYear, final int[] curricularSemester) {
        CurricularCourse curricularCourse = null;
        for (int i = 0; i < curricularYear.length; i++) {
            final CurricularPeriod curricularPeriod = curricularPeriodFor(degreeCurricularPlan, curricularYear[i], curricularSemester[i]);
            if (curricularPeriod == null) {
                throw new NullPointerException("No curricular period found for " + curricularYear[i] + " " + curricularSemester[i]);
            }
            if (curricularCourse == null) {
                curricularCourse = degreeCurricularPlan.createOptionalCurricularCourse(hassGroup, namePT, nameEN,
                        CurricularStage.DRAFT, curricularPeriod, semester, null);
            } else {
                hassGroup.addContext(curricularCourse, curricularPeriod, semester, null);
            }
        }
        return curricularCourse;
    }

    private static CurricularPeriod curricularPeriodFor(final DegreeCurricularPlan degreeCurricularPlan, int year, int semester) {
        final CurricularPeriod curricularPeriod = degreeCurricularPlan.getCurricularPeriodFor(year, semester);
        return curricularPeriod == null ? degreeCurricularPlan.createCurricularPeriodFor(year, semester) : curricularPeriod;
    }

    private static CurricularRuleParametersDTO limitDTO(final double min, final double max) {
        final CurricularRuleParametersDTO limitDTO = new CurricularRuleParametersDTO();
        limitDTO.setMinimumCredits(min);
        limitDTO.setMaximumCredits(max);
        return limitDTO;
    }

    private static ExecutionSemester tryNext(final ExecutionSemester semester) {
        final ExecutionYear year = semester.getExecutionYear();
        final ExecutionYear next = year.getNextExecutionYear();
        return next == null ? semester : next.getFirstExecutionPeriod();
    }

}
