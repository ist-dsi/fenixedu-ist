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
package pt.ist.fenixedu.integration.api.infra;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fenixedu.commons.i18n.I18N;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class FenixAPIFromExternalServer {

    private static final Logger logger = LoggerFactory.getLogger(FenixAPIFromExternalServer.class);

    private static final Client HTTP_CLIENT = ClientBuilder.newClient();

    private static final String shuttleUrl = FenixEduIstIntegrationConfiguration.getConfiguration().getFenixApiShuttleUrl();
    private static final String contactsUrl = FenixEduIstIntegrationConfiguration.getConfiguration().getFenixApiContactsUrl();

    private static JsonObject empty = new JsonObject();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String datePattern = "dd/MM/yyyy";
    private static JsonParser parser = new JsonParser();

    private static JsonObject getInformation(String url) {

        JsonObject infoJson;
        try {
            Response response =
                    HTTP_CLIENT.target(url).request(MediaType.APPLICATION_JSON).header("Authorization", getServiceAuth()).get();

            if (response.getStatus() == 200) {
                infoJson = parser.parse(response.readEntity(String.class)).getAsJsonObject();
                logger.debug("got info from url {}, return it. ", url);
            } else {
                logger.debug("url errored and returned {}", response.getStatus());
                infoJson = empty;
            }

        } catch (ProcessingException | JsonSyntaxException e) {
            logger.debug("http error or json parsing error : {}", e.getLocalizedMessage());
            infoJson = empty;
        }

        return infoJson;
    }

    public static String getCanteen(String daySearch) {
        String canteenName = FenixEduIstIntegrationConfiguration.getConfiguration().getFenixAPICanteenDefaultName();
        return getCanteen(daySearch, canteenName);
    }

    public static String getCanteen(String daySearch, String canteenName) {

        String canteenUrl =
                FenixEduIstIntegrationConfiguration.getConfiguration().getFenixApiCanteenUrl().concat("?name=" + canteenName);
        JsonObject canteenInfo = getInformation(canteenUrl);
        String lang = I18N.getLocale().toLanguageTag();

        if (!canteenInfo.has(lang)) {
            return empty.toString();
        }
        JsonArray jsonArrayWithLang = canteenInfo.getAsJsonObject().getAsJsonArray(lang);

        DateTime dayToCompareStart;
        DateTime dayToCompareEnd;

        DateTime dateTime = DateTime.parse(daySearch, DateTimeFormat.forPattern(datePattern));
        int dayOfWeek = dateTime.getDayOfWeek();
        if (dayOfWeek != 7) {
            dayToCompareStart = dateTime.minusDays(dayOfWeek);
            dayToCompareEnd = dateTime.plusDays(7 - dayOfWeek);
        } else {
            dayToCompareStart = dateTime;
            dayToCompareEnd = dateTime.plusDays(7);
        }

        Interval validInterval = new Interval(dayToCompareStart, dayToCompareEnd);
        JsonArray jsonResult = new JsonArray();

        for (JsonElement jObj : jsonArrayWithLang) {

            DateTime dateToCompare =
                    DateTime.parse(((JsonObject) jObj).get("day").getAsString(), DateTimeFormat.forPattern(datePattern));

            if (validInterval.contains(dateToCompare)) {
                jsonResult.add(jObj);
            }
        }

        return gson.toJson(jsonResult);
    }

    public static String getShuttle() {
        JsonObject shuttleInfo = getInformation(shuttleUrl);
        return gson.toJson(shuttleInfo);
    }

    public static String getContacts() {
        JsonObject contactsInfo = getInformation(contactsUrl);
        if (contactsInfo.has(I18N.getLocale().toLanguageTag())) {
            return contactsInfo.get(I18N.getLocale().toLanguageTag()).toString();
        }

        return contactsInfo.toString();
    }

    private static String getServiceAuth() {
        FenixEduIstIntegrationConfiguration.ConfigurationProperties config =
                FenixEduIstIntegrationConfiguration.getConfiguration();
        String userpass = config.getFenixApiCanteenUser() + ":" + config.getFenixApiCanteenSecret();
        String encoding = new String(BaseEncoding.base64().encode(userpass.getBytes()));
        return "Basic " + encoding;
    }

}