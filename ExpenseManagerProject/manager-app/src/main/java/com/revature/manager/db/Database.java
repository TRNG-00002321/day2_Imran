package com.revature.manager.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
    private static final Logger logger = Logger.getLogger(Database.class.getName());

    private final Path dbPath;
    private final String jdbcUrl;

    public Database(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public Path getDbPath() {
        return dbPath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Ensures all required tables and columns exist before any DAO uses the database.
     */
    public void initSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            createUsersTable(stmt);
            createExpensesTable(stmt);
            addCategoryColumn(stmt);
            createApprovalsTable(stmt);
            logger.log(Level.INFO, () -> "Database schema verified at " + dbPath.toAbsolutePath());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize schema", e);
        }
    }

    private void createUsersTable(Statement stmt) throws SQLException {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL
                );
                """);
    }

    private void createExpensesTable(Statement stmt) throws SQLException {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT NOT NULL,
                    date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    reviewer TEXT,
                    comment TEXT,
                    review_date TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
                """);
    }

    private void addCategoryColumn(Statement stmt) {
        try {
            stmt.execute("ALTER TABLE expenses ADD COLUMN category TEXT DEFAULT 'Uncategorized'");
        } catch (SQLException e) {
            logger.fine("Category column already exists on expenses table");
        }
    }

    private void createApprovalsTable(Statement stmt) throws SQLException {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS approvals (
                    id TEXT PRIMARY KEY,
                    expense_id TEXT NOT NULL,
                    status TEXT NOT NULL,
                    reviewer TEXT,
                    comment TEXT,
                    review_date TEXT NOT NULL,
                    FOREIGN KEY (expense_id) REFERENCES expenses(id)
                );
                """);
    }
}
