package pt.ist.fenixedu.integration.servlet;

import java.util.Locale;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.commons.stream.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@WebListener
public class PushNotificationsInitializer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationsInitializer.class);

    private static final Client HTTP_CLIENT = ClientBuilder.newBuilder().register(JsonBodyReaderWriter.class).build();

    private final static String pushNotificationServerUrl =
            FenixEduIstIntegrationConfiguration.getConfiguration().getPushNotificationsServer();
    private final static String token = FenixEduIstIntegrationConfiguration.getConfiguration().getPushNotificationsToken();

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        /***
         * registerWithoutTransaction will prevent the handler to mess with the same transaction
         * that creates the post, and runs only after the commit is successful.
         */
        Signal.<DomainObjectEvent<Post>> registerWithoutTransaction(Post.SIGNAL_CREATED, (event) -> {
            sendNotification(event);
        });
    }

    @Atomic(mode = TxMode.READ)
    private void sendNotification(DomainObjectEvent<Post> event) {
        Post post = event.getInstance();

        if (post.getSite().getExecutionCourse()!=null && post.isVisible()
                && post.getCategoriesSet().stream().anyMatch(cat -> "announcement".equalsIgnoreCase(cat.getSlug()))) {

            JsonArray usernames = post.getSite().getExecutionCourse().getAttendsSet().stream()
                    .map(a -> a.getRegistration().getPerson().getUser().getUsername()).distinct().map(JsonPrimitive::new)
                    .collect(StreamUtils.toJsonArray());

            final String postAuthor = post.getCreatedBy().getUsername();
            JsonArray teacherUsernames = post.getSite().getExecutionCourse().getTeacherGroupSet().stream()
                    .flatMap(PersistentGroup::getMembers).map(User::getUsername)
                    .filter(u -> !postAuthor.equals(u)).distinct().map(JsonPrimitive::new).collect(StreamUtils.toJsonArray());

            usernames.addAll(teacherUsernames);
            JsonObject title = new JsonObject();
            for (Locale locale : post.getName().getLocales()) {
                JsonObject localizedMessage = new JsonObject();
                title.add(locale.toLanguageTag(), "Técnico Lisboa");
            }
            JsonObject body = new JsonObject();
            for (Locale locale : post.getName().getLocales()) {
                JsonObject localizedMessage = new JsonObject();
                body.add(locale.toLanguageTag(), post.getSite().getExecutionCourse().getSigla() + ":" + post.getName().getContent(locale));
            }
            JsonObject returnObj = new JsonObject();
            returnObj.add("usernames", usernames);
            returnObj.add("title", title);
            returnObj.add("body", body);
            try {
                HTTP_CLIENT.target(pushNotificationServerUrl).path("api/v1/messages").request().header("Authorization", "Bearer "+token)
                        .post(Entity.entity(returnObj, "application/json; charset=utf8"));
            } catch (Throwable wae) {
                logger.warn("error when creating post sending message", wae);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
    }
}