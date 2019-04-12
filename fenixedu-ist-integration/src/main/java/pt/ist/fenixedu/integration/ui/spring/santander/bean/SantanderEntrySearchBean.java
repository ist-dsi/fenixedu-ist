package pt.ist.fenixedu.integration.ui.spring.santander.bean;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.domain.SantanderEntryNew;

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

    public List<SantanderEntryNew> search() {
        User user = User.findByUsername(username);

        if (user != null) {
            return SantanderEntryNew.getSantanderEntryHistory(user);
        }
        return new ArrayList<>();
    }

}
