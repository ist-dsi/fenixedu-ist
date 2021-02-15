package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.spreadsheet.WorkbookExportFormat;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.SharedFunction;
import pt.ist.fenixedu.teacher.evaluation.domain.DepartmentCreditsPool;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.OtherServiceExemption;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean.CreditsPoolByDepartmentBean;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.io.ByteStreams;

@Service
public class CreditsManagementService {

    @Autowired
    private MessageSource messageSource;

    @Atomic(mode = TxMode.WRITE)
    public void editCreditsPool(CreditsPoolBean creditsPoolBean) {
        for (CreditsPoolByDepartmentBean creditsPoolByDepartmentBean : creditsPoolBean.getCreditsPoolByDepartments()) {
            DepartmentCreditsPool departmentCreditsPool =
                    DepartmentCreditsPool.getDepartmentCreditsPool(creditsPoolByDepartmentBean.getDepartment(), creditsPoolBean.getExecutionYear());
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

    public Set<String> uploadSharedFunctionsCredits(MultipartFile file) throws Exception {
        String[] lines = getFileLines(file);
        return setSharedFunctionsCredits(lines);
    }

    @Atomic(mode = TxMode.WRITE)
    public Set<String> setSharedFunctionsCredits(String[] lines) {
        Set<String> output = new HashSet<String>();
        for (String line : lines) {
            String[] values = getLineValues(line);
            if (values.length < 5) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            String sharedFunctionOid = values[3];
            if (StringUtils.isBlank(sharedFunctionOid)) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }

            SharedFunction sharedFunction = FenixFramework.getDomainObject(sharedFunctionOid);
            if (sharedFunction == null) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            BigDecimal credits = null;
            if (values.length > 5) {
                String bkString = values[5].trim();
                try {
                    credits = new BigDecimal(bkString);
                } catch (NumberFormatException e) {
                    output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                    continue;
                }
            }
            sharedFunction.setCredits(credits);
        }
        return output;
    }

    public Set<String> uploadOtherServiceExemptions(MultipartFile file) throws Exception {
        String[] lines = getFileLines(file);
        return createOtherServiceExemptions(lines);
    }

    @Atomic(mode = TxMode.WRITE)
    public Set<String> createOtherServiceExemptions(String[] lines) {
        Set<String> output = new HashSet<String>();
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] values = getLineValues(line);
            if (values.length < 4) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            String username = values[0];
            if (StringUtils.isBlank(username)) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            User user = User.findByUsername(username);
            if (user == null) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            if (StringUtils.isBlank(values[1])) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
            LocalDate beginDate = new LocalDate(values[1]);
            LocalDate endDate = StringUtils.isBlank(values[2]) ? null : new LocalDate(values[2]);
            String description = values[3];
            try {
                OtherServiceExemption.create(user.getPerson(), beginDate, endDate, description);
            } catch (DomainException e) {
                output.add(messageSource.getMessage("error.upload.file.line", new Object[] { line }, I18N.getLocale()));
                continue;
            }
        }
        return output;
    }

    private String[] getLineValues(String line) {
        String[] values = line.split(WorkbookExportFormat.TSV.getSeparator());
        if (values.length == 1) {
            values = line.split(WorkbookExportFormat.CSV.getSeparator());
        }
        return values;
    }

    @Atomic(mode = TxMode.WRITE)
    public void deleteOtherServiceExemption(OtherServiceExemption otherServiceExemption) {
        otherServiceExemption.delete();
    }
}