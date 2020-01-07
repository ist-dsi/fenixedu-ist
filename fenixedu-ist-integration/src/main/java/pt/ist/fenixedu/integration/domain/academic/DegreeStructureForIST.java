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
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.I18N;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DegreeStructureForIST {

    public static void resetDegreeCurricularPlan(final DegreeCurricularPlan degreeCurricularPlan) {
        final Set<DegreeModule> toDelete = new HashSet<>();
        final RootCourseGroup rootCourseGroup = degreeCurricularPlan.getRoot();
        rootCourseGroup.getChildContextsSet().stream().forEach(c -> delete(c, toDelete));
        toDelete.forEach(DegreeStructureForIST::delete);
        rootCourseGroup.delete();
        final RootCourseGroup newRoot = RootCourseGroup.createRoot(degreeCurricularPlan, degreeCurricularPlan.getName(), degreeCurricularPlan.getName());

        newRoot.getChildContextsSet().stream()
                .map(c -> c.getChildDegreeModule())
                .filter(dm -> dm instanceof CycleCourseGroup)
                .map(dm -> (CycleCourseGroup) dm)
                .filter(ccg -> ccg.getCycleType() == CycleType.FIRST_CYCLE || ccg.getCycleType() == CycleType.SECOND_CYCLE)
                .forEach(ccg -> initFirstCycleDegreeStructure(ccg));
    }

    private static void delete(final DegreeModule degreeModule) {
        if (degreeModule.isCourseGroup()) {
            final CourseGroup courseGroup = (CourseGroup) degreeModule;
            courseGroup.setProgramConclusion(null);
        }
        degreeModule.delete();
    }

    private static void delete(final Context context, final Set<DegreeModule> toDelete) {
        final DegreeModule degreeModule = context.getChildDegreeModule();
        if (degreeModule == null) {
        } else if (degreeModule.isCourseGroup()) {
            ((CourseGroup) degreeModule).getChildContextsSet().stream().forEach(c -> delete(c, toDelete));
            toDelete.add(degreeModule);
        } else {
            toDelete.add(degreeModule);
        }
        context.delete();
    }

    private static void initFirstCycleDegreeStructure(final CourseGroup courseGroup) {
        if (courseGroup instanceof CycleCourseGroup) {
            final CycleCourseGroup cycleCourseGroup = (CycleCourseGroup) courseGroup;
            final ExecutionSemester semester = tryNext(ExecutionSemester.readActualExecutionSemester());
            if (cycleCourseGroup.getCycleType() == CycleType.FIRST_CYCLE) {
                createGroup(courseGroup, "label.degree.structure.base.education", semester, 0d, 60d);
                createGroup(courseGroup, "label.degree.structure.hass", semester, 0d, 9d);
                createGroup(courseGroup, "label.degree.structure.main.education", semester, 87d, 111d);
                createGroup(courseGroup, "label.degree.structure.pre.major", semester, 0d, 9d);
                createGroup(courseGroup, "label.degree.structure.integration.project", semester, 6d, 12d);
            }
            if (cycleCourseGroup.getCycleType() == CycleType.SECOND_CYCLE) {
                createGroup(courseGroup, "label.degree.structure.main.education", semester, 0d, 60d);
                final CourseGroup optionsGroup = createGroup(courseGroup, "label.degree.structure.options", semester, 18d, 30d);
                createGroup(optionsGroup, "label.degree.structure.minor", semester, 0d, 18d);
                createGroup(courseGroup, "label.degree.structure.dissertation", semester, 0d, 42d);
            }
        }
    }

    private static CourseGroup createGroup(final CourseGroup parent, final String nameKey, final ExecutionSemester semester,
                                    double minCredits, double maxCredits) {
        final Locale pt = new Locale("pt");
        final Locale en = new Locale("en");
        final CourseGroup result = new CourseGroup(parent,
                BundleUtil.getString("resources.FenixeduIstIntegrationResources", pt, nameKey),
                BundleUtil.getString("resources.FenixeduIstIntegrationResources", en, nameKey),
                semester, null);
        CurricularRulesManager.createCurricularRule(result, semester, null,
                CurricularRuleType.CREDITS_LIMIT, limitDTO(minCredits, maxCredits));
        return result;
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
                        CurricularStage.DRAFT, curricularPeriod, null, semester, null);
            } else {
                hassGroup.addContext(curricularCourse, curricularPeriod, null, semester, null);
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
        return next == null ? semester : tryNext(next.getFirstExecutionPeriod());
    }

}
