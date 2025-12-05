package com.revature.manager.service;

import com.revature.manager.dao.UserDao;
import com.revature.manager.model.User;

import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> login(Scanner scanner) {
        System.out.println("\n--- Manager Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("Login failed: user not found.");
            logger.warning("Login failed for username " + username + ": not found");
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!"Manager".equalsIgnoreCase(user.getRole())) {
            System.out.println("Login failed: account exists but role is not Manager.");
            logger.warning("Login failed for username " + username + ": invalid role " + user.getRole());
            return Optional.empty();
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (!user.getPassword().equals(password)) {
            System.out.println("Login failed: incorrect password.");
            logger.warning("Login failed for username " + username + ": wrong password");
            return Optional.empty();
        }

        logger.info("Login successful for " + username);
        return Optional.of(user);
    }
}
