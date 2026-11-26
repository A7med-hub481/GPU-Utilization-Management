package model;

/**
 * Abstract parent class of Student and Administrator.
 */
public abstract class User {

    private String username;
    private String password;
    private String name;

    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    // every subclass returns its own role
    public abstract String getRole();

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return getRole() + ": " + name + " (" + username + ")";
    }
}
