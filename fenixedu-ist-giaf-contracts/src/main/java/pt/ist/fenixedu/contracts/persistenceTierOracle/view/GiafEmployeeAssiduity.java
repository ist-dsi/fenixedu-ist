package pt.ist.fenixedu.contracts.persistenceTierOracle.view;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.persistenceTierOracle.DbConnector.ResultSetConsumer;
import pt.ist.fenixedu.contracts.persistenceTierOracle.GiafDbConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GiafEmployeeAssiduity {

    public static JsonObject readAssiduityOfEmployee(final String username) {
        final JsonObject result = new JsonObject();
        final User user = User.findByUsername(username);
        result.addProperty("username", username);
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

                + "FROM MYGIAF_CASS_PICAGEM a "

                + "WHERE a.ID_EMPREGADO = ? AND (a.DATA = ? OR a.DATA = ?)"

                + "ORDER BY a.id_picagem";
            }

            @Override
            public void prepare(final PreparedStatement statement) throws SQLException {
                final String giafNumber = convertIntoGiafNumber(getEmployeeNumber(user));
                final LocalDate date = new LocalDate();
                statement.setString(1, giafNumber);
                statement.setString(2, date.toString("yyyy-MM-dd"));
                statement.setString(3, date.minusDays(1).toString("yyyy-MM-dd"));
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
        return StringUtils.leftPad(employeeNumber, 6, '0');
    }

    private static String getEmployeeNumber(final User user) throws SQLException {
        final Person person = user == null ? null : user.getPerson();
        final Employee employee = person == null ? null : person.getEmployee();
        return employee == null ? null : employee.getEmployeeNumber().toString();
    }

}
