package com.scc.dao;

import com.scc.model.BorrowRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowRequestDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private static final String BASE_SELECT = """
            SELECT br.request_id,
                   br.borrower_id,
                   u.username AS borrower_name,
                   br.equipment_id,
                   e.equipment_name,
                   br.quantity,
                   br.purpose,
                   br.status,
                   br.remarks,
                   br.request_date
            FROM borrow_requests br
            JOIN users u ON br.borrower_id = u.id
            JOIN equipment e ON br.equipment_id = e.equipment_id
            """;

    public boolean createRequest(int borrowerId, int equipmentId, int quantity, String purpose) {
        String sql = "INSERT INTO borrow_requests (borrower_id, equipment_id, quantity, purpose, status, remarks) VALUES (?, ?, ?, ?, 'PENDING_DEPT_HEAD', '')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, borrowerId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, quantity);
            ps.setString(4, purpose);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int reqId = keys.getInt(1);
                        logDao.logAction(borrowerId, "BORROW_REQUEST_CREATED", "Borrower " + borrowerId + " requested " + quantity + " of equipment ID " + equipmentId + " (Req #" + reqId + ")");
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error submitting borrow request: " + e.getMessage());
        }
        return false;
    }

    public List<BorrowRequest> getRequestsByBorrower(int borrowerId) {
        List<BorrowRequest> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE br.borrower_id = ? ORDER BY br.request_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching borrower requests: " + e.getMessage());
        }
        return list;
    }

    public List<BorrowRequest> getPendingDeptHeadRequests() {
        List<BorrowRequest> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE br.status = 'PENDING_DEPT_HEAD' ORDER BY br.request_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching pending department head requests: " + e.getMessage());
        }
        return list;
    }

    public List<BorrowRequest> getAllRequests() {
        List<BorrowRequest> list = new ArrayList<>();
        String sql = BASE_SELECT + " ORDER BY br.request_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching all borrow requests: " + e.getMessage());
        }
        return list;
    }

    public List<BorrowRequest> getRequestsByStatus(String status) {
        List<BorrowRequest> list = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE br.status = ? ORDER BY br.request_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching requests by status: " + e.getMessage());
        }
        return list;
    }

    public Optional<BorrowRequest> getRequestById(int requestId) {
        String sql = BASE_SELECT + " WHERE br.request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching borrow request: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean reviewByDeptHead(int deptHeadId, int requestId, boolean approve, String remarks) {
        String newStatus = approve ? "APPROVED_BY_DEPT_HEAD" : "REJECTED_BY_DEPT_HEAD";
        String sql = "UPDATE borrow_requests SET status = ?, remarks = ? WHERE request_id = ? AND status = 'PENDING_DEPT_HEAD'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, remarks);
            ps.setInt(3, requestId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                String action = approve ? "DEPT_HEAD_APPROVED" : "DEPT_HEAD_REJECTED";
                logDao.logAction(deptHeadId, action, "Dept Head " + deptHeadId + " set request #" + requestId + " to " + newStatus + " (" + remarks + ")");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error updating request review: " + e.getMessage());
        }
        return false;
    }

    public boolean markAsBorrowed(int staffId, int requestId, String remarks) {
        String sql = "UPDATE borrow_requests SET status = 'BORROWED', remarks = ? WHERE request_id = ? AND status = 'APPROVED_BY_DEPT_HEAD'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remarks);
            ps.setInt(2, requestId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(staffId, "REQUEST_BORROWED", "Staff " + staffId + " released items for request #" + requestId);
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error marking request as borrowed: " + e.getMessage());
        }
        return false;
    }

    public boolean markAsReturned(int staffId, int requestId, String remarks) {
        String sql = "UPDATE borrow_requests SET status = 'RETURNED', remarks = ? WHERE request_id = ? AND status = 'BORROWED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, remarks);
            ps.setInt(2, requestId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                logDao.logAction(staffId, "REQUEST_RETURNED", "Staff " + staffId + " marked request #" + requestId + " as RETURNED");
            }
            return updated;
        } catch (SQLException e) {
            System.out.println("Error marking request as returned: " + e.getMessage());
        }
        return false;
    }

    private BorrowRequest mapRow(ResultSet rs) throws SQLException {
        return new BorrowRequest(
                rs.getInt("request_id"),
                rs.getInt("borrower_id"),
                rs.getString("borrower_name"),
                rs.getInt("equipment_id"),
                rs.getString("equipment_name"),
                rs.getInt("quantity"),
                rs.getString("purpose"),
                rs.getString("status"),
                rs.getString("remarks"),
                rs.getString("request_date")
        );
    }
}