package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.EquipmentDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.Equipment;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final User adminUser;
    private final UserDao userDao;
    private final EquipmentDao equipmentDao;
    private final ActivityLogDao logDao;
    private final Scanner scanner;

    private static final String[] ROLES = {
            "ADMINISTRATOR", "EQUIPMENT STAFF", "BORROWER", "DEPARTMENT HEAD"
    };

    public AdminMenu(User adminUser, UserDao userDao, Scanner scanner) {
        this.adminUser = adminUser;
        this.userDao = userDao;
        this.equipmentDao = new EquipmentDao();
        this.logDao = new ActivityLogDao();
        this.scanner = scanner;
    }

    public void show() {
        boolean managing = true;
        while (managing) {
            System.out.println("\n==================================================");
            System.out.println("                 ADMIN DASHBOARD                  ");
            System.out.println("==================================================");
            System.out.println("Logged in as: " + adminUser.username() + " (" + adminUser.roles() + ")");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User");
            System.out.println("6. Delete User");
            System.out.println("7. View All Equipment / Add Equipment");
            System.out.println("8. View System Audit Logs");
            System.out.println("9. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingRegistrations();
                case "3" -> approveUserAccount();
                case "4" -> addNewUser();
                case "5" -> updateUser();
                case "6" -> deleteUser();
                case "7" -> viewEquipmentManagement();
                case "8" -> viewSystemAuditLogs();
                case "9" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    managing = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number between 1 and 9.");
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("\n--- All System Users ---");
        List<User> users = userDao.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        String border = "+--------+----------------------+------------------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-20s | %-22s | %-12s |%n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : users) {
            System.out.printf("| %-6d | %-20s | %-22s | %-12s |%n", u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(border);
    }

    private void viewPendingRegistrations() {
        System.out.println("\n--- Pending User Approvals ---");
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts awaiting approval.");
            return;
        }
        String border = "+--------+----------------------+------------------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-20s | %-22s | %-12s |%n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : pendingUsers) {
            System.out.printf("| %-6d | %-20s | %-22s | %-12s |%n", u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(border);
    }

    private void approveUserAccount() {
        System.out.println("\n--- Approve User Registration ---");
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        String border = "+--------+----------------------+------------------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-20s | %-22s | %-12s |%n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : pendingUsers) {
            System.out.printf("| %-6d | %-20s | %-22s | %-12s |%n", u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(border);

        int id = readIntInput("Enter User ID to approve (or enter 0 to cancel): ");
        if (id == 0) {
            System.out.println("Approval action cancelled.");
            return;
        }

        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == id);
        if (!exists) {
            System.out.println("Error: User ID not found in the pending list.");
            return;
        }

        if (userDao.approveUser(adminUser.id(), id)) {
            System.out.println("User account successfully approved and updated to 'APPROVED'!");
        } else {
            System.out.println("Failed to approve user account.");
        }
    }

    private void addNewUser() {
        System.out.println("\n--- Add New User ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        String role = readValidRole();
        String status = readValidStatus();

        if (userDao.adminAddUser(adminUser.id(), username, password, role, status)) {
            System.out.println("User added successfully!");
        } else {
            System.out.println("Failed to add user.");
        }
    }

    private void updateUser() {
        viewAllUsers();
        System.out.println("\n--- Update User ---");
        int targetUserId = readIntInput("Enter User ID to update (or enter 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (targetUserId == adminUser.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        String newRole = readValidRole();
        String newStatus = readValidStatus();

        if (userDao.updateUserRoleAndStatus(adminUser.id(), targetUserId, newRole, newStatus)) {
            System.out.println("User updated successfully!");
        } else {
            System.out.println("Failed to update user.");
        }
    }

    private void deleteUser() {
        viewAllUsers();
        System.out.println("\n--- Delete User ---");
        int id = readIntInput("Enter User ID to delete (or enter 0 to cancel): ");
        if (id == 0) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (id == adminUser.id()) {
            System.out.println("WARNING: You cannot delete your own active admin account.");
            return;
        }

        System.out.print("Are you sure you want to delete user ID " + id + "? (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("y")) {
            if (userDao.deleteUser(adminUser.id(), id)) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void viewEquipmentManagement() {
        boolean managingEq = true;
        while (managingEq) {
            System.out.println("\n--- Equipment Management Menu ---");
            System.out.println("1. View All Equipment");
            System.out.println("2. Add New Equipment");
            System.out.println("3. Update Equipment Info & Status");
            System.out.println("4. Delete Equipment Record");
            System.out.println("5. Back to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> displayEquipmentList(equipmentDao.getAllEquipment());
                case "2" -> addEquipmentRecord();
                case "3" -> updateEquipmentRecord();
                case "4" -> deleteEquipmentRecord();
                case "5" -> managingEq = false;
                default -> System.out.println("Invalid choice. Please enter a number between 1 and 5.");
            }
        }
    }

    private void displayEquipmentList(List<Equipment> list) {
        System.out.println("\n--- Equipment Records ---");
        if (list.isEmpty()) {
            System.out.println("No equipment records found.");
            return;
        }
        String border = "+--------+------------------------+--------------------+----------+----------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-22s | %-18s | %-8s | %-14s | %-12s |%n", "ID", "Equipment Name", "Category", "Qty", "Condition", "Status");
        System.out.println(border);
        for (Equipment eq : list) {
            System.out.printf("| %-6d | %-22s | %-18s | %-8d | %-14s | %-12s |%n",
                    eq.equipmentId(), eq.equipmentName(), eq.category(), eq.quantity(), eq.condition(), eq.status());
        }
        System.out.println(border);
    }

    private void addEquipmentRecord() {
        System.out.println("\n--- Add New Equipment ---");
        System.out.print("Equipment Name (e.g., Projector, Laptop): ");
        String name = scanner.nextLine().trim();
        System.out.print("Category: ");
        String category = scanner.nextLine().trim();
        int quantity = readIntInput("Quantity: ");
        System.out.print("Condition (e.g., NEW, GOOD, FAIR): ");
        String condition = scanner.nextLine().trim().toUpperCase();
        System.out.print("Status (e.g., AVAILABLE, BORROWED, DAMAGED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (name.isEmpty() || category.isEmpty()) {
            System.out.println("Name and category cannot be empty.");
            return;
        }

        if (equipmentDao.addEquipment(adminUser.id(), name, category, quantity, condition, status)) {
            System.out.println("Equipment added successfully!");
        } else {
            System.out.println("Failed to add equipment.");
        }
    }

    private void updateEquipmentRecord() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        displayEquipmentList(list);
        if (list.isEmpty()) return;

        int eqId = readIntInput("Enter Equipment ID to update (or 0 to cancel): ");
        if (eqId == 0) return;

        System.out.print("New Equipment Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("New Category: ");
        String category = scanner.nextLine().trim();
        int quantity = readIntInput("New Quantity: ");
        System.out.print("New Condition (NEW, GOOD, DAMAGED): ");
        String condition = scanner.nextLine().trim().toUpperCase();
        System.out.print("New Status (AVAILABLE, BORROWED, MAINTENANCE): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (equipmentDao.updateEquipment(adminUser.id(), eqId, name, category, quantity, condition, status)) {
            System.out.println("Equipment updated successfully!");
        } else {
            System.out.println("Failed to update equipment.");
        }
    }

    private void deleteEquipmentRecord() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        displayEquipmentList(list);
        if (list.isEmpty()) return;

        int eqId = readIntInput("Enter Equipment ID to delete (or 0 to cancel): ");
        if (eqId == 0) return;

        System.out.print("Are you sure you want to delete equipment ID " + eqId + "? (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("y")) {
            if (equipmentDao.deleteEquipment(adminUser.id(), eqId)) {
                System.out.println("Equipment deleted successfully.");
            } else {
                System.out.println("Failed to delete equipment.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void viewSystemAuditLogs() {
        boolean viewing = true;
        while (viewing) {
            System.out.println("\n--- System Audit Logs Menu ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Back to Admin Dashboard");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> displayLogs(logDao.getAllLogs());
                case "2" -> {
                    int targetUserId = readIntInput("Enter User ID to filter by: ");
                    displayLogs(logDao.getLogsByUserId(targetUserId));
                }
                case "3" -> viewing = false;
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    private void displayLogs(List<ActivityLog> logs) {
        System.out.println("\n--- Activity Logs ---");
        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }
        String border = "+--------+----------+--------------------------+-----------------------------------------------+----------------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-8s | %-24s | %-45s | %-20s |%n", "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);
        for (ActivityLog log : logs) {
            System.out.printf("| %-6d | %-8d | %-24s | %-45s | %-20s |%n",
                    log.id(), log.userId(), log.actionType(), log.description(), log.timestamp());
        }
        System.out.println(border);
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid numeric integer.");
            }
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.println("Select role:");
            for (int i = 0; i < ROLES.length; i++) {
                System.out.println((i + 1) + ". " + ROLES[i]);
            }
            System.out.print("Choose role (1-" + ROLES.length + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= ROLES.length) {
                    return ROLES[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                for (String r : ROLES) {
                    if (r.equalsIgnoreCase(input)) {
                        return r;
                    }
                }
            }
            System.out.println("Invalid role choice. Please try again.");
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter status (APPROVED, PENDING): ");
            String status = scanner.nextLine().trim().toUpperCase();
            if (status.equals("APPROVED") || status.equals("PENDING")) {
                return status;
            }
            System.out.println("Invalid status. Only 'APPROVED' or 'PENDING' are accepted.");
        }
    }
}