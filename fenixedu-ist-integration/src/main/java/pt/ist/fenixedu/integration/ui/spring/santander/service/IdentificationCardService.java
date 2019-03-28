package pt.ist.fenixedu.integration.ui.spring.santander.service;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.idcards.domain.RegisterAction;
import org.fenixedu.idcards.service.SantanderCardMissingDataException;
import org.fenixedu.idcards.service.SantanderRequestCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pt.ist.fenixedu.integration.domain.santander.RequestCardUtils;

@Service
public class IdentificationCardService {

    private SantanderRequestCardService santanderRequestCardService;

    @Autowired
    public IdentificationCardService(SantanderRequestCardService santanderRequestCardService) {
        this.santanderRequestCardService = santanderRequestCardService;
    }

    public void createRegister(Person person, ExecutionYear executionYear, RegisterAction action)
            throws SantanderCardMissingDataException {
        String tuiEntry = RequestCardUtils.generateLine(person, executionYear, action.getName());
        santanderRequestCardService.createRegister(tuiEntry, person);
    }

    //TODO encapsulate all needed SantanderCardService functions

    public List<ExecutionYear> getExecutionYears() {
        return Bennu.getInstance().getSantanderEntriesNewSet().stream().map(sen -> sen.getExecutionYear()).distinct()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());
    }
}
