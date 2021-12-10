package pt.ist.fenixedu.integration.util;

import com.google.common.io.BaseEncoding;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.photograph.Picture;
import org.fenixedu.academic.domain.photograph.PictureMode;
import org.fenixedu.academic.ui.spring.controller.PhotographController;
import org.fenixedu.academic.util.ContentType;
import org.fenixedu.bennu.core.domain.Avatar;
import org.fenixedu.bennu.core.domain.User;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class PhotoUtils {

    public static String toBase64Png(final Person person, final boolean checkAccess) {
        final User user = person.getUser();
        final byte[] content = user == null || (checkAccess && !person.isPhotoAvailableToCurrentUser()) ? mysteryman()
                : Avatar.photoProvider.apply(user).getCustomAvatar(100, 100, PictureMode.FIT.name());
        return BaseEncoding.base64().encode(content);
    }

    public static String toBase64Jpeg(final Person person) {
        final User user = person.getUser();
        final Avatar.PhotoProvider photoProvider = user == null ? null : Avatar.photoProvider.apply(user);
        return photoProvider == null ? null : toBase64Jpeg(photoProvider);
    }

    private static String toBase64Jpeg(final Avatar.PhotoProvider photoProvider) {
        final byte[] avatar = photoProvider.getCustomAvatar(100, 100, PictureMode.FIT.name());
        final BufferedImage image = Picture.readImage(avatar);
        final BufferedImage jpeg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        jpeg.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);
        return BaseEncoding.base64().encode(Picture.writeImage(jpeg, ContentType.JPG));
    }

    private static byte[] mysteryman() {
        try (final InputStream mm = PhotographController.class.getClassLoader().getResourceAsStream("META-INF/resources/img/mysteryman.png")) {
            return Avatar.process(mm, "image/png", 100);
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

}
