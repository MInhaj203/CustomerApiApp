package com.example.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HashExistingPasswords {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            // Fetch all users and passwords
            String selectSql = "SELECT username, password FROM users";
            PreparedStatement selectStmt = conn.prepareStatement(selectSql);
            ResultSet rs = selectStmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String plainPassword = rs.getString("password");

                // Skip if already hashed (assuming SHA-256 length = 64 characters)
                if (plainPassword != null && plainPassword.length() == 64 && plainPassword.matches("[a-f0-9]+")) {
                    System.out.println("Skipping already hashed password for: " + username);
                    continue;
                }

                String hashedPassword = PasswordUtil.hashPassword(plainPassword);
                String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, hashedPassword);
                updateStmt.setString(2, username);
                updateStmt.executeUpdate();

                System.out.println("🔐 Password hashed for user: " + username);
            }

            System.out.println("✅ Password update completed.");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to update passwords.");
        }
    }
}
