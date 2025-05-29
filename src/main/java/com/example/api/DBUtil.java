package com.example.api;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {
    private static final String URL = "jdbc:mysql://u3omantxarlsbr7j:wHCcSmOCDfcXZI301Xsp@boxxce1lr5wnrn08idjz-mysql.services.clever-cloud.com:3306/boxxce1lr5wnrn08idjz";
    private static final String USER = "u3omantxarlsbr7j"; // change if needed
    private static final String PASS = "wHCcSmOCDfcXZI301Xsp"; // change if needed

    public static Connection getConnection() {
        Connection conn = null;
        try {
            System.out.println("🔌 Attempting to load JDBC driver...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ JDBC Driver loaded.");

            System.out.println("🔍 Connecting to DB: " + URL);
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ Connection successful.");
        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        return conn;
    }

}
