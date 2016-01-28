package pt.ist.fenixedu.contracts.persistenceTierOracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DbConnector {

    private static final String ORACLE_DRIVER_NAME = "oracle.jdbc.driver.OracleDriver";
    private static final String MYSQL_DRIVER_NAME = "com.mysql.jdbc.Driver";

    public static interface ResultSetConsumer {
        String query();

        default void prepare(final PreparedStatement statement) throws SQLException {
        }

        void accept(final ResultSet resultSet) throws SQLException;
    }

    protected DbConnector() {
        try {
            Class.forName(ORACLE_DRIVER_NAME);
            Class.forName(MYSQL_DRIVER_NAME);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    private Connection getConnection() {
        final Connection connection;
        try {
            connection = DriverManager.getConnection(dbProtocol() + dbAlias(), dbUser(), dbPass());
        } catch (final SQLException e) {
            throw new Error(e);
        }
        try {
            connection.setAutoCommit(true);
        } catch (final SQLException e) {
            try {
                connection.close();
            } catch (SQLException e1) {
                throw new Error(e1);
            }
            throw new Error(e);
        }
        return connection;
    }

    protected abstract String dbProtocol();

    protected abstract String dbAlias();

    protected abstract String dbUser();

    protected abstract String dbPass();

    public void executeQuery(final ResultSetConsumer consumer) {
        try (final Connection connection = getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement(consumer.query())) {
                consumer.prepare(statement);
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        consumer.accept(resultSet);
                    }
                }
            }
        } catch (final SQLException e) {
            throw new Error(e);
        }
    }

}
