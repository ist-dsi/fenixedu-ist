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
package pt.ist.fenixedu.cmscomponents.domain.homepage.components;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.CMSComponent;
import org.fenixedu.cms.exceptions.ResourceNotFoundException;

import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageSite;

/**
 * Created by borgez on 02-12-2014.
 */
public abstract class HomepageSiteComponent implements CMSComponent {

    protected Person owner(Page page) {
        return site(page).getOwner();
    }

    protected Site site(Page page) {
        if (page.getSite().getOwner()!=null) {
            return page.getSite();
        }
        throw new ResourceNotFoundException();
    }

    public static boolean supportsSite(Site site) {
        return site.getHomepageSite()!=null;
    }
}
