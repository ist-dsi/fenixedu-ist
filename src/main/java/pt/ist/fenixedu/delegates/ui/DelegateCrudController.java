/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Spaces.
 *
 * FenixEdu Spaces is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Spaces is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Spaces.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.ui.services.DelegateService;
import pt.ist.fenixframework.FenixFramework;

@SpringFunctionality(app = DelegatesController.class, title = "title.delegates.management")
@RequestMapping("/delegates-management")
public class DelegateCrudController {

    @Autowired
    DelegateService delegateService;

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model) {
        DelegateSearchBean delegateSearchBean = new DelegateSearchBean();
        return search(delegateSearchBean, model);
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String search(@ModelAttribute DelegateSearchBean searchBean, Model model) {
        searchBean.setExecutionYear(ExecutionYear.readCurrentExecutionYear());
        searchBean = delegateService.generateNewBean(searchBean);
        model.addAttribute("searchBean", searchBean);
        Set<DelegateBean> delegateBeanSet =
                delegateService.searchDelegates(searchBean, DateTime.now()).stream().collect(Collectors.toSet());
        delegateBeanSet.addAll(delegateService.getDegreePositions(searchBean.getDegree()));
        model.addAttribute("delegates", delegateBeanSet.stream().distinct().collect(Collectors.toList()));
        return search(model);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(Model model) {
        model.addAttribute("action", "/delegates-management/search");
        model.addAttribute("terminate", "/delegates-management/terminatePosition");
        model.addAttribute("assign", "/delegates-management/attributePosition");
        return "delegates/management/search";
    }

    private Object getObjectIf(String objId) {
        if (objId.isEmpty()) {
            return null;
        }
        return FenixFramework.getDomainObject(objId);
    }

    @RequestMapping(value = "/attributePosition/{degree}", method = RequestMethod.GET)
    public String attributePosition(@PathVariable Degree degree, @RequestParam(defaultValue = "") String delegateOID,
            @RequestParam(defaultValue = "") String curricularYear, @RequestParam(defaultValue = "") String cycleType, Model model) {
        CycleType ct = null;
        try {
            if (!cycleType.isEmpty()) {
                ct = CycleType.valueOf(cycleType);
            }
            Delegate delegate = (Delegate) getObjectIf(delegateOID);
            if (delegate != null && delegate.getDegree() != degree) {
                return toSearch(degree, model);
            }
        } catch (IllegalArgumentException iae) {
            return toSearch(degree, model);
        }
        model.addAttribute("delegatePositionBean", new DelegatePositionBean((Delegate) getObjectIf(delegateOID), ct,
                (CurricularYear) getObjectIf(curricularYear), degree));
        model.addAttribute("action", "/delegates-management/attributePosition");
        return "delegates/management/attribute";
    }

    @RequestMapping(value = "/attributePosition", method = RequestMethod.POST)
    public String attributePosition(@ModelAttribute DelegatePositionBean delegatePositionBean, BindingResult errors, Model model) {
        delegateService.attributeDelegatePosition(delegatePositionBean);
        return toSearch(delegatePositionBean.getDegree(), model);
    }

    private String toSearch(Degree degree, Model model) {
        DelegateSearchBean delegateSearchBean = new DelegateSearchBean();
        delegateSearchBean.setDegree(degree);
        return search(delegateSearchBean, model);
    }

    @RequestMapping(value = "/terminatePosition/{degree}/{delegate}", method = RequestMethod.GET)
    public String terminatePosition(@PathVariable Degree degree, @PathVariable Delegate delegate, Model model) {
        if (degree == delegate.getDegree()) {
            delegateService.terminateDelegatePosition(delegate);
        }
        return toSearch(degree, model);
    }
}
