package pt.ist.fenixedu.integration.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;

@Path("/fenix/v1/push-notifications")
public class PushNotifications {

    private static final Client HTTP_CLIENT = ClientBuilder.newBuilder().register(JsonBodyReaderWriter.class).build();

    private final static String pushNotificationServerUrl =
            FenixEduIstIntegrationConfiguration.getConfiguration().getPushNotificationsServer();
    private final static String header = FenixEduIstIntegrationConfiguration.getConfiguration().getPushNotificationsHeader();
    private final static String token = FenixEduIstIntegrationConfiguration.getConfiguration().getPushNotificationsToken();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @OAuthEndpoint(FenixAPIv1.PERSONAL_SCOPE)
    public Response register(JsonElement payload) {

        JsonObject request = payload.getAsJsonObject();

        JsonObject pushRequest = new JsonObject();
        pushRequest.addProperty("username", Authenticate.getUser().getUsername());
        pushRequest.addProperty("device_id", request.get("device_id").getAsString());
        pushRequest.addProperty("registration_id", request.get("registration_id").getAsString());
        pushRequest.addProperty("lang", request.get("lang").getAsString());
        pushRequest.addProperty("device_type", request.get("device_type").getAsString());

        try {
            return HTTP_CLIENT.target(pushNotificationServerUrl).path("api").path("v1").path("device/").request()
                    .header(header, token).post(Entity.entity(pushRequest, MediaType.APPLICATION_JSON_TYPE));
        } catch (WebApplicationException wae) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/unregister/{device_id}")
    public Response unregister(@PathParam("device_id") String deviceId, @QueryParam("registration_id") String registrationId) {

        try {
            return HTTP_CLIENT.target(pushNotificationServerUrl).path("api").path("v1").path("device").path(deviceId + "/")
                    .queryParam("registration_id", registrationId).request().header(header, token).delete();
        } catch (WebApplicationException wae) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}