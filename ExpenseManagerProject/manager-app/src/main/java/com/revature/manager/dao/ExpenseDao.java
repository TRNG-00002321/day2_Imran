package com.revature.manager.dao;

import com.revature.manager.db.Database;
import com.revature.manager.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpenseDao {
    private static final Logger logger = Logger.getLogger(ExpenseDao.class.getName());

    private final Database database;

    public ExpenseDao(Database database) {
        this.database = database;
    }

    public List<Expense> listPending() {
        return runExpenseQuery("WHERE e.status = 'pending' ORDER BY e.date ASC");
    }

    public List<Expense> listByUser(String userId) {
        return runExpenseQuery("WHERE e.user_id = ? ORDER BY e.date DESC", userId);
    }

    public List<Expense> listByStatus(String status) {
        return runExpenseQuery("WHERE e.status = ? ORDER BY e.date DESC", status);
    }

    public List<Expense> listByCategory(String category) {
        return runExpenseQuery("WHERE lower(e.category) = lower(?) ORDER BY e.date DESC", category);
    }

    public List<Expense> listByDateRange(String startDateInclusive, String endDateInclusive) {
        return runExpenseQuery("WHERE e.date BETWEEN ? AND ? ORDER BY e.date ASC", startDateInclusive, endDateInclusive);
    }

    /**
     * Changes the status of the expense and adds an approvals row in one transaction.
     */
    public boolean updateStatus(String expenseId, String status, String reviewer, String comment) {
        String updateSql = """
                UPDATE expenses
                SET status = ?, reviewer = ?, comment = ?, review_date = ?
                WHERE id = ?
                """;
        String approvalSql = """
                INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = database.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement approvalStmt = conn.prepareStatement(approvalSql)) {

            conn.setAutoCommit(false);

            String reviewDate = LocalDate.now().toString();
            updateStmt.setString(1, status);
            updateStmt.setString(2, reviewer);
            updateStmt.setString(3, comment);
            updateStmt.setString(4, reviewDate);
            updateStmt.setString(5, expenseId);

            int updatedRows = updateStmt.executeUpdate();
            if (updatedRows == 0) {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }

            approvalStmt.setString(1, UUID.randomUUID().toString());
            approvalStmt.setString(2, expenseId);
            approvalStmt.setString(3, status);
            approvalStmt.setString(4, reviewer);
            approvalStmt.setString(5, comment);
            approvalStmt.setString(6, reviewDate);
            approvalStmt.executeUpdate();

            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update expense " + expenseId, e);
            return false;
        }
    }

    private List<Expense> runExpenseQuery(String clause, Object... args) {
        String sql = """
                SELECT e.id, e.user_id, e.amount, e.description, e.date, e.status,
                       e.reviewer, e.comment, e.review_date, e.category, u.username
                FROM expenses e
                LEFT JOIN users u ON e.user_id = u.id
                """ + clause;

        List<Expense> result = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapExpense(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error querying expenses", e);
        }
        return result;
    }

    private Expense mapExpense(ResultSet rs) throws SQLException {
        return new Expense(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("username"),
            rs.getString("category"),
            rs.getDouble("amount"),
            rs.getString("description"),
            rs.getString("date"),
            rs.getString("status"),
            rs.getString("reviewer"),
            rs.getString("comment"),
            rs.getString("review_date")
        );
    }
}
