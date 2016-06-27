/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.enrollment.bolonha.BolonhaStudentEnrollmentBean;
import org.fenixedu.academic.thesis.domain.StudentThesisCandidacy;
import org.fenixedu.academic.thesis.ui.service.ThesisProposalsService;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;
import org.fenixedu.cms.domain.CMSFolder;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseSite;

import pt.ist.fenixedu.integration.domain.student.AffinityCyclesManagement;
import pt.ist.fenixedu.integration.domain.student.PreEnrolment;
import pt.ist.fenixedu.integration.dto.QucProfessorshipEvaluation;
import pt.ist.fenixedu.teacher.evaluation.domain.ProfessorshipEvaluationBean;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

import com.google.common.collect.Sets;

@WebListener
public class FenixEduISTLegacyContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Person.class, (person, blockers) -> {
            if (!person.getPersistentGroupsSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.person.cannot.be.deleted"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Unit.class, (unit, blockers) -> {
            if (!unit.getFilesSet().isEmpty() || !unit.getPersistentGroupsSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Unit.class, unit -> {
            for (; !unit.getUnitFileTagsSet().isEmpty(); unit.getUnitFileTagsSet().iterator().next().delete()) {
                ;
            }
            unit.getAllowedPeopleToUploadFilesSet().clear();
        });

        FenixFramework.getDomainModel().registerDeletionListener(Category.class, cat -> {
            cat.getBookmarkedBySet().clear();
        });

        Site.getRelationFolderHasSites().addListener(new RelationAdapter<Site, CMSFolder>() {
            @Override
            public void afterAdd(Site site, CMSFolder folder) {
                if (site instanceof ExecutionCourseSite && folder != null
                        && !folder.getFunctionality().getPath().equals("disciplinas")) {
                    Bennu.getInstance().getCmsFolderSet().stream()
                            .filter(x -> x.getFunctionality().getPath().equals("disciplinas")).findAny()
                            .ifPresent(x -> site.setFolder(x));
                }
            }
        });

        Signal.register(
                Enrolment.SIGNAL_CREATED,
                ((DomainObjectEvent<Enrolment> e) -> {

                    Enrolment enrolment = e.getInstance();

                    if (enrolment.getCurricularCourse().isDissertation()) {
                        Optional<StudentThesisCandidacy> hit =
                                enrolment
                                        .getRegistration()
                                        .getStudentThesisCandidacySet()
                                        .stream()
                                        .filter(c -> partOf(enrolment.getCurricularCourse().getAssociatedExecutionCoursesSet(), c))
                                        .filter(StudentThesisCandidacy::getAcceptedByAdvisor)
                                        .min(StudentThesisCandidacy.COMPARATOR_BY_CANDIDACY_PERIOD_AND_PREFERENCE_NUMBER);

                        if (hit.isPresent()) {
                            StudentThesisCandidacy candidacy = hit.get();
                            ThesisProposalsService.createThesisForStudent(candidacy);
                        }
                    }
                }));

        Signal.register(Enrolment.SIGNAL_CREATED, ((DomainObjectEvent<Enrolment> e) -> {
            Enrolment enrolment = e.getInstance();
            new AffinityCyclesManagement(enrolment.getRegistration().getLastStudentCurricularPlan())
                    .createCycleOrRepeateSeparate();
        }));
        ProfessorshipEvaluationBean.professorshipEvaluation = new QucProfessorshipEvaluation();

        BolonhaStudentEnrollmentBean.registerStudentEnrolmentHandler(FenixEduISTLegacyContextListener::setPreEnrolledCourses);
    }

    private static boolean partOf(Set<ExecutionCourse> degrees, StudentThesisCandidacy studentThesisCandidacy) {
        return Sets.intersection(degrees, studentThesisCandidacy.getThesisProposal().getExecutionDegreeSet()).isEmpty();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private static void setPreEnrolledCourses(BolonhaStudentEnrollmentBean bolonhaStudentEnrollmentBean) {
        Registration registration = bolonhaStudentEnrollmentBean.getRegistration();
        User user = registration.getPerson().getUser();
        List<IDegreeModuleToEvaluate> degreeModulesToEnrol = new ArrayList<IDegreeModuleToEvaluate>();
        if (registration.getEnrolments(bolonhaStudentEnrollmentBean.getExecutionPeriod()).size() == 0) {
            for (PreEnrolment preEnrolment : user.getPreEnrolmentsSet()) {
                Optional<Context> courseContext =
                        preEnrolment
                                .getCourseGroup()
                                .getOpenChildContexts(CurricularCourse.class, preEnrolment.getExecutionSemester())
                                .stream()
                                .filter(ctx -> ctx.getChildDegreeModule() == preEnrolment.getCurricularCourse()
                                        && ctx.getCurricularPeriod().getChildOrder() == preEnrolment.getExecutionSemester()
                                                .getSemester()).findAny();

                CurriculumGroup curriculumGroup =
                        bolonhaStudentEnrollmentBean.getStudentCurricularPlan().getRoot()
                                .findCurriculumGroupFor(preEnrolment.getCourseGroup());

                if (courseContext.isPresent()) {
                    degreeModulesToEnrol.add(new DegreeModuleToEnrol(curriculumGroup, courseContext.get(), preEnrolment
                            .getExecutionSemester()));
                }
            }
            bolonhaStudentEnrollmentBean.setDegreeModulesToEvaluate(degreeModulesToEnrol);
        }
    }
}
