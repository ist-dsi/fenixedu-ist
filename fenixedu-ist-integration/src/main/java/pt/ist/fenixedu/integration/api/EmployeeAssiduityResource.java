package pt.ist.fenixedu.integration.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;

import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafAssiduityTeamResponsible;
import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafEmployeeAssiduity;

import com.google.gson.JsonObject;

@Path("/fenix/v1/employeeAssiduity")
public class EmployeeAssiduityResource {

    public static final String ASSIDUITY_SCOPE = "ASSIDUITY";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/employee")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getEmployeeAssiduityInformation() {
        return respond(user -> GiafEmployeeAssiduity.readAssiduityOfEmployee(user));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/responsible")
    @OAuthEndpoint(ASSIDUITY_SCOPE)
    public String getAssiduityInformationForEmployees() {
        return respond(user -> GiafAssiduityTeamResponsible.readListOfAssiduityEmployees(user));
    }

    private String respond(java.util.function.Function<User, JsonObject> function) {
        final User user = Authenticate.getUser();
        return user == null ? "{}" : function.apply(user).getAsString();
    }

}
