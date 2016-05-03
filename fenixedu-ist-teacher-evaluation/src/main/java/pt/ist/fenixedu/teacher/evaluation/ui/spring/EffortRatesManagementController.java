package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.spreadsheet.SheetData;
import org.fenixedu.commons.spreadsheet.SpreadsheetBuilder;
import org.fenixedu.commons.spreadsheet.WorkbookExportFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.DepartmentCreditsBean;

@SpringFunctionality(app = CreditsManagementApplication.class, title = "title.creditsManagement.effortRates",
        accessGroup = "#scientificCouncil")
@RequestMapping("/effortRates")
public class EffortRatesManagementController {

    @Autowired
    CreditsManagementService service;

    @RequestMapping(method = GET)
    public String home(Model model, @ModelAttribute DepartmentCreditsBean departmentCreditsBean) {
        if (departmentCreditsBean == null) {
            departmentCreditsBean = new DepartmentCreditsBean();
        }
        setRequest(model, departmentCreditsBean);
        return "/creditsManagement/creditsPool/manageEffortRates";
    }

    private void setRequest(Model model, DepartmentCreditsBean departmentCreditsBean) {
        List<ExecutionCourse> executionCourses = departmentCreditsBean.getExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(es -> es.getAssociatedExecutionCoursesSet().stream()).filter(ec -> !ec.getProjectTutorialCourse())
                .sorted(ExecutionCourse.EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME).collect(Collectors.toList());
        model.addAttribute("executionCourses", executionCourses);
        model.addAttribute("departmentCreditsBean", departmentCreditsBean);
        model.addAttribute("executionYears", Bennu.getInstance().getExecutionYearsSet().stream().distinct()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).limit(4).collect(Collectors.toList()));
    }

    @RequestMapping(method = GET, value = "exportExecutionCoursesEffortRates", produces = "text/csv")
    public String exportExecutionCoursesEffortRates(Model model, @ModelAttribute ExecutionYear executionYear,
            final HttpServletResponse response) throws IOException {
        if (executionYear != null) {
            List<ExecutionCourse> executionCourses = executionYear.getExecutionPeriodsSet().stream()
                    .flatMap(es -> es.getAssociatedExecutionCoursesSet().stream()).filter(ec -> !ec.getProjectTutorialCourse())
                    .sorted(ExecutionCourse.EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME)
                    .collect(Collectors.toList());

            SpreadsheetBuilder builder = new SpreadsheetBuilder();

            builder.addSheet(executionYear.getName(), new SheetData<ExecutionCourse>(executionCourses) {
                @Override
                protected void makeLine(ExecutionCourse executionCourse) {
                    addCell(GepReportFile.getExecutionCourseCode(executionCourse));
                    addCell(executionCourse.getEffortRate());
                }
            });
            response.setContentType("text/csv");

            String fileName =
                    BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.effortRate").replaceAll(" ", "_") + "_"
                            + executionYear.getName().replaceAll("/", "_") + ".csv";
            response.setHeader("Content-Disposition", "filename=" + fileName);
            builder.build(WorkbookExportFormat.CSV, response.getOutputStream());
            response.flushBuffer();
        }
        return null;
    }

    @RequestMapping(method = POST, value = "uploadExecutionCoursesEffortRates")
    public String uploadExecutionCoursesEffortRates(Model model, @RequestParam ExecutionYear executionYear,
            @RequestParam MultipartFile file) throws Exception {
        DepartmentCreditsBean departmentCreditsBean = new DepartmentCreditsBean();
        if (executionYear != null) {
            Set<String> output = service.uploadExecutionCourseEffortRate(file);
            model.addAttribute("output", output);
            departmentCreditsBean.setExecutionYear(executionYear);
        }
        return home(model, departmentCreditsBean);
    }

}
