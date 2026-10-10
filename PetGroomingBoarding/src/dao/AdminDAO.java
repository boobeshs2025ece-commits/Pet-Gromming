package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.ConsoleUtil;
import util.DBConnection;
import util.PasswordUtil;

/** Administrator authentication. */
public class AdminDAO {
    public static boolean login(String username, String rawPassword) {
        String sql = "SELECT 1 FROM admin WHERE username = ? AND password = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(rawPassword));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
