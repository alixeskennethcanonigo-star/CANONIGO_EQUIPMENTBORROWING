package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final User adminUser;
    private final UserDao userDao;
    private final Scanner scanner;

    private static final String[] ROLES = {
            "ADMINISTRATOR", "EQUIPMENT STAFF", "BORROWER", "DEPARTMENT HEAD"
    };

    public AdminMenu(User adminUser, UserDao userDao, Scanner scanner) {
        this.adminUser = adminUser;
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome Admin, " + adminUser.username());

        boolean managing = true;
        while (managing) {
            System.out.println("\n------ Administrator Dashboard ------");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role & Status");
            System.out.println("6. Delete User");
            System.out.println("7. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingRegistrations();
                case "3" -> approveUserAccount();
                case "4" -> addNewUser();
                case "5" -> updateUser();
                case "6" -> deleteUser();
                case "7" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    managing = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number between 1 and 7.");
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
        System.out.printf("%-5s | %-15s | %-20s | %-10s%n", "ID", "Username", "Role", "Status");
        System.out.println("-".repeat(60));
        for (User u : users) {
            System.out.printf("%-5d | %-15s | %-20s | %-10s%n", u.id(), u.username(), u.roles(), u.status());
        }
    }

    private void viewPendingRegistrations() {
        System.out.println("\n--- Pending Registrations ---");
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts awaiting approval.");
            return;
        }
        System.out.printf("%-5s | %-15s | %-20s | %-10s%n", "ID", "Username", "Role", "Status");
        System.out.println("-".repeat(60));
        for (User u : pendingUsers) {
            System.out.printf("%-5d | %-15s | %-20s | %-10s%n", u.id(), u.username(), u.roles(), u.status());
        }
    }

    private void approveUserAccount() {
        System.out.println("\n--- Approve User Account ---");
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        System.out.printf("%-5s | %-15s | %-20s | %-10s%n", "ID", "Username", "Role", "Status");
        System.out.println("-".repeat(60));
        for (User u : pendingUsers) {
            System.out.printf("%-5d | %-15s | %-20s | %-10s%n", u.id(), u.username(), u.roles(), u.status());
        }

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

        if (userDao.approveUser(id)) {
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

        if (userDao.adminAddUser(username, password, role, status)) {
            System.out.println("User added successfully!");
        } else {
            System.out.println("Failed to add user.");
        }
    }

    private void updateUser() {
        viewAllUsers();
        System.out.println("\n--- Update User Role & Status ---");
        int targetUserId = readIntInput("Enter User ID to update (or enter 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        // Self-Edit Restriction:
        if (targetUserId == adminUser.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        String newRole = readValidRole();
        String newStatus = readValidStatus();

        if (userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus)) {
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
            if (userDao.deleteUser(id)) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // --- INPUT VALIDATION WRAPPERS ---

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