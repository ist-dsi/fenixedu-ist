/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.domain;

import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.joda.time.DateTime;

public class OtherCourseVigilancy extends OtherCourseVigilancy_Base {

    public OtherCourseVigilancy() {
        super();
    }

    @Override
    public int getEstimatedPoints() {
        return getAssociatedVigilantGroup().getPointsForConvoked();
    }

    @Override
    public int getPoints() {

        if (this.getWrittenEvaluation() == null) {
            throw new DomainException("vigilancy.error.InvalidConvokeNoEvaluationAvailable");
        }

        DateTime currentDate = new DateTime();
        if (currentDate.isBefore(this.getBeginDate())) {
            return POINTS_WON_FOR_CONVOKE_YET_TO_HAPPEN;
        }

        if (!isActive() || isStatusUndefined()) {
            return getAssociatedVigilantGroup().getPointsForDisconvoked();
        }
        if (isDismissed()) {
            return getAssociatedVigilantGroup().getPointsForDismissed();
        }
        if (getWrittenEvaluation().getAttendedVigilanciesSet().isEmpty()) {
            return POINTS_WON_FOR_CONVOKE_YET_TO_HAPPEN;
        }
        if (this.getAttendedToConvoke()) {
            return getAssociatedVigilantGroup().getPointsForConvoked();
        }

        return getAssociatedVigilantGroup().getPointsForMissing();
    }

    public OtherCourseVigilancy(WrittenEvaluation writtenEvaluation) {
        this();
        super.setWrittenEvaluation(writtenEvaluation);
        super.setConfirmed(false);
        super.initStatus();
    }

    @Override
    public void setConfirmed(Boolean confirmed) {
        if (isSelfAccessing()) {
            super.setConfirmed(confirmed);
        } else {
            throw new DomainException("vigilancy.error.notAuthorized");
        }
    }

    public boolean isConfirmed() {
        return getConfirmed();
    }

}
