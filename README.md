# GPU Utilization Management System

COSC230 Object-Oriented Programming, Fall 2026, Khalifa University

A Java Swing program that manages four simulated NVIDIA H100 GPUs. Students register and the administrator approves them. Approved students submit jobs, which run First Come First Served on a simulated clock.

## How to run (Eclipse)

1. **File → New → Java Project**.
2. Set the name to `GPUManager`, untick **Use default location**, and **Browse** to this folder. Click **Finish**.
3. If Eclipse asks about `module-info.java`, click **Don't Create**.
4. Right-click `src/gui/Main.java` → **Run As → Java Application**.

## First run

A window asks for the start date and time of the simulation:

- **Start Simulation** starts with no data.
- **Load Sample Data** loads the test data.

After this, the clock moves only with the **Advance Minute** button.

## Accounts

- **Administrator:** `admin` / `gpu2026`
- **Students:** `100071234`, `100072345`, `100073456`, `100074567`, `100075678`. The password for all of them is `pass1234`.
- **Pending requests:** `100077890`, `100078901`. **Rejected request:** `100076789`.

## Project structure (15 files)

```
src/model/     the logic (no GUI code)
   User (abstract), Student, Administrator, AccessRequest,
   Job, GPU, TimeAllowance, Scheduler      <- the 8 suggested classes
   Saveable (interface)                    <- toFileLine() for saving
   GPUSystem                               <- accounts, rules, files, sample data
src/gui/       the windows
   Main, LoginFrame, DashboardFrame (abstract), AdminFrame, StudentFrame
```

## Sample test data

The `sample-data` folder holds the test data as saved files: 5 approved students, 2 pending requests, 1 rejected request, 4 GPUs and 20 jobs in every state, at 24/11/2026 10:37. To use it, copy the folder and rename the copy to `data`, or click **Load Sample Data** on the first run, which creates the same data.

## Saved files

All data is saved in the `data` folder inside the project folder:

- `system.txt`
- `students.txt`
- `history.txt`
- `requests.txt`
- `jobs.txt`

To start again, close the program and delete the `data` folder. In Eclipse, press F5 on the project to see the folder.
