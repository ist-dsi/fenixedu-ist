package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import pt.ist.fenixedu.teacher.evaluation.domain.TeacherCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.NonExerciseSituation;
import pt.ist.fenixedu.teacher.evaluation.domain.contracts.OtherServiceExemption;

@SpringFunctionality(app = CreditsManagementApplication.class, title = "title.creditsManagement.nonExerciseSituation",
        accessGroup = "#scientificCouncil")
@RequestMapping("/nonExerciseSituations")
public class NonExerciseSituationsManagementController {

    @Autowired
    CreditsManagementService service;

    @RequestMapping(method = GET)
    public String home(Model model) {
        List<NonExerciseSituation> otherServiceExemptions =
                Bennu.getInstance()
                        .getNonExerciseSituationSet()
                        .stream()
                        .filter(nes -> nes instanceof OtherServiceExemption
                                && (nes.getEndDate() == null || getTeacherCreditsStateOpen(nes.getEndDate())))
                        .sorted(Comparator.comparing((NonExerciseSituation nes) -> nes.getBeginDate()).reversed()).collect(Collectors.toList());

        model.addAttribute("otherServiceExemptions", otherServiceExemptions);
        return "/creditsManagement/manageNonExerciseSituations";
    }

    private boolean getTeacherCreditsStateOpen(LocalDate date) {
        for (TeacherCreditsState teacherCreditsState : Bennu.getInstance().getTeacherCreditsStateSet()) {
            if (teacherCreditsState.getExecutionSemester().equals(ExecutionSemester.readByDateTime(date.toDateTimeAtStartOfDay()))) {
                return teacherCreditsState == null || teacherCreditsState.isOpenState();
            }
        }
        return false;
    }

    @RequestMapping(method = POST, value = "uploadOtherServiceExemptions")
    public String uploadOtherServiceExemptions(Model model, @RequestParam MultipartFile file) throws Exception {
        Set<String> output = service.uploadOtherServiceExemptions(file);
        model.addAttribute("output", output);
        return home(model);
    }

    @RequestMapping(method = GET, value = "deleteOtherServiceExemption/{otherServiceExemption}")
    public String deleteOtherServiceExemption(Model model, @PathVariable OtherServiceExemption otherServiceExemption) throws Exception {
        service.deleteOtherServiceExemption(otherServiceExemption);
        return home(model);
    }

}
