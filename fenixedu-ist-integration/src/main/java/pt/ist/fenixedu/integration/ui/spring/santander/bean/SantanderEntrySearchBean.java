package pt.ist.fenixedu.integration.ui.spring.santander.bean;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.domain.SantanderEntry;

public class SantanderEntrySearchBean {

    private String username;

    public SantanderEntrySearchBean() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<SantanderEntry> search() {
        User user = User.findByUsername(username);

        if (user != null) {
            return SantanderEntry.getSantanderEntryHistory(user);
        }
        return new ArrayList<>();
    }

}
