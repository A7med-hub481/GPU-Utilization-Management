package model;

/**
 * An approved student. The university ID is the username.
 */
public class Student extends User implements Saveable {

    private String email;
    private String phoneNumber;
    private TimeAllowance allowance;

    public Student(String universityId, String password, String name, String email,
                   String phoneNumber, TimeAllowance allowance) {
        super(universityId, password, name);
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.allowance = allowance;
    }

    @Override
    public String getRole() {
        return "Student";
    }

    public String getUniversityId() { return getUsername(); }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public TimeAllowance getAllowance() { return allowance; }

    @Override
    public String toFileLine() {
        return getUniversityId() + "|" + getPassword() + "|" + getName() + "|" + email + "|" + phoneNumber + "|"
                + allowance.getAllocatedTime() + "|" + allowance.getUsedTime() + "|" + allowance.getReservedTime();
    }

    // creates a Student from a line of students.txt
    public static Student fromFileLine(String line) {
        String[] f = line.split("\\|");
        TimeAllowance allowance = new TimeAllowance(Integer.parseInt(f[5]), Integer.parseInt(f[6]),
                Integer.parseInt(f[7]));
        return new Student(f[0], f[1], f[2], f[3], f[4], allowance);
    }
}
