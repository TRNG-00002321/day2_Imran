package com.revature.manager.ui;

import com.revature.manager.exceptions.ValidationException;
import com.revature.manager.model.Expense;
import com.revature.manager.model.User;
import com.revature.manager.service.AuthService;
import com.revature.manager.service.ExpenseService;
import com.revature.manager.utils.InputValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

public class Menu {
    private static final Logger logger = Logger.getLogger(Menu.class.getName());

    private final AuthService authService;
    private final ExpenseService expenseService;

    public Menu(AuthService authService, ExpenseService expenseService) {
        this.authService = authService;
        this.expenseService = expenseService;
    }

    /**
     * Drives the console loop until the manager chooses to exit.
     */
    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            Optional<User> manager = authService.login(scanner);
            if (manager.isEmpty()) {
                return;
            }

            boolean keepRunning = true;
            while (keepRunning) {
                printMenuOptions();
                String option = scanner.nextLine().trim();
                keepRunning = handleOption(option, scanner, manager.get());
            }
        }
    }

    private void printMenuOptions() {
        System.out.println("\n--- Manager Menu ---");
        System.out.println("1. View Pending Expenses");
        System.out.println("2. Approve an Expense");
        System.out.println("3. Deny an Expense");
        System.out.println("4. Report by User");
        System.out.println("5. Report by Status");
        System.out.println("6. Report by Category");
        System.out.println("7. Report by Date Range");
        System.out.println("8. Exit");
        System.out.print("Enter a number: ");
    }

    private boolean handleOption(String option, Scanner scanner, User manager) {
        switch (option) {
            case "1" -> {
                printExpenses(expenseService.getPendingExpenses(), true);
                return true;
            }
            case "2" -> {
                reviewExpense(scanner, manager, true);
                return true;
            }
            case "3" -> {
                reviewExpense(scanner, manager, false);
                return true;
            }
            case "4" -> {
                reportByUser(scanner);
                return true;
            }
            case "5" -> {
                reportByStatus(scanner);
                return true;
            }
            case "6" -> {
                reportByCategory(scanner);
                return true;
            }
            case "7" -> {
                reportByDateRange(scanner);
                return true;
            }
            case "8" -> {
                System.out.println("Goodbye.");
                return false;
            }
            default -> {
                System.out.println("Not a valid option.");
                return true;
            }
        }
    }

    private void reviewExpense(Scanner scanner, User manager, boolean approve) {
        List<Expense> pending = expenseService.getPendingExpenses();
        if (pending.isEmpty()) {
            System.out.println("No pending expenses.");
            return;
        }

        printExpenses(pending, true);
        System.out.print("Enter the number of the expense to " + (approve ? "approve" : "deny") + ": ");
        String input = scanner.nextLine().trim();

        int selection;
        try {
            selection = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a number.");
            return;
        }

        if (selection < 1 || selection > pending.size()) {
            System.out.println("Selection is out of range.");
            return;
        }

        Expense target = pending.get(selection - 1);
        System.out.print("Add a comment (optional): ");
        String comment = scanner.nextLine().trim();

        boolean updated = approve
            ? expenseService.approveExpense(target.getId(), manager, comment)
            : expenseService.denyExpense(target.getId(), manager, comment);

        if (updated) {
            System.out.println("Expense " + target.getId() + " updated.");
        } else {
            System.out.println("Expense update failed.");
        }
    }

    private void reportByUser(Scanner scanner) {
        System.out.print("Enter username to report on: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username is required.");
            return;
        }
        Optional<User> userOpt = expenseService.findUserByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("User not found.");
            return;
        }
        List<Expense> expenses = expenseService.listExpensesByUser(userOpt.get().getId());
        printReport(expenses, "User", username);
    }

    private void reportByStatus(Scanner scanner) {
        System.out.print("Enter status (pending/approved/denied): ");
        String status = scanner.nextLine().trim().toLowerCase();
        try {
            InputValidator.requireStatus(status);
            List<Expense> expenses = expenseService.listExpensesByStatus(status);
            printReport(expenses, "Status", status);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void reportByCategory(Scanner scanner) {
        System.out.print("Enter category: ");
        String category = scanner.nextLine().trim();
        try {
            InputValidator.requireNonEmpty(category, "Category");
            List<Expense> expenses = expenseService.listExpensesByCategory(category);
            printReport(expenses, "Category", category);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void reportByDateRange(Scanner scanner) {
        System.out.print("Enter start date (YYYY-MM-DD): ");
        String start = scanner.nextLine().trim();
        System.out.print("Enter end date (YYYY-MM-DD): ");
        String end = scanner.nextLine().trim();
        try {
            LocalDate startDate = InputValidator.parseIsoDate(start, "Start date");
            LocalDate endDate = InputValidator.parseIsoDate(end, "End date");
            if (endDate.isBefore(startDate)) {
                System.out.println("End date cannot be before start date.");
                return;
            }
            List<Expense> expenses = expenseService.listExpensesByDateRange(startDate.toString(), endDate.toString());
            printReport(expenses, "Date range", start + " to " + end);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printReport(List<Expense> expenses, String label, String value) {
        System.out.println("\nReport for " + label + ": " + value);
        printExpenses(expenses, false);
    }

    private void printExpenses(List<Expense> expenses, boolean showIndex) {
        if (expenses.isEmpty()) {
            System.out.println("No expenses to show.");
            return;
        }

        System.out.println("-----------------------------------------------------------------------------------------------");
        if (showIndex) {
            System.out.println("NUM | ID       | User       | Category    | Amount  | Date       | Status    | Description");
        } else {
            System.out.println("ID       | User       | Category    | Amount  | Date       | Status    | Description");
        }
        System.out.println("-----------------------------------------------------------------------------------------------");

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            String line = buildExpenseLine(showIndex, i + 1, expense);
            System.out.println(line);
        }
        System.out.println("-----------------------------------------------------------------------------------------------\n");
        logger.info("Displayed " + expenses.size() + " rows in menu");
    }

    private String buildExpenseLine(boolean showIndex, int rowNumber, Expense expense) {
        StringBuilder builder = new StringBuilder();
        if (showIndex) {
            builder.append(String.format("%3d | ", rowNumber));
        }
        builder.append(String.format(
            "%-8s | %-10s | %-11s | $%6.2f | %-10s | %-9s",
            shrink(expense.getId(), 8),
            shrink(expense.getUsername(), 10),
            shrink(expense.getCategory(), 11),
            expense.getAmount(),
            shrink(expense.getDate(), 10),
            shrink(expense.getStatus(), 9)
        ));
        builder.append(" | ").append(shrink(expense.getDescription(), 28));
        return builder.toString();
    }

    private String shrink(String value, int maxWidth) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxWidth) {
            return value;
        }
        return value.substring(0, maxWidth - 3) + "...";
    }
}
