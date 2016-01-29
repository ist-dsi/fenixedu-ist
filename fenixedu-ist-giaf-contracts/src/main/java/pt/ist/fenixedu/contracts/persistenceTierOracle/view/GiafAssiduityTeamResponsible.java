/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.persistenceTierOracle.view;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fenixedu.bennu.core.domain.User;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.contracts.persistenceTierOracle.DbConnector.ResultSetConsumer;
import pt.ist.fenixedu.contracts.persistenceTierOracle.GiafDbConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GiafAssiduityTeamResponsible {

    @Deprecated
    public static JsonObject readListOfAssiduityEmployees(final User user) {
        return readListOfAssiduityEmployees(user, new LocalDate());
    }

    public static JsonObject readListOfAssiduityEmployees(final User user, final LocalDate date) {
        final JsonObject result = new JsonObject();
        result.addProperty("username", user.getUsername());
        result.addProperty("name", user.getProfile().getDisplayName());
        result.addProperty("avatarUrl", user.getProfile().getAvatarUrl());

        final Map<String, Set<String>> employeeMap = new HashMap<String, Set<String>>();

        GiafDbConnector.getInstance().executeQuery(new ResultSetConsumer() {

            @Override
            public String query() {
                return "SELECT "

                + "UTILIZADOR_DESCR, "

                + "DEPARTAMENTO_DESC, "

                + "UTILIZADOR_ID "

                + "FROM MYGIAF_V_RESP_SUBORDINADO "

                + "WHERE chefe_equipa_id = ? AND UTILIZADOR_ID <> ? "

                + "ORDER BY departamento_id";
            }

            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                final String arg = user.getUsername().toUpperCase();
                statement.setString(1, arg);
                statement.setString(2, arg);
            }

            @Override
            public void accept(final ResultSet resultSet) throws SQLException {
                final String unitName = resultSet.getString(2);
                final String employeeUsername = resultSet.getString(3).toLowerCase();

                if (!employeeMap.containsKey(unitName)) {
                    employeeMap.put(unitName, new HashSet<>());
                }
                employeeMap.get(unitName).add(employeeUsername);
            }

        });

        final JsonArray unitInfos = new JsonArray();
        employeeMap.entrySet().forEach(e -> addEmployeeRecords(unitInfos, e.getKey(), e.getValue(), date));
        result.add("unitInfos", unitInfos);

        return result;
    }

    private static void addEmployeeRecords(final JsonArray unitInfos, final String unitName, final Set<String> usernames,
            final LocalDate dateParam) {
        final JsonObject unitInfo = new JsonObject();
        unitInfo.addProperty("name", unitName);
        final JsonArray employeeAssiduity = new JsonArray();
        usernames.forEach(u -> employeeAssiduity.add(GiafEmployeeAssiduity.readAssiduityOfEmployee(u, dateParam)));
        unitInfo.add("employeeAssiduity", employeeAssiduity);
        unitInfos.add(unitInfo);
    }

}
