package pt.ist.fenixedu.integration.domain;

import org.fenixedu.academic.domain.Person;

public class CardDataAuthorizationLog extends CardDataAuthorizationLog_Base {
    
    public CardDataAuthorizationLog(String title, String description, String answer, Person person) {
        super();
        setTitle(title);
        setDescription(description);
        setAnswer(answer);
        setPerson(person);
    }
    
}
