package com.scc.ui;

import com.scc.dao.BorrowRequestDao;
import com.scc.dao.EquipmentDao;
import com.scc.model.BorrowRequest;
import com.scc.model.Equipment;
import com.scc.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class BorrowerMenu {

    private final User user;
    private final Scanner scanner;
    private final EquipmentDao equipmentDao;
    private final BorrowRequestDao requestDao;

    public BorrowerMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
        this.equipmentDao = new EquipmentDao();
        this.requestDao = new BorrowRequestDao();
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username());
        System.out.println("Role: " + user.roles() + " | Status: " + user.status());

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==================================================");
            System.out.println("                 BORROWER MENU                    ");
            System.out.println("==================================================");
            System.out.println("1. View Available Laboratory Equipment");
            System.out.println("2. Request to Borrow Equipment");
            System.out.println("3. View My Borrow Requests");
            System.out.println("4. View My Profile");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewEquipment();
                case "2" -> submitBorrowRequest();
                case "3" -> viewMyRequests();
                case "4" -> viewProfile();
                case "5" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1 to 5.");
            }
        }
    }

    private void viewEquipment() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        System.out.println("\n--- Laboratory Equipment Inventory ---");
        if (list.isEmpty()) {
            System.out.println("No equipment available in the system.");
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

    private void submitBorrowRequest() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        viewEquipment();
        if (list.isEmpty()) {
            return;
        }

        System.out.println("\n--- Submit Borrow Request ---");
        int eqId = readIntInput("Enter Equipment ID to borrow (or 0 to cancel): ");
        if (eqId == 0) {
            System.out.println("Request cancelled.");
            return;
        }

        Optional<Equipment> optEq = equipmentDao.getEquipmentById(eqId);
        if (optEq.isEmpty()) {
            System.out.println("Equipment ID not found.");
            return;
        }

        Equipment eq = optEq.get();
        if (eq.quantity() <= 0 || !"AVAILABLE".equalsIgnoreCase(eq.status())) {
            System.out.println("Equipment is currently not available for borrowing (Status: " + eq.status() + ", Qty: " + eq.quantity() + ").");
            return;
        }

        int qty = readIntInput("Enter quantity to borrow (Max available: " + eq.quantity() + "): ");
        if (qty <= 0 || qty > eq.quantity()) {
            System.out.println("Invalid quantity. Must be between 1 and " + eq.quantity() + ".");
            return;
        }

        System.out.print("Purpose / Lab activity details: ");
        String purpose = scanner.nextLine().trim();
        if (purpose.isEmpty()) {
            System.out.println("Purpose cannot be empty.");
            return;
        }

        if (requestDao.createRequest(user.id(), eqId, qty, purpose)) {
            System.out.println("Borrow request submitted! Status: PENDING_DEPT_HEAD");
        } else {
            System.out.println("Failed to submit borrow request.");
        }
    }

    private void viewMyRequests() {
        List<BorrowRequest> requests = requestDao.getRequestsByBorrower(user.id());
        System.out.println("\n--- My Borrow Requests ---");
        if (requests.isEmpty()) {
            System.out.println("You have not submitted any borrow requests yet.");
            return;
        }
        printRequestsTable(requests);
    }

    private void viewProfile() {
        System.out.println("\n--- User Profile ---");
        System.out.println("ID:       " + user.id());
        System.out.println("Username: " + user.username());
        System.out.println("Role:     " + user.roles());
        System.out.println("Status:   " + user.status());
    }

    private void printRequestsTable(List<BorrowRequest> requests) {
        String border = "+--------+----------------------+------+----------------------+-----------------------+--------------------+";
        System.out.println(border);
        System.out.printf("| %-6s | %-20s | %-4s | %-20s | %-21s | %-18s |%n",
                "Req ID", "Equipment", "Qty", "Status", "Purpose", "Remarks");
        System.out.println(border);
        for (BorrowRequest r : requests) {
            System.out.printf("| %-6d | %-20s | %-4d | %-20s | %-21s | %-18s |%n",
                    r.requestId(),
                    truncate(r.equipmentName(), 20),
                    r.quantity(),
                    truncate(r.status(), 20),
                    truncate(r.purpose(), 21),
                    truncate(r.remarks() == null ? "" : r.remarks(), 18));
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