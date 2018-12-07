/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.bullet.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.GsonBuilder;

import pt.ist.fenixedu.bullet.domain.BulletObjectType;
import pt.ist.fenixedu.bullet.domain.DumpContext;

@SpringApplication(group = "logged", path = "bullet", title = "title.bullet", hint = "title.bullet")
@SpringFunctionality(app = BulletController.class, title = "title.bullet")
@RequestMapping("/bullet")
public class BulletController {

    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {
        final Collection<ExecutionSemester> semesters = Bennu.getInstance().getExecutionYearsSet().stream()
                .flatMap(y -> y.getExecutionPeriodsSet().stream())
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        model.addAttribute("semesters", semesters);
        final Collection<BulletObjectType> types = Arrays.asList(BulletObjectType.values());
        model.addAttribute("types", types);
        return "bullet/home";
    }

    @RequestMapping(path = "/{executionSemester}/exportJson", method = RequestMethod.GET, produces = "application/json")
    public void exportJson(@PathVariable final ExecutionSemester executionSemester, final Model model, final HttpServletResponse response) {
        final DumpContext context = new DumpContext(executionSemester);
        download(response, "data.json", new GsonBuilder().setPrettyPrinting().create().toJson(context.toJson()).getBytes());
    }

    @RequestMapping(path = "/{executionSemester}/{type}/exportXls", method = RequestMethod.GET, produces = "application/xlsx")
    public void exportJson(@PathVariable final ExecutionSemester executionSemester, @PathVariable final BulletObjectType type, final Model model, final HttpServletResponse response) {
        final DumpContext context = new DumpContext(executionSemester);
        download(response, "data.xlsx", context.toXlsx(type));
    }

    private void download(final HttpServletResponse response, final String filename, final byte[] content) {
        try {
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.getOutputStream().write(content);
            response.flushBuffer();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }
}
