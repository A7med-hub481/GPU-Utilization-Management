package model;

import java.time.LocalDateTime;

/**
 * A registration request sent by a student and reviewed by the administrator.
 */
public class AccessRequest implements Saveable {

    private int requestId;
    private String universityId;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String status;              // Pending, Approved or Rejected
    private LocalDateTime requestTime;

    public AccessRequest(int requestId, String universityId, String name, String email,
                         String phoneNumber, String password, LocalDateTime requestTime) {
        this.requestId = requestId;
        this.universityId = universityId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.requestTime = requestTime;
        this.status = "Pending";
    }

    // a request can be approved or rejected only once
    public void approve() {
        checkPending();
        status = "Approved";
    }

    public void reject() {
        checkPending();
        status = "Rejected";
    }

    private void checkPending() {
        if (!status.equals("Pending")) {
            throw new IllegalStateException("Request " + requestId + " was already " + status + ".");
        }
    }

    public int getRequestId() { return requestId; }
    public String getUniversityId() { return universityId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPassword() { return password; }
    public String getStatus() { return status; }
    public LocalDateTime getRequestTime() { return requestTime; }

    @Override
    public String toFileLine() {
        return requestId + "|" + universityId + "|" + name + "|" + email + "|" + phoneNumber + "|"
                + password + "|" + status + "|" + Scheduler.format(requestTime);
    }

    // creates an AccessRequest from a line of requests.txt
    public static AccessRequest fromFileLine(String line) {
        String[] f = line.split("\\|");
        AccessRequest r = new AccessRequest(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4], f[5],
                Scheduler.parse(f[7]));
        r.status = f[6];
        return r;
    }
}
