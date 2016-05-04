package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.ExecutionYear;
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

import pt.ist.fenixedu.contracts.domain.organizationalStructure.SharedFunction;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.DepartmentCreditsBean;

@SpringFunctionality(app = CreditsManagementApplication.class, title = "title.creditsManagement.sharedFunctionsPool",
        accessGroup = "#scientificCouncil")
@RequestMapping("/sharedFunctionsPool")
public class SharedFunctionsManagementController {
    @Autowired
    CreditsManagementService service;

    @RequestMapping(method = GET)
    public String home(Model model, @ModelAttribute DepartmentCreditsBean departmentCreditsBean) {
        if (departmentCreditsBean == null) {
            departmentCreditsBean = new DepartmentCreditsBean();
        }
        model.addAttribute("departmentCreditsBean", departmentCreditsBean);
        model.addAttribute("executionYears", Bennu.getInstance().getExecutionYearsSet().stream().distinct()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).limit(4).collect(Collectors.toList()));

        ExecutionYear executionYear = departmentCreditsBean.getExecutionYear();
        Set<SharedFunction> sharedFunctions =
                Bennu.getInstance().getAccountabilityTypesSet().stream()
                        .filter(at -> at.isSharedFunction() && ((SharedFunction) at).belongsToPeriod(
                                executionYear.getBeginDateYearMonthDay(), executionYear.getEndDateYearMonthDay()))
                .map(at -> (SharedFunction) at).collect(Collectors.toSet());

        model.addAttribute("sharedFunctions", sharedFunctions);
        return "/creditsManagement/creditsPool/manageSharedFunctionsPool";
    }

    @RequestMapping(method = GET, value = "exportSharedFunctionsPool", produces = "text/csv")
    public String exportSharedFunctionsPool(Model model, @ModelAttribute ExecutionYear executionYear,
            final HttpServletResponse response) throws IOException {
        if (executionYear != null) {
            Set<SharedFunction> sharedFunctions =
                    Bennu.getInstance().getAccountabilityTypesSet().stream()
                            .filter(at -> at.isSharedFunction() && ((SharedFunction) at).belongsToPeriod(
                                    executionYear.getBeginDateYearMonthDay(), executionYear.getEndDateYearMonthDay()))
                    .map(at -> (SharedFunction) at).collect(Collectors.toSet());

            SpreadsheetBuilder builder = new SpreadsheetBuilder();

            builder.addSheet(executionYear.getName(), new SheetData<SharedFunction>(sharedFunctions) {
                @Override
                protected void makeLine(SharedFunction sharedFunction) {
                    addCell(sharedFunction.getUnit().getExternalId());
                    addCell(sharedFunction.getUnit().getAcronym());
                    addCell(sharedFunction.getUnit().getName());
                    addCell(sharedFunction.getExternalId());
                    addCell(sharedFunction.getName());
                    addCell(sharedFunction.getCredits());
                }
            });
            response.setContentType("text/csv");
            String fileName =
                    BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.credits.managementPositions.simpleCode")
                            + "_" + executionYear.getName().replaceAll("/", "_") + ".tsv";
            response.setHeader("Content-Disposition", "filename=" + fileName);
            builder.build(WorkbookExportFormat.TSV, response.getOutputStream());
            response.flushBuffer();
        }
        return null;
    }

    @RequestMapping(method = POST, value = "uploadSharedFunctionsPool")
    public String uploadSharedFunctionsPool(Model model, @RequestParam ExecutionYear executionYear,
            @RequestParam MultipartFile file) throws Exception {
        DepartmentCreditsBean departmentCreditsBean = new DepartmentCreditsBean();
        if (executionYear != null) {
            Set<String> output = service.uploadSharedFunctionsCredits(file);
            model.addAttribute("output", output);
            departmentCreditsBean.setExecutionYear(executionYear);
        }
        return home(model, departmentCreditsBean);
    }

}
