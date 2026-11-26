package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

/**
 * The main class of the domain logic. The GUI only calls these methods.
 * It keeps the accounts and requests, checks every input, and saves/loads the text files.
 */
public class GPUSystem {

    private Administrator admin = new Administrator();
    private LinkedHashMap<String, Student> students = new LinkedHashMap<String, Student>();
    private ArrayList<AccessRequest> requests = new ArrayList<AccessRequest>();
    private Scheduler scheduler = new Scheduler(students);
    private int nextRequestId = 1;

    // ================= accounts =================

    public User login(String username, String password) {
        if (username.equals(admin.getUsername()) && admin.checkPassword(password)) {
            return admin;
        }
        Student student = students.get(username);
        if (student != null && student.checkPassword(password)) {
            return student;
        }
        String requestStatus = "";               // status of this user's latest request
        for (AccessRequest r : requests) {
            if (r.getUniversityId().equals(username)) {
                requestStatus = r.getStatus();
            }
        }
        if (student == null && requestStatus.equals("Pending")) {
            throw new IllegalArgumentException("Your request is waiting for the administrator's approval.");
        }
        if (student == null && requestStatus.equals("Rejected")) {
            throw new IllegalArgumentException("Your registration request was rejected.");
        }
        throw new IllegalArgumentException("Wrong username or password.");
    }

    public AccessRequest register(String id, String name, String email, String phone, String password) {
        id = id.trim();
        name = clean(name);
        email = email.trim();
        phone = phone.trim();
        if (!id.matches("[0-9]{5,12}")) {
            throw new IllegalArgumentException("University ID must be 5 to 12 digits.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Please enter your name.");
        }
        if (!email.matches("[^@ |]+@[^@ |]+\\.[^@ |]+")) {
            throw new IllegalArgumentException("Please enter a valid email (example: name@ku.ac.ae).");
        }
        if (!phone.matches("[0-9]{7,15}")) {
            throw new IllegalArgumentException("Phone number must be 7 to 15 digits.");
        }
        if (password.length() < 6 || password.contains(" ") || password.contains("|")) {
            throw new IllegalArgumentException("Password must be at least 6 characters with no spaces.");
        }
        if (students.containsKey(id)) {
            throw new IllegalArgumentException("An account with this university ID already exists.");
        }
        for (AccessRequest r : requests) {
            if (r.getUniversityId().equals(id) && r.getStatus().equals("Pending")) {
                throw new IllegalArgumentException("A request with this university ID is already pending.");
            }
        }
        AccessRequest request = new AccessRequest(nextRequestId++, id, name, email, phone, password,
                scheduler.getCurrentTime());
        requests.add(request);
        return request;
    }

    public void approveRequest(int requestId, int minutes) {
        AccessRequest r = findRequest(requestId);
        if (minutes < 0) {
            throw new IllegalArgumentException("Minutes cannot be negative.");
        }
        if (students.containsKey(r.getUniversityId())) {
            throw new IllegalStateException("This student already has an account.");
        }
        r.approve();
        students.put(r.getUniversityId(), new Student(r.getUniversityId(), r.getPassword(), r.getName(),
                r.getEmail(), r.getPhoneNumber(), new TimeAllowance(minutes)));
    }

    public void rejectRequest(int requestId) {
        findRequest(requestId).reject();
    }

    public void grantExtraTime(String id, int minutes, String reason) {
        checkReason(reason);
        findStudent(id).getAllowance().addExtraTime(minutes, clean(reason), scheduler.getCurrentTime());
    }

    public void changeAllocatedTime(String id, int minutes, String reason) {
        checkReason(reason);
        findStudent(id).getAllowance().changeAllocatedTime(minutes, clean(reason), scheduler.getCurrentTime());
    }

    // ================= jobs =================

    public Job submitJob(Student student, String title, String application, String description, String size) {
        title = clean(title);
        application = clean(application);
        description = clean(description);
        if (title.isEmpty() || application.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("Please fill in the title, application name and description.");
        }
        if (student.getAllowance().getAvailableTime() <= 0) {
            throw new IllegalArgumentException("You have 0 available minutes. You cannot submit new jobs.");
        }
        student.getAllowance().reserve(Job.getWorkloadUnits(size));   // rejects jobs that are too long
        return scheduler.createJob(student, title, application, description, size);
    }

