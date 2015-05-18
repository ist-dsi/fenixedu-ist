package pt.ist.fenixedu.integration.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafAssiduityTeamResponsible;
import pt.ist.fenixedu.contracts.persistenceTierOracle.view.GiafEmployeeAssiduity;

@Path("/fenix/v1/employeeAssiduity")
public class EmployeeAssiduityResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/employee/{username}")
    public String getEmployeeAssiduityInformation(@PathParam("username") String username) {
        return GiafEmployeeAssiduity.readAssiduityOfEmployee(username).getAsString();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/responsible/{username}")
    public String getAssiduityInformationForEmployees(@PathParam("username") String username) {
        return GiafAssiduityTeamResponsible.readListOfAssiduityEmployees(username).getAsString();
    }

}
