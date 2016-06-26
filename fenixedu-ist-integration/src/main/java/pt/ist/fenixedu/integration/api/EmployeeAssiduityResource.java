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

import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.google.common.base.Strings;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafAssiduityTeamResponsible;
import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafEmployeeAssiduity;

@Path("/fenix/v1/employeeAssiduity")
public class EmployeeAssiduityResource {

    public static final String ASSIDUITY_SCOPE = "ASSIDUITY";
    public static final String dayPattern = "yyyy-MM-dd";

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    @Path("/employee")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getEmployeeAssiduityInformation(final @QueryParam("date") String date, final @QueryParam("username") String username) {
        final User userToQuery = username == null ? null : User.findByUsername(username);
        final LocalDate dateToQuery = !Strings.isNullOrEmpty(date) ? parse(date) : new LocalDate();
        return respond(user -> GiafEmployeeAssiduity.readAssiduityOfEmployee(userToQuery == null ? user : userToQuery, dateToQuery, user));
    }

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    @Path("/responsible")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getAssiduityInformationForEmployees(final @QueryParam("date") String date) {
        final LocalDate dateToQuery = !Strings.isNullOrEmpty(date) ? parse(date) : new LocalDate();
        return respond(user -> GiafAssiduityTeamResponsible.readListOfAssiduityEmployees(user, dateToQuery));
    }

    private String respond(final Function<User, JsonObject> function) {
        final User user = Authenticate.getUser();
        return user == null ? "{}" : function.apply(user).toString();
    }

    private LocalDate parse(final String date) {
        try {
            return DateTimeFormat.forPattern(dayPattern).parseLocalDate(date);
        } catch (Exception e) {
            throw newApplicationError(Response.Status.PRECONDITION_FAILED, "format_error", "day must be " + dayPattern);
        }
    }

    private WebApplicationException newApplicationError(Response.Status status, String error, String description) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", error);
        errorObject.addProperty("description", description);
        return new WebApplicationException(Response.status(status).entity(errorObject.toString()).build());
    }
}
