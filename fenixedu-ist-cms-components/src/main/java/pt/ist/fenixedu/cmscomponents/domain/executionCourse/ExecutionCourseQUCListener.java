package pt.ist.fenixedu.cmscomponents.domain.executionCourse;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.Atomic;

import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;
import static org.fenixedu.cms.domain.component.Component.forType;

/**
 * Created by diutsu on 05/04/17.
 */
public class ExecutionCourseQUCListener {
    
    
    public static final String BUNDLE = "resources.FenixEduQucResources";
    public static final LocalizedString QUC_TITLE = getLocalizedString(BUNDLE, "link.coordinator.QUCResults");
    
    public static void createQUCComponent() {
        ExecutionSemester.readActualExecutionSemester().getPreviousExecutionPeriod().getAssociatedExecutionCoursesSet().stream()
                .map(executionCourse -> executionCourse.getSite())
                .filter(site -> site.getPagesSet().stream().noneMatch(
                        page -> page.getComponentsSet().stream().anyMatch(component -> component.componentType().equals(ExecutionCourseQUCComponent.class))
                ))
                .forEach(site -> createComponent(site));
    }
    
    @Atomic(mode = Atomic.TxMode.WRITE)
    private static void createComponent(Site site) {
        Authenticate.mock(site.getCreatedBy());
        Menu menu = site.getMenusSet().stream().filter(m -> m.getPrivileged()).findAny().get();
        Page.create(site, menu, null, QUC_TITLE, true, "evaluations", site.getCreatedBy(), forType(ExecutionCourseQUCComponent.class));
        Authenticate.unmock();
    }
}
