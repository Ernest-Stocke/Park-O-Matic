package com.parkomatic.model;

/** A login account. Only a salted password hash is stored, never the password. */
public class User {
    private final String username;
    private final String passwordHash;
    private final Role role;

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }

    @Override
    public String toString() { return username + " (" + role + ")"; }
}
