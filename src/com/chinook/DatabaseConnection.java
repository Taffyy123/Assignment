package com.chinook;

import java.sql.*;

public class DatabaseConnection {
    // EXACT environment variable names as specified (typos and all)
    private static final String PROTOCOL = System.getenv("CHINOOK DB PROTO");
    private static final String HOST = System.getenv("CHINOOK DB HOST");
    private static final String PORT = System.getenv("CHINOOK DB PORT");
    private static final String DATABASE = System.getenv("CHINOOK DB NAME");
    private static final String USER = System.getenv("CHINOOK DB USERNAME");
    private static final String PASSWORD = System.getenv("CHINOOK DB PASSWORD");

    // Build URL from environment variables
    private static final String URL;

    static {
        // Set defaults if environment variables aren't set
        String protocol = PROTOCOL != null ? PROTOCOL : "jdbc:mysql";
        String host = HOST != null ? HOST : "localhost";
        String port = PORT != null ? PORT : "3306";
        String db = DATABASE != null ? DATABASE : "Chinook";

        URL = String.format("%s://%s:%s/%s", protocol, host, port, db);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (USER == null) {
            throw new SQLException(
                    "Database credentials not set. Please set CHINOOK DB USERNAME environment variable.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD != null ? PASSWORD : "");
    }

    public static void main(String[] args) {
        // Test the connection
        System.out.println("=== Database Configuration ===");
        System.out.println("URL: " + URL);
        System.out.println("User: " + (USER != null ? USER : "NOT SET"));
        System.out.println("Password: " + (PASSWORD != null ? "********" : "NOT SET"));
        System.out.println();

        try (Connection conn = getConnection()) {
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.err.println("\nPlease set environment variables:");
            System.err.println("  CHINOOK DB PROTO=jdbc:mysql");
            System.err.println("  CHINOOK DB HOST=localhost");
            System.err.println("  CHINOOK DB PORT=3306");
            System.err.println("  CHINOOK DB NAME=Chinook");
            System.err.println("  CHINOOK DB USERNAME=your_username");
            System.err.println("  CHINOOK DB PASSWORD=your_password");
        }
    }
}