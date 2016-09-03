/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Person;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.routing.CMSRenderer;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseRequestHandler;
import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageRequestHandler;
import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageSite;
import pt.ist.fenixedu.cmscomponents.domain.unit.UnitRequestHandler;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by borgez on 20-02-2015.
 */
@WebListener
public class FenixISTLearningContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionListener(Person.class, (person) -> {
            Site homepageSite = person.getHomepage();
            if (homepageSite != null) {
                person.setHomepage(null);
                homepageSite.getHomepageSite().delete();
            }
        });

        FenixFramework.getDomainModel().registerDeletionListener(Unit.class, (unit) -> {
            Site site = unit.getSite();
            if (site != null) {
                unit.setSite(null);
                site.delete();
            }
        });

        CMSRenderer.addHandler(new HomepageRequestHandler());
        CMSRenderer.addHandler(new UnitRequestHandler());

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
