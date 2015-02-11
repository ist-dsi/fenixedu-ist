package pt.ist.fenixedu.delegates.ui.struts;

import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.StrutsApplication;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateApplication {

    @StrutsApplication(bundle = "DelegateResources", path = "consult", titleKey = "label.delegates.consult",
            accessGroup = "delegate", hint = "Delegate")
    public static class DelegateConsultApp {

    }

//    @StrutsApplication(bundle = "DelegateResources", path = "communication", titleKey = "label.delegates.comunication",
//            accessGroup = "role(DELEGATE)", hint = "Delegate")
//    public static class DelegateMessagingApp {
//
//    }
//
//    @StrutsApplication(bundle = "DelegateResources", path = "participate", titleKey = "label.participate",
//            accessGroup = "role(DELEGATE)", hint = "Delegate")
//    public static class DelegateParticipateApp {
//
//    }

    @StrutsFunctionality(app = DelegateConsultApp.class, path = "evaluations", titleKey = "link.evaluations",
            accessGroup = "delegate")
    @Mapping(path = "/evaluationsForDelegates", module = "delegate")
    public static class EvaluationsForDelegatesAction extends Action {

        @Override
        public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                HttpServletResponse response) throws Exception {

            User user = Authenticate.getUser();
            Set<Delegate> activeDelegates = user.getDelegatesSet().stream().filter(d -> d.isActive()).collect(Collectors.toSet());
            if (activeDelegates.size() > 0) {
                return new ActionForward("/evaluationsForDelegates.faces?degreeID="
                        + activeDelegates.iterator().next().getDegree().getExternalId());
            }
            return null;
        }
    }

}