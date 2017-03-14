/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.ui.spring;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.ui.struts.action.teacher.ManageExecutionCourseDA;
import org.fenixedu.bennu.spring.security.CSRFTokenBean;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.MenuItem;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.RedirectView;
import pt.ist.fenixframework.Atomic;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static org.fenixedu.academic.predicate.AccessControl.check;
import static org.fenixedu.academic.predicate.AccessControl.getPerson;
import static pt.ist.fenixframework.FenixFramework.atomic;

@Controller
@RequestMapping("/teacher/{executionCourse}/pages")
public class TeacherPagesController extends ExecutionCourseController {

    @Autowired
    PagesAdminService service;
    
    // hack
    @Autowired
    CSRFTokenBean csrfTokenBean;
    
    @ModelAttribute("csrf")
    public CSRFTokenBean getCSRF(){
        return csrfTokenBean;
    }
    
    @RequestMapping(method = RequestMethod.GET)
    public TeacherPagesView all(Model model, @PathVariable ExecutionCourse executionCourse) {
        hasAccess(executionCourse);
        model.addAttribute("executionCourse", executionCourse);
        model.addAttribute("professorship", executionCourse.getProfessorship(getPerson()));
        model.addAttribute("site", executionCourse.getSite());
        model.addAttribute("previousExecutionCourses", previousExecutionCourses(executionCourse).collect(toList()));
        return new TeacherPagesView();
    }

    @RequestMapping(value = "options", method = RequestMethod.POST)
    public RedirectView editOptions(@PathVariable ExecutionCourse executionCourse, @RequestParam(required = false,
            defaultValue = "") String alternativeSite) {
        hasAccess(executionCourse);
        atomic(() -> executionCourse.getSite().setAlternativeSite(alternativeSite));
        return new RedirectView(String.format("/teacher/%s/pages", executionCourse.getExternalId()), true);
    }




    @RequestMapping(value = "copyContent", method = RequestMethod.POST)
    public RedirectView     copyContent(@PathVariable ExecutionCourse executionCourse,
            @RequestParam ExecutionCourse previousExecutionCourse, RedirectAttributes redirectAttributes) {
        canCopyContent(executionCourse, previousExecutionCourse);
        try {
            copyContent(previousExecutionCourse.getSite(), executionCourse.getSite());
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(TeacherPagesController.class).error("error importing site content", e);
            //error occurred while importing content
            redirectAttributes.addFlashAttribute("importError", true);
            return new RedirectView(String.format("/teacher/%s/pages", executionCourse.getExternalId()), true);
        }
        return new RedirectView(String.format("/teacher/%s/pages", executionCourse.getExternalId()), true);
    }

    @Atomic
    private void copyContent(Site from, Site to) {
        Menu newMenu = to.getMenusSet().stream().findAny().get();
        LocalizedString newPageName =
                new LocalizedString()
                        .with(Locale.getDefault(), from.getExecutionCourse().getExecutionPeriod().getQualifiedName());
        MenuItem emptyPageParent = service.create(to, null, newPageName, new LocalizedString(), new LocalizedString()).get();
        emptyPageParent.getPage().setPublished(false);
        emptyPageParent.setTop(newMenu);
        for (Menu oldMenu : from.getMenusSet()) {
            oldMenu.getToplevelItemsSorted().forEach(menuItem -> service.copyStaticPage(menuItem, to, newMenu, emptyPageParent));
        }
    }

    private Stream<ExecutionCourse> previousExecutionCourses(ExecutionCourse executionCourse) {
        Set<Degree> degrees =
                executionCourse.getAssociatedCurricularCoursesSet().stream().map(c -> c.getDegreeCurricularPlan().getDegree())
                        .distinct().collect(Collectors.toSet());
        return executionCourse.getCompetenceCourses().stream()
                .flatMap(competence -> competence.getAssociatedCurricularCoursesSet().stream())
                .filter(curricularCourse -> degrees.contains(curricularCourse.getDegreeCurricularPlan().getDegree()))
                .flatMap(curricularCourse -> curricularCourse.getAssociatedExecutionCoursesSet().stream())
                .filter(ec -> ec != executionCourse).filter(ec -> ec.getSite() != null).distinct()
                .sorted(ExecutionCourse.EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME.reversed());
    }

    private void canCopyContent(ExecutionCourse executionCourse, ExecutionCourse previousExecutionCourse) {
        hasAccess(executionCourse);
        check(p -> previousExecutionCourses(executionCourse).filter(isEqual(previousExecutionCourse)).findAny().isPresent());
    }

    private void hasAccess(ExecutionCourse executionCourse) {
        Professorship professorship = executionCourse.getProfessorship(getPerson());
        check(person -> professorship != null && professorship.getPermissions().getSections());
    }

    @Override
    protected Class<?> getFunctionalityType() {
        return ManageExecutionCourseDA.class;
    }

    @Override
    Boolean getPermission(Professorship prof) {
        return prof.getPermissions().getSections();
    }

    public class TeacherPagesView extends JstlView {

        @Override
        protected void exposeHelpers(HttpServletRequest request) throws Exception {
            setServletContext(request.getServletContext());
            super.exposeHelpers(request);
            request.setAttribute("teacher$actual$page", "/WEB-INF/fenix-learning/teacherPages.jsp");
        }

        @Override
        public String getUrl() {
            return "/teacher/executionCourse/executionCourseFrame.jsp";
        }

    }

}
