/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration;

import java.util.Map;

import org.fenixedu.bennu.portal.domain.PortalConfiguration;
import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

import pt.ist.fenixedu.integration.util.HostAccessControl;

public class FenixEduIstIntegrationConfiguration {
    @ConfigurationManager(description = "FenixEdu IST Integration specific properties")
    public interface ConfigurationProperties {
        @ConfigurationProperty(key = "barra.as.authentication.broker",
                description = "CAS ticket validation through barra: https://fenix-ashes.ist.utl.pt/fenixWiki/Barra",
                defaultValue = "false")
        public Boolean barraAsAuthenticationBroker();

        @ConfigurationProperty(key = "fenix.api.events.rss.url.pt")
        public String getFenixApiEventsRSSUrlPt();

        @ConfigurationProperty(key = "fenix.api.news.rss.url.pt")
        public String getFenixApiNewsRSSUrlPt();

        @ConfigurationProperty(key = "fenix.api.events.rss.url.en")
        public String getFenixApiEventsRSSUrlEn();

        @ConfigurationProperty(key = "fenix.api.news.rss.url.en")
        public String getFenixApiNewsRSSUrlEn();

        @ConfigurationProperty(key = "fenix.api.canteen.file",
                description = "if is not defined it will fallback to the same property url", defaultValue = "")
        public String getFenixApiCanteenFile();

        @ConfigurationProperty(key = "fenix.api.shuttle.file",
                description = "if is not defined it will fallback to the same property url", defaultValue = "")
        public String getFenixApiShuttleFile();

        @ConfigurationProperty(key = "fenix.api.contacts.file",
                description = "if is not defined it will fallback to the same property url", defaultValue = "")
        public String getFenixApiContactsFile();

        @ConfigurationProperty(key = "fenix.api.canteen.url", defaultValue = "")
        public String getFenixApiCanteenUrl();

        @ConfigurationProperty(key = "fenix.api.shuttle.url", defaultValue = "")
        public String getFenixApiShuttleUrl();

        @ConfigurationProperty(key = "fenix.api.contacts.url", defaultValue = "")
        public String getFenixApiContactsUrl();

        @ConfigurationProperty(key = "fenix.api.canteen.user", defaultValue = "")
        public String getFenixApiCanteenUser();

        @ConfigurationProperty(key = "fenix.api.canteen.secret", defaultValue = "")
        public String getFenixApiCanteenSecret();

        @ConfigurationProperty(key = "barra.loginUrl",
                description = "Login URL to use when barra is set as authentication broker")
        public String barraLoginUrl();

        @ConfigurationProperty(key = "dges.username.prefix",
                description = "The prefix for the username of students created via the DGES Student Importation Process.",
                defaultValue = "ist1")
        public String dgesUsernamePrefix();

        @ConfigurationProperty(key = "ciistCostCenterCode", description = "Deprecated, to be removed", defaultValue = "8431")
        public Integer getCIISTCostCenterCode();

        @ConfigurationProperty(key = "externalServices.ISTConnect.password")
        public String getExternalServicesISTConnectPassword();

        @ConfigurationProperty(key = "externalServices.ISTConnect.username")
        public String getExternalServicesISTConnectUsername();

        @ConfigurationProperty(key = "externalServices.koha.password")
        public String getExternalServicesKohaPassword();

        @ConfigurationProperty(key = "externalServices.koha.username")
        public String getExternalServicesKohaUsername();

        @Deprecated
        @ConfigurationProperty(
                key = "host.control.name.*",
                description = "Comma separated hostname values that are allowed to access the url host.control.name. See HostAccessControl.isAllowed(name, request)")
        public Map<String, String> getHostControlName();

        @ConfigurationProperty(
                key = "sibs.destinationInstitutionId",
                description = "used in header payments file and represents entity service identification (i.e. sibs). Default value '50000000' (sibs identification)",
                defaultValue = "50000000")
        public String getSibsDestinationInstitutionId();

        @ConfigurationProperty(
                key = "sibs.sourceInstitutionId",
                description = "used in header payments file, and represents institution identification accordding to transfer service (i.e. sibs). Must be given by the entity that is peforming this service. Format: '9XXXXXXX'",
                defaultValue = "11111111")
        public String getSibsSourceInstitutionId();

        @ConfigurationProperty(key = "ldap.sync.services.password")
        public String ldapSyncServicesPassword();

        @ConfigurationProperty(key = "ldap.sync.services.username")
        public String ldapSyncServicesUsername();

        @ConfigurationProperty(key = "legacyFilesRedirectMapLocation", defaultValue = "")
        public String legacyFilesRedirectMapLocation();

        @ConfigurationProperty(key = "merge.units.emails",
                description = "comma separated emails of persons who want to receive emails about merge of units.")
        public String getMergeUnitsEmails();

        @ConfigurationProperty(key = "store.quota.warning.email",
                description = "The email address to send notifications about the AFS store quotas.")
        public String getStoreQuotaWarningEmail();

        @ConfigurationProperty(key = "api.parking.url", description = "The url to get parking information")
        public String getApiParkingUrl();

        @ConfigurationProperty(key = "api.parking.username", description = "The username for parking information")
        public String getApiParkingUsername();

        @ConfigurationProperty(key = "api.parking.password", description = "The password for parking information")
        public String getApiParkingPassword();
    }

    public static boolean barraLogin() {
        return barraLogin && "ashes-ist".equals(PortalConfiguration.getInstance().getTheme());
    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

    @Deprecated
    public static HostAccessControl getHostAccessControl() {
        return hostAccessControl;
    }

    public static void setBarraLogin(boolean state) {
        barraLogin = state;
    }

    @Deprecated
    private static HostAccessControl hostAccessControl = new HostAccessControl(getConfiguration().getHostControlName());

    private static boolean barraLogin = getConfiguration().barraAsAuthenticationBroker();

}
