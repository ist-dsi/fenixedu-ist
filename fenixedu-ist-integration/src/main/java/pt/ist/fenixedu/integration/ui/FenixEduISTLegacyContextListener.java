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
import java.util.Calendar;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.domain.accounting.VatNumberResolver;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.dto.student.enrollment.bolonha.BolonhaStudentEnrollmentBean;
import org.fenixedu.academic.service.StudentWarningsService;
import org.fenixedu.academic.thesis.domain.StudentThesisCandidacy;
import org.fenixedu.academic.thesis.ui.service.ThesisProposalsService;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.cms.domain.CMSFolder;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Site;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Sets;

import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.integration.domain.student.AffinityCyclesManagement;
import pt.ist.fenixedu.integration.domain.student.PreEnrolment;
import pt.ist.fenixedu.integration.dto.QucProfessorshipEvaluation;
import pt.ist.fenixedu.teacher.evaluation.domain.ProfessorshipEvaluationBean;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

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
                if (site.getExecutionCourse()!=null && folder != null
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

        Consumer<DomainObjectEvent<Summary>> handler = (DomainObjectEvent<Summary> event) -> {
            Optional<Calendar> gradeSubmissionEndDate = event.getInstance().getExecutionCourse().getExecutionDegrees().stream()
                    .flatMap(ed -> ed.getPeriods(OccupationPeriodType.GRADE_SUBMISSION)).map(OccupationPeriod::getEndDate)
                    .max(Calendar::compareTo);

            if (gradeSubmissionEndDate.map(v -> v.before(Calendar.getInstance())).orElse(false)) {
                throw new DomainException("error.summary.current.date.after.end.period");
            }
        };
        Signal.register(Summary.CREATE_SIGNAL, handler);
        Signal.register(Summary.EDIT_SIGNAL, handler);

        ProfessorshipEvaluationBean.professorshipEvaluation = new QucProfessorshipEvaluation();

        BolonhaStudentEnrollmentBean.registerStudentEnrolmentHandler(FenixEduISTLegacyContextListener::setPreEnrolledCourses);

        VatNumberResolver.RESOLVER = new VatNumberResolver() {

        	@Override
        	public String uVATNumberFor(final Person person) {
        		return ClientMap.uVATNumberFor(person);
			}

        };

        StudentWarningsService.register(new Function<Student, Collection<String>>() {

            @Override
            public Collection<String> apply(final Student student) {
                final Collection<String> warnings = new TreeSet<String>();

                final Person person = student.getPerson();
                final Country country = person.getCountry();
                if (country == null) {
                    warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.country.none"));
                }

                final PhysicalAddress address = Utils.toAddress(person);
                final Country countryOfAddress = address == null ? null : address.getCountryOfResidence();
                if (address == null) {
                    warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.address.none"));
                } else {
                    if (countryOfAddress == null) {
                        warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.address.country.none"));
                    } else if ("PT".equals(countryOfAddress.getCode())) {
                        if (!isValidPostCode(hackAreaCodePT(address.getAreaCode(), countryOfAddress))) {
                            warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.address.postCode.invalid"));
                        }
                    }
                }

                final String vat = ClientMap.uVATNumberFor(person);
                if (vat == null) {
                    warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.vatNumber.none"));
                } else if ("PT999999990".equals(vat)) {
                    warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.vatNumber.invalid"));
                } else if ((country != null && "PT".equals(country.getCode())) || (countryOfAddress != null && "PT".equals(countryOfAddress.getCode()))) {
                    if (!ClientMap.isVatValidForPT(vat.substring(2))) {
                        warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.person.details.vatNumber.invalid"));
                    }
                }

                final Stream<Event> events = person.getEventsSet().stream();
                if (events.anyMatch(e -> isOverDue(e))) {
                    warnings.add(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.event.overdue"));
                }

                return warnings;
            };
        });
    }

    private static boolean isOverDue(final Event event) {
        return !event.isCancelled() && event.isInDebt() && Utils.getDueDate(event).before(new Date());
    }

    private static boolean isValidPostCode(final String postalCode) {
        if (postalCode != null) {
            final String v = postalCode.trim();
            return v.length() == 8 && v.charAt(4) == '-' && CharMatcher.DIGIT.matchesAllOf(v.substring(0, 4))
                    && CharMatcher.DIGIT.matchesAllOf(v.substring(5));
        }
        return false;
    }

    private static String hackAreaCodePT(final String areaCode, final Country countryOfResidence) {
        if (countryOfResidence != null && "PT".equals(countryOfResidence.getCode())) {
            if (areaCode == null || areaCode.isEmpty()) {
                return "0000-000";
            }
            if (areaCode.length() == 4) {
                return areaCode + "-001";
            }
        }
        return areaCode;
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
