package pt.ist.fenixedu.contracts.persistenceTierOracle.view;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fenixedu.bennu.core.domain.User;

import pt.ist.fenixedu.contracts.persistenceTierOracle.DbConnector.ResultSetConsumer;
import pt.ist.fenixedu.contracts.persistenceTierOracle.GiafDbConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GiafAssiduityTeamResponsible {

    public static JsonObject readListOfAssiduityEmployees(final User user) {
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
        employeeMap.entrySet().forEach(e -> addEmployeeRecords(unitInfos, e.getKey(), e.getValue()));
        result.add("unitInfos", unitInfos);

        return result;
    }

    private static void addEmployeeRecords(final JsonArray unitInfos, final String unitName, final Set<String> usernames) {
        final JsonObject unitInfo = new JsonObject();
        unitInfo.addProperty("name", unitName);
        final JsonArray employeeAssiduity = new JsonArray();
        usernames.forEach(u -> employeeAssiduity.add(GiafEmployeeAssiduity.readAssiduityOfEmployee(u)));
        unitInfo.add("employeeAssiduity", employeeAssiduity);
        unitInfos.add(unitInfo);
    }

}
