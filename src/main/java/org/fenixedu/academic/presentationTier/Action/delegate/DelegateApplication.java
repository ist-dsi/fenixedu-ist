/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.presentationTier.Action.delegate;

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

    @StrutsApplication(bundle = "DelegateResources", path = "communication", titleKey = "label.delegates.comunication",
            accessGroup = "delegate", hint = "Delegate")
    public static class DelegateMessagingApp {

    }

    @StrutsApplication(bundle = "DelegateResources", path = "participate", titleKey = "label.participate",
            accessGroup = "delegate", hint = "Delegate")
    public static class DelegateParticipateApp {

    }

    // TODO Fix the access group!
    @StrutsFunctionality(app = DelegateConsultApp.class, path = "evaluations", titleKey = "link.evaluations",
            accessGroup = "anyone")
    @Mapping(path = "/evaluationsForDelegates", module = "delegate")
    public static class EvaluationsForDelegatesAction extends Action {

        @Override
        public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            User user = Authenticate.getUser();
            if (user != null) {
                Delegate function = user.getDelegatesSet().stream().filter(d -> d.isActive()).findFirst().get();
                if (function != null) {
                    return new ActionForward("/evaluationsForDelegates.faces?degreeID=" + function.getDegree().getExternalId());
                }
            }
            return null;
        }
    }

}
