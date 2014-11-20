package net.sourceforge.fenixedu.presentationTier.Action.teacher.tutor;

import org.fenixedu.bennu.struts.portal.StrutsApplication;

@StrutsApplication(bundle = "ApplicationResources", path = "tutor", titleKey = "link.teacher.tutor.operations",
        accessGroup = "role(TEACHER) | professors", hint = "Teacher")
public class TeacherTutorApp {
}