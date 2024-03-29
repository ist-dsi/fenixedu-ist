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
package org.fenixedu.bennu;

import org.fenixedu.bennu.spring.BennuSpringModule;
import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@BennuSpringModule(basePackages = { "pt.ist.fenixedu.integration" }, bundles = "FenixEduIstIntegrationResources")
public class FenixEduIstIntegrationConfiguration {

    @ConfigurationManager(description = "Fenixedu IST Configuration")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "pt.ist.fenixedu.integration.scholarThesesToken",
                description = "Token used for scholar authentication")
        public String scholarThesesToken();
    }

    public static FenixEduIstIntegrationConfiguration.ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(FenixEduIstIntegrationConfiguration.ConfigurationProperties.class);
    }

}
