package com.scc.dao;

import com.scc.model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean addEquipment(int adminId, String name, String category, int quantity, String condition, String status) {
        String sql = "INSERT INTO equipment (equipment_name, category, quantity, condition, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, quantity);
            ps.setString(4, condition);
            ps.setString(5, status);
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int eqId = generatedKeys.getInt(1);
                        logDao.logAction(adminId, "EQUIPMENT_CREATED", "Admin " + adminId + " added equipment: " + name + " (ID: " + eqId + ")");
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error adding equipment: " + e.getMessage());
        }
        return false;
    }

    public List<Equipment> getAllEquipment() {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT equipment_id, equipment_name, category, quantity, condition, status FROM equipment";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Equipment(
                        rs.getInt("equipment_id"),
                        rs.getString("equipment_name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getString("condition"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving equipment: " + e.getMessage());
        }
        return list;
    }

    public boolean updateEquipment(int adminId, int equipmentId, String name, String category, int quantity, String condition, String status) {
        String sql = "UPDATE equipment SET equipment_name = ?, category = ?, quantity = ?, condition = ?, status = ? WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, quantity);
            ps.setString(4, condition);
            ps.setString(5, status);
            ps.setInt(6, equipmentId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                logDao.logAction(adminId, "EQUIPMENT_UPDATED", "Admin " + adminId + " updated equipment ID " + equipmentId + " status to " + status);
            }
            return success;
        } catch (SQLException e) {
            System.out.println("Error updating equipment: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEquipment(int adminId, int equipmentId) {
        String sql = "DELETE FROM equipment WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                logDao.logAction(adminId, "EQUIPMENT_DELETED", "Admin " + adminId + " deleted equipment ID " + equipmentId);
            }
            return success;
        } catch (SQLException e) {
            System.out.println("Error deleting equipment: " + e.getMessage());
            return false;
        }
    }
}