package pt.ist.fenixedu.cmscomponents.domain.homepage;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.cms.domain.CMSTheme;
import org.fenixedu.cms.domain.Role;
import org.fenixedu.cms.domain.RoleTemplate;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.consistencyPredicates.ConsistencyPredicate;

/**
 * Created by diutsu on 31/01/17.
 */
public class HomepageSiteBuilder extends HomepageSiteBuilder_Base {
    
    
    public HomepageSiteBuilder() {
        this.setTheme(CMSTheme.forType("fenixedu-homepages-theme"));
        this.setSlug(HomepageSite.class.getSimpleName());
    }
    
    public static HomepageSiteBuilder getInstance() {
        return Bennu.getInstance().getSiteBuildersSet().stream().filter(siteBuilder -> siteBuilder instanceof HomepageSiteBuilder)
                .map(siteBuilder -> (HomepageSiteBuilder) siteBuilder)
                .findFirst().orElseGet(() -> new HomepageSiteBuilder());
    }
    
    public Site create(Person person) {
        Site site = super.create(
                new LocalizedString(I18N.getLocale(), person.getProfile().getDisplayName()),
                new LocalizedString(I18N.getLocale(), person.getProfile().getDisplayName()));
        
        site.setSlug(person.getUsername());
        
        new HomepageSite(site);
        site.setOwner(person);
        
        
        RoleTemplate roleTemplate = site.getBuilder().getRoleTemplateSet().stream().findAny().orElseThrow(() -> new DomainException("error.missing.role"));
        Role editor = new Role(roleTemplate, site);
        editor.setGroup(Group.users(person.getUser()));
        
        return site;
    }
    
}
