/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
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

    private static final String RECORD_PATH = "api/v1/researchers/participations";

    private static final String RECORD_URL_PREFIX = BASE_URL + "record/";
        
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
        WebTarget resource = HTTP_CLIENT.target(BASE_URL).path(RECORD_PATH).path(user.getUsername());
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
        private String url;

        public Publication(JsonObject publication) {
            List<String> parts = new ArrayList<>();

            if (publication.has("author")) {
                JsonElement jsonElement = publication.get("author");
                if (jsonElement.isJsonArray()) {
                    List<String> authors =
                            StreamUtils.of(jsonElement.getAsJsonArray())
                                    .map(author -> author.getAsJsonObject().get("name").getAsString())
                                    .collect(Collectors.toList());
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
                JsonElement journalName = publication.get("journal").getAsJsonObject().get("name");
                if (!journalName.isJsonNull()) {
                    otherData.add(journalName.getAsString());
                }
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
                if (publication.get("publisher").isJsonObject()) {
                    otherData.add(publication.get("publisher").getAsJsonObject().get("name").getAsString());
                } else {
                    otherData.add(publication.get("publisher").getAsString());
                }
            }
            parts.add(Joiner.on(", ").join(otherData));
            publicationString = Joiner.on(". ").join(parts);

            if (publication.has("url")) {
                url = publication.get("url").getAsString();
            } else {
                url = RECORD_URL_PREFIX + publication.get("id").getAsString();
            }
        }

        public boolean isYearBetween(Integer beginYear, Integer endYear) {
            try {
                return (!Strings.isNullOrEmpty(getYear()) && Integer.parseInt(getYear()) >= beginYear && Integer
                        .parseInt(getYear()) <= endYear);
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

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }
}
