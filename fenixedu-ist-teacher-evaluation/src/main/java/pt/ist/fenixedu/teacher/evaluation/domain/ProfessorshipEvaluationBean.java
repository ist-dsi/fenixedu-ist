package pt.ist.fenixedu.teacher.evaluation.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;

import com.google.common.base.Joiner;

import pt.ist.fenixedu.teacher.evaluation.domain.teacher.DegreeTeachingService;
import pt.ist.fenixedu.teacher.evaluation.service.external.ProfessorshipEvaluation;

public class ProfessorshipEvaluationBean implements Serializable, Comparable<ProfessorshipEvaluationBean> {

    private Professorship professorship;
    private String shiftTypesPrettyPrint;
    private BigDecimal hours;
    private Integer enrolmentsNumber;
    private String description;
    public static ProfessorshipEvaluation professorshipEvaluation = null;
    public Double professorshipEvaluationValue;

    public ProfessorshipEvaluationBean(Professorship professorship, List<DegreeTeachingService> degreeTeachingServices) {
        this.professorship = professorship;
        hours = new BigDecimal(degreeTeachingServices.stream().mapToDouble(d -> d.getEfectiveLoad()).sum()).setScale(2,
                BigDecimal.ROUND_HALF_UP);
        enrolmentsNumber = degreeTeachingServices.stream().mapToInt(d -> d.getShift().getStudentsSet().size()).sum();

        shiftTypesPrettyPrint = degreeTeachingServices.iterator().next().getShift().getShiftTypesPrettyPrint();
        String shiftTypesString = Joiner.on(", ").join(
                degreeTeachingServices.iterator().next().getShift().getSortedTypes().stream().map(st -> st.getName()).iterator());
        ShiftType shiftType = ShiftType.valueOf(shiftTypesString);
        if (shiftType != null) {
            professorshipEvaluationValue = professorshipEvaluation.getProfessorshipEvaluation(getProfessorship(), shiftType);
        }
        this.description = Joiner.on(", ").join(getProfessorship().getExecutionCourse().getExecutionPeriod().getQualifiedName(),
                getProfessorship().getExecutionCourse().getName(),
                getProfessorship().getExecutionCourse().getDegreePresentationString(), shiftTypesPrettyPrint);
    }

    public static Set<ProfessorshipEvaluationBean> getProfessorshipEvaluationBeanSet(Professorship professorship) {
        Set<ProfessorshipEvaluationBean> result = new TreeSet<ProfessorshipEvaluationBean>();
        Map<SortedSet<ShiftType>, List<DegreeTeachingService>> degreeTeachingServiceMap = professorship
                .getDegreeTeachingServicesSet().stream().collect(Collectors.groupingBy(dts -> dts.getShift().getSortedTypes()));
        degreeTeachingServiceMap
                .forEach((shidtTypeSet, dtsSet) -> result.add(new ProfessorshipEvaluationBean(professorship, dtsSet)));
        return result;
    }

    public Professorship getProfessorship() {
        return professorship;
    }

    public void setProfessorship(Professorship professorship) {
        this.professorship = professorship;
    }

    public String getShiftTypesPrettyPrint() {
        return shiftTypesPrettyPrint;
    }

    public void setShiftTypesPrettyPrint(String shiftTypesPrettyPrint) {
        this.shiftTypesPrettyPrint = shiftTypesPrettyPrint;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public Integer getEnrolmentsNumber() {
        return enrolmentsNumber;
    }

    public void setEnrolmentsNumber(Integer enrolmentsNumber) {
        this.enrolmentsNumber = enrolmentsNumber;
    }

    public static ProfessorshipEvaluation getProfessorshipEvaluation() {
        return professorshipEvaluation;
    }

    public static void setProfessorshipEvaluation(ProfessorshipEvaluation professorshipEvaluation) {
        ProfessorshipEvaluationBean.professorshipEvaluation = professorshipEvaluation;
    }

    public Double getProfessorshipEvaluationValue() {
        return professorshipEvaluationValue;
    }

    public void setProfessorshipEvaluationValue(Double professorshipEvaluationValue) {
        this.professorshipEvaluationValue = professorshipEvaluationValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int compareTo(ProfessorshipEvaluationBean o) {
        int compare = getProfessorship().getExecutionCourse().getExecutionPeriod()
                .compareTo(o.getProfessorship().getExecutionCourse().getExecutionPeriod());
        if (compare == 0) {
            compare = Collator.getInstance().compare(getProfessorship().getExecutionCourse().getNome(),
                    o.getProfessorship().getExecutionCourse().getNome());
            if (compare == 0) {
                compare = getProfessorship().getExecutionCourse().getDegreePresentationString()
                        .compareTo(o.getProfessorship().getExecutionCourse().getDegreePresentationString());
            }
        }
        return compare == 0 ? getShiftTypesPrettyPrint().compareTo(o.getShiftTypesPrettyPrint()) : compare;
    }

}
