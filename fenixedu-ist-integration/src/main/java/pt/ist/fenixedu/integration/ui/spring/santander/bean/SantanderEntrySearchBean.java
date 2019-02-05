package pt.ist.fenixedu.integration.ui.spring.santander.bean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.domain.SantanderEntryNew;

public class SantanderEntrySearchBean {

    private String username;

    private ExecutionYear executionYear;

    public SantanderEntrySearchBean() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public List<ExecutionYear> getExecutionYears() {
        return Bennu.getInstance().getSantanderEntriesNewSet().stream().map(sen -> sen.getExecutionYear()).distinct()
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    public List<SantanderEntryNew> search() {

        List<SantanderEntryNew> santanderEntryNews = new ArrayList<>();

        User user = User.findByUsername(username);

        if (user != null) {
            List<SantanderEntryNew> entries = SantanderEntryNew.getSantanderEntryHistory(user.getPerson());
            santanderEntryNews.addAll(entries);
        } else if (executionYear != null) {
            List<SantanderEntryNew> entries = Bennu.getInstance().getSantanderEntriesNewSet().stream()
                    .filter(sen -> sen.getExecutionYear().equals(executionYear))
                    .sorted(SantanderEntryNew.COMPARATOR_BY_CREATED_DATE).collect(Collectors.toList());
            santanderEntryNews.addAll(entries);
        }

        return santanderEntryNews;
    }

}
