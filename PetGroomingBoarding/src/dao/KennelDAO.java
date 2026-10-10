package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import model.Kennel;
import util.ConsoleUtil;
import util.DBConnection;

/** Boarding kennels / rooms. */
public class KennelDAO {

    public static boolean add(Kennel k) {
        String sql = "INSERT INTO kennel (kennel_name, kennel_type, pet_type, daily_rate, status) VALUES (?,?,?,?,'Available')";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, k.getKennelName());
            ps.setString(2, k.getKennelType());
            ps.setString(3, k.getPetType());
            ps.setDouble(4, k.getDailyRate());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) ConsoleUtil.error("A kennel with this name already exists.");
            else ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static Kennel getById(int id) {
        String sql = "SELECT * FROM kennel WHERE kennel_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Kennel(rs.getInt("kennel_id"), rs.getString("kennel_name"), rs.getString("kennel_type"),
                        rs.getString("pet_type"), rs.getDouble("daily_rate"), rs.getString("status")) : null;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return null;
        }
    }

    public static void viewAll() {
        ConsoleUtil.showQuery("SELECT kennel_id AS 'Kennel ID', kennel_name AS 'Name', kennel_type AS 'Type', pet_type AS 'For', "
                + "daily_rate AS 'Rate/Day (Rs.)', status AS 'Status' FROM kennel ORDER BY kennel_id");
    }

    /** Kennels that are in service, suit the species and have no overlapping stay in [checkIn, checkOut). */
    public static int viewAvailable(LocalDate checkIn, LocalDate checkOut, String species) {
        String sql = "SELECT k.kennel_id AS 'Kennel ID', k.kennel_name AS 'Name', k.kennel_type AS 'Type', "
                + "k.pet_type AS 'For', k.daily_rate AS 'Rate/Day (Rs.)' FROM kennel k "
                + "WHERE k.status = 'Available' AND (k.pet_type = 'Any' OR k.pet_type = ?) "
                + "AND NOT EXISTS (SELECT 1 FROM boarding b WHERE b.kennel_id = k.kennel_id "
                + "AND b.status IN ('Booked','Checked-In') AND b.check_in_date < ? AND b.check_out_date > ?) "
                + "ORDER BY k.daily_rate";
        return ConsoleUtil.showQuery(sql, species, checkOut, checkIn);
    }

    public static boolean setStatus(int id, String status) {
        String sql = "UPDATE kennel SET status = ? WHERE kennel_id = ?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }

    public static boolean delete(int id) {
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM boarding WHERE kennel_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ConsoleUtil.error("Cannot delete: boarding history exists for this kennel. Set it to 'Maintenance' instead.");
                        return false;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM kennel WHERE kennel_id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            ConsoleUtil.dbError(e);
            return false;
        }
    }
}
