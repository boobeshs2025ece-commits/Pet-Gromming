package util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Console output helpers: banners, messages and a generic ASCII table printer for any SQL query. */
public class ConsoleUtil {
    private static final int MAX_COL = 38;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(count * str.length());
        for (int i = 0; i < count; i++) sb.append(str);
        return sb.toString();
    }

    public static void header(String title) {
        String line = repeat("=", Math.max(60, title.length() + 6));
        System.out.println("\n" + line);
        System.out.println("   " + title);
        System.out.println(line);
    }

    public static void success(String msg) { System.out.println("  [OK] " + msg); }
    public static void error(String msg)   { System.out.println("  [ERROR] " + msg); }
    public static void info(String msg)    { System.out.println("  [i] " + msg); }
    public static void dbError(SQLException e) { System.out.println("  [DATABASE ERROR] " + e.getMessage()); }

    /** Binds parameters, converting java.time types to JDBC types. */
    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate) ps.setDate(i + 1, Date.valueOf((LocalDate) p));
            else if (p instanceof LocalTime) ps.setTime(i + 1, Time.valueOf((LocalTime) p));
            else ps.setObject(i + 1, p);
        }
    }

    /** Runs a SELECT and prints the result as a table. Returns row count (-1 on error). */
    public static int showQuery(String sql, Object... params) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return printResultSet(rs);
            }
        } catch (SQLException e) {
            dbError(e);
            return -1;
        }
    }

    public static int printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        String[] heads = new String[n];
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            heads[i] = md.getColumnLabel(i + 1);
            w[i] = heads[i].length();
        }
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] r = new String[n];
            for (int i = 0; i < n; i++) {
                Object o = rs.getObject(i + 1);
                String s;
                if (o == null) s = "-";
                else if (o instanceof LocalDateTime) s = ((LocalDateTime) o).format(DT);
                else if (o instanceof Double || o instanceof Float) s = String.format("%.2f", ((Number) o).doubleValue());
                else s = o.toString();
                if (s.length() > MAX_COL) s = s.substring(0, MAX_COL - 3) + "...";
                r[i] = s;
                w[i] = Math.max(w[i], s.length());
            }
            rows.add(r);
        }
        StringBuilder sep = new StringBuilder("  +");
        for (int i = 0; i < n; i++) sep.append(repeat("-", w[i] + 2)).append("+");
        System.out.println(sep);
        printRow(heads, w);
        System.out.println(sep);
        for (String[] r : rows) printRow(r, w);
        if (!rows.isEmpty()) System.out.println(sep);
        if (rows.isEmpty()) System.out.println("  (no records found)");
        else System.out.println("  " + rows.size() + " record(s)");
        return rows.size();
    }

    private static void printRow(String[] cells, int[] w) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < cells.length; i++) sb.append(' ').append(String.format("%-" + w[i] + "s", cells[i])).append(" |");
        System.out.println(sb);
    }
}
