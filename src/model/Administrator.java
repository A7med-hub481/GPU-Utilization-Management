package model;

/**
 * The predefined administrator account (admin / gpu2026).
 */
public class Administrator extends User {

    public Administrator() {
        super("admin", "gpu2026", "Administrator");
    }

    @Override
    public String getRole() {
        return "Administrator";
    }
}
