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
package pt.ist.fenixedu.integration.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response.Status;

import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;

@Path("/fenix/v1/parking")
public class FenixParking {

    private static final Client client = ClientBuilder.newClient();

    /**
     * get information about parking
     */

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    public String parking() {
        try {
            return client.target(FenixEduIstIntegrationConfiguration.getConfiguration().getApiParkingUrl())
                    .queryParam("username", FenixEduIstIntegrationConfiguration.getConfiguration().getApiParkingUsername())
                    .queryParam("password", FenixEduIstIntegrationConfiguration.getConfiguration().getApiParkingPassword())
                    .request().get(String.class);
        } catch (Exception e) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
}