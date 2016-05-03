package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.spreadsheet.WorkbookExportFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.ByteStreams;

import pt.ist.fenixedu.teacher.evaluation.domain.DepartmentCreditsPool;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean.CreditsPoolByDepartmentBean;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Service
public class CreditsManagementService {

    @Autowired
    private MessageSource messageSource;

    @Atomic(mode = TxMode.WRITE)
    public void editCreditsPool(CreditsPoolBean creditsPoolBean) {
        for (CreditsPoolByDepartmentBean creditsPoolByDepartmentBean : creditsPoolBean.getCreditsPoolByDepartments()) {
            DepartmentCreditsPool departmentCreditsPool = DepartmentCreditsPool
                    .getDepartmentCreditsPool(creditsPoolByDepartmentBean.getDepartment(), creditsPoolBean.getExecutionYear());
            if (departmentCreditsPool == null) {
                new DepartmentCreditsPool(creditsPoolByDepartmentBean.getDepartment(), creditsPoolBean.getExecutionYear(),
                        creditsPoolByDepartmentBean.getOriginalCreditsPool(), creditsPoolByDepartmentBean.getCreditsPool());
            } else {
                departmentCreditsPool.setOriginalCreditsPool(creditsPoolByDepartmentBean.getOriginalCreditsPool());
                departmentCreditsPool.setCreditsPool(creditsPoolByDepartmentBean.getCreditsPool());
            }
        }

    }

    public Set<String> uploadExecutionCourseEffortRate(MultipartFile file) throws Exception {
        String[] lines = getFileLines(file);
        return setExecutionCourseEffortRate(lines);
    }

    private String[] getFileLines(MultipartFile file) throws Exception {
        try (InputStream stream = file.getInputStream()) {
            byte[] bytes = ByteStreams.toByteArray(file.getInputStream());
            return new String(bytes).split("[\\r\\n]");
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private Set<String> setExecutionCourseEffortRate(String[] lines) {
        Set<String> output = new HashSet<String>();
        for (String line : lines) {
            String[] values = line.split(WorkbookExportFormat.CSV.getSeparator());
            if (values.length < 1) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            String executionCourseCode = values[0];
            if (StringUtils.isBlank(executionCourseCode)) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            ExecutionCourse executionCourse = getExecutionCourse(executionCourseCode);
            if (executionCourse == null) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            BigDecimal bk = null;
            if (values.length > 1) {
                String bkString = values[1].trim();
                try {
                    bk = new BigDecimal(bkString);
                } catch (NumberFormatException e) {
                    output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                    continue;
                }
            }
            executionCourse.setEffortRate(bk);
        }
        return output;
    }

    private ExecutionCourse getExecutionCourse(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        return decodedParts.length > 2 ? ExecutionCourse.readBySiglaAndExecutionPeriod(decodedParts[0],
                getExecutionSemester(decodedParts[1], decodedParts[2])) : null;
    }

    private ExecutionSemester getExecutionSemester(String codePart1, String codePart2) {
        return ExecutionSemester.readBySemesterAndExecutionYear(Integer.valueOf(codePart1), codePart2);
    }

}