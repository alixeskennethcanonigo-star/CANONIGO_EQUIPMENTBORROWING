package com.scc.ui;

import com.scc.model.User;

import java.util.Scanner;

public class UserMenu {

    private final User user;
    private final Scanner scanner;

    public UserMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username());
        System.out.println("Role: " + user.roles() + " | Status: " + user.status());

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. View Profile");
            System.out.println("2. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.println("ID:       " + user.id());
                    System.out.println("Username: " + user.username());
                    System.out.println("Role:     " + user.roles());
                    System.out.println("Status:   " + user.status());
                }
                case "2" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}