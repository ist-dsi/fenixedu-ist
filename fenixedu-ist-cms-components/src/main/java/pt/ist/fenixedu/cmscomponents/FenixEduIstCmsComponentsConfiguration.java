package pt.ist.fenixedu.cmscomponents;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class FenixEduIstCmsComponentsConfiguration {
    @ConfigurationManager(description = "FenixEdu IST CMS Components specific properties")
    public interface ConfigurationProperties {
        @ConfigurationProperty(key = "sotisURL", defaultValue = "https://sotis.tecnico.ulisboa.pt")
        public String sotisURL();
    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }
}
