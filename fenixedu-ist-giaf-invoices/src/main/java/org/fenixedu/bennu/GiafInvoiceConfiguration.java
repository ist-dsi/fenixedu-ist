/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.bennu;

import org.fenixedu.bennu.spring.BennuSpringModule;
import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

@BennuSpringModule(basePackages = "pt.ist.fenixedu.giaf.invoices.ui", bundles = "GiafInvoicesResources")
public class GiafInvoiceConfiguration {

    @ConfigurationManager(description = "Giaf Invoice Configuration")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "pt.indra.mygiaf.invoice.dir", defaultValue = "/dev/null",
                description = "File store used to store files regarding invoices and reciepts from GIAF.")
        public String giafInvoiceDir();

        @ConfigurationProperty(key = "pt.indra.mygiaf.client.map", defaultValue = "/dev/null",
                description = "Filename to the client list from GIAF and corresponding VAT numbers.")
        public String clientMapFilename();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