    // students can cancel only their own jobs; the administrator can cancel any job
    public void cancelJob(User user, int jobId, String reason) {
        checkReason(reason);
        Job job = scheduler.findJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job " + jobId + " was not found.");
        }
        if (user instanceof Student && !job.getStudentId().equals(user.getUsername())) {
            throw new IllegalArgumentException("You can only cancel your own jobs.");
        }
        scheduler.cancelJob(job, user.getUsername(), clean(reason));
    }

    public void advanceMinute() {
        scheduler.advanceMinute();
    }

    // ================= helpers =================

    public Student findStudent(String id) {
        Student s = students.get(id.trim());
        if (s == null) {
            throw new IllegalArgumentException("No approved student with ID " + id + ".");
        }
        return s;
    }

    private AccessRequest findRequest(int requestId) {
        for (AccessRequest r : requests) {
            if (r.getRequestId() == requestId) {
                return r;
            }
        }
        throw new IllegalArgumentException("Request " + requestId + " was not found.");
    }

    private void checkReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a reason.");
        }
    }

    // "|" separates fields in our files, so it is not allowed inside text
    private String clean(String text) {
        return text.replace("|", "/").replace("\n", " ").trim();
    }

    public LinkedHashMap<String, Student> getStudents() { return students; }
    public ArrayList<AccessRequest> getRequests() { return requests; }
    public Scheduler getScheduler() { return scheduler; }

    // ================= files (data folder) =================

    public void saveData() throws FileNotFoundException {
        new File("data").mkdir();

        PrintWriter out = new PrintWriter("data/system.txt");
        out.println(Scheduler.format(scheduler.getStartTime()));
        out.println(Scheduler.format(scheduler.getCurrentTime()));
        out.println(nextRequestId);
        for (GPU gpu : scheduler.getGpus()) {
            out.println(gpu.getBusyMinutes());
        }
        out.close();

        saveList("data/students.txt", new ArrayList<Saveable>(students.values()));
        saveList("data/requests.txt", new ArrayList<Saveable>(requests));
        saveList("data/jobs.txt", new ArrayList<Saveable>(scheduler.getJobs()));

        out = new PrintWriter("data/history.txt");
        for (Student s : students.values()) {
            for (String entry : s.getAllowance().getHistory()) {
                out.println(s.getUniversityId() + "|" + entry);
            }
        }
        out.close();
    }

    // the same method saves students, requests and jobs (polymorphism)
    private void saveList(String fileName, ArrayList<Saveable> list) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(fileName);
        for (Saveable item : list) {
            out.println(item.toFileLine());
        }
        out.close();
    }

    // returns false if there is no saved data yet
    public boolean loadData() throws Exception {
        if (!new File("data/system.txt").exists()) {
            return false;
        }
        String fileName = "system.txt";
        try {
            Scanner in = new Scanner(new File("data/system.txt"));
            scheduler.setStartTime(Scheduler.parse(in.nextLine()));
            scheduler.setCurrentTime(Scheduler.parse(in.nextLine()));
            nextRequestId = Integer.parseInt(in.nextLine());
            for (GPU gpu : scheduler.getGpus()) {
                gpu.setBusyMinutes(Integer.parseInt(in.nextLine()));
            }
            in.close();

            fileName = "students.txt";
            for (String line : readLines(fileName)) {
                Student s = Student.fromFileLine(line);
                students.put(s.getUniversityId(), s);
            }
            fileName = "history.txt";
            for (String line : readLines(fileName)) {
                String id = line.substring(0, line.indexOf("|"));
                students.get(id).getAllowance().getHistory().add(line.substring(line.indexOf("|") + 1));
            }
            fileName = "requests.txt";
            for (String line : readLines(fileName)) {
                requests.add(AccessRequest.fromFileLine(line));
            }
            fileName = "jobs.txt";
            for (String line : readLines(fileName)) {
                scheduler.restoreJob(Job.fromFileLine(line));
            }
        } catch (Exception e) {
            throw new Exception("The file data/" + fileName + " is missing or damaged.");
        }
        return true;
    }

    private ArrayList<String> readLines(String fileName) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<String>();
        Scanner in = new Scanner(new File("data/" + fileName));
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        in.close();
        return lines;
    }

    // ================= sample data for testing =================

    /**
     * 5 approved students, 2 pending and 1 rejected request, and 20 jobs.
     * At the end all four GPUs are busy and five jobs are waiting. Password of all students: pass1234
     */
    public void loadSampleData() {
        scheduler.setStartTime(LocalDateTime.of(2026, 11, 24, 10, 0));
        String[][] people = {
                {"100071234", "Hamad Al Dhaheri", "hamad@ku.ac.ae", "0521112233"},
                {"100072345", "Aisha Al Nuaimi", "aisha@ku.ac.ae", "0522223344"},
                {"100073456", "Rashid Al Ketbi", "rashid@ku.ac.ae", "0523334455"},
                {"100074567", "Sara Al Mazrouei", "sara@ku.ac.ae", "0524445566"},
                {"100075678", "Zayed Al Shamsi", "zayed@ku.ac.ae", "0525556677"},
                {"100076789", "Maitha Al Falasi", "maitha@ku.ac.ae", "0526667788"},
                {"100077890", "Hessa Al Suwaidi", "hessa@ku.ac.ae", "0527778899"},
                {"100078901", "Abdulla Al Marri", "abdulla@ku.ac.ae", "0528889900"}};
        for (String[] p : people) {
            register(p[0], p[1], p[2], p[3], "pass1234");
        }
        approveRequest(1, 100);
        approveRequest(2, 100);
        approveRequest(3, 100);
        approveRequest(4, 80);                 // allowance adjusted by the administrator
        approveRequest(5, 100);
        rejectRequest(6);                      // requests 7 and 8 stay pending
        grantExtraTime("100073456", 40, "Graduation project deadline");

        Student hamad = findStudent("100071234");
        Student aisha = findStudent("100072345");
        Student rashid = findStudent("100073456");
        Student sara = findStudent("100074567");
        Student zayed = findStudent("100075678");

        submitJob(hamad, "Crop disease detector", "PyTorch", "Train a CNN on palm leaf images", "Large");        // J1
        submitJob(aisha, "Arabic speech model", "Whisper", "Fine-tune a speech model", "Medium");               // J2
        submitJob(rashid, "CUDA warm-up", "CUDA C++", "Vector addition benchmark", "Small");                    // J3
        advance(5);
        submitJob(sara, "Climate forecast", "TensorFlow", "Forecast Abu Dhabi temperatures", "Very large");     // J4
        submitJob(zayed, "Traffic sign reader", "YOLOv8", "Detect road signs in videos", "Medium");             // J5
        submitJob(hamad, "Quick test", "Python", "Check the data loader", "Small");                             // J6
        cancelJob(hamad, 6, "Submitted by mistake");                         // student cancels a queued job
        advance(10);
        submitJob(aisha, "Medical image segmentation", "MONAI", "Segment lung CT scans", "Large");              // J7
        submitJob(rashid, "Molecule screening", "PyTorch", "Score molecules with a GNN", "Medium");             // J8
        submitJob(zayed, "Lane detection", "OpenCV", "Detect lane markings", "Small");                          // J9
        advance(5);
        cancelJob(admin, 4, "GPU maintenance");                              // admin cancels a running job
        advance(10);
        submitJob(sara, "Solar panel defects", "Keras", "Classify solar panel photos", "Small");                // J10
        submitJob(zayed, "Drone path planning", "Isaac Sim", "Train a drone navigation model", "Large");       // J11
        advance(5);
        submitJob(hamad, "Hyperparameter search", "Optuna", "Tune learning rate", "Medium");                    // J12
        submitJob(aisha, "Report figures", "Matplotlib", "Draw result figures", "Small");                       // J13
        cancelJob(zayed, 11, "Wrong settings");                              // student cancels a running job
        submitJob(rashid, "Protein structure", "OpenFold", "Predict protein structures", "Very large");         // J14
        submitJob(sara, "Satellite images", "PyTorch", "Segment mangroves in satellite images", "Large");       // J15
        submitJob(hamad, "Model export", "ONNX", "Export the final model", "Small");                            // J16
        submitJob(zayed, "Drone path planning v2", "Isaac Sim", "Corrected settings", "Medium");                // J17
        submitJob(aisha, "Data augmentation", "Albumentations", "Create extra CT images", "Medium");            // J18
        submitJob(rashid, "Docking test", "AutoDock", "Dock the top 10 molecules", "Small");                    // J19
        submitJob(sara, "Satellite images copy", "PyTorch", "Same as job 15", "Medium");                        // J20
        cancelJob(admin, 20, "Duplicate submission");                        // admin cancels a queued job
        advance(2);
    }

    private void advance(int minutes) {
        for (int i = 0; i < minutes; i++) {
            scheduler.advanceMinute();
        }
    }
}
