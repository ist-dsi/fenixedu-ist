package pt.ist.fenixedu.integration.api;

import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafAssiduityTeamResponsible;
import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafEmployeeAssiduity;

import com.google.gson.JsonObject;

@Path("/fenix/v1/employeeAssiduity")
public class EmployeeAssiduityResource {

    public static final String ASSIDUITY_SCOPE = "ASSIDUITY";

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    @Path("/employee")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getEmployeeAssiduityInformation(final @QueryParam("date") String date) {
        return respond(user -> GiafEmployeeAssiduity.readAssiduityOfEmployee(user, parse(date)));
    }

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    @Path("/responsible")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getAssiduityInformationForEmployees(final @QueryParam("date") String date) {
        return respond(user -> GiafAssiduityTeamResponsible.readListOfAssiduityEmployees(user, parse(date)));
    }

    private String respond(final Function<User, JsonObject> function) {
        final User user = Authenticate.getUser();
        return user == null ? "{}" : function.apply(user).toString();
    }

    private LocalDate parse(final String date) {
        return DateTimeFormat.forPattern("yyyy-MM-dd").parseLocalDate(date);
    }

}
