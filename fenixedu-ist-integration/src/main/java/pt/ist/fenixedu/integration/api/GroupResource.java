package pt.ist.fenixedu.integration.api;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.exceptions.BennuCoreDomainException;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.fenixedu.commons.stream.StreamUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Path("/_internal/groups")
public class GroupResource {

    @POST
    @Path("members")
    @OAuthEndpoint(value = "_internal", serviceOnly = true)
    public JsonElement members(@FormParam("groupExpression") String groupExpression) {
        try {
            Group group = Group.parse(groupExpression);
            JsonObject result = new JsonObject();
            result.addProperty("groupExpression", groupExpression);
            result.add("members", group.getMembers().map(User::getUsername).map(JsonPrimitive::new)
                    .collect(StreamUtils.toJsonArray()));
            return result;
        } catch (BennuCoreDomainException e) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    @POST
    @Path("belongs")
    @OAuthEndpoint(value = "_internal", serviceOnly = true)
    public JsonElement belongs(@FormParam("groupExpression") String groupExpression, @FormParam("username") String username) {
        User user = User.findByUsername(username);

        if (user == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        try {
            Group group = Group.parse(groupExpression);
            JsonObject result = new JsonObject();
            result.addProperty("groupExpression", groupExpression);
            result.addProperty("username", username);
            result.addProperty("belongs", group.isMember(user));
            return result;
        } catch (BennuCoreDomainException e) {
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }
}