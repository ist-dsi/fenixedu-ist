/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Tutorship.
 *
 * FenixEdu Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.domain;

import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.TutorshipSummaryRelationBean;
import pt.ist.fenixframework.Atomic;

public class TutorshipSummaryRelation extends TutorshipSummaryRelation_Base {

    public TutorshipSummaryRelation(final Tutorship tutorship, final TutorshipSummary tutorshipSummary) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setTutorship(tutorship);
        setTutorshipSummary(tutorshipSummary);
    }

    public StudentCurricularPlan getStudentPlan() {
        return getTutorship().getStudentCurricularPlan();
    }

    @Atomic
    public void update(final TutorshipSummaryRelationBean bean) {
        setParticipationType(bean.getParticipationType());
        setWithoutEnrolments(bean.isWithoutEnrolments());
        setHighPerformance(bean.isHighPerformance());
        setHighPerformance(bean.isHighPerformance());
        setLowPerformance(bean.isLowPerformance());
        setOutOfTouch(bean.isOutOfTouch());
        setParticipationNone(bean.isParticipationNone());
        setParticipationRegularly(bean.isParticipationRegularly());
    }

    @Atomic
    static public TutorshipSummaryRelation create(final TutorshipSummaryRelationBean bean) {
        TutorshipSummaryRelation tutorshipSummaryRelation =
                new TutorshipSummaryRelation(bean.getTutorship(), bean.getTutorshipSummary());

        tutorshipSummaryRelation.setParticipationType(bean.getParticipationType());
        tutorshipSummaryRelation.setWithoutEnrolments(bean.isWithoutEnrolments());
        tutorshipSummaryRelation.setHighPerformance(bean.isHighPerformance());
        tutorshipSummaryRelation.setLowPerformance(bean.isLowPerformance());
        tutorshipSummaryRelation.setOutOfTouch(bean.isOutOfTouch());
        tutorshipSummaryRelation.setParticipationNone(bean.isParticipationNone());
        tutorshipSummaryRelation.setParticipationRegularly(bean.isParticipationRegularly());

        return tutorshipSummaryRelation;
    }

}
