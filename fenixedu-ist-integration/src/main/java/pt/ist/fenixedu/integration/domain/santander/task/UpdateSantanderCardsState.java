package pt.ist.fenixedu.integration.domain.santander.task;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.exception.SantanderNoRoleAvailableException;
import org.fenixedu.santandersdk.exception.SantanderValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Updates User Santander Card States", readOnly = true)
public class UpdateSantanderCardsState extends CronTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSantanderCardsState.class);

    private SantanderIdCardsService service;

    @Override
    public void runTask() throws Exception {
        service = BennuSpringContextHelper.getBean(SantanderIdCardsService.class);
        Bennu.getInstance().getUserSet().stream().forEach(user -> updateCardState(user));
    }

    public void updateCardState(User user) {
        SantanderEntry entry = service.getOrUpdateState(user);
        if (entry == null || entry.canRenovateCard()) {
            FenixFramework.atomic(() -> {
                try {
                    SantanderEntry createRegister = service.createRegister(user);
                    service.sendRegister(user, createRegister);
                } catch (SantanderNoRoleAvailableException e) {
                    LOGGER.debug("No role available for {}", user.getUsername());
                } catch (SantanderValidationException sve) {
                    LOGGER.error(String.format("error generating card for %s%n", user.getUsername()), sve);
                }
            });
        
        }
    }
}