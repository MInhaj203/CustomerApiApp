package com.example.api;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo(); // /register or /login
        response.setContentType("application/json");

        if (path == null || path.equals("/register")) {
            handleRegistration(request, response);
        } else if (path.equals("/login")) {
            handleLogin(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().print("{\"error\": \"Invalid endpoint\"}");
        }
    }

    private void handleRegistration(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        System.out.println("🔐 Registration request received");

        BufferedReader reader = request.getReader();
        User user = gson.fromJson(reader, User.class);

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("{\"error\": \"Unauthorized - Please login first\"}");
            return;
        }

        String currentRole = (String) session.getAttribute("role");

        if ("limited".equalsIgnoreCase(currentRole)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print("{\"error\": \"Limited users cannot create new users.\"}");
            return;
        }

        if (!"admin".equalsIgnoreCase(currentRole) && "admin".equalsIgnoreCase(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().print("{\"error\": \"Only admin users can create admin accounts.\"}");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "INSERT INTO users (username, password, role, created_by, created_on) VALUES (?, ?, ?, ?, NOW())";
            PreparedStatement stmt = conn.prepareStatement(sql);

            String hashedPassword = PasswordUtil.hashPassword(user.getPassword()); // 🔒 hash the password before storing

            stmt.setString(1, user.getUsername());
            stmt.setString(2, hashedPassword); // ✅ store hashed password
            stmt.setString(3, user.getRole() == null ? "limited" : user.getRole().toLowerCase());
            stmt.setString(4, (String) session.getAttribute("username"));

            int rows = stmt.executeUpdate();

            PrintWriter out = response.getWriter();
            if (rows > 0) {
                out.print("{\"message\": \"User registered successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Registration failed\"}");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().print("{\"error\": \"Username already exists\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Internal error occurred\"}");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        System.out.println("🔐 Login request received");

        BufferedReader reader = request.getReader();
        LoginRequest loginRequest = gson.fromJson(reader, LoginRequest.class);
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT username, password, role FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            PrintWriter out = response.getWriter();

            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");
                String hashedInput = PasswordUtil.hashPassword(password); // 🔒 hash input for comparison

                if (storedHashedPassword.equals(hashedInput)) {
                    // ✅ Valid login
                    HttpSession session = request.getSession(true);
                    session.setAttribute("username", rs.getString("username"));
                    session.setAttribute("role", rs.getString("role"));
                    out.write("{\"message\": \"Login successful\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.write("{\"error\": \"Invalid username or password\"}");
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid username or password\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Login error\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("🚪 Logout request received");

        HttpSession session = request.getSession(false);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (session != null) {
            session.invalidate();
            out.print("{\"message\": \"Logout successful\"}");
        } else {
            out.print("{\"message\": \"No active session to logout\"}");
        }
    }

    // Inner classes for request mapping
    private static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    private static class User {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
