package pt.ist.fenixedu.tutorship.ui;

import org.fenixedu.bennu.struts.portal.StrutsApplication;

public class TutorshipApplications {

    @StrutsApplication(bundle = "PedagogicalCouncilResources", path = "tutorship", titleKey = "link.tutorship",
            accessGroup = "#tutorship", hint = "Tutorship")
    public static class TutorshipApp {
    }

    @StrutsApplication(bundle = "ApplicationResources", path = "tutor", titleKey = "link.teacher.tutor.operations",
            accessGroup = "activeTeachers | professors", hint = "Teacher")
    public static class TeacherTutorApp {
    }

}
