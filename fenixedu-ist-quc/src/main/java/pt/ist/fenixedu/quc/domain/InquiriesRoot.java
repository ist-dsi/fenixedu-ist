/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.domain;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public final class InquiriesRoot extends InquiriesRoot_Base {
    private InquiriesRoot() {
        super();
        setRoot(Bennu.getInstance());
    }

    public static InquiriesRoot getInstance() {
        if (Bennu.getInstance().getInquiriesRoot() == null) {
            return initialize();
        }
        return Bennu.getInstance().getInquiriesRoot();
    }

    @Atomic(mode = TxMode.WRITE)
    private static InquiriesRoot initialize() {
        if (Bennu.getInstance().getInquiriesRoot() == null) {
            return new InquiriesRoot();
        }
        return Bennu.getInstance().getInquiriesRoot();
    }

    public static boolean isAvailableDegreeTypeForInquiries(Registration registration) {
        return isAvailableDegreeForInquiries(registration.getDegree());
    }

    private static boolean isAvailableDegreeForInquiries(Degree degree) {
        return degree.getDegreeType().isBolonhaDegree() || degree.getDegreeType().isIntegratedMasterDegree()
                || degree.getDegreeType().isBolonhaMasterDegree() || degree.getDegreeType().getUnstructured() || isAvailableDegree(degree);
    }

    private static boolean isAvailableDegree(Degree degree) {
        return getInstance().getDegreesAvailableForInquiriesSet().contains(degree);
    }

    public static boolean isMasterDegreeDFAOnly(ExecutionCourse executionCourse) {
        for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
            if (isAvailableDegreeForInquiries(curricularCourse.getDegreeCurricularPlan().getDegree())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isDegreeAvailableForInquiries(ExecutionCourse executionCourse) {
        for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
            if (isAvailableDegreeForInquiries(curricularCourse.getDegreeCurricularPlan().getDegree())) {
                return true;
            }
        }
        return false;
    }

    public static Boolean getAvailableForInquiries(ExecutionCourse executionCourse) {
        if (isDegreeAvailableForInquiries(executionCourse)) {
            return executionCourse.getAvailableForInquiries() != null;
        }
        return Boolean.FALSE;
    }

    public static boolean isAvailableForInquiry(ExecutionCourse executionCourse) {
        return getAvailableForInquiries(executionCourse) && executionCourse.hasEnrolmentsInAnyCurricularCourse()
                && !isMasterDegreeDFAOnly(executionCourse);
    }

}
