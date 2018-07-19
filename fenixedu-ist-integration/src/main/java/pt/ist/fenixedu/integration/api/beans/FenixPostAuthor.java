package pt.ist.fenixedu.integration.api.beans;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;

public class FenixPostAuthor {

    private String name;
    private String email;
    private String username;

    public FenixPostAuthor(Person person) {
        this.name = person.getDisplayName();

        final EmailAddress defaultEmailAddress = person.getDefaultEmailAddress();
        this.email = defaultEmailAddress != null ? defaultEmailAddress.getPresentationValue() : "";

        this.username = person.getUsername();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}