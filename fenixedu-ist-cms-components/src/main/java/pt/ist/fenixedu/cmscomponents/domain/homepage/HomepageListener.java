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
package pt.ist.fenixedu.cmscomponents.domain.homepage;

import static org.fenixedu.academic.domain.contacts.WebAddress.createWebAddress;
import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;

import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.TeacherGroup;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.cms.domain.*;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixedu.cmscomponents.domain.homepage.components.PresentationComponent;
import pt.ist.fenixedu.cmscomponents.domain.homepage.components.ResearcherComponent;

/**
 * Created by borgez on 24-11-2014.
 */
public class HomepageListener {
    private static final String BUNDLE = "resources.FenixEduLearningResources";
    private static final String INTERESTS_KEY = "homepage.researcher.interests";
    private static final String PATENTS_KEY = "homepage.researcher.patents";
    private static final String PUBLICATIONS_KEY = "homepage.researcher.publications";
    private static final String ACTIVITIES_KEY = "homepage.researcher.activities";
    private static final String PRIZES_KEY = "homepage.researcher.prizes";

    private static final LocalizedString PRESENTATION_TITLE = getLocalizedString(BUNDLE, "homepage.presentation.title");
    private static final LocalizedString MENU_TITLE = getLocalizedString("resources.FenixEduLearningResources", "label.menu");
    private static final LocalizedString INTERESTS_TITLE = getLocalizedString(BUNDLE, INTERESTS_KEY);
    private static final LocalizedString PATENTS_TITLE = getLocalizedString(BUNDLE, PATENTS_KEY);
    private static final LocalizedString PUBLICATIONS_TITLE = getLocalizedString(BUNDLE, PUBLICATIONS_KEY);
    private static final LocalizedString ACTIVITIES_TITLE = getLocalizedString(BUNDLE, ACTIVITIES_KEY);
    private static final LocalizedString PRIZES_TITLE = getLocalizedString(BUNDLE, PRIZES_KEY);

    private static final String HOMEPAGE_FOLDER_PATH = "homepage";

    public static Site create(Person person) {
        LocalizedString name = new LocalizedString(I18N.getLocale(), person.getProfile().getDisplayName());
        Site newsite = new Site(name,name);
        Menu menu = new Menu(newsite, MENU_TITLE);

        new HomepageSite(newsite);
        newsite.setOwner(person);

        newsite.setTheme(CMSTheme.forType("fenixedu-homepages-theme"));
        createDefaultContents(newsite, menu, person.getUser());
        getHomepageFolder().ifPresent(newsite::setFolder);
        addContact(person, newsite);
        createSiteRoles(newsite,person.getUser());
        newsite.setPublished(true);
        newsite.getHomepageSite().setShowPhoto(true);
        return newsite;
    }

    public static void createDefaultContents(Site newSite, Menu menu, User user) {
        Component presentationComponent = Component.forType(PresentationComponent.class);
        Component interestsComponent = new ResearcherComponent(INTERESTS_KEY, BUNDLE, "interests");
        Component prizesComponent = new ResearcherComponent(PRIZES_KEY, BUNDLE, "prizes");
        Component activitiesComponent = new ResearcherComponent(ACTIVITIES_KEY, BUNDLE, "activities");
        Component patentsComponent = new ResearcherComponent(PATENTS_KEY, BUNDLE, "patents");
        Component publicationsComponent = new ResearcherComponent(PUBLICATIONS_KEY, BUNDLE, "publications");

        Page initialPage =
                Page.create(newSite, menu, null, PRESENTATION_TITLE, true, "presentation", user, presentationComponent);
        Page.create(newSite, menu, null, INTERESTS_TITLE, false, "researcherSection", user, interestsComponent);
        Page.create(newSite, menu, null, PRIZES_TITLE, false, "researcherSection", user, prizesComponent);
        Page.create(newSite, menu, null, ACTIVITIES_TITLE, false, "researcherSection", user, activitiesComponent);
        Page.create(newSite, menu, null, PATENTS_TITLE, false, "researcherSection", user, patentsComponent);
        Page.create(newSite, menu, null, PUBLICATIONS_TITLE, false, "researcherSection", user, publicationsComponent);

        newSite.setInitialPage(initialPage);
    }

    private static void createSiteRoles(Site newSite,User user) {
        new Role(DefaultRoles.getInstance().getAdminRole(), newSite);
        new Role(DefaultRoles.getInstance().getAuthorRole(), newSite);
        new Role(DefaultRoles.getInstance().getContributorRole(), newSite);
        Role editor = new Role(DefaultRoles.getInstance().getEditorRole(), newSite);
        editor.setGroup(editor.getGroup().toGroup().grant(user).toPersistentGroup());
    }

    private static void addContact(Person owner, Site homepageSite) {
        createWebAddress(owner, homepageSite.getFullUrl(), PartyContactType.INSTITUTIONAL, true);
    }

    private static Optional<CMSFolder> getHomepageFolder() {
        return Bennu.getInstance().getCmsFolderSet().stream()
                .filter(folder -> HOMEPAGE_FOLDER_PATH.equals(folder.getFunctionality().getPath())).findFirst();
    }
}
