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

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.persistenceTierOracle.DbConnector.ResultSetConsumer;
import pt.ist.fenixedu.contracts.persistenceTierOracle.GiafDbConnector;

public class GiafEmployeeAssiduity {

    @Deprecated
    public static JsonObject readAssiduityOfEmployee(final String username) {
        return readAssiduityOfEmployee(username, new LocalDate());
    }

    public static JsonObject readAssiduityOfEmployee(final String username, final LocalDate date) {
        final User user = User.findByUsername(username);
        return readAssiduityOfEmployee(user, date);
    }

    @Deprecated
    public static JsonObject readAssiduityOfEmployee(final User user) {
        return readAssiduityOfEmployee(user, new LocalDate());
    }

    public static JsonObject readAssiduityOfEmployee(final User user, final LocalDate date) {
        return readAssiduityOfEmployee(user, date, Authenticate.getUser());
    }

    public static JsonObject readAssiduityOfEmployee(final User user, final LocalDate date, final User responsible) {
        final JsonObject result = new JsonObject();
        result.addProperty("username", user.getUsername());
        result.addProperty("name", user.getProfile().getDisplayName());
        result.addProperty("avatarUrl", user.getProfile().getAvatarUrl());

        final JsonArray records = new JsonArray();
        result.add("assiduityRecords", records);

        GiafDbConnector.getInstance().executeQuery(new ResultSetConsumer() {

            @Override
            public String query() {
                return "SELECT "

                + "a.ID_PICAGEM, "

                + "a.ID_EMPREGADO, "

                + "to_char(a.data,'YYYY-MM-DD'), "

                + "to_char(a.HORA, 'HH24:MI'), "

                + "a.OPERACAO, "

                + "a.TIPO "

                + "FROM MYGIAF_CASS_PICAGEM a"

                + (responsible == user ? " " : ", MYGIAF_V_RESP_SUBORDINADO b ")

                + "WHERE a.ID_EMPREGADO = ? AND (a.DATA = ? OR a.DATA = ?) "

                + (responsible == user ? "" : "AND a.ID_EMPREGADO = b.UTILIZADOR_NUM_EMP AND b.chefe_equipa_id = ? ")

                + "ORDER BY a.id_picagem";
            }

            @Override
            public void prepare(final PreparedStatement statement) throws SQLException {
                final String giafNumber = convertIntoGiafNumber(getEmployeeNumber(user));
                final LocalDate ld = (date == null ? new LocalDate() : date);
                statement.setString(1, giafNumber);
                statement.setString(2, ld.toString("yyyy-MM-dd"));
                statement.setString(3, ld.minusDays(1).toString("yyyy-MM-dd"));
                if (responsible != user) {
                    statement.setString(4, responsible.getUsername().toUpperCase());
                }
            }

            @Override
            public void accept(final ResultSet resultSet) throws SQLException {
                final JsonObject view = new JsonObject();
                view.addProperty("moment", resultSet.getString(3) + " " + resultSet.getString(4));
                view.addProperty("operation", resultSet.getString(5));
                records.add(view);
            }

        });

        return result;
    }

    private static String convertIntoGiafNumber(final String employeeNumber) {
        return Strings.padStart(employeeNumber, 6, '0');

    }

    private static String getEmployeeNumber(final User user) throws SQLException {
        final Person person = user == null ? null : user.getPerson();
        final Employee employee = person == null ? null : person.getEmployee();
        return employee == null ? null : employee.getEmployeeNumber().toString();
    }

}
