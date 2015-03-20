/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Pre Bolonha.
 *
 * FenixEdu IST Pre Bolonha is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Pre Bolonha is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Pre Bolonha.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.ui.struts.action;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduPreBolonhaContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionBlockerListener(ExecutionDegree.class, (executionDegree, blockers) -> {
            if (executionDegree.getMasterDegreeCandidatesSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "execution.degree.cannot.be.deleted"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Person.class, (person, blockers) -> {
            if (person.getMasterDegreeCandidatesSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.person.cannot.be.deleted"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionListener(StudentCurricularPlan.class, (studentCurricularPlan) -> {
            studentCurricularPlan.setMasterDegreeThesis(null);
        });

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
