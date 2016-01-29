package pt.ist.fenixedu.teacher.evaluation.service.external;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.stream.StreamUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SotisPublications {

    private static final String BASE_URL = "https://sotis.tecnico.ulisboa.pt/";

    private static final String RECORD_PATH = "api/sotis-core/record/list";

    public List<String> get(User user) {
        SortedSet<Publication> publications = getPublications(user);
        return publications.stream().map(p -> p.getPublicationString()).collect(Collectors.toList());
    }

    public List<String> get(User user, Integer beginYear, Integer endYear) {
        SortedSet<Publication> publications = getPublications(user);
        return publications.stream().filter(p -> p.isYearBetween(beginYear, endYear)).map(p -> p.getPublicationString())
                .collect(Collectors.toList());
    }

    public Set<Publication> getPublications(User user, Integer beginYear, Integer endYear) {
        SortedSet<Publication> publications = getPublications(user);
        return publications.stream().filter(p -> p.isYearBetween(beginYear, endYear)).collect(Collectors.toSet());
    }

    private SortedSet<Publication> getPublications(User user) {
        SortedSet<Publication> publications = new TreeSet<Publication>();
        Client HTTP_CLIENT = ClientBuilder.newClient();
        WebTarget resource = HTTP_CLIENT.target(BASE_URL).path(RECORD_PATH);
        resource = resource.queryParam("researchers", user.getUsername());
        try {
            String allPublications = resource.request().get(String.class);
            JsonParser parser = new JsonParser();
            JsonObject records = (JsonObject) parser.parse(allPublications);
            for (Object teacherPublication : (JsonArray) records.get("records")) {
                JsonObject publication = (JsonObject) teacherPublication;
                publications.add(new Publication(publication));
            }
        } finally {
            HTTP_CLIENT.close();
        }
        return publications;
    }

    public class Publication implements Comparable<Publication> {
        private String publicationString;
        private String year;
        private Integer authorsNumber = 0;

        public Publication(JsonObject publication) {
            List<String> parts = new ArrayList<>();

            if (publication.has("author")) {
                JsonElement jsonElement = publication.get("author");
                if (jsonElement.isJsonArray()) {
                    List<String> authors = StreamUtils.of(jsonElement.getAsJsonArray())
                            .map(author -> author.getAsJsonObject().get("name").getAsString()).collect(Collectors.toList());
                    parts.add(Joiner.on(", ").join(authors));
                    authorsNumber += authors.size();
                } else {
                    authorsNumber++;
                    parts.add(jsonElement.getAsJsonObject().get("name").getAsString());
                }

            }
            if (publication.has("date")) {
                year = publication.get("date").getAsJsonObject().get("year").getAsString();
                parts.add("(" + year + ")");
            }
            if (publication.has("title")) {
                parts.add(publication.get("title").getAsString());
            }

            List<String> otherData = new ArrayList<>();
            if (publication.has("journal")) {
                otherData.add(publication.get("journal").getAsJsonObject().get("name").getAsString());
                if (publication.has("volume") && publication.has("number")) {
                    otherData.add(publication.get("volume").getAsString() + " (" + publication.get("number").getAsString() + ")");
                }
            }
            if (publication.has("event")) {
                otherData.add(publication.get("event").getAsJsonObject().get("name").getAsString());
            }
            if (publication.has("pages")) {
                otherData.add(publication.get("pages").getAsString());
            }
            if (publication.has("publisher")) {
                otherData.add(publication.get("publisher").getAsJsonObject().get("name").getAsString());
            }
            parts.add(Joiner.on(", ").join(otherData));
            publicationString = Joiner.on(". ").join(parts);
        }

        public boolean isYearBetween(Integer beginYear, Integer endYear) {
            try {
                return (!Strings.isNullOrEmpty(getYear()) && Integer.parseInt(getYear()) >= beginYear
                        && Integer.parseInt(getYear()) <= endYear);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public int compareTo(Publication o) {
            if (Objects.equal(year, o.year)) {
                return publicationString.compareTo(o.publicationString);
            } else if (year == null) {
                return 1;
            } else if (o.year == null) {
                return -1;
            }
            return -year.compareTo(o.year);
        }

        public String getPublicationString() {
            return publicationString;
        }

        public void setPublicationString(String publicationString) {
            this.publicationString = publicationString;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public Integer getAuthorsNumber() {
            return authorsNumber;
        }

        public void setAuthorsNumber(Integer authorsNumber) {
            this.authorsNumber = authorsNumber;
        }

    }
}
