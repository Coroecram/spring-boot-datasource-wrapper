package datasourcewrapper;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcUtils;
import connectionwrapper.ConnectionWrapper;

public class DataSourceWrapper implements DataSource {

    // PARAMETER INIT
    public final static String USER_NAME = "user";
    public final static String PASSWORD =  "password";
    public final static String DATABASE_NAME = "databaseName";

    // ERRORS
    private static final String AUTHENTICATION_ERROR = "The username or password is incorrect";
    private static final String AUTHORIZATION_ERROR = "Insufficient privileges to connect to the database";
    private static final String CONNECTION_REFUSED_ERROR = "Connection refused";
    private static final String DATABASE_NOT_FOUND_ERROR = ".*Database .* not found";

    private DataSource datasource;
    private final ThreadLocal<ConnectionWrapper> authenticatedConnection = new ThreadLocal<>();
    private final ThreadLocal<Map<String, String>> parameters = new ThreadLocal<>();

    public DataSourceWrapper(DataSource datasource) {
        super();
        this.datasource = datasource;
      }

    public void setParameters(final Map<String, String> parameters) {
        this.parameters.set(parameters);
      }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Connection getConnection() throws SQLException {

      PreparedStatement stmt = null;

      try {

        ConnectionWrapper connection = this.authenticatedConnection.get();
        String command;
        if (connection == null) {
          connection = new ConnectionWrapper(this.datasource.getConnection());

          // The CONNECT command allows indicating a user name, a password
          // and a database to initiate a
          // new session in the server with a new profile.

          command = "CONNECT DATABASE ?";

          stmt = connection.prepareStatement(command);
          stmt.setString(1, this.parameters.get().get(DATABASE_NAME));
        } else {
          command = "CONNECT USER ? PASSWORD ? DATABASE ?";

          stmt = connection.prepareStatement(command);
          stmt.setString(1, this.parameters.get().get(USER_NAME));
          stmt.setString(2, this.parameters.get().get(PASSWORD));
          stmt.setString(3, this.parameters.get().get(DATABASE_NAME));
        }

        stmt.execute();
        this.authenticatedConnection.set(connection);

        return connection;
      } catch (final SQLException e) {
        if (e.getMessage() != null) {
          if (e.getMessage().contains(CONNECTION_REFUSED_ERROR)) { // Check connection refused
            //logger.error("Connection refused", e);
            throw new ConnectException(e);
          }
          if (e.getMessage().contains(AUTHENTICATION_ERROR)) { // Check invalid credentials
            //logger.error("Invalid credentials", e);
            throw new AuthenticationException(e);
          }
          if (e.getMessage().contains(AUTHORIZATION_ERROR)) { // Check insufficient privileges
            //logger.error("Insufficient privileges", e);
            throw new AuthorizationException(e);
          }
          if (e.getMessage().matches(DATABASE_NOT_FOUND_ERROR)) { // Check data base name exists
            //logger.error("Database not found", e);
            throw new ResourceNotFoundException(e);
          }
        }
        //logger.error(e);
        throw e;
      } finally {
          //Make sure to close statement connection
        if (stmt != null) {
          JdbcUtils.closeStatement(stmt);
        }
      }
    }

    @Override
    public Connection getConnection(final String username, final String password)
        throws SQLException {
      throw new UnsupportedOperationException("Not supported by DataSource");
    }

    public DataSource getDatasource() {
        return datasource;
    }

    public void setDatasource(DataSource datasource) {
        this.datasource = datasource;
    }

}
