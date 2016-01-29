/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.domain.teacher;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.ReductionServiceBean;

public class ReductionService extends ReductionService_Base {

    public ReductionService(final TeacherService teacherService, final BigDecimal creditsReduction) {
        super();
        setRootDomainObject(Bennu.getInstance());
        if (teacherService == null) {
            throw new DomainException("arguments can't be null");
        }
        if (teacherService.getReductionService() != null) {
            throw new DomainException("error.already.requested.reduction");
        }
        setTeacherService(teacherService);
        setCreditsReduction(creditsReduction);
    }

    public ReductionService(final TeacherService teacherService, final Boolean requestCreditsReduction) {
        super();
        setRootDomainObject(Bennu.getInstance());
        if (teacherService == null) {
            throw new DomainException("arguments can't be null");
        }
        if (teacherService.getReductionService() != null) {
            throw new DomainException("error.already.requested.reduction");
        }
        setTeacherService(teacherService);
        setRequestCreditsReduction(requestCreditsReduction);
    }

    public ReductionService(final BigDecimal creditsReductionAttributed, final TeacherService teacherService) {
        super();
        setRootDomainObject(Bennu.getInstance());
        if (teacherService == null) {
            throw new DomainException("arguments can't be null");
        }
        if (teacherService.getReductionService() != null) {
            throw new DomainException("error.already.requested.reduction");
        }
        setTeacherService(teacherService);
        setCreditsReductionAttributed(creditsReductionAttributed);
    }

    @Override
    public void setCreditsReduction(BigDecimal creditsReduction) {
        checkCredits(creditsReduction);
        BigDecimal maxCreditsFromEvaluationAndAge = getMaxCreditsFromEvaluationAndAge();
        if (creditsReduction.compareTo(maxCreditsFromEvaluationAndAge) > 0) {
            throw new DomainException("label.creditsReduction.exceededMaxAllowed.evaluationAndAge",
                    maxCreditsFromEvaluationAndAge.toString());
        }
        super.setCreditsReduction(creditsReduction);
        Department lastDepartment = getDepartment();
        setPendingApprovalFromDepartment(lastDepartment);
        log("label.teacher.schedule.reductionService.edit", getCreditsReduction());
    }

    private Department getDepartment() {
        return getTeacherService().getTeacher().getLastDepartment(getTeacherService().getExecutionPeriod().getAcademicInterval());
    }

    @Override
    public void setRequestCreditsReduction(Boolean requestCreditsReduction) {
        checkTeacherCategory();
        super.setRequestCreditsReduction(requestCreditsReduction);
        Department lastDepartment = requestCreditsReduction ? getDepartment() : null;
        setPendingApprovalFromDepartment(lastDepartment);
        log("label.teacher.schedule.reductionService.edit", getRequestCreditsReduction());
    }

    @Override
    public void setCreditsReductionAttributed(BigDecimal creditsReductionAttributed) {
        checkCredits(creditsReductionAttributed);
        super.setCreditsReductionAttributed(creditsReductionAttributed);
        setAttributionDate(new DateTime());
        setPendingApprovalFromDepartment(null);
        log("label.teacher.schedule.reductionService.approve", getCreditsReductionAttributed());
    }

    private void checkCredits(BigDecimal creditsReduction) {
        if (creditsReduction == null) {
            creditsReduction = BigDecimal.ZERO;
        }
        checkTeacherCategory();
        BigDecimal maxCreditsReduction = getMaxCreditsReduction();
        if (creditsReduction.compareTo(maxCreditsReduction) > 0) {
            throw new DomainException("label.creditsReduction.exceededMaxAllowed", maxCreditsReduction.toString());
        }
    }

    private void checkTeacherCategory() {
        boolean isTeacherProfessorCategory =
                getTeacherService().getTeacher().getCategory(getTeacherService().getExecutionPeriod().getAcademicInterval())
                        .map(tc -> tc.getProfessionalCategory()).map(pc -> pc.isTeacherProfessorCategory()).orElse(false);
        if (!isTeacherProfessorCategory) {
            throw new DomainException("label.creditsReduction.invalidCategory");
        }
    }

    public BigDecimal getMaxCreditsFromEvaluationAndAge() {
        ReductionServiceBean reductionServiceBean = new ReductionServiceBean(this);
        return reductionServiceBean.getMaxCreditsFromEvaluationAndAge();
    }

    private BigDecimal getMaxCreditsReduction() {
        ReductionServiceBean reductionServiceBean = new ReductionServiceBean(this);
        return reductionServiceBean.getMaxCreditsReduction();
    }

    private void log(final String key, BigDecimal credits) {
        final StringBuilder log = new StringBuilder();
        log.append(BundleUtil.getString("resources.TeacherCreditsSheetResources", key));
        log.append(credits);
        new TeacherServiceLog(getTeacherService(), log.toString());
    }

    private void log(final String key, Boolean requested) {
        final StringBuilder log = new StringBuilder();
        log.append(BundleUtil.getString("resources.TeacherCreditsSheetResources", key));
        log.append(BundleUtil.getString("resources.TeacherCreditsSheetResources", requested ? "message.yes" : "message.no"));
        new TeacherServiceLog(getTeacherService(), log.toString());
    }

}
