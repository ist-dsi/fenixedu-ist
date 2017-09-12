package org.fenixedu.ulisboa.integration.sas.webservices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.integration.sas.dto.ActiveDegreeBean;
import org.fenixedu.ulisboa.integration.sas.dto.CycleBean;

import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;

@WebService
public class ActiveDegreesWebService extends BennuWebService {
    //We need a placeholder to represent courses without a related school level
    static final String FREE_COURSES_CODE = "XXXXX";
    private static final String FREE_COURSES_DESIGNATION = "Formação Livre";

    @WebMethod
    public Collection<ActiveDegreeBean> getActiveDegrees() {
        return populateActiveDegrees();
    }

    //Consider moving this logic to a different place
    private Collection<ActiveDegreeBean> populateActiveDegrees() {
        Predicate<Degree> hasSchoolLevel = degree -> !degree.getDegreeType().isEmpty();
        List<ActiveDegreeBean> collect =
                Bennu.getInstance().getDegreesSet().stream().filter(hasSchoolLevel.and(Degree::isActive))
                        .map(d -> populateActiveDegree(d)).collect(Collectors.toList());
        collect.add(getFreeCoursesPlaceholder());

        return collect;
    }

    private ActiveDegreeBean populateActiveDegree(Degree degree) {
        ActiveDegreeBean activeDegreeBean = new ActiveDegreeBean();

        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

        activeDegreeBean.setDegreeCode(degree.getCode());
        activeDegreeBean.setDesignation(normalizeString(degree.getNameFor(currentExecutionYear).getContent(Locale.getDefault())));

        activeDegreeBean.setSchoolLevel("TODO");
        //TODO analyse how to represent a degree with multiple cycles        
        activeDegreeBean.setCycles(getDegreeCycles(degree));

        activeDegreeBean.setDuration(Integer.toString(getDegreeDuration(degree, currentExecutionYear)));

        activeDegreeBean.setOficialCode(degree.getMinistryCode());
        return activeDegreeBean;
    }

    // The degree duration must be calculted from a degree curricular plan, not from a degree
    // In this case we are assuming that diferent curricular plans for the same degree will have the same duration
    // This is usually valid since changing the duration of a degree may imply the creation of a new degree (with a different degree code)
    private int getDegreeDuration(Degree degree, ExecutionYear currentExecutionYear) {
        List<DegreeCurricularPlan> degreeCurricularPlansForYear = degree.getDegreeCurricularPlansForYear(currentExecutionYear);
        if (degreeCurricularPlansForYear.isEmpty()) {
            System.out.println("Degree " + degree.getName()
                    + " has no degree curricular plans for the current execution year. Unable to calculate duration");
            return 0;
        }
        return degreeCurricularPlansForYear.iterator().next().getDurationInYears();
    }

    private List<CycleBean> getDegreeCycles(Degree degree) {
        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        List<DegreeCurricularPlan> degreeCurricularPlansForYear = degree.getDegreeCurricularPlansForYear(currentExecutionYear);
        DegreeCurricularPlan currentDegreeCurricularPlan = null;
        if (!degreeCurricularPlansForYear.isEmpty()) {
            currentDegreeCurricularPlan = degreeCurricularPlansForYear.get(0);
        }
        Collection<CycleType> cycleTypes = degree.getDegreeType().getCycleTypes();
        List<CycleBean> values = new ArrayList<CycleBean>();

        int i = 0;
        for (CycleType ct : cycleTypes) {
            // This "if" check is only valid in tests with inconsistent data, 
            // we should not be taking care of degrees without degrees without curricular plans for the current year
            int duration;
            if (currentDegreeCurricularPlan != null) {
                duration = currentDegreeCurricularPlan.getDurationInYears(ct);
            } else {
                //Value for test purposes only
                duration = 3;
            }

            values.add(new CycleBean("" + ct.getWeight(), duration));
        }

        return values;
    }

    //Some schools may have degrees with fully capitalized names. Normalize it 
    private String normalizeString(String string) {
        if (!StringUtils.isEmpty(string)) {
            String[] split = string.split(" ");
            String output = "";
            for (int i = 0; i < split.length; i++) {
                if (i != 0) {
                    output += " ";
                }
                String part = split[i];
                if (part.length() > 0) {
                    output += part.substring(0, 1).toUpperCase();
                }

                if (part.length() > 1) {
                    output += part.substring(1, part.length()).toLowerCase();
                }
            }
            return output;
        }
        return "";
    }

    private ActiveDegreeBean getFreeCoursesPlaceholder() {
        ActiveDegreeBean freeCoursesPlaceHolder = new ActiveDegreeBean();

        freeCoursesPlaceHolder.setDegreeCode(FREE_COURSES_CODE);
        freeCoursesPlaceHolder.setDesignation(FREE_COURSES_DESIGNATION);

        //Empty values
        freeCoursesPlaceHolder.setOficialCode("");
        freeCoursesPlaceHolder.setCycles(Collections.<CycleBean> emptyList());
        freeCoursesPlaceHolder.setDuration("-1");
        freeCoursesPlaceHolder.setSchoolLevel(SchoolLevelType.UNKNOWN.getLocalizedName());

        return freeCoursesPlaceHolder;
    }

}