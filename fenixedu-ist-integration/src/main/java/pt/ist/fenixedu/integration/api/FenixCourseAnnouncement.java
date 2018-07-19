package pt.ist.fenixedu.integration.api;

import org.fenixedu.cms.domain.Post;
import pt.ist.fenixedu.integration.api.beans.FenixPostAuthor;

public class FenixCourseAnnouncement {
    private String title;
    private String description;
    private String link;
    private FenixPostAuthor author;
    private String date;

    public FenixCourseAnnouncement(Post post) {
        this.title = post.getName().getContent();
        this.description = post.getBody().getContent();
        this.link = post.getAddress();
        if (post.getCreatedBy().getProfile() != null && post.getCreatedBy().getProfile().getPerson() != null) {
            this.author = new FenixPostAuthor(post.getCreatedBy().getProfile().getPerson());
        }
        this.date = post.getCreationDate().toString("yyyy-MM-dd HH:mm:ss");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public FenixPostAuthor getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
