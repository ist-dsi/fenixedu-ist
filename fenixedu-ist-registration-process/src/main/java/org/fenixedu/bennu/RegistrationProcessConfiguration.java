package org.fenixedu.bennu;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class RegistrationProcessConfiguration {

    @ConfigurationManager(description = "Registration Process Configuration")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "signer.url", defaultValue = "https://localhost:8443/")
        public String signerUrl();

        @ConfigurationProperty(key = "signer.jwt.secret")
        String signerJwtSecret();

        @ConfigurationProperty(key = "signer.jwt.user", defaultValue = "fenix")
        public String signerJwtUser();

        @ConfigurationProperty(key = "signer.queue.alameda", defaultValue = "1")
        public String signerAlamedaQueue();

        @ConfigurationProperty(key = "signer.queue.taguspark", defaultValue = "2")
        public String signerTagusparkQueue();

        @ConfigurationProperty(key = "certifier.url")
        public String certifierUrl();

        @ConfigurationProperty(key = "certifier.jwt.secret")
        String certifierJwtSecret();

        @ConfigurationProperty(key = "store.url", defaultValue = "http://localhost:8080/drive")
        public String storeUrl();

        @ConfigurationProperty(key = "store.app.id")
        public String storeAppId();

        @ConfigurationProperty(key = "store.app.refresh.token")
        public String storeAppRefreshToken();

        @ConfigurationProperty(key = "store.user")
        public String storeAppUser();

        @ConfigurationProperty(key = "store.directory.name", defaultValue = "documentos-oficiais/declaracoes")
        public String storeDirectoryName();
    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

    public static byte[] signerJwtSecret() {
        return getConfiguration().signerJwtSecret().getBytes();
    }

    public static byte[] certifierJwtSecret() {
        return getConfiguration().certifierJwtSecret().getBytes();
    }

    public final static String RESOURCE_BUNDLE = "resources.RegistrationProcessResources";

}
