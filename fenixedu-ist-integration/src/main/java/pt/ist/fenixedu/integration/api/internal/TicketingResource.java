package pt.ist.fenixedu.integration.api.internal;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.DateTime;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.Key;

@Path("/ticketing")
public class TicketingResource {
    public static final String TICKETS_SCOPE = "TICKETS";

    private static final Client HTTP_CLIENT = ClientBuilder.newBuilder().register(JsonBodyReaderWriter.class).build();

    private static final String TICKET_SERVER_URL = FenixEduIstIntegrationConfiguration.getConfiguration().getTicketingUrl();
    private static final String JWT_SIGNING_KEY = FenixEduIstIntegrationConfiguration.getConfiguration().getTicketingJwtSecret();
    private static final String HEADER = "Authorization";
    private static final String AUTHORIZATION_TYPE = "Token";
    private static final int JWT_EXPIRATION_PERIOD = 6;

    @GET
    @Path("services")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getServices() {
        try {
            return getRootWebTarget().path("services")
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch (RuntimeException wae) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("services/{id}/queues")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getQueues(@PathParam("id") String serviceId) {
        try {
            return getRootWebTarget().path("services").path(serviceId).path("queues")
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch (WebApplicationException wae) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("services/{id}/queues/{queueId}/tickets")
    @Consumes(MediaType.APPLICATION_JSON)
    @OAuthEndpoint(TICKETS_SCOPE)
    public Response requestTicket(@PathParam("id") String serviceId, @PathParam("queueId") String queueId) {
        User user = Authenticate.getUser();
        String accessToken = getUserToken(user);

        try {
            return getRootWebTarget().path("services").path(serviceId).path("queues").path(queueId).path("tickets/")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HEADER, String.format("%s %s", AUTHORIZATION_TYPE, accessToken))
                    .post(Entity.json(""));
        } catch (WebApplicationException wae) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("tickets")
    @Consumes(MediaType.APPLICATION_JSON)
    @OAuthEndpoint(TICKETS_SCOPE)
    public Response getTickets() {
        User user = Authenticate.getUser();
        String accessToken = getUserToken(user);

        try {
            return getRootWebTarget().path("user").path("tickets")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HEADER, String.format("%s %s", AUTHORIZATION_TYPE, accessToken))
                    .get();
        } catch (WebApplicationException wae) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private WebTarget getRootWebTarget() {
        return HTTP_CLIENT.target(TICKET_SERVER_URL).path("api").path("v1");
    }

    private String getUserToken(User user) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        byte[] apiKeySecretBytes = JWT_SIGNING_KEY.getBytes();
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(DateTime.now().toDate())
                .setExpiration(DateTime.now().plusHours(JWT_EXPIRATION_PERIOD).toDate())
                .setSubject(user.getUsername())
                .claim("name", user.getName())
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
}
