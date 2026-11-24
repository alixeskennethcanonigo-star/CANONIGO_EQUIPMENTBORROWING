package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.BorrowRequestDao;
import com.scc.dao.EquipmentDao;
import com.scc.model.ActivityLog;
import com.scc.model.BorrowRequest;
import com.scc.model.Equipment;
import com.scc.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class EquipmentStaffMenu {

    private final User user;
    private final Scanner scanner;
    private final EquipmentDao equipmentDao;
    private final BorrowRequestDao requestDao;
    private final ActivityLogDao logDao;

    public EquipmentStaffMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
        this.equipmentDao = new EquipmentDao();
        this.requestDao = new BorrowRequestDao();
        this.logDao = new ActivityLogDao();
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username());
        System.out.println("Role: " + user.roles() + " | Status: " + user.status());

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==================================================");
            System.out.println("               EQUIPMENT STAFF MENU               ");
            System.out.println("==================================================");
            System.out.println("1. View All Equipment");
            System.out.println("2. Add New Equipment");
            System.out.println("3. Update Equipment Details / Condition / Status");
            System.out.println("4. Release Approved Equipment to Borrower");
            System.out.println("5. Process Returned Equipment");
            System.out.println("6. View All Borrow Requests");
            System.out.println("7. View Activity Logs");
            System.out.println("8. View Profile");
            System.out.println("9. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewEquipment();
                case "2" -> addEquipment();
                case "3" -> updateEquipment();
                case "4" -> releaseApprovedEquipment();
                case "5" -> processReturnedEquipment();
                case "6" -> viewAllRequests();
                case "7" -> viewLogs();
                case "8" -> viewProfile();
                case "9" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1 to 9.");
            }
        }
    }

    private void viewEquipment() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        System.out.println("\n--- Laboratory Equipment Inventory ---");
        if (list.isEmpty()) {
            System.out.println("No equipment records found.");
            return;
        }
        String border = "+--------+------------------------+--------------------+----------+----------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-22s | %-18s | %-8s | %-14s | %-12s |%n",
                "ID", "Equipment Name", "Category", "Qty", "Condition", "Status");
        System.out.println(border);
        for (Equipment eq : list) {
            System.out.printf("| %-6d | %-22s | %-18s | %-8d | %-14s | %-12s |%n",
                    eq.equipmentId(), eq.equipmentName(), eq.category(), eq.quantity(), eq.condition(), eq.status());
        }
        System.out.println(border);
    }

    private void addEquipment() {
        System.out.println("\n--- Add New Equipment ---");
        System.out.print("Equipment Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Category: ");
        String category = scanner.nextLine().trim();
        int quantity = readIntInput("Quantity: ");
        System.out.print("Condition (e.g., NEW, GOOD, FAIR): ");
        String condition = scanner.nextLine().trim().toUpperCase();
        System.out.print("Status (e.g., AVAILABLE, MAINTENANCE): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (name.isEmpty() || category.isEmpty() || quantity < 0) {
            System.out.println("Invalid input. Name and category are required, and quantity must be 0 or higher.");
            return;
        }

        if (equipmentDao.staffAddEquipment(user.id(), name, category, quantity, condition, status)) {
            System.out.println("Equipment added successfully!");
        } else {
            System.out.println("Failed to add equipment.");
        }
    }

    private void updateEquipment() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        viewEquipment();
        if (list.isEmpty()) {
            return;
        }

        int eqId = readIntInput("Enter Equipment ID to update (or 0 to cancel): ");
        if (eqId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        Optional<Equipment> optEq = equipmentDao.getEquipmentById(eqId);
        if (optEq.isEmpty()) {
            System.out.println("Equipment ID not found.");
            return;
        }

        System.out.print("New Equipment Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("New Category: ");
        String category = scanner.nextLine().trim();
        int quantity = readIntInput("New Quantity: ");
        System.out.print("New Condition (NEW, GOOD, FAIR, DAMAGED): ");
        String condition = scanner.nextLine().trim().toUpperCase();
        System.out.print("New Status (AVAILABLE, BORROWED, MAINTENANCE, DAMAGED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (name.isEmpty() || category.isEmpty() || quantity < 0) {
            System.out.println("Invalid input.");
            return;
        }

        if (equipmentDao.staffUpdateEquipment(user.id(), eqId, name, category, quantity, condition, status)) {
            System.out.println("Equipment updated successfully!");
        } else {
            System.out.println("Failed to update equipment.");
        }
    }

    private void releaseApprovedEquipment() {
        List<BorrowRequest> approved = requestDao.getRequestsByStatus("APPROVED_BY_DEPT_HEAD");
        System.out.println("\n--- Requests Approved by Department Head (Ready to Release) ---");
        if (approved.isEmpty()) {
            System.out.println("No approved borrow requests waiting for release.");
            return;
        }
        printRequestsTable(approved);

        int reqId = readIntInput("Enter Request ID to release (or 0 to cancel): ");
        if (reqId == 0) {
            System.out.println("Release cancelled.");
            return;
        }

        Optional<BorrowRequest> optReq = requestDao.getRequestById(reqId);
        if (optReq.isEmpty() || !"APPROVED_BY_DEPT_HEAD".equalsIgnoreCase(optReq.get().status())) {
            System.out.println("Invalid Request ID or request is not in APPROVED_BY_DEPT_HEAD status.");
            return;
        }

        BorrowRequest req = optReq.get();
        boolean deducted = equipmentDao.deductQuantityForRelease(user.id(), req.equipmentId(), req.quantity());
        if (!deducted) {
            System.out.println("Insufficient stock to release " + req.quantity() + " unit(s) of " + req.equipmentName() + ".");
            return;
        }

        System.out.print("Release notes/remarks (optional): ");
        String remarks = scanner.nextLine().trim();

        if (requestDao.markAsBorrowed(user.id(), reqId, remarks)) {
            System.out.println("Equipment released to borrower! Request #" + reqId + " is now BORROWED.");
        } else {
            System.out.println("Failed to mark request as BORROWED.");
        }
    }

    private void processReturnedEquipment() {
        List<BorrowRequest> borrowed = requestDao.getRequestsByStatus("BORROWED");
        System.out.println("\n--- Active Borrowed Requests (Pending Return) ---");
        if (borrowed.isEmpty()) {
            System.out.println("No active borrowed equipment to return.");
            return;
        }
        printRequestsTable(borrowed);

        int reqId = readIntInput("Enter Request ID being returned (or 0 to cancel): ");
        if (reqId == 0) {
            System.out.println("Return cancelled.");
            return;
        }

        Optional<BorrowRequest> optReq = requestDao.getRequestById(reqId);
        if (optReq.isEmpty() || !"BORROWED".equalsIgnoreCase(optReq.get().status())) {
            System.out.println("Invalid Request ID or request is not currently BORROWED.");
            return;
        }

        BorrowRequest req = optReq.get();
        System.out.print("Returned Condition (GOOD, FAIR, DAMAGED): ");
        String condition = scanner.nextLine().trim().toUpperCase();
        if (condition.isEmpty()) {
            condition = "GOOD";
        }

        String updatedStatus = "DAMAGED".equals(condition) ? "MAINTENANCE" : "AVAILABLE";
        System.out.print("Return inspection remarks (optional): ");
        String remarks = scanner.nextLine().trim();

        boolean restored = equipmentDao.restoreQuantityOnReturn(user.id(), req.equipmentId(), req.quantity(), condition, updatedStatus);
        if (restored && requestDao.markAsReturned(user.id(), reqId, remarks)) {
            System.out.println("Return processed! Request #" + reqId + " is now RETURNED.");
        } else {
            System.out.println("Failed to process return.");
        }
    }

    private void viewAllRequests() {
        List<BorrowRequest> all = requestDao.getAllRequests();
        System.out.println("\n--- All Borrow Requests ---");
        if (all.isEmpty()) {
            System.out.println("No borrow requests found.");
            return;
        }
        printRequestsTable(all);
    }

    private void viewLogs() {
        List<ActivityLog> logs = logDao.getAllLogs();
        System.out.println("\n--- Recent Activity Logs ---");
        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }
        String border = "+--------+----------+--------------------------+-----------------------------------------------+----------------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-8s | %-24s | %-45s | %-20s |%n",
                "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);
        for (ActivityLog log : logs) {
            System.out.printf("| %-6d | %-8d | %-24s | %-45s | %-20s |%n",
                    log.id(),
                    log.userId(),
                    truncate(log.actionType(), 24),
                    truncate(log.description(), 45),
                    truncate(log.timestamp(), 20));
        }
        System.out.println(border);
    }

    private void viewProfile() {
        System.out.println("\n--- User Profile ---");
        System.out.println("ID:       " + user.id());
        System.out.println("Username: " + user.username());
        System.out.println("Role:     " + user.roles());
        System.out.println("Status:   " + user.status());
    }

    private void printRequestsTable(List<BorrowRequest> requests) {
        String border = "+--------+---------------+----------------------+------+----------------------+----------------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-13s | %-20s | %-4s | %-20s | %-20s |%n",
                "Req ID", "Borrower", "Equipment", "Qty", "Status", "Purpose");
        System.out.println(border);
        for (BorrowRequest r : requests) {
            System.out.printf("| %-6d | %-13s | %-20s | %-4d | %-20s | %-20s |%n",
                    r.requestId(),
                    truncate(r.borrowerName(), 13),
                    truncate(r.equipmentName(), 20),
                    r.quantity(),
                    truncate(r.status(), 20),
                    truncate(r.purpose(), 20));
        }
        System.out.println(border);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
    }
}