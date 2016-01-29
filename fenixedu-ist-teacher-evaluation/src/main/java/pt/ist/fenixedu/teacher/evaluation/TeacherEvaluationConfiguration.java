package pt.ist.fenixedu.teacher.evaluation;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class TeacherEvaluationConfiguration {

    @ConfigurationManager(description = "Teacher Credits Configuration")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "lastSemesterForCredits")
        public String getLastSemesterForCredits();

        @ConfigurationProperty(key = "lastYearForCredits", defaultValue = "2010/2011")
        public String getLastYearForCredits();

        @ConfigurationProperty(key = "startSemesterForCredits", defaultValue = "2")
        public String getStartSemesterForCredits();

        @ConfigurationProperty(key = "startYearForCredits", defaultValue = "2002/2003")
        public String getStartYearForCredits();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
