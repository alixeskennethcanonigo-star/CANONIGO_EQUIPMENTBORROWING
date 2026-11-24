package com.scc.dao;

import com.scc.model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Optional<Equipment> getEquipmentById(int equipmentId) {
        String sql = "SELECT equipment_id, equipment_name, category, quantity, condition, status FROM equipment WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Equipment(
                            rs.getInt("equipment_id"),
                            rs.getString("equipment_name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getString("condition"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving equipment by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean staffAddEquipment(int staffId, String name, String category, int quantity, String condition, String status) {
        String sql = "INSERT INTO equipment (equipment_name, category, quantity, condition, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, quantity);
            ps.setString(4, condition);
            ps.setString(5, status);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int eqId = keys.getInt(1);
                        logDao.logAction(staffId, "STAFF_EQUIPMENT_ADDED", "Staff " + staffId + " added equipment: " + name + " (ID: " + eqId + ")");
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error adding equipment: " + e.getMessage());
        }
        return false;
    }

    public boolean staffUpdateEquipment(int staffId, int equipmentId, String name, String category, int quantity, String condition, String status) {
        String sql = "UPDATE equipment SET equipment_name = ?, category = ?, quantity = ?, condition = ?, status = ? WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, quantity);
            ps.setString(4, condition);
            ps.setString(5, status);
            ps.setInt(6, equipmentId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(staffId, "STAFF_EQUIPMENT_UPDATED", "Staff " + staffId + " updated equipment ID " + equipmentId + " (Qty: " + quantity + ", Status: " + status + ")");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error updating equipment: " + e.getMessage());
        }
        return false;
    }

    public boolean deductQuantityForRelease(int staffId, int equipmentId, int qtyToBorrow) {
        Optional<Equipment> optEq = getEquipmentById(equipmentId);
        if (optEq.isEmpty()) {
            return false;
        }
        Equipment eq = optEq.get();
        if (eq.quantity() < qtyToBorrow) {
            return false;
        }
        int remaining = eq.quantity() - qtyToBorrow;
        String newStatus = remaining == 0 ? "BORROWED" : eq.status();

        String sql = "UPDATE equipment SET quantity = ?, status = ? WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, remaining);
            ps.setString(2, newStatus);
            ps.setInt(3, equipmentId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(staffId, "EQUIPMENT_RELEASED", "Staff " + staffId + " released " + qtyToBorrow + " of equipment ID " + equipmentId + " (Remaining: " + remaining + ")");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error deducting equipment quantity: " + e.getMessage());
        }
        return false;
    }

    public boolean restoreQuantityOnReturn(int staffId, int equipmentId, int qtyReturned, String returnCondition, String newStatus) {
        Optional<Equipment> optEq = getEquipmentById(equipmentId);
        if (optEq.isEmpty()) {
            return false;
        }
        Equipment eq = optEq.get();
        int updatedQty = eq.quantity() + qtyReturned;

        String sql = "UPDATE equipment SET quantity = ?, condition = ?, status = ? WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, updatedQty);
            ps.setString(2, returnCondition);
            ps.setString(3, newStatus);
            ps.setInt(4, equipmentId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(staffId, "EQUIPMENT_RETURNED", "Staff " + staffId + " received return of " + qtyReturned + " for equipment ID " + equipmentId + " (Condition: " + returnCondition + ")");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error restoring equipment quantity: " + e.getMessage());
        }
        return false;
    }
}