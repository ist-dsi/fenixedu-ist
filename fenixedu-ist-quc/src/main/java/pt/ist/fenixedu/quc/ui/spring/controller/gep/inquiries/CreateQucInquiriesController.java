/**
 * Copyright © 2017 Instituto Superior Técnico
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
package pt.ist.fenixedu.quc.ui.spring.controller.gep.inquiries;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.quc.domain.CoordinatorInquiryTemplate;
import pt.ist.fenixedu.quc.domain.CurricularCourseInquiryTemplate;
import pt.ist.fenixedu.quc.domain.DelegateInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryTemplate;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;
import pt.ist.fenixedu.quc.domain.ResultsInquiryTemplate;
import pt.ist.fenixedu.quc.domain.StudentTeacherInquiryTemplate;
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@SpringApplication(hint = "Gep", group = "role(GEP)", path = "create-inquiries", title = "label.create.inquiries")
@SpringFunctionality(accessGroup = "role(GEP)", app = CreateQucInquiriesController.class, title = "label.create.inquiries")
@RequestMapping("/create-quc-inquiries")
public class CreateQucInquiriesController {

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model) {
        ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        InquiryTemplate inquiryTemplate = InquiryTemplate.getInquiryTemplate(actualExecutionSemester);
        if (inquiryTemplate != null) {
            model.addAttribute("alreadyExists", true);
        }
        model.addAttribute("executionSemester", actualExecutionSemester);
        model.addAttribute("success", false);
        return "fenixedu-ist-quc/gep/inquiries/createInquiries";
    }

    @RequestMapping(method = RequestMethod.GET, value = "create")
    public String create(Model model) {
        ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        InquiryTemplate inquiryTemplate = InquiryTemplate.getInquiryTemplate(actualExecutionSemester);
        if (inquiryTemplate != null) {
            model.addAttribute("alreadyExists", true);
        } else {
            createInquiries(actualExecutionSemester);
            model.addAttribute("success", true);
        }
        model.addAttribute("executionSemester", actualExecutionSemester);
        return "fenixedu-ist-quc/gep/inquiries/createInquiries";
    }

    @Atomic(mode = TxMode.WRITE)
    private void createInquiries(ExecutionSemester executionSemester) {
        //setting dates do the past, so that the inquiries only be active when set through interface
        DateTime begin = new DateTime();
        int dayOfMonth = begin.get(DateTimeFieldType.dayOfMonth());
        DateTime end = begin.minusDays(dayOfMonth);
        begin = end.minusDays(dayOfMonth);

        // Curricular inquiry
        CurricularCourseInquiryTemplate newCourseInquiryTemplate = new CurricularCourseInquiryTemplate(begin, end);
        newCourseInquiryTemplate.setExecutionPeriod(executionSemester);
        CurricularCourseInquiryTemplate previousCourseInquiryTemplate =
                CurricularCourseInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousCourseInquiryTemplate.getInquiryBlocksSet()) {
            newCourseInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Teachers inquiry      
        StudentTeacherInquiryTemplate newStudentTeacherInquiryTemplate = new StudentTeacherInquiryTemplate(begin, end);
        newStudentTeacherInquiryTemplate.setExecutionPeriod(executionSemester);
        StudentTeacherInquiryTemplate previousStudentTeacherInquiryTemplate =
                StudentTeacherInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousStudentTeacherInquiryTemplate.getInquiryBlocksSet()) {
            newStudentTeacherInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Delegates inquiry
        DelegateInquiryTemplate newDelegateInquiryTemplate = new DelegateInquiryTemplate(begin, end);
        newDelegateInquiryTemplate.setExecutionPeriod(executionSemester);
        DelegateInquiryTemplate previousDelegateInquiryTemplate =
                DelegateInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousDelegateInquiryTemplate.getInquiryBlocksSet()) {
            newDelegateInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Teachers inquiry
        TeacherInquiryTemplate newTeacherInquiryTemplate = new TeacherInquiryTemplate(begin, end);
        newTeacherInquiryTemplate.setExecutionPeriod(executionSemester);
        TeacherInquiryTemplate previousTeacherInquiryTemplate =
                TeacherInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousTeacherInquiryTemplate.getInquiryBlocksSet()) {
            newTeacherInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Regents inquiry
        RegentInquiryTemplate newRegentInquiryTemplate = new RegentInquiryTemplate(begin, end);
        newRegentInquiryTemplate.setExecutionPeriod(executionSemester);
        RegentInquiryTemplate previousRegentInquiryTemplate =
                RegentInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousRegentInquiryTemplate.getInquiryBlocksSet()) {
            newRegentInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Coordinators inquiry
        CoordinatorInquiryTemplate newCoordinatorInquiryTemplate = new CoordinatorInquiryTemplate(begin, end, true);
        newCoordinatorInquiryTemplate.setExecutionPeriod(executionSemester);
        CoordinatorInquiryTemplate previousCoordinatorInquiryTemplate =
                CoordinatorInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousCoordinatorInquiryTemplate.getInquiryBlocksSet()) {
            newCoordinatorInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }

        // Results inquiry
        ResultsInquiryTemplate newResultsInquiryTemplate = new ResultsInquiryTemplate();
        newResultsInquiryTemplate.setExecutionPeriod(executionSemester);
        ResultsInquiryTemplate previousResultsInquiryTemplate =
                ResultsInquiryTemplate.getTemplateByExecutionPeriod(executionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousResultsInquiryTemplate.getInquiryBlocksSet()) {
            newResultsInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }
    }
}
