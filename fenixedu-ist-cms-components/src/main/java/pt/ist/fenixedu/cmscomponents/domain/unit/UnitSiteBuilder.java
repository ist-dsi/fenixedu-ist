package pt.ist.fenixedu.cmscomponents.domain.unit;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;

/**
 * Created by diutsu on 30/01/17.
 */
public class UnitSiteBuilder extends UnitSiteBuilder_Base {
    
    private UnitSiteBuilder(){
        super();
        this.setSlug(UnitSiteBuilder.class.getSimpleName());
        Bennu.getInstance().getSiteBuildersSet().add(this);
    }
    
    public static UnitSiteBuilder getInstance(){
        return Bennu.getInstance().getSiteBuildersSet().stream().filter(siteBuilder -> siteBuilder instanceof UnitSiteBuilder)
                .map(siteBuilder -> (UnitSiteBuilder) siteBuilder)
                .findFirst().orElseGet(()->new UnitSiteBuilder());
    }
    
    public Site create(LocalizedString name, LocalizedString description, String slug){
        Site site = super.create(name, description);
        site.setSlug(slug);
        return site;
    }
    
    public Site create(Unit unit) {
        return create(unit.getNameI18n(), unit.getNameI18n(), unit.getAcronym());
    }
    
    public Site create(Department department) {
        return create(department.getDepartmentUnit().getNameI18n(),
                department.getDepartmentUnit().getNameI18n(),
                department.getAcronym());
    }
    
}
