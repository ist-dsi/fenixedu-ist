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

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class FenixEduIstIntegrationConfiguration {
    @ConfigurationManager(description = "FenixEdu IST Integration specific properties")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "fenix.api.news.rss.url.pt")
        public String getFenixApiNewsRSSUrlPt();

        @ConfigurationProperty(key = "fenix.api.news.rss.url.en")
        public String getFenixApiNewsRSSUrlEn();

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

        @ConfigurationProperty(key = "fenix.api.canteen.defaultName", defaultValue = "alameda")
        public String getFenixAPICanteenDefaultName();

        @ConfigurationProperty(key = "externalServices.ISTConnect.password")
        public String getExternalServicesISTConnectPassword();

        @ConfigurationProperty(key = "externalServices.ISTConnect.username")
        public String getExternalServicesISTConnectUsername();

        @ConfigurationProperty(key = "externalServices.koha.password")
        public String getExternalServicesKohaPassword();

        @ConfigurationProperty(key = "externalServices.koha.username")
        public String getExternalServicesKohaUsername();

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

        @ConfigurationProperty(key = "store.quota.warning.email",
                description = "The email address to send notifications about the AFS store quotas.")
        public String getStoreQuotaWarningEmail();

        @ConfigurationProperty(key = "api.parking.url", description = "The url to get parking information")
        public String getApiParkingUrl();

        @ConfigurationProperty(key = "api.parking.username", description = "The username for parking information")
        public String getApiParkingUsername();

        @ConfigurationProperty(key = "api.parking.password", description = "The password for parking information")
        public String getApiParkingPassword();

        @ConfigurationProperty(key = "pushnotifications.server", defaultValue = "http://127.0.0.1:8000/")
        public String getPushNotificationsServer();

        @ConfigurationProperty(key = "pushnotifications.token", defaultValue = "someaccesstoken")
        public String getPushNotificationsToken();

        @ConfigurationProperty(key = "ist.cas.enabled", defaultValue = "false")
        public boolean istCasEnable();
    
        @ConfigurationProperty(key = "scheduler.watchdog.file", defaultValue = "/tmp/fenix-scheduler-watchdog")
        public String getSchedulerWatchdogFilePath();

        @ConfigurationProperty(key = "ticketing.url", defaultValue = "http://127.0.0.1:8000/")
        public String getTicketingUrl();

        @ConfigurationProperty(key = "ticketing.jwt.secret")
        public String getTicketingJwtSecret();
    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
