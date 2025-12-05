package com.revature.manager;

import com.revature.manager.dao.ExpenseDao;
import com.revature.manager.dao.UserDao;
import com.revature.manager.db.Database;
import com.revature.manager.service.AuthService;
import com.revature.manager.service.ExpenseService;
import com.revature.manager.ui.Menu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static final String DEFAULT_DB = "revature_expense_manager.db";
    private static final String LOG_FILE = "manager_app.log";

    public static void main(String[] args) {
        configureLogging();

        Path dbPath = resolveDbPath();
        Database database = new Database(dbPath);
        database.initSchema();

        Menu menu = createMenu(database);

        logger.log(Level.INFO, () -> "Launching Manager App using database at " + dbPath);
        menu.start();
        logger.log(Level.INFO, () -> "Manager App shut down");
    }

    private static Menu createMenu(Database database) {
        UserDao userDao = new UserDao(database);
        ExpenseDao expenseDao = new ExpenseDao(database);
        AuthService authService = new AuthService(userDao);
        ExpenseService expenseService = new ExpenseService(expenseDao, userDao);
        return new Menu(authService, expenseService);
    }

    private static Path resolveDbPath() {
        String envPath = System.getenv("EXPENSE_DB_FILE");
        if (envPath != null && !envPath.isBlank()) {
            return Path.of(envPath);
        }
        Path local = Path.of(DEFAULT_DB);
        if (local.toFile().exists()) {
            return local;
        }
        Path parent = Path.of("..", DEFAULT_DB);
        if (parent.toFile().exists()) {
            return parent;
        }
        return local;
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        consoleHandler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(consoleHandler);

        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to start file logging", e);
        }
    }
}
