package com.scc.ui;

import com.scc.dao.BorrowRequestDao;
import com.scc.dao.EquipmentDao;
import com.scc.model.BorrowRequest;
import com.scc.model.Equipment;
import com.scc.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class DepartmentHeadMenu {

    private final User user;
    private final Scanner scanner;
    private final BorrowRequestDao requestDao;
    private final EquipmentDao equipmentDao;

    public DepartmentHeadMenu(User user, Scanner scanner) {
        this.user = user;
        this.scanner = scanner;
        this.requestDao = new BorrowRequestDao();
        this.equipmentDao = new EquipmentDao();
    }

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username());
        System.out.println("Role: " + user.roles() + " | Status: " + user.status());

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==================================================");
            System.out.println("               DEPARTMENT HEAD MENU               ");
            System.out.println("==================================================");
            System.out.println("1. View Pending Borrow Requests");
            System.out.println("2. Approve / Reject Borrow Request");
            System.out.println("3. View All Borrow Requests");
            System.out.println("4. View Equipment Inventory");
            System.out.println("5. View Profile");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewPendingRequests();
                case "2" -> reviewRequest();
                case "3" -> viewAllRequests();
                case "4" -> viewEquipment();
                case "5" -> viewProfile();
                case "6" -> {
                    System.out.println("Logged out. Returning to main menu...");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1 to 6.");
            }
        }
    }

    private void viewPendingRequests() {
        List<BorrowRequest> pending = requestDao.getPendingDeptHeadRequests();
        System.out.println("\n--- Pending Borrow Requests (For Dept Head Review) ---");
        if (pending.isEmpty()) {
            System.out.println("No pending borrow requests at the moment.");
            return;
        }
        printRequestsTable(pending);
    }

    private void reviewRequest() {
        List<BorrowRequest> pending = requestDao.getPendingDeptHeadRequests();
        System.out.println("\n--- Approve / Reject Borrow Request ---");
        if (pending.isEmpty()) {
            System.out.println("No pending requests to review.");
            return;
        }
        printRequestsTable(pending);

        int reqId = readIntInput("Enter Request ID to review (or 0 to cancel): ");
        if (reqId == 0) {
            System.out.println("Review cancelled.");
            return;
        }

        Optional<BorrowRequest> optReq = requestDao.getRequestById(reqId);
        if (optReq.isEmpty() || !"PENDING_DEPT_HEAD".equalsIgnoreCase(optReq.get().status())) {
            System.out.println("Invalid Request ID or request is no longer pending Department Head review.");
            return;
        }

        System.out.println("1. Approve Request");
        System.out.println("2. Reject Request");
        System.out.print("Decision: ");
        String decision = scanner.nextLine().trim();

        boolean approve;
        if ("1".equals(decision)) {
            approve = true;
        } else if ("2".equals(decision)) {
            approve = false;
        } else {
            System.out.println("Invalid decision.");
            return;
        }

        System.out.print("Remarks (optional): ");
        String remarks = scanner.nextLine().trim();

        if (requestDao.reviewByDeptHead(user.id(), reqId, approve, remarks)) {
            System.out.println("Request #" + reqId + (approve ? " APPROVED for release by Equipment Staff." : " REJECTED."));
        } else {
            System.out.println("Failed to update request status.");
        }
    }

    private void viewAllRequests() {
        List<BorrowRequest> all = requestDao.getAllRequests();
        System.out.println("\n--- All Borrow Requests ---");
        if (all.isEmpty()) {
            System.out.println("No borrow requests recorded.");
            return;
        }
        printRequestsTable(all);
    }

    private void viewEquipment() {
        List<Equipment> list = equipmentDao.getAllEquipment();
        System.out.println("\n--- Equipment Inventory ---");
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