package pt.ist.fenixedu.cmscomponents.domain.unit;

import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.rendering.TemplateContext;
import org.fenixedu.cms.routing.CMSRenderer;

/**
 * Created by diutsu on 01/03/16.
 */
public class UnitRequestHandler implements CMSRenderer.RenderingPageHandler {
    @Override
    public void accept(Page page, TemplateContext templateContext) {
        if(page.getSite().getUnit() !=null ){
            templateContext.put("siteObject",page.getSite().getUnit());
        }
    }
}
