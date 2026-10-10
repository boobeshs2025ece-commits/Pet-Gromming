package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Connection manager. Credentials are discovered in this order:
 *  1. JVM properties (-Ddb.user=root -Ddb.password=1234)
 *  2. Environment variables (DB_USER, DB_PASSWORD)
 *  3. db.properties file in the project folder
 *  4. Common local defaults (root/1234, root/root, root/blank ...)
 *  5. Ask the user, then save to db.properties for next time.
 */
public class DBConnection {
    public static final String DB_NAME = "pet_care_db";
    private static final String PROP_FILE = "db.properties";
    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String host = "localhost";
    private static String port = "3306";
    private static String user;
    private static String password;
    private static boolean resolved = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("\n[ERROR] MySQL JDBC driver not found on the classpath.");
            System.out.println("        Put mysql-connector-j-8.0.33.jar inside the lib/ folder and add it to the");
            System.out.println("        Eclipse Build Path (Right-click project > Build Path > Configure Build Path > Libraries).\n");
        }
    }

    private static String baseUrl() { return "jdbc:mysql://" + host + ":" + port + "/"; }

    /** Connection to the MySQL server without selecting a database (used to create the DB). */
    public static Connection getServerConnection() throws SQLException {
        resolveCredentials();
        return DriverManager.getConnection(baseUrl() + OPTIONS, user, password);
    }

    /** Connection to pet_care_db. */
    public static Connection getConnection() throws SQLException {
        resolveCredentials();
        return DriverManager.getConnection(baseUrl() + DB_NAME + OPTIONS, user, password);
    }

    public static void rollbackQuietly(Connection con) {
        try { if (con != null) con.rollback(); } catch (SQLException ignored) { }
    }

    public static void closeQuietly(Connection con) {
        try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException ignored) { }
    }

    private static synchronized void resolveCredentials() throws SQLException {
        if (resolved) return;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(PROP_FILE)) { p.load(in); } catch (IOException ignored) { }

        host = firstNonEmpty(System.getProperty("db.host"), System.getenv("DB_HOST"), p.getProperty("db.host"), "localhost");
        port = firstNonEmpty(System.getProperty("db.port"), System.getenv("DB_PORT"), p.getProperty("db.port"), "3306");
        String u = firstNonEmpty(System.getProperty("db.user"), System.getenv("DB_USER"), p.getProperty("db.user"), null);
        String pw = firstNonNull(System.getProperty("db.password"), System.getenv("DB_PASSWORD"), p.getProperty("db.password"));

        List<String[]> candidates = new ArrayList<>();
        if (u != null) candidates.add(new String[] { u, pw == null ? "" : pw });
        String[] defaults = { "Boobesh558S@", "Boobesh558S", "1234", "root", "", "admin", "password" };
        for (String d : defaults) candidates.add(new String[] { "root", d });

        for (String[] c : candidates) {
            if (tryLogin(c[0], c[1])) { remember(c[0], c[1]); return; }
        }

        System.out.println("\n[!] Could not log in to MySQL automatically (is the MySQL service running?).");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String uu = InputUtil.readLine("MySQL username [root]: ");
            if (uu.isEmpty()) uu = "root";
            String pp = InputUtil.readLine("MySQL password: ");
            if (tryLogin(uu, pp)) { remember(uu, pp); saveProperties(); return; }
            System.out.println("  Login failed (" + attempt + "/3).");
        }
        throw new SQLException("Unable to connect to MySQL at " + host + ":" + port
                + ". Check that MySQL is running and the credentials in db.properties are correct.");
    }

    private static boolean tryLogin(String u, String pw) {
        try (Connection c = DriverManager.getConnection(baseUrl() + OPTIONS, u, pw)) {
            return c != null;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void remember(String u, String pw) {
        user = u; password = pw; resolved = true;
    }

    private static void saveProperties() {
        Properties p = new Properties();
        p.setProperty("db.host", host);
        p.setProperty("db.port", port);
        p.setProperty("db.user", user);
        p.setProperty("db.password", password);
        try (FileOutputStream out = new FileOutputStream(PROP_FILE)) {
            p.store(out, "MySQL Connection Properties for Pet Grooming and Boarding Management System");
        } catch (IOException e) {
            System.out.println("  (Could not save db.properties: " + e.getMessage() + ")");
        }
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return null;
    }

    private static String firstNonNull(String... vals) {
        for (String v : vals) if (v != null) return v;
        return null;
    }
}
