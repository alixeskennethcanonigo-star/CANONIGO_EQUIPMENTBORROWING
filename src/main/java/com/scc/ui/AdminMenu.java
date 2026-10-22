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
            System.out.println("\n--- Administrator Dashboard ---");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Add New User");
            System.out.println("4. Update User");
            System.out.println("5. Delete User");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> managePendingUsers();
                case "3" -> addNewUser();
                case "4" -> updateUser();
                case "5" -> deleteUser();
                case "6" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    managing = false;
                }
                default -> System.out.println("Invalid choice.");
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
        System.out.println("-".repeat(58));
        for (User u : users) {
            System.out.printf("%-5d | %-15s | %-20s | %-10s%n", u.id(), u.username(), u.roles(), u.status());
        }
    }

    private void managePendingUsers() {
        System.out.println("\n--- Pending User Approvals ---");
        List<User> pendingUsers = userDao.getUsersByStatus("PENDING");
        if (pendingUsers.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        System.out.printf("%-5s | %-15s | %-20s%n", "ID", "Username", "Role");
        System.out.println("-".repeat(45));
        for (User u : pendingUsers) {
            System.out.printf("%-5d | %-15s | %-20s%n", u.id(), u.username(), u.roles());
        }

        System.out.print("Enter User ID to approve/reject (or 0 to go back): ");
        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }

        if (id == 0) return;

        System.out.print("Action - Type 'APPROVE' or 'REJECT': ");
        String action = scanner.nextLine().trim().toUpperCase();

        String newStatus = switch (action) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            default -> null;
        };

        if (newStatus == null) {
            System.out.println("Invalid action.");
            return;
        }

        if (userDao.updateStatus(id, newStatus)) {
            System.out.println("User status successfully updated to " + newStatus + "!");
        } else {
            System.out.println("Failed to update user status.");
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

        System.out.println("Select role:");
        for (int i = 0; i < ROLES.length; i++) {
            System.out.println((i + 1) + ". " + ROLES[i]);
        }
        System.out.print("Choose role: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            choice = -1;
        }
        if (choice < 1 || choice > ROLES.length) {
            System.out.println("Invalid role choice.");
            return;
        }

        System.out.print("Set Status (e.g., APPROVED, PENDING): ");
        String status = scanner.nextLine().trim().toUpperCase();
        if (status.isEmpty()) status = "APPROVED";

        if (userDao.adminAddUser(username, password, ROLES[choice - 1], status)) {
            System.out.println("User added successfully!");
        } else {
            System.out.println("Failed to add user.");
        }
    }

    private void updateUser() {
        viewAllUsers();
        System.out.println("\n--- Update User ---");
        System.out.print("Enter User ID to update: ");
        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }

        System.out.print("New Username: ");
        String username = scanner.nextLine().trim();

        System.out.println("Select new role:");
        for (int i = 0; i < ROLES.length; i++) {
            System.out.println((i + 1) + ". " + ROLES[i]);
        }
        System.out.print("Choose role: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            choice = -1;
        }
        if (choice < 1 || choice > ROLES.length) {
            System.out.println("Invalid role choice.");
            return;
        }

        System.out.print("New Status (e.g., APPROVED, PENDING, REJECTED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        if (userDao.updateUser(id, username, ROLES[choice - 1], status)) {
            System.out.println("User updated successfully!");
        } else {
            System.out.println("Failed to update user.");
        }
    }

    private void deleteUser() {
        viewAllUsers();
        System.out.println("\n--- Delete User ---");
        System.out.print("Enter User ID to delete: ");
        int id;
        try {
            id = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
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
}