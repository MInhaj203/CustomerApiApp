package com.example.api;

public class Customer {
    private Integer id;     // Changed from int to Integer for null support
    private String name;
    private String email;
    private String city;

    // ✅ Default constructor (required for Gson)
    public Customer() {}

    // ✅ Constructor used in GET responses
    public Customer(Integer id, String name, String email, String city) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.city = city;
    }

    // ✅ Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
