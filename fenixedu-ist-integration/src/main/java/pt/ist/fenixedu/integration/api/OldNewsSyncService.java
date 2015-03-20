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

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.Site;
import org.joda.time.DateTime;
import org.joda.time.Interval;

// DO NOT CHANGE THIS CLASS EVER AGAIN.

@Path("/fenix-ist/oldNewsSyncService")
public class OldNewsSyncService extends BennuRestResource {
    private static final String EVENTS = "678604861135";
    private static final String NEWS = "678604861136";

    final static Locale PT = new Locale("pt");
    final static Locale EN = new Locale("en");

    @GET
    @Produces("application/xml")
    public Response get(@QueryParam("announcementBoardId") String announcementBoardId,
            @QueryParam("selectedYear") int selectedYear, @QueryParam("selectedMonth") int selectedMonth,
            @QueryParam("language") String language) {
        Set<Post> posts;
        if (announcementBoardId.equals(EVENTS)) {
            posts = Site.fromSlug("tecnicolisboa").categoryForSlug("eventos").getPostsSet();
        } else {
            posts = Site.fromSlug("tecnicolisboa").categoryForSlug("noticias").getPostsSet();
        }
        Locale locale;
        if ("pt".equals(language)) {
            locale = PT;
        } else {
            locale = EN;
        }

        DateTime begin =
                new DateTime().toDateMidnight().withYear(selectedYear).withMonthOfYear(selectedMonth).withDayOfMonth(1)
                        .toDateTime();
        DateTime end =
                begin.dayOfMonth().withMaximumValue().hourOfDay().withMaximumValue().minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue();
        Interval i = new Interval(begin, end);

        String result = "SUCCESS\n";
        result += "<list>\n";
        int index = 1;
        for (Post post : posts.stream().sorted(Post.CREATION_DATE_COMPARATOR).filter(x -> i.contains(x.getPublicationBegin()))
                .collect(Collectors.toList())) {
            result += "  <net.sourceforge.fenixedu.presentationTier.Action.externalServices.AnnouncementDTO>\n";
            result += "    <creationDate>" + post.getCreationDate().toString("dd/MM/yyyy HH:mm:ss") + "</creationDate>\n";
            result +=
                    "    <referedSubjectBegin>"
                            + (post.getPublicationBegin() != null ? post.getPublicationBegin().toString("dd/MM/yyyy HH:mm:ss") : "")
                            + "</referedSubjectBegin>\n";
            result +=
                    "    <publicationBegin>"
                            + (post.getPublicationBegin() != null ? post.getPublicationBegin().toString("dd/MM/yyyy HH:mm:ss") : "")
                            + "</publicationBegin>\n";
            result +=
                    "    <publicationEnd>"
                            + (post.getPublicationEnd() != null ? post.getPublicationEnd().toString("dd/MM/yyyy HH:mm:ss") : "")
                            + "</publicationEnd>\n";
            result +=
                    "    <lastModification>"
                            + (post.getModificationDate() != null ? post.getModificationDate().toString("dd/MM/yyyy HH:mm:ss") : "")
                            + "</lastModification>\n";

            result += "    <subject><![CDATA[" + post.getName().getContent(locale) + "]]></subject>\n";
            result += "    <keywords></keywords>\n";
            result += "    <body><![CDATA[" + post.getBody().getContent(locale) + "]]></body>\n";
            result += "    <author>GCRP</author>\n";
            result += "    <authorEmail>gcrp@ist.utl.pt</authorEmail>\n";
            result += "    <place></place>";
            result += "    <visible>" + post.isVisible() + "</visible>\n";
            result += "    <id>" + post.getExternalId() + "</id>\n";
            result += "    <photoUrl></photoUrl>\n";
            result += "    <campus>Alameda</campus>\n";
            result += "    <categories/>\n";
            result += "    <pressRelease>false</pressRelease>\n";
            result += "    <sticky>" + false + "</sticky>\n";
            result += "    <priority>" + index++ + "</priority>\n";
            result += "  </net.sourceforge.fenixedu.presentationTier.Action.externalServices.AnnouncementDTO>\n";
        }

        result += "</list>";
        return Response.ok(result).build();

    }
}
