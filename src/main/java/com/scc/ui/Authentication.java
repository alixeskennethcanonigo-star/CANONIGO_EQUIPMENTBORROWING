package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Optional;
import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;

    private static final String[] ROLES = {
            "ADMINISTRATOR", "EQUIPMENT STAFF", "BORROWER", "DEPARTMENT HEAD"
    };

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Equipment Borrowing and Monitoring System ===");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register Account ---");
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
        System.out.print("Choose: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            choice = -1;
        }
        if (choice < 1 || choice > ROLES.length) {
            System.out.println("Invalid role.");
            return;
        }

        if (userDao.register(username, password, ROLES[choice - 1])) {
            System.out.println("Registration successful! Status: PENDING");
        } else {
            System.out.println("Registration failed.");
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        Optional<User> result = userDao.login(username, password);
        if (result.isPresent()) {
            User user = result.get();
            // Route Administrator to AdminMenu, others to UserMenu
            if (user.roles().equalsIgnoreCase("ADMINISTRATOR")) {
                new AdminMenu(user, userDao, scanner).show();
            } else {
                new UserMenu(user, scanner).show();
            }
        } else {
            System.out.println("Invalid username or password.");
        }
    }
}