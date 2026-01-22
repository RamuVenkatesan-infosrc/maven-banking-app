================================================================================
FIXED CODE FOR: src/main/java/util/ConnectionFactory.java
================================================================================
package util;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

public class ConnectionFactory {
    private static Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);
    private static final List<String> ALLOWED_HOSTS = Arrays.asList("localhost", "127.0.0.1", "your-production-db-host.com");
    private static final List<Integer> ALLOWED_PORTS = Arrays.asList(5432, 5433);
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile("[<>\\[\\]{}\\\\^`]");

    public static Connection getConnection() {
        if (conn == null) {
            try {
                URI dbUri = new URI(System.getenv("DATABASE_URL"));
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

                validateDatabaseUrl(dbUrl);

                conn = DriverManager.getConnection(dbUrl, username, password);
            } catch (URISyntaxException | SQLException | IllegalArgumentException e) {
                logger.error("Error establishing database connection", e);
                throw new RuntimeException("Unable to establish database connection", e);
            }
        }
        return conn;
    }

    private static void validateDatabaseUrl(String dbUrl) {
        try {
            URL url = new URL(dbUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();

            if (!"jdbc:postgresql".equals(protocol)) {
                throw new IllegalArgumentException("Invalid database protocol");
            }

            if (!ALLOWED_HOSTS.contains(host)) {
                throw new IllegalArgumentException("Invalid database host");
            }

            if (port == -1 || !ALLOWED_PORTS.contains(port)) {
                throw new IllegalArgumentException("Invalid database port");
            }

            if (FORBIDDEN_PATTERN.matcher(dbUrl).find()) {
                throw new IllegalArgumentException("Database URL contains forbidden characters");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed database URL", e);
        }
    }
}