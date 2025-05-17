package com.example.api;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/customers/*")
public class CustomerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();

    // Handle GET (Search customers)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Unauthorized - Please login first\"}");
            return;
        }

        String role = (String) session.getAttribute("role");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String idParam = request.getParameter("id");
        String city = request.getParameter("city");

        try (Connection conn = DBUtil.getConnection()) {
            List<Customer> customers = new ArrayList<>();

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT id, name, email, city");

            if (!"limited".equalsIgnoreCase(role)) {
                sql.append(", created_by, created_on, modified_by, modified_on");
            }
            sql.append(" FROM customers WHERE 1=1");

            if (name != null && !name.isEmpty()) {
                sql.append(" AND name LIKE ?");
            }
            if (email != null && !email.isEmpty()) {
                sql.append(" AND email LIKE ?");
            }
            if (idParam != null && !idParam.isEmpty()) {
                sql.append(" AND id = ?");
            }
            if (city != null && !city.isEmpty()) {
                sql.append(" AND city LIKE ?");
            }

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            int paramIndex = 1;
            if (name != null && !name.isEmpty()) {
                stmt.setString(paramIndex++, "%" + name + "%");
            }
            if (email != null && !email.isEmpty()) {
                stmt.setString(paramIndex++, "%" + email + "%");
            }
            if (idParam != null && !idParam.isEmpty()) {
                stmt.setInt(paramIndex++, Integer.parseInt(idParam));
            }
            if (city != null && !city.isEmpty()) {
                stmt.setString(paramIndex++, "%" + city + "%");
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Customer customer = new Customer();
                customer.setId(rs.getInt("id"));
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setCity(rs.getString("city"));

                if (!"limited".equalsIgnoreCase(role)) {
                    customer.setCreatedBy(rs.getString("created_by"));
                    customer.setCreatedOn(rs.getTimestamp("created_on"));
                    customer.setModifiedBy(rs.getString("modified_by"));
                    customer.setModifiedOn(rs.getTimestamp("modified_on"));
                }
                customers.add(customer);
            }

            String customersJson = gson.toJson(customers);
            out.print(customersJson);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error fetching customers\"}");
        }
    }

    // Handle POST (Create customer)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Unauthorized - Please login first\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        BufferedReader reader = request.getReader();
        Customer customer = gson.fromJson(reader, Customer.class);

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "INSERT INTO customers (name, email, city, created_by, created_on) VALUES (?, ?, ?, ?, NOW())";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getCity());
            stmt.setString(4, username);

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                out.print("{\"message\": \"Customer created successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Failed to create customer\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error creating customer\"}");
        }
    }

    // Handle PUT (Update customer)
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Unauthorized - Please login first\"}");
            return;
        }

        String role = (String) session.getAttribute("role");
        if ("limited".equalsIgnoreCase(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print("{\"error\": \"Permission denied: Limited users cannot update customers.\"}");
            return;
        }

        String username = (String) session.getAttribute("username");

        BufferedReader reader = request.getReader();
        Customer customer = gson.fromJson(reader, Customer.class);

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "UPDATE customers SET name = ?, email = ?, city = ?, modified_by = ?, modified_on = NOW() WHERE id = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getCity());
            stmt.setString(4, username);
            stmt.setInt(5, customer.getId());

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                out.print("{\"message\": \"Customer updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Customer not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error updating customer\"}");
        }
    }

    // 🔥 Handle DELETE (Delete customer)
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Check if the user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Unauthorized - Please login first\"}");
            return;
        }

        // Check if the user has permission to delete
        String role = (String) session.getAttribute("role");
        if ("limited".equalsIgnoreCase(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print("{\"error\": \"Permission denied: Limited users cannot delete customers.\"}");
            return;
        }

        // Extract the ID from the URL path info (e.g., /customers/123 → 123)
        String pathInfo = request.getPathInfo(); // returns "/123"
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Customer ID is required in the URL.\"}");
            return;
        }

        // Remove the leading slash to get the ID
        String idStr = pathInfo.substring(1);

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "DELETE FROM customers WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(idStr));

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                out.print("{\"message\": \"Customer deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Customer not found or already deleted\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid customer ID format\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error deleting customer\"}");
        }
    }

    // 📦 Define internal Customer class
    private static class Customer {
        private int id;
        private String name;
        private String email;
        private String city;
        private String createdBy;
        private Timestamp createdOn;
        private String modifiedBy;
        private Timestamp modifiedOn;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public Timestamp getCreatedOn() { return createdOn; }
        public void setCreatedOn(Timestamp createdOn) { this.createdOn = createdOn; }

        public String getModifiedBy() { return modifiedBy; }
        public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

        public Timestamp getModifiedOn() { return modifiedOn; }
        public void setModifiedOn(Timestamp modifiedOn) { this.modifiedOn = modifiedOn; }
    }
}
