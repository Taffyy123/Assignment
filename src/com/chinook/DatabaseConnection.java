package com.chinook;

import java.sql.*;
import java.io.*;
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            loadConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() throws IOException {
        Properties props = new Properties();
        String configPath = System.getProperty("user.home") + "/.chinook/config.properties";

        // Try to load from user home first
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        } else {
            // Try classpath as fallback
            try (InputStream is = DatabaseConnection.class.getResourceAsStream("/config.properties")) {
                if (is != null) {
                    props.load(is);
                }
            }
        }

        URL = props.getProperty("db.url", "jdbc:mysql://localhost:3306/Chinook");
        USER = props.getProperty("db.user", "root");
        PASSWORD = props.getProperty("db.password", "");
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}