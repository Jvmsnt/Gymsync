import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GYMSYNC: Gym Membership and Workout Plan Management System
 * Play Store Fitness App Edition — Java Swing + JDBC Desktop Application
 * ============================================================================
 * Case Study Team:
 *   - Musnit, Jhan Vince Aaron (Project Manager / DB Schema & Exercise Data)
 *   - Teston, Christian        (Developer / Java Classes, JDBC, CRUD & Logic)
 *   - Barizo, MikeAndrew       (QA Tester / Verification, Validation & Reports)
 *   - Texon, Broderick         (UI/UX Designer / Play Store Mobile-Style UI)
 * ============================================================================
 */
@SuppressWarnings({"serial", "unchecked"})
public final class gymsyncapp extends JFrame {
    private static final long serialVersionUID = 1L;

    // ── Studio Light Default / GitHub Dark Toggle Palette (FlatLaf-Native) ──
    public static boolean darkMode = false;
    public static Color BG_OBSIDIAN     = new Color(248, 250, 252);
    public static Color BG_SURFACE      = new Color(255, 255, 255);
    public static Color CARD_SURFACE    = new Color(255, 255, 255);
    public static Color CARD_HIGHLIGHT  = new Color(241, 245, 249);
    public static Color BORDER_SUBTLE   = new Color(226, 232, 240);
    public static Color INPUT_BG        = new Color(255, 255, 255);

    public static Color TEXT_PRIMARY    = new Color(15, 23, 42);
    public static Color TEXT_SECONDARY  = new Color(71, 85, 105);
    public static Color TEXT_MUTED      = new Color(100, 116, 139);

    public static Color NEON_EMERALD    = new Color(5, 150, 105);
    public static Color CYBER_CYAN      = new Color(13, 148, 136);
    public static Color ELECTRIC_INDIGO = new Color(37, 99, 235);
    public static Color VIOLET_GLOW     = new Color(100, 116, 139);
    public static Color CORAL_RED       = new Color(225, 29, 72);
    public static Color AMBER_GOLD      = new Color(217, 119, 6);

    public static Color TABLE_EVEN_ROW  = new Color(255, 255, 255);
    public static Color TABLE_ALT_ROW   = new Color(248, 250, 252);
    public static Color TABLE_SELECT    = new Color(236, 253, 245);
    public static Color TABLE_SELECT_FG = new Color(6, 95, 70);

    private final DataRepository repo;
    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootContainer = new JPanel(rootCardLayout);
    private User currentUser;

    public gymsyncapp() {
        super("GymSync — Fitness & Membership App");
        this.repo = new DataRepository();
        this.repo.initializeDatabase();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_OBSIDIAN);

        showLoginScreen();
        setContentPane(rootContainer);
    }

    // ========================================================================
    // 1. DOMAIN MODELS (Matching Proposal Section 7 Entities)
    // ========================================================================

    public static class User {
        public int userId;
        public String username;
        public String passwordHash;
        public String role; // "Admin", "Staff", "Member"
        public String firstName;
        public String lastName;
        public String contactNo;
        public LocalDate birthdate;
        public String gender;
        public String address;
        public String emergencyContactName;
        public String emergencyContactNo;
        public String fitnessLevel; // "Beginner", "Intermediate", "Advanced"
        public LocalDate dateRegistered;

        public String getFullName() {
            return firstName + " " + lastName;
        }

        public String getInitials() {
            String f = (firstName != null && !firstName.isEmpty()) ? firstName.substring(0, 1).toUpperCase() : "G";
            String l = (lastName != null && !lastName.isEmpty()) ? lastName.substring(0, 1).toUpperCase() : "S";
            return f + l;
        }
    }

    public static class Membership {
        public int membershipId;
        public int userId;
        public LocalDate startDate;
        public LocalDate endDate;

        public String getComputedStatus(LocalDate referenceDate) {
            if (endDate == null) return "Expired";
            return !endDate.isBefore(referenceDate) ? "Active" : "Expired";
        }

        public long getDaysRemaining(LocalDate referenceDate) {
            if (endDate == null) return -999;
            return ChronoUnit.DAYS.between(referenceDate, endDate);
        }
    }

    public static class Exercise {
        public int exerciseId;
        public String name;
        public String muscleGroup;
        public String difficulty;
        public String equipment;
        public String description;
        public String customGifOrImagePath;

        public static boolean isMachineEquipment(String equipment) {
            if (equipment == null) return false;
            String eq = equipment.toLowerCase(Locale.ROOT);
            return eq.contains("machine") || eq.contains("cable") || eq.contains("pulley")
                    || eq.contains("sled") || eq.contains("station") || eq.contains("tower")
                    || eq.contains("treadmill") || eq.contains("rower") || eq.contains("chair")
                    || eq.contains("pec deck") || eq.contains("smith");
        }

        public boolean requiresMachine() {
            return isMachineEquipment(equipment);
        }

        public String getMachineBadgeText() {
            if (!requiresMachine()) return "✓ No Machine (" + equipment + ")";
            return "⚙ " + equipment.replace("Machine: ", "");
        }

        public String getDefaultGifFilename() {
            switch (exerciseId) {
                case 1:  return "ex_01_lat_pulldown.gif";
                case 2:  return "ex_02_seated_cable_row.gif";
                case 3:  return "ex_03_assisted_pullup.gif";
                case 4:  return "ex_04_chest_press_machine.gif";
                case 5:  return "ex_05_pec_deck_fly.gif";
                case 6:  return "ex_06_incline_smith_press.gif";
                case 7:  return "ex_07_leg_press_sled.gif";
                case 8:  return "ex_08_leg_extension.gif";
                case 9:  return "ex_09_hamstring_curl.gif";
                case 10: return "ex_10_calf_raise.gif";
                case 11: return "ex_11_hack_squat.gif";
                case 12: return "ex_12_shoulder_press_machine.gif";
                case 13: return "ex_13_rear_delt_fly.gif";
                case 14: return "ex_14_cable_lateral_raise.gif";
                case 15: return "ex_15_tricep_pushdown.gif";
                case 16: return "ex_16_preacher_curl.gif";
                case 17: return "ex_17_cable_bicep_curl.gif";
                case 18: return "ex_18_captains_chair.gif";
                case 19: return "ex_19_ab_crunch_machine.gif";
                case 20: return "ex_20_treadmill_incline.gif";
                case 21: return "ex_21_rowing_machine.gif";
                case 22: return "ex_22_push_up.gif";
                case 23: return "ex_23_air_squat.gif";
                case 24: return "ex_24_walking_lunge.gif";
                case 25: return "ex_25_forearm_plank.gif";
                case 26: return "ex_26_bicycle_crunches.gif";
                case 27: return "ex_27_goblet_squat.gif";
                case 28: return "ex_28_dumbbell_chest_press.gif";
                case 29: return "ex_29_dumbbell_row.gif";
                case 30: return "ex_30_dumbbell_shoulder_press.gif";
                case 31: return "ex_31_dumbbell_bicep_curl.gif";
                case 32: return "ex_32_barbell_bench_press.gif";
                default: return (muscleGroup != null ? muscleGroup.toLowerCase(Locale.ROOT).replace(" ", "_") : "general") + ".gif";
            }
        }

        public String getPrimaryMuscles() {
            String n = name != null ? name.toLowerCase(Locale.ROOT) : "";
            if (n.contains("pulldown") || n.contains("pull-up")) return "Latissimus Dorsi, Upper Lats";
            if (n.contains("row") && !n.contains("ergometer")) return "Middle Back (Rhomboids, Traps), Lats";
            if (n.contains("incline")) return "Upper Pectoralis Major (Clavicular)";
            if (n.contains("pec deck") || n.contains("chest") || n.contains("push-up") || n.contains("bench")) return "Pectoralis Major (Sternal)";
            if (n.contains("extension")) return "Quadriceps (Rectus Femoris, Vastus)";
            if (n.contains("hamstring") || n.contains("leg curl")) return "Hamstrings (Biceps Femoris)";
            if (n.contains("calf")) return "Gastrocnemius & Soleus (Calves)";
            if (n.contains("leg press") || n.contains("squat") || n.contains("lunge")) return "Quadriceps, Gluteus Maximus";
            if (n.contains("rear delt")) return "Posterior Deltoid (Rear Delts)";
            if (n.contains("lateral")) return "Lateral Deltoid (Side Delts)";
            if (n.contains("shoulder")) return "Anterior & Lateral Deltoids";
            if (n.contains("tricep") || n.contains("pushdown")) return "Triceps Brachii (Long & Lateral Head)";
            if (n.contains("curl")) return "Biceps Brachii & Brachialis";
            if (n.contains("captain") || n.contains("crunch") || n.contains("plank")) return "Rectus Abdominis & Obliques";
            return "Full Body (Quads, Glutes, Lats, Core)";
        }

        public String getSecondaryMuscles() {
            String mg = muscleGroup != null ? muscleGroup.toLowerCase(Locale.ROOT) : "";
            if (mg.contains("chest")) return "Anterior Deltoids, Triceps Brachii";
            if (mg.contains("back")) return "Biceps Brachii, Rear Deltoids, Forearms";
            if (mg.contains("leg")) return "Hamstrings, Calves, Core Stabilizers";
            if (mg.contains("shoulder")) return "Triceps Brachii, Upper Trapezius";
            if (mg.contains("arm")) return "Forearm Flexors, Anterior Delts";
            if (mg.contains("core")) return "Hip Flexors, Transverse Abdominis";
            return "Hamstrings, Shoulders, Cardiovascular System";
        }

        @Override
        public String toString() {
            return name + " [" + muscleGroup + " • " + (requiresMachine() ? "⚙ " + equipment : equipment) + "]";
        }
    }

    public static class WorkoutPlan {
        public int workoutPlanId;
        public int userId;
        public String planType; // "suggested" or "custom"
        public String goal;
        public LocalDate dateCreated;
        public List<WorkoutPlanExercise> exercises = new ArrayList<>();

        @Override
        public String toString() {
            String typeLabel = planType != null ? planType.toUpperCase(Locale.ROOT) : "PLAN";
            String dateLabel = dateCreated != null ? " (" + dateCreated + ")" : "";
            return "#" + workoutPlanId + " [" + typeLabel + "] " + goal + dateLabel;
        }
    }

    public static class WorkoutPlanExercise {
        public int id;
        public int workoutPlanId;
        public int exerciseId;
        public String exerciseName;
        public String muscleGroup;
        public String equipment;
        public String dayOfWeek;
        public int sets;
        public String reps;
    }

    // ========================================================================
    // 2. SECURITY, VALIDATION & CORE BUSINESS LOGIC
    // ========================================================================

    public static class SecurityUtil {
        public static String sha256(String input) {
            if (input == null) return "";
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }
    }

    public static class BusinessRules {
        public static LocalDate[] calculateRenewalDates(LocalDate latestEndDate, LocalDate renewalDate) {
            LocalDate newStart;
            if (latestEndDate != null && !latestEndDate.isBefore(renewalDate)) {
                newStart = latestEndDate;
            } else {
                newStart = renewalDate;
            }
            LocalDate newEnd = newStart.plusMonths(1);
            return new LocalDate[]{newStart, newEnd};
        }

        public static WorkoutPlan generateRuleBasedPlan(
                int userId,
                String goal,
                String fitnessLevel,
                int daysPerWeek,
                String equipmentPref,
                List<Exercise> allExercises) {
            return generateRuleBasedPlan(userId, goal, fitnessLevel, daysPerWeek, equipmentPref, true, allExercises);
        }

        public static WorkoutPlan generateRuleBasedPlan(
                int userId,
                String goal,
                String fitnessLevel,
                int daysPerWeek,
                String equipmentPref,
                boolean includeMachines,
                List<Exercise> allExercises) {

            WorkoutPlan plan = new WorkoutPlan();
            plan.userId = userId;
            plan.planType = "suggested";
            plan.goal = goal + " (" + fitnessLevel + " • " + daysPerWeek + "d/wk • " + equipmentPref + ")";
            plan.dateCreated = LocalDate.now();

            List<Exercise> pool = allExercises.stream()
                    .filter(ex -> includeMachines || !ex.requiresMachine())
                    .filter(ex -> matchesEquipment(ex.equipment, equipmentPref, includeMachines))
                    .filter(ex -> matchesDifficulty(ex.difficulty, fitnessLevel))
                    .collect(Collectors.toList());

            if (pool.isEmpty()) {
                pool = allExercises.stream()
                        .filter(ex -> includeMachines || !ex.requiresMachine())
                        .collect(Collectors.toList());
            }
            if (pool.isEmpty()) {
                pool = new ArrayList<>(allExercises);
            }

            int defaultSets;
            String defaultReps;
            String goalLower = goal != null ? goal.toLowerCase(Locale.ROOT) : "";
            if (goalLower.contains("weight loss")) {
                defaultSets = 3;
                defaultReps = "12-15";
            } else if (goalLower.contains("muscle")) {
                defaultSets = 4;
                defaultReps = "8-12";
            } else if (goalLower.contains("strength")) {
                defaultSets = 4;
                defaultReps = "6-8";
            } else {
                defaultSets = 3;
                defaultReps = "10-12";
            }

            String[] scheduleDays = getScheduleDays(daysPerWeek);

            for (int d = 0; d < scheduleDays.length; d++) {
                String dayName = scheduleDays[d];
                List<Exercise> daySelection = selectBalancedExercisesForDay(pool, d, 4);
                for (Exercise ex : daySelection) {
                    WorkoutPlanExercise wpe = new WorkoutPlanExercise();
                    wpe.exerciseId = ex.exerciseId;
                    wpe.exerciseName = ex.name;
                    wpe.muscleGroup = ex.muscleGroup;
                    wpe.equipment = ex.equipment;
                    wpe.dayOfWeek = dayName;
                    wpe.sets = defaultSets;
                    wpe.reps = ex.muscleGroup.equalsIgnoreCase("Core") && ex.name.contains("Plank")
                            ? "45 sec" : defaultReps;
                    plan.exercises.add(wpe);
                }
            }
            return plan;
        }

        private static boolean matchesEquipment(String exEquip, String pref, boolean includeMachines) {
            String eq = exEquip != null ? exEquip.toLowerCase(Locale.ROOT) : "";
            boolean isMachine = Exercise.isMachineEquipment(exEquip);
            if (!includeMachines && isMachine) return false;
            if (pref.equalsIgnoreCase("Full Gym")) return true;
            if (pref.equalsIgnoreCase("Gym Machines Only")) return isMachine;
            if (pref.equalsIgnoreCase("Bodyweight")) return eq.contains("bodyweight");
            if (pref.equalsIgnoreCase("Dumbbells")) {
                return eq.contains("bodyweight") || eq.contains("dumbbell");
            }
            if (pref.equalsIgnoreCase("Machine")) {
                return isMachine || eq.contains("bodyweight");
            }
            return true;
        }

        private static boolean matchesDifficulty(String exDiff, String level) {
            if (level.equalsIgnoreCase("Beginner")) return exDiff.equalsIgnoreCase("Beginner");
            if (level.equalsIgnoreCase("Intermediate")) {
                return exDiff.equalsIgnoreCase("Beginner") || exDiff.equalsIgnoreCase("Intermediate");
            }
            return true;
        }

        private static String[] getScheduleDays(int daysPerWeek) {
            switch (daysPerWeek) {
                case 2:  return new String[]{"Tuesday", "Friday"};
                case 4:  return new String[]{"Monday", "Tuesday", "Thursday", "Friday"};
                case 5:  return new String[]{"Monday", "Tuesday", "Wednesday", "Friday", "Saturday"};
                case 3:
                default: return new String[]{"Monday", "Wednesday", "Friday"};
            }
        }

        private static List<Exercise> selectBalancedExercisesForDay(List<Exercise> pool, int dayIndex, int count) {
            List<Exercise> chosen = new ArrayList<>();
            String[][] dayTargets = {
                    {"Legs", "Chest", "Back", "Core"},
                    {"Back", "Shoulders", "Legs", "Arms"},
                    {"Legs", "Chest", "Arms", "Core"},
                    {"Full Body", "Back", "Shoulders", "Core"},
                    {"Legs", "Chest", "Back", "Full Body"}
            };
            String[] targets = dayTargets[dayIndex % dayTargets.length];
            Set<Integer> usedIds = new HashSet<>();

            for (String targetGroup : targets) {
                for (Exercise ex : pool) {
                    if (!usedIds.contains(ex.exerciseId) && ex.muscleGroup.equalsIgnoreCase(targetGroup)) {
                        chosen.add(ex);
                        usedIds.add(ex.exerciseId);
                        break;
                    }
                }
            }
            for (Exercise ex : pool) {
                if (chosen.size() >= count) break;
                if (!usedIds.contains(ex.exerciseId)) {
                    chosen.add(ex);
                    usedIds.add(ex.exerciseId);
                }
            }
            return chosen;
        }
    }

    // ========================================================================
    // 3. DATA REPOSITORY (JDBC SQLite/MySQL + Automatic Embedded Fallback)
    // ========================================================================

    public static class DataRepository {
        private static String getConfig(String propKey, String envKey, String defaultVal) {
            String val = System.getProperty(propKey);
            if (val != null && !val.trim().isEmpty()) return val.trim();
            val = System.getenv(envKey);
            if (val != null && !val.trim().isEmpty()) return val.trim();
            return defaultVal;
        }

        private static final String JDBC_URL = getConfig("gymsync.db.url", "GYMSYNC_DB_URL", "jdbc:sqlite:gymsync.db");
        private static final String JDBC_USER = getConfig("gymsync.db.user", "GYMSYNC_DB_USER", "");
        private static final String JDBC_PASS = getConfig("gymsync.db.password", "GYMSYNC_DB_PASSWORD", "");
        private boolean jdbcAvailable = false;

        private static boolean isMySqlUrl() {
            return JDBC_URL.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql:");
        }

        public boolean isMySql() {
            return isMySqlUrl();
        }

        private static Connection openConnection() throws SQLException {
            Connection conn = !JDBC_USER.isEmpty()
                    ? DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)
                    : DriverManager.getConnection(JDBC_URL);
            if (!isMySqlUrl()) {
                try (Statement st = conn.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
            }
            return conn;
        }

        private final List<User> users = new ArrayList<>();
        private final List<Membership> memberships = new ArrayList<>();
        private final List<Exercise> exercises = new ArrayList<>();
        private final List<WorkoutPlan> workoutPlans = new ArrayList<>();
        private int nextUserId = 1;
        private int nextMembershipId = 1;
        private int nextExerciseId = 1;
        private int nextPlanId = 1;
        private int nextPlanExId = 1;

        public void initializeDatabase() {
            seedInMemoryDefaults();
            try (Connection conn = openConnection()) {
                jdbcAvailable = true;
                createTablesIfNotExist(conn);
                syncIfDatabaseEmpty(conn);
                loadFromJdbc(conn);
            } catch (SQLException ex) {
                jdbcAvailable = false;
                System.err.println("[DataRepository] JDBC initialization failed, using in-memory fallback: " + ex.getMessage());
            }
        }

        private void logSqlError(String operation, SQLException ex) {
            System.err.println("[DataRepository] " + operation + " failed: " + ex.getMessage());
        }

        public boolean isJdbcConnected() {
            return jdbcAvailable;
        }

        private void createTablesIfNotExist(Connection conn) throws SQLException {
            boolean isMySql = isMySqlUrl();
            String pkAuto = isMySql ? "INTEGER PRIMARY KEY AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
            String tableSuffix = isMySql ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" : "";
            try (Statement st = conn.createStatement()) {
                if (!isMySql) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
                st.execute("CREATE TABLE IF NOT EXISTS User (" +
                        "user_id " + pkAuto + "," +
                        "username VARCHAR(50) NOT NULL UNIQUE," +
                        "password_hash VARCHAR(64) NOT NULL," +
                        "role VARCHAR(15) NOT NULL," +
                        "first_name VARCHAR(60) NOT NULL," +
                        "last_name VARCHAR(60) NOT NULL," +
                        "contact_no VARCHAR(25) NOT NULL," +
                        "birthdate DATE NULL," +
                        "gender VARCHAR(20) NULL," +
                        "address VARCHAR(255) NULL," +
                        "emergency_contact_name VARCHAR(100) NULL," +
                        "emergency_contact_no VARCHAR(25) NULL," +
                        "fitness_level VARCHAR(20) NULL," +
                        "date_registered DATE NOT NULL)" + tableSuffix);
                st.execute("CREATE TABLE IF NOT EXISTS Membership (" +
                        "membership_id " + pkAuto + "," +
                        "user_id INTEGER NOT NULL," +
                        "start_date DATE NOT NULL," +
                        "end_date DATE NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE)" + tableSuffix);
                st.execute("CREATE TABLE IF NOT EXISTS Exercise (" +
                        "exercise_id " + pkAuto + "," +
                        "name VARCHAR(100) NOT NULL UNIQUE," +
                        "muscle_group VARCHAR(40) NOT NULL," +
                        "difficulty VARCHAR(20) NOT NULL," +
                        "equipment VARCHAR(80) NOT NULL," +
                        "description TEXT NOT NULL)" + tableSuffix);
                st.execute("CREATE TABLE IF NOT EXISTS WorkoutPlan (" +
                        "workout_plan_id " + pkAuto + "," +
                        "user_id INTEGER NOT NULL," +
                        "plan_type VARCHAR(20) NOT NULL," +
                        "goal VARCHAR(120) NOT NULL," +
                        "date_created DATE NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE)" + tableSuffix);
                st.execute("CREATE TABLE IF NOT EXISTS WorkoutPlanExercise (" +
                        "id " + pkAuto + "," +
                        "workout_plan_id INTEGER NOT NULL," +
                        "exercise_id INTEGER NOT NULL," +
                        "day_of_week VARCHAR(20) NOT NULL," +
                        "sets INTEGER NOT NULL," +
                        "reps VARCHAR(25) NOT NULL," +
                        "FOREIGN KEY (workout_plan_id) REFERENCES WorkoutPlan(workout_plan_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id) ON DELETE RESTRICT)" + tableSuffix);
            }
        }

        private void syncIfDatabaseEmpty(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM User")) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (User u : users) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO User (user_id, username, password_hash, role, first_name, last_name, " +
                                    "contact_no, birthdate, gender, address, emergency_contact_name, emergency_contact_no, " +
                                    "fitness_level, date_registered) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        ps.setInt(1, u.userId);
                        ps.setString(2, u.username);
                        ps.setString(3, u.passwordHash);
                        ps.setString(4, u.role);
                        ps.setString(5, u.firstName);
                        ps.setString(6, u.lastName);
                        ps.setString(7, u.contactNo);
                        ps.setString(8, u.birthdate != null ? u.birthdate.toString() : null);
                        ps.setString(9, u.gender);
                        ps.setString(10, u.address);
                        ps.setString(11, u.emergencyContactName);
                        ps.setString(12, u.emergencyContactNo);
                        ps.setString(13, u.fitnessLevel);
                        ps.setString(14, u.dateRegistered.toString());
                        ps.executeUpdate();
                    }
                }
                for (Membership m : memberships) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO Membership (membership_id, user_id, start_date, end_date) VALUES (?,?,?,?)")) {
                        ps.setInt(1, m.membershipId);
                        ps.setInt(2, m.userId);
                        ps.setString(3, m.startDate.toString());
                        ps.setString(4, m.endDate.toString());
                        ps.executeUpdate();
                    }
                }
                for (Exercise ex : exercises) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO Exercise (exercise_id, name, muscle_group, difficulty, equipment, description) VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, ex.exerciseId);
                        ps.setString(2, ex.name);
                        ps.setString(3, ex.muscleGroup);
                        ps.setString(4, ex.difficulty);
                        ps.setString(5, ex.equipment);
                        ps.setString(6, ex.description);
                        ps.executeUpdate();
                    }
                }
                for (WorkoutPlan wp : workoutPlans) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO WorkoutPlan (workout_plan_id, user_id, plan_type, goal, date_created) VALUES (?,?,?,?,?)")) {
                        ps.setInt(1, wp.workoutPlanId);
                        ps.setInt(2, wp.userId);
                        ps.setString(3, wp.planType);
                        ps.setString(4, wp.goal);
                        ps.setString(5, wp.dateCreated.toString());
                        ps.executeUpdate();
                    }
                    for (WorkoutPlanExercise wpe : wp.exercises) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO WorkoutPlanExercise (id, workout_plan_id, exercise_id, day_of_week, sets, reps) VALUES (?,?,?,?,?,?)")) {
                            ps.setInt(1, wpe.id);
                            ps.setInt(2, wpe.workoutPlanId);
                            ps.setInt(3, wpe.exerciseId);
                            ps.setString(4, wpe.dayOfWeek);
                            ps.setInt(5, wpe.sets);
                            ps.setString(6, wpe.reps);
                            ps.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            } finally {
                conn.setAutoCommit(prevAutoCommit);
            }
        }

        private static LocalDate parseSqlDate(String raw, LocalDate fallback) {
            if (raw == null) return fallback;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) return fallback;
            if (trimmed.length() > 10) trimmed = trimmed.substring(0, 10);
            return LocalDate.parse(trimmed);
        }

        private void loadFromJdbc(Connection conn) throws SQLException {
            users.clear();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM User ORDER BY user_id")) {
                while (rs.next()) {
                    User u = new User();
                    u.userId = rs.getInt("user_id");
                    u.username = rs.getString("username");
                    u.passwordHash = rs.getString("password_hash");
                    u.role = rs.getString("role");
                    u.firstName = rs.getString("first_name");
                    u.lastName = rs.getString("last_name");
                    u.contactNo = rs.getString("contact_no");
                    u.birthdate = parseSqlDate(rs.getString("birthdate"), null);
                    u.gender = rs.getString("gender");
                    u.address = rs.getString("address");
                    u.emergencyContactName = rs.getString("emergency_contact_name");
                    u.emergencyContactNo = rs.getString("emergency_contact_no");
                    u.fitnessLevel = rs.getString("fitness_level");
                    u.dateRegistered = parseSqlDate(rs.getString("date_registered"), LocalDate.now());
                    users.add(u);
                    nextUserId = Math.max(nextUserId, u.userId + 1);
                }
            }

            memberships.clear();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM Membership ORDER BY membership_id")) {
                while (rs.next()) {
                    Membership m = new Membership();
                    m.membershipId = rs.getInt("membership_id");
                    m.userId = rs.getInt("user_id");
                    m.startDate = parseSqlDate(rs.getString("start_date"), LocalDate.now());
                    m.endDate = parseSqlDate(rs.getString("end_date"), LocalDate.now());
                    memberships.add(m);
                    nextMembershipId = Math.max(nextMembershipId, m.membershipId + 1);
                }
            }

            exercises.clear();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM Exercise ORDER BY exercise_id")) {
                while (rs.next()) {
                    Exercise ex = new Exercise();
                    ex.exerciseId = rs.getInt("exercise_id");
                    ex.name = rs.getString("name");
                    ex.muscleGroup = rs.getString("muscle_group");
                    ex.difficulty = rs.getString("difficulty");
                    ex.equipment = rs.getString("equipment");
                    ex.description = rs.getString("description");
                    exercises.add(ex);
                    nextExerciseId = Math.max(nextExerciseId, ex.exerciseId + 1);
                }
            }

            workoutPlans.clear();
            Map<Integer, WorkoutPlan> planMap = new LinkedHashMap<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM WorkoutPlan ORDER BY workout_plan_id DESC")) {
                while (rs.next()) {
                    WorkoutPlan wp = new WorkoutPlan();
                    wp.workoutPlanId = rs.getInt("workout_plan_id");
                    wp.userId = rs.getInt("user_id");
                    wp.planType = rs.getString("plan_type");
                    wp.goal = rs.getString("goal");
                    wp.dateCreated = parseSqlDate(rs.getString("date_created"), LocalDate.now());
                    workoutPlans.add(wp);
                    planMap.put(wp.workoutPlanId, wp);
                    nextPlanId = Math.max(nextPlanId, wp.workoutPlanId + 1);
                }
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT wpe.*, e.name AS ex_name, e.muscle_group, e.equipment " +
                         "FROM WorkoutPlanExercise wpe JOIN Exercise e ON wpe.exercise_id = e.exercise_id " +
                         "ORDER BY wpe.id")) {
                while (rs.next()) {
                    WorkoutPlanExercise wpe = new WorkoutPlanExercise();
                    wpe.id = rs.getInt("id");
                    wpe.workoutPlanId = rs.getInt("workout_plan_id");
                    wpe.exerciseId = rs.getInt("exercise_id");
                    wpe.exerciseName = rs.getString("ex_name");
                    wpe.muscleGroup = rs.getString("muscle_group");
                    wpe.equipment = rs.getString("equipment");
                    wpe.dayOfWeek = rs.getString("day_of_week");
                    wpe.sets = rs.getInt("sets");
                    wpe.reps = rs.getString("reps");
                    WorkoutPlan parent = planMap.get(wpe.workoutPlanId);
                    if (parent != null) parent.exercises.add(wpe);
                    nextPlanExId = Math.max(nextPlanExId, wpe.id + 1);
                }
            }
        }

        private void seedInMemoryDefaults() {
            LocalDate today = LocalDate.now();
            addSeedUser("admin", "admin123", "Admin", "Jhan Vince", "Musnit", "0917-555-0101",
                    LocalDate.of(2001, 4, 12), "Male", "Imus, Cavite", "Elena Musnit", "0917-555-0199", null, today.minusMonths(6));
            addSeedUser("staff", "staff123", "Staff", "Christian", "Teston", "0918-555-0102",
                    LocalDate.of(2002, 8, 23), "Male", "Bacoor, Cavite", "Roberto Teston", "0918-555-0198", null, today.minusMonths(5));

            User uCarlos = addSeedUser("carlos_m", "member123", "Member", "Carlos", "Mendoza", "0919-555-0201",
                    LocalDate.of(1999, 5, 14), "Male", "Bayan Luma, Imus, Cavite", "Maria Mendoza", "0919-555-0299", "Beginner", today.minusDays(72));
            User uAngela = addSeedUser("angela_r", "member123", "Member", "Angela", "Reyes", "0920-555-0202",
                    LocalDate.of(2003, 11, 2), "Female", "Tanzang Luma, Imus, Cavite", "Liza Reyes", "0920-555-0298", "Intermediate", today.minusDays(56));
            User uMark = addSeedUser("mark_d", "member123", "Member", "Mark", "Dela Cruz", "0921-555-0203",
                    LocalDate.of(1997, 2, 19), "Male", "Anabu I-C, Imus, Cavite", "Ramon Dela Cruz", "0921-555-0297", "Beginner", today.minusDays(69));
            User uSofia = addSeedUser("sofia_l", "member123", "Member", "Sofia", "Lim", "0922-555-0204",
                    LocalDate.of(2004, 7, 30), "Female", "Bucandala, Imus, Cavite", "Grace Lim", "0922-555-0296", "Beginner", today.minusDays(8));
            User uKevin = addSeedUser("kevin_t", "member123", "Member", "Kevin", "Torres", "0923-555-0205",
                    LocalDate.of(1995, 12, 8), "Male", "Palico IV, Imus, Cavite", "Diana Torres", "0923-555-0295", "Advanced", today.minusDays(45));
            User uHannah = addSeedUser("hannah_g", "member123", "Member", "Hannah", "Garcia", "0924-555-0206",
                    LocalDate.of(2001, 9, 25), "Female", "Medicion I-A, Imus, Cavite", "Noel Garcia", "0924-555-0294", "Beginner", today.minusDays(28));

            addSeedMembership(uCarlos.userId, today.minusDays(72), today.minusDays(42));
            addSeedMembership(uCarlos.userId, today.minusDays(42), today.minusDays(12));
            addSeedMembership(uCarlos.userId, today.minusDays(12), today.plusDays(18));
            addSeedMembership(uAngela.userId, today.minusDays(56), today.minusDays(26));
            addSeedMembership(uAngela.userId, today.minusDays(26), today.plusDays(4));
            addSeedMembership(uMark.userId, today.minusDays(69), today.minusDays(39));
            addSeedMembership(uMark.userId, today.minusDays(39), today.minusDays(9));
            addSeedMembership(uSofia.userId, today.minusDays(8), today.plusDays(22));
            addSeedMembership(uKevin.userId, today.minusDays(45), today.minusDays(15));
            addSeedMembership(uHannah.userId, today.minusDays(28), today.plusDays(2));

            // 21 Commercial Gym Machine Workouts
            addSeedExercise("Lat Pulldown Machine", "Back", "Beginner", "Machine: Lat Pulldown Station", "Sit tall with thighs secured under pads; pull wide bar smoothly down to upper chest while squeezing lats.");
            addSeedExercise("Seated Cable Row Machine", "Back", "Beginner", "Machine: Seated Cable Row Station", "Brace feet on platform, keep spine neutral, and pull V-handle to midsection while retracting shoulder blades.");
            addSeedExercise("Assisted Pull-Up Machine", "Back", "Beginner", "Machine: Assisted Dip & Chin-Up Machine", "Kneel on counter-weighted pad, grip overhead handles, and pull chin above bar with controlled tempo.");
            addSeedExercise("Seated Chest Press Machine", "Chest", "Beginner", "Machine: Converging Chest Press Machine", "Adjust seat so handles align with mid-chest; press forward until arms extend without locking elbows.");
            addSeedExercise("Pec Deck Butterfly Machine", "Chest", "Beginner", "Machine: Pec Deck Fly Station", "Sit with back flat against pad and bring padded arms together in front of chest in a wide arc.");
            addSeedExercise("Incline Smith Machine Press", "Chest", "Intermediate", "Machine: Angled Smith Machine", "Set bench to 30 degrees inside Smith Machine; unlatch bar, lower to upper chest, and press up.");
            addSeedExercise("45° Leg Press Machine", "Legs", "Beginner", "Machine: 45-Degree Leg Press Sled", "Place feet shoulder-width on sled platform, release safety catches, lower until knees reach 90°, and drive up.");
            addSeedExercise("Seated Leg Extension Machine", "Legs", "Beginner", "Machine: Quad Extension Machine", "Align knee joint with machine pivot, hook ankles behind roller pad, and extend legs until straight.");
            addSeedExercise("Lying Hamstring Curl Machine", "Legs", "Beginner", "Machine: Prone Leg Curl Machine", "Lie face down with roller pad just above heels; curl legs up toward glutes and lower slowly.");
            addSeedExercise("Standing Calf Raise Machine", "Legs", "Beginner", "Machine: Standing Calf Machine", "Place shoulders under pads and balls of feet on step; rise onto toes for full calf contraction.");
            addSeedExercise("Hack Squat Machine", "Legs", "Intermediate", "Machine: Linear Hack Squat Sled", "Rest back against angled carriage pad, unlock safety handles, squat to parallel, and drive through heels.");
            addSeedExercise("Seated Shoulder Press Machine", "Shoulders", "Beginner", "Machine: Overhead Press Machine", "Sit upright with handles at ear level and press overhead smoothly without arching lower back.");
            addSeedExercise("Rear Delt Reverse Fly Machine", "Shoulders", "Beginner", "Machine: Rear Delt / Pec Deck Machine", "Face the pad with chest supported, grasp outer handles, and sweep arms back to target rear deltoids.");
            addSeedExercise("Cable Lateral Raise", "Shoulders", "Intermediate", "Machine: Dual Adjustable Pulley Tower", "Stand beside low pulley, grasp D-handle, and raise arm out to shoulder height with a slight elbow bend.");
            addSeedExercise("Cable Rope Tricep Pushdown", "Arms", "Beginner", "Machine: High Pulley Cable Station", "Pin elbows to sides, grip rope attachment, and extend forearms downward until triceps fully contract.");
            addSeedExercise("Seated Preacher Curl Machine", "Arms", "Beginner", "Machine: Bicep Preacher Curl Machine", "Rest upper arms flat on angled preacher pad, grasp handles, and curl upward with full control.");
            addSeedExercise("Low-Pulley Cable Bicep Curl", "Arms", "Beginner", "Machine: Low Pulley Cable Station", "Stand tall facing low pulley with EZ-curl bar attachment and curl toward shoulders while keeping elbows fixed.");
            addSeedExercise("Captain's Chair Knee Raise", "Core", "Beginner", "Machine: Captain's Chair / Roman Chair", "Support body on forearm pads with back pressed against support and raise knees toward chest.");
            addSeedExercise("Seated Ab Crunch Machine", "Core", "Beginner", "Machine: Abdominal Crunch Machine", "Grip top handles, brace core, and curl torso forward against resistance pad without pulling with arms.");
            addSeedExercise("Treadmill Incline Power Walk / Jog", "Full Body", "Beginner", "Machine: Motorized Gym Treadmill", "Set incline to 4–8% at a brisk walking or jogging pace to burn calories and condition the lower body.");
            addSeedExercise("Stationary Rowing Ergometer", "Full Body", "Beginner", "Machine: Indoor Concept Rower", "Drive with legs first, hinge back slightly, and pull handle to lower ribs in a smooth continuous rhythm.");

            // 11 Free Weight & Bodyweight Workouts (No Machine Required)
            addSeedExercise("Push-Up", "Chest", "Beginner", "Bodyweight", "Keep body in a straight plank line, lower chest to floor, and press back up.");
            addSeedExercise("Bodyweight Air Squat", "Legs", "Beginner", "Bodyweight", "Stand shoulder-width apart, sit hips back and down keeping chest upright, then stand.");
            addSeedExercise("Forward Walking Lunge", "Legs", "Beginner", "Bodyweight", "Step forward with one leg, lower hips until both knees form 90-degree angles, push back.");
            addSeedExercise("Forearm Plank Hold", "Core", "Beginner", "Bodyweight", "Hold a rigid straight-body position supported on forearms and toes with braced core.");
            addSeedExercise("Bicycle Crunches", "Core", "Beginner", "Bodyweight", "Lie on back, alternate touching elbow to opposite knee while extending the other leg.");
            addSeedExercise("Dumbbell Goblet Squat", "Legs", "Beginner", "Dumbbells", "Hold one dumbbell vertically at chest height and perform a controlled full squat.");
            addSeedExercise("Flat Dumbbell Chest Press", "Chest", "Beginner", "Dumbbells", "Lie on flat bench and press dumbbells from chest level up until arms are extended.");
            addSeedExercise("One-Arm Dumbbell Row", "Back", "Beginner", "Dumbbells", "Support one hand on bench, keep back flat, and pull dumbbell toward hip crease.");
            addSeedExercise("Seated Dumbbell Shoulder Press", "Shoulders", "Beginner", "Dumbbells", "Sit upright on bench and press dumbbells overhead without arching the lower back.");
            addSeedExercise("Alternating Dumbbell Bicep Curl", "Arms", "Beginner", "Dumbbells", "Stand tall with elbows tucked at sides and curl dumbbells upward with full control.");
            addSeedExercise("Barbell Bench Press", "Chest", "Intermediate", "Barbell", "Lie on flat bench, unrack barbell, lower with control to mid-chest, and press upward.");

            WorkoutPlan planCarlos = addSeedWorkoutPlan(
                    uCarlos.userId, "suggested", "Weight Loss (Beginner - 3 Days - Dumbbells)", today.minusDays(10));
            addSeedPlanExercise(planCarlos, 27, "Monday", 3, "12-15");
            addSeedPlanExercise(planCarlos, 28, "Monday", 3, "12-15");
            addSeedPlanExercise(planCarlos, 25, "Monday", 3, "30 sec");
            addSeedPlanExercise(planCarlos, 29, "Wednesday", 3, "12-15");
            addSeedPlanExercise(planCarlos, 30, "Wednesday", 3, "12-15");
            addSeedPlanExercise(planCarlos, 26, "Wednesday", 3, "15");
            addSeedPlanExercise(planCarlos, 24, "Friday", 3, "12 each leg");
            addSeedPlanExercise(planCarlos, 31, "Friday", 3, "12-15");
            addSeedPlanExercise(planCarlos, 22, "Friday", 3, "10-12");

            WorkoutPlan planSofia = addSeedWorkoutPlan(
                    uSofia.userId, "suggested", "General Fitness (Beginner - 3 Days - Full Gym)", today.minusDays(7));
            addSeedPlanExercise(planSofia, 7, "Monday", 3, "10-12");
            addSeedPlanExercise(planSofia, 4, "Monday", 3, "10-12");
            addSeedPlanExercise(planSofia, 25, "Monday", 3, "30 sec");
            addSeedPlanExercise(planSofia, 1, "Wednesday", 3, "10-12");
            addSeedPlanExercise(planSofia, 12, "Wednesday", 3, "10-12");
            addSeedPlanExercise(planSofia, 19, "Wednesday", 3, "12-15");
            addSeedPlanExercise(planSofia, 27, "Friday", 3, "10-12");
            addSeedPlanExercise(planSofia, 2, "Friday", 3, "10-12");
            addSeedPlanExercise(planSofia, 20, "Friday", 1, "15 min");

            WorkoutPlan planAngela = addSeedWorkoutPlan(
                    uAngela.userId, "custom", "Muscle Gain & Lower Body Focus", today.minusDays(14));
            addSeedPlanExercise(planAngela, 11, "Monday", 4, "8-10");
            addSeedPlanExercise(planAngela, 9, "Monday", 4, "10-12");
            addSeedPlanExercise(planAngela, 32, "Wednesday", 4, "8-10");
            addSeedPlanExercise(planAngela, 1, "Wednesday", 4, "10-12");
            addSeedPlanExercise(planAngela, 7, "Friday", 4, "10-12");
            addSeedPlanExercise(planAngela, 14, "Friday", 3, "12-15");
        }

        private WorkoutPlan addSeedWorkoutPlan(int userId, String planType, String goal, LocalDate dateCreated) {
            WorkoutPlan wp = new WorkoutPlan();
            wp.workoutPlanId = nextPlanId++;
            wp.userId = userId;
            wp.planType = planType;
            wp.goal = goal;
            wp.dateCreated = dateCreated;
            workoutPlans.add(0, wp);
            return wp;
        }

        private void addSeedPlanExercise(WorkoutPlan plan, int exerciseId, String dayOfWeek, int sets, String reps) {
            Exercise ex = exercises.stream()
                    .filter(e -> e.exerciseId == exerciseId)
                    .findFirst()
                    .orElse(null);
            if (ex == null) return;
            WorkoutPlanExercise wpe = new WorkoutPlanExercise();
            wpe.id = nextPlanExId++;
            wpe.workoutPlanId = plan.workoutPlanId;
            wpe.exerciseId = ex.exerciseId;
            wpe.exerciseName = ex.name;
            wpe.muscleGroup = ex.muscleGroup;
            wpe.equipment = ex.equipment;
            wpe.dayOfWeek = dayOfWeek;
            wpe.sets = sets;
            wpe.reps = reps;
            plan.exercises.add(wpe);
        }

        private User addSeedUser(String username, String rawPass, String role, String fn, String ln,
                                 String contact, LocalDate bdate, String gender, String addr,
                                 String emName, String emNo, String fitLevel, LocalDate regDate) {
            User u = new User();
            u.userId = nextUserId++;
            u.username = username;
            u.passwordHash = SecurityUtil.sha256(rawPass);
            u.role = role;
            u.firstName = fn;
            u.lastName = ln;
            u.contactNo = contact;
            u.birthdate = bdate;
            u.gender = gender;
            u.address = addr;
            u.emergencyContactName = emName;
            u.emergencyContactNo = emNo;
            u.fitnessLevel = fitLevel;
            u.dateRegistered = regDate;
            users.add(u);
            return u;
        }

        private void addSeedMembership(int userId, LocalDate start, LocalDate end) {
            Membership m = new Membership();
            m.membershipId = nextMembershipId++;
            m.userId = userId;
            m.startDate = start;
            m.endDate = end;
            memberships.add(m);
        }

        private void addSeedExercise(String name, String group, String diff, String equip, String desc) {
            Exercise ex = new Exercise();
            ex.exerciseId = nextExerciseId++;
            ex.name = name;
            ex.muscleGroup = group;
            ex.difficulty = diff;
            ex.equipment = equip;
            ex.description = desc;
            exercises.add(ex);
        }

        public User authenticate(String username, String rawPassword) {
            String hash = SecurityUtil.sha256(rawPassword);
            for (User u : users) {
                if (u.username.equalsIgnoreCase(username.trim()) && u.passwordHash.equalsIgnoreCase(hash)) {
                    return u;
                }
            }
            return null;
        }

        public List<User> getAllUsers() { return new ArrayList<>(users); }

        public List<User> getMembers() {
            return users.stream().filter(u -> "Member".equalsIgnoreCase(u.role)).collect(Collectors.toList());
        }

        public List<Exercise> getAllExercises() { return new ArrayList<>(exercises); }

        public Membership getLatestMembership(int userId) {
            return memberships.stream()
                    .filter(m -> m.userId == userId)
                    .max(Comparator.comparing((Membership m) -> m.endDate).thenComparingInt(m -> m.membershipId))
                    .orElse(null);
        }

        public List<Membership> getMembershipsByUser(int userId) {
            return memberships.stream()
                    .filter(m -> m.userId == userId)
                    .sorted(Comparator.comparing((Membership m) -> m.endDate).reversed())
                    .collect(Collectors.toList());
        }

        public List<Membership> getAllMemberships() { return new ArrayList<>(memberships); }

        public boolean usernameExists(String username, int excludeUserId) {
            return users.stream().anyMatch(u -> u.userId != excludeUserId && u.username.equalsIgnoreCase(username.trim()));
        }

        public User saveUser(User u, String rawPasswordIfNewOrChanged) {
            if (rawPasswordIfNewOrChanged != null && !rawPasswordIfNewOrChanged.isEmpty()) {
                u.passwordHash = SecurityUtil.sha256(rawPasswordIfNewOrChanged);
            }
            if (u.userId == 0) {
                u.userId = nextUserId++;
                if (u.dateRegistered == null) u.dateRegistered = LocalDate.now();
                users.add(u);
                if (jdbcAvailable) {
                    try (Connection conn = openConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO User (user_id, username, password_hash, role, first_name, last_name, " +
                                 "contact_no, birthdate, gender, address, emergency_contact_name, emergency_contact_no, " +
                                 "fitness_level, date_registered) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        ps.setInt(1, u.userId);
                        ps.setString(2, u.username);
                        ps.setString(3, u.passwordHash);
                        ps.setString(4, u.role);
                        ps.setString(5, u.firstName);
                        ps.setString(6, u.lastName);
                        ps.setString(7, u.contactNo);
                        ps.setString(8, u.birthdate != null ? u.birthdate.toString() : null);
                        ps.setString(9, u.gender);
                        ps.setString(10, u.address);
                        ps.setString(11, u.emergencyContactName);
                        ps.setString(12, u.emergencyContactNo);
                        ps.setString(13, u.fitnessLevel);
                        ps.setString(14, u.dateRegistered.toString());
                        ps.executeUpdate();
                    } catch (SQLException sqlEx) {
                        logSqlError("JDBC write", sqlEx);
                    }
                }
            } else {
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).userId == u.userId) {
                        users.set(i, u);
                        break;
                    }
                }
                if (jdbcAvailable) {
                    try (Connection conn = openConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "UPDATE User SET username=?, password_hash=?, role=?, first_name=?, last_name=?, " +
                                 "contact_no=?, birthdate=?, gender=?, address=?, emergency_contact_name=?, " +
                                 "emergency_contact_no=?, fitness_level=? WHERE user_id=?")) {
                        ps.setString(1, u.username);
                        ps.setString(2, u.passwordHash);
                        ps.setString(3, u.role);
                        ps.setString(4, u.firstName);
                        ps.setString(5, u.lastName);
                        ps.setString(6, u.contactNo);
                        ps.setString(7, u.birthdate != null ? u.birthdate.toString() : null);
                        ps.setString(8, u.gender);
                        ps.setString(9, u.address);
                        ps.setString(10, u.emergencyContactName);
                        ps.setString(11, u.emergencyContactNo);
                        ps.setString(12, u.fitnessLevel);
                        ps.setInt(13, u.userId);
                        ps.executeUpdate();
                    } catch (SQLException sqlEx) {
                        logSqlError("JDBC write", sqlEx);
                    }
                }
            }
            return u;
        }

        public void deleteUser(int userId) {
            users.removeIf(u -> u.userId == userId);
            memberships.removeIf(m -> m.userId == userId);
            workoutPlans.removeIf(wp -> wp.userId == userId);
            if (jdbcAvailable) {
                try (Connection conn = openConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM User WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                } catch (SQLException sqlEx) {
                    logSqlError("JDBC write", sqlEx);
                }
            }
        }

        public Membership addMembership(int userId, LocalDate startDate, LocalDate endDate) {
            Membership m = new Membership();
            m.membershipId = nextMembershipId++;
            m.userId = userId;
            m.startDate = startDate;
            m.endDate = endDate;
            memberships.add(m);
            if (jdbcAvailable) {
                try (Connection conn = openConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO Membership (membership_id, user_id, start_date, end_date) VALUES (?,?,?,?)")) {
                    ps.setInt(1, m.membershipId);
                    ps.setInt(2, m.userId);
                    ps.setString(3, m.startDate.toString());
                    ps.setString(4, m.endDate.toString());
                    ps.executeUpdate();
                } catch (SQLException sqlEx) {
                    logSqlError("JDBC write", sqlEx);
                }
            }
            return m;
        }

        public boolean exerciseNameExists(String name, int excludeExerciseId) {
            if (name == null) return false;
            String trimmed = name.trim();
            return exercises.stream().anyMatch(e -> e.exerciseId != excludeExerciseId && e.name.equalsIgnoreCase(trimmed));
        }

        public void saveExercise(Exercise ex) {
            if (ex.exerciseId == 0) {
                ex.exerciseId = nextExerciseId++;
                exercises.add(ex);
                if (jdbcAvailable) {
                    try (Connection conn = openConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO Exercise (exercise_id, name, muscle_group, difficulty, equipment, description) VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, ex.exerciseId);
                        ps.setString(2, ex.name);
                        ps.setString(3, ex.muscleGroup);
                        ps.setString(4, ex.difficulty);
                        ps.setString(5, ex.equipment);
                        ps.setString(6, ex.description);
                        ps.executeUpdate();
                    } catch (SQLException sqlEx) {
                        logSqlError("JDBC write", sqlEx);
                    }
                }
            } else {
                for (int i = 0; i < exercises.size(); i++) {
                    if (exercises.get(i).exerciseId == ex.exerciseId) {
                        exercises.set(i, ex);
                        break;
                    }
                }
                for (WorkoutPlan wp : workoutPlans) {
                    for (WorkoutPlanExercise wpe : wp.exercises) {
                        if (wpe.exerciseId == ex.exerciseId) {
                            wpe.exerciseName = ex.name;
                            wpe.muscleGroup = ex.muscleGroup;
                            wpe.equipment = ex.equipment;
                        }
                    }
                }
                if (jdbcAvailable) {
                    try (Connection conn = openConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "UPDATE Exercise SET name=?, muscle_group=?, difficulty=?, equipment=?, description=? WHERE exercise_id=?")) {
                        ps.setString(1, ex.name);
                        ps.setString(2, ex.muscleGroup);
                        ps.setString(3, ex.difficulty);
                        ps.setString(4, ex.equipment);
                        ps.setString(5, ex.description);
                        ps.setInt(6, ex.exerciseId);
                        ps.executeUpdate();
                    } catch (SQLException sqlEx) {
                        logSqlError("JDBC write", sqlEx);
                    }
                }
            }
        }

        public boolean isExerciseUsedInPlans(int exerciseId) {
            return workoutPlans.stream()
                    .flatMap(wp -> wp.exercises.stream())
                    .anyMatch(wpe -> wpe.exerciseId == exerciseId);
        }

        public void deleteExercise(int exerciseId) {
            if (isExerciseUsedInPlans(exerciseId)) {
                return;
            }
            exercises.removeIf(e -> e.exerciseId == exerciseId);
            if (jdbcAvailable) {
                try (Connection conn = openConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM Exercise WHERE exercise_id=?")) {
                    ps.setInt(1, exerciseId);
                    ps.executeUpdate();
                } catch (SQLException sqlEx) {
                    logSqlError("JDBC write", sqlEx);
                }
            }
        }

        public List<WorkoutPlan> getWorkoutPlansByUser(int userId) {
            return workoutPlans.stream()
                    .filter(wp -> wp.userId == userId)
                    .sorted(Comparator.comparingInt((WorkoutPlan wp) -> wp.workoutPlanId).reversed())
                    .collect(Collectors.toList());
        }

        public void saveWorkoutPlan(WorkoutPlan plan) {
            plan.workoutPlanId = nextPlanId++;
            if (plan.dateCreated == null) plan.dateCreated = LocalDate.now();
            for (WorkoutPlanExercise wpe : plan.exercises) {
                wpe.id = nextPlanExId++;
                wpe.workoutPlanId = plan.workoutPlanId;
            }
            workoutPlans.add(0, plan);
            if (jdbcAvailable) {
                try (Connection conn = openConnection()) {
                    boolean prevAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO WorkoutPlan (workout_plan_id, user_id, plan_type, goal, date_created) VALUES (?,?,?,?,?)")) {
                            ps.setInt(1, plan.workoutPlanId);
                            ps.setInt(2, plan.userId);
                            ps.setString(3, plan.planType);
                            ps.setString(4, plan.goal);
                            ps.setString(5, plan.dateCreated.toString());
                            ps.executeUpdate();
                        }
                        for (WorkoutPlanExercise wpe : plan.exercises) {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "INSERT INTO WorkoutPlanExercise (id, workout_plan_id, exercise_id, day_of_week, sets, reps) VALUES (?,?,?,?,?,?)")) {
                                ps.setInt(1, wpe.id);
                                ps.setInt(2, wpe.workoutPlanId);
                                ps.setInt(3, wpe.exerciseId);
                                ps.setString(4, wpe.dayOfWeek);
                                ps.setInt(5, wpe.sets);
                                ps.setString(6, wpe.reps);
                                ps.executeUpdate();
                            }
                        }
                        conn.commit();
                    } catch (SQLException sqlEx) {
                        try {
                            conn.rollback();
                        } catch (SQLException rollbackEx) {
                            sqlEx.addSuppressed(rollbackEx);
                        }
                        throw sqlEx;
                    } finally {
                        conn.setAutoCommit(prevAutoCommit);
                    }
                } catch (SQLException sqlEx) {
                    logSqlError("JDBC write", sqlEx);
                }
            }
        }

        public void deleteWorkoutPlan(int workoutPlanId) {
            workoutPlans.removeIf(wp -> wp.workoutPlanId == workoutPlanId);
            if (jdbcAvailable) {
                try (Connection conn = openConnection()) {
                    boolean prevAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement psChild = conn.prepareStatement(
                                "DELETE FROM WorkoutPlanExercise WHERE workout_plan_id=?")) {
                            psChild.setInt(1, workoutPlanId);
                            psChild.executeUpdate();
                        }
                        try (PreparedStatement psParent = conn.prepareStatement(
                                "DELETE FROM WorkoutPlan WHERE workout_plan_id=?")) {
                            psParent.setInt(1, workoutPlanId);
                            psParent.executeUpdate();
                        }
                        conn.commit();
                    } catch (SQLException sqlEx) {
                        try {
                            conn.rollback();
                        } catch (SQLException rollbackEx) {
                            sqlEx.addSuppressed(rollbackEx);
                        }
                        throw sqlEx;
                    } finally {
                        conn.setAutoCommit(prevAutoCommit);
                    }
                } catch (SQLException sqlEx) {
                    logSqlError("JDBC write", sqlEx);
                }
            }
        }
    }

    // ========================================================================
    // 4. MODERN STUDIO UI ENGINE (FlatLaf + Light/Dark Theme + VectorIcons)
    // ========================================================================

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static void installLookAndFeel(boolean dark) {
        darkMode = dark;
        if (dark) {
            BG_OBSIDIAN     = new Color(13, 17, 23);    // #0D1117 GitHub Dark Canvas
            BG_SURFACE      = new Color(22, 27, 34);    // #161B22 Sidebar & Surface
            CARD_SURFACE    = new Color(22, 27, 34);    // #161B22 Card Surface
            CARD_HIGHLIGHT  = new Color(33, 38, 45);    // #21262D Hover / Control
            BORDER_SUBTLE   = new Color(48, 54, 61);    // #30363D 1px Border
            INPUT_BG        = new Color(13, 17, 23);
            TEXT_PRIMARY    = new Color(240, 246, 252);
            TEXT_SECONDARY  = new Color(139, 148, 158);
            TEXT_MUTED      = new Color(110, 118, 129);
            NEON_EMERALD    = new Color(16, 185, 129);
            CYBER_CYAN      = new Color(20, 184, 166);
            ELECTRIC_INDIGO = new Color(56, 139, 253);
            VIOLET_GLOW     = new Color(139, 148, 158);
            CORAL_RED       = new Color(248, 81, 73);
            AMBER_GOLD      = new Color(210, 153, 34);
            TABLE_EVEN_ROW  = new Color(13, 17, 23);
            TABLE_ALT_ROW   = new Color(22, 27, 34);
            TABLE_SELECT    = new Color(19, 47, 38);
            TABLE_SELECT_FG = new Color(236, 253, 245);
        } else {
            BG_OBSIDIAN     = new Color(248, 250, 252); // #F8FAFC Crisp Studio Canvas
            BG_SURFACE      = new Color(255, 255, 255); // #FFFFFF Pure White Sidebar/Surface
            CARD_SURFACE    = new Color(255, 255, 255); // #FFFFFF Pure White Card
            CARD_HIGHLIGHT  = new Color(241, 245, 249); // #F1F5F9 Slate-100 Control
            BORDER_SUBTLE   = new Color(226, 232, 240); // #E2E8F0 Crisp 1px Hairline
            INPUT_BG        = new Color(255, 255, 255);
            TEXT_PRIMARY    = new Color(15, 23, 42);    // #0F172A Slate-900 Ink
            TEXT_SECONDARY  = new Color(71, 85, 105);   // #475569 Slate-600
            TEXT_MUTED      = new Color(100, 116, 139); // #64748B Slate-500
            NEON_EMERALD    = new Color(5, 150, 105);   // #059669 Emerald-600 Accent
            CYBER_CYAN      = new Color(13, 148, 136);  // #0D9488 Teal-600
            ELECTRIC_INDIGO = new Color(37, 99, 235);   // #2563EB Slate-Blue Info
            VIOLET_GLOW     = new Color(100, 116, 139);
            CORAL_RED       = new Color(225, 29, 72);   // #E11D48 Rose-600 Danger
            AMBER_GOLD      = new Color(217, 119, 6);   // #D97706 Amber-600 Warning
            TABLE_EVEN_ROW  = new Color(255, 255, 255);
            TABLE_ALT_ROW   = new Color(248, 250, 252);
            TABLE_SELECT    = new Color(236, 253, 245); // #ECFDF5 Soft Emerald Select
            TABLE_SELECT_FG = new Color(6, 95, 70);     // #065F46 Emerald-800 Text
        }

        try {
            String[] candidates = dark
                    ? new String[]{
                        "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme",
                        "com.formdev.flatlaf.FlatDarkLaf"
                    }
                    : new String[]{
                        "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme",
                        "com.formdev.flatlaf.FlatLightLaf"
                    };
            boolean loaded = false;
            for (String cls : candidates) {
                try {
                    Object ok = Class.forName(cls).getMethod("setup").invoke(null);
                    if (Boolean.TRUE.equals(ok)) {
                        loaded = true;
                        break;
                    }
                } catch (ReflectiveOperationException ignored) {}
            }
            if (!loaded) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } else {
                UIManager.put("Component.arc", 8);
                UIManager.put("Button.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ProgressBar.arc", 999);
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("Component.focusColor", NEON_EMERALD);
                UIManager.put("Component.focusedBorderColor", NEON_EMERALD);
                UIManager.put("Component.borderColor", BORDER_SUBTLE);
                UIManager.put("ScrollBar.width", 10);
                UIManager.put("ScrollBar.showButtons", false);
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.track", BG_OBSIDIAN);
                UIManager.put("ScrollBar.thumb", BORDER_SUBTLE);
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", false);
                UIManager.put("Table.gridColor", BORDER_SUBTLE);
                UIManager.put("Table.selectionBackground", TABLE_SELECT);
                UIManager.put("Table.selectionForeground", TABLE_SELECT_FG);
                UIManager.put("SplitPane.dividerSize", 6);
                UIManager.put("SplitPaneDivider.gripDotCount", 0);
            }
            UIManager.put("Panel.background", BG_OBSIDIAN);
            UIManager.put("OptionPane.background", CARD_SURFACE);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            UIManager.put("Label.foreground", TEXT_PRIMARY);
        } catch (Exception ex) {
            System.err.println("[UI] LookAndFeel setup warning: " + ex.getMessage());
        }
    }

    private void toggleTheme() {
        installLookAndFeel(!darkMode);
        SwingUtilities.updateComponentTreeUI(this);
        getContentPane().setBackground(BG_OBSIDIAN);
        if (currentUser != null) {
            routeToRoleDashboard(currentUser);
        } else {
            showLoginScreen();
        }
    }

    private JPanel createCardPanel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CARD_SURFACE);
        p.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        p.putClientProperty("FlatLaf.style",
                "background: " + hex(CARD_SURFACE) + "; border: 1,1,1,1," + hex(BORDER_SUBTLE) + ",1,10");
        return p;
    }

    private JButton createPrimaryButton(String text, VectorIcon.Kind iconKind) {
        JButton b = new JButton(text);
        if (iconKind != null) {
            b.setIcon(new VectorIcon(iconKind, 14, Color.WHITE));
            b.setIconTextGap(7);
        }
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(NEON_EMERALD);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("FlatLaf.style",
                "background: " + hex(NEON_EMERALD) + "; foreground: #ffffff; " +
                "hoverBackground: " + hex(NEON_EMERALD.darker()) + "; " +
                "pressedBackground: " + hex(NEON_EMERALD.darker().darker()) + "; " +
                "borderWidth: 0; arc: 8; margin: 7,14,7,14");
        return b;
    }

    private JButton createSecondaryButton(String text, VectorIcon.Kind iconKind, Color iconColor) {
        JButton b = new JButton(text);
        if (iconKind != null) {
            b.setIcon(new VectorIcon(iconKind, 14, iconColor != null ? iconColor : TEXT_SECONDARY));
            b.setIconTextGap(7);
        }
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(TEXT_PRIMARY);
        b.setBackground(CARD_SURFACE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("FlatLaf.style",
                "background: " + hex(CARD_SURFACE) + "; foreground: " + hex(TEXT_PRIMARY) + "; " +
                "hoverBackground: " + hex(CARD_HIGHLIGHT) + "; " +
                "borderColor: " + hex(BORDER_SUBTLE) + "; borderWidth: 1; arc: 8; margin: 7,12,7,12");
        return b;
    }

    private JButton createDangerButton(String text, VectorIcon.Kind iconKind) {
        JButton b = new JButton(text);
        if (iconKind != null) {
            b.setIcon(new VectorIcon(iconKind, 14, CORAL_RED));
            b.setIconTextGap(7);
        }
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(CORAL_RED);
        b.setBackground(CARD_SURFACE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color tintHover = darkMode ? new Color(60, 24, 30) : new Color(255, 241, 242);
        b.putClientProperty("FlatLaf.style",
                "background: " + hex(CARD_SURFACE) + "; foreground: " + hex(CORAL_RED) + "; " +
                "hoverBackground: " + hex(tintHover) + "; " +
                "borderColor: " + hex(BORDER_SUBTLE) + "; borderWidth: 1; arc: 8; margin: 7,12,7,12");
        return b;
    }

    private JToggleButton createFilterPill(String text, boolean selected) {
        JToggleButton tb = new JToggleButton(text, selected);
        tb.setFont(new Font("SansSerif", Font.BOLD, 12));
        tb.setFocusPainted(false);
        tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tb.putClientProperty("FlatLaf.style",
                "background: " + hex(CARD_HIGHLIGHT) + "; foreground: " + hex(TEXT_SECONDARY) + "; " +
                "selectedBackground: " + hex(TEXT_PRIMARY) + "; selectedForeground: " + hex(BG_SURFACE) + "; " +
                "borderWidth: 0; arc: 8; margin: 5,11,5,11");
        return tb;
    }

    private JTextField createStyledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.putClientProperty("FlatLaf.style", "arc: 8; margin: 6,10,6,10");
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        pf.putClientProperty("FlatLaf.style", "arc: 8; margin: 6,10,6,10; showRevealButton: true");
        return pf;
    }

    private <T> JComboBox<T> createStyledComboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.putClientProperty("FlatLaf.style", "arc: 8");
        return cb;
    }

    private JScrollPane wrapTableScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        sp.getViewport().setBackground(TABLE_EVEN_ROW);
        return sp;
    }

    private void styleModernTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(TABLE_EVEN_ROW);
        table.setSelectionBackground(TABLE_SELECT);
        table.setSelectionForeground(TABLE_SELECT_FG);
        table.setGridColor(BORDER_SUBTLE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBackground(darkMode ? new Color(22, 27, 34) : new Color(248, 250, 252));
                lbl.setForeground(TEXT_MUTED);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, BORDER_SUBTLE),
                        new EmptyBorder(6, 12, 6, 12)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));
                if (isSelected) {
                    lbl.setBackground(TABLE_SELECT);
                    lbl.setForeground(TABLE_SELECT_FG);
                } else {
                    lbl.setBackground(row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW);
                    lbl.setForeground(TEXT_PRIMARY);
                }
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, rowRenderer);
        table.setDefaultRenderer(Integer.class, rowRenderer);
    }

    private JPanel createKpiStatCard(String label, String value, String subtext, Color accent, VectorIcon.Kind iconKind) {
        JPanel card = createCardPanel(new BorderLayout(10, 6));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_MUTED);

        JLabel iconBox = new JLabel(new VectorIcon(iconKind, 15, accent));
        topRow.add(lbl, BorderLayout.WEST);
        topRow.add(iconBox, BorderLayout.EAST);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 26));
        valLbl.setForeground(TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLbl.setForeground(TEXT_SECONDARY);

        card.add(topRow, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    /**
     * Crisp Lucide-style 2D vector icons drawn with Graphics2D.
     */
    private static class VectorIcon implements Icon {
        enum Kind { USERS, CHART, DUMBBELL, SHIELD, BELL, SEARCH, PLUS, REFRESH, EDIT, TRASH, DOWNLOAD, LOGOUT, SUN, MOON, CHECK }

        private final Kind kind;
        private final int size;
        private final Color color;

        VectorIcon(Kind kind, int size, Color color) {
            this.kind = kind;
            this.size = size;
            this.color = color;
        }

        static Kind forTab(String label) {
            String s = label != null ? label.toLowerCase(Locale.ROOT) : "";
            if (s.contains("analytics") || s.contains("report")) return Kind.CHART;
            if (s.contains("exercise") || s.contains("workout")) return Kind.DUMBBELL;
            if (s.contains("account") || s.contains("staff") || s.contains("admin")) return Kind.SHIELD;
            if (s.contains("alert") || s.contains("expiry")) return Kind.BELL;
            return Kind.USERS;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 16.0, size / 16.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (kind) {
                case USERS:
                    g2.drawOval(3, 2, 5, 5);
                    g2.drawArc(1, 9, 9, 6, 0, 180);
                    g2.drawArc(9, 3, 4, 4, -90, 180);
                    g2.drawArc(10, 9, 5, 5, 0, 90);
                    break;
                case CHART:
                    g2.drawLine(2, 14, 14, 14);
                    g2.drawRect(3, 8, 2, 6);
                    g2.drawRect(7, 5, 2, 9);
                    g2.drawRect(11, 2, 2, 12);
                    break;
                case DUMBBELL:
                    g2.drawLine(4, 8, 12, 8);
                    g2.drawRoundRect(2, 4, 2, 8, 1, 1);
                    g2.drawRoundRect(12, 4, 2, 8, 1, 1);
                    g2.drawLine(1, 6, 1, 10);
                    g2.drawLine(15, 6, 15, 10);
                    break;
                case SHIELD:
                    java.awt.geom.Path2D sp = new java.awt.geom.Path2D.Double();
                    sp.moveTo(8, 1.5);
                    sp.lineTo(13.5, 3.8);
                    sp.lineTo(13.5, 8.2);
                    sp.curveTo(13.5, 11.5, 10.8, 13.5, 8, 14.5);
                    sp.curveTo(5.2, 13.5, 2.5, 11.5, 2.5, 8.2);
                    sp.lineTo(2.5, 3.8);
                    sp.closePath();
                    g2.draw(sp);
                    break;
                case BELL:
                    g2.drawArc(4, 2, 8, 9, 0, 180);
                    g2.drawLine(4, 6, 3, 11);
                    g2.drawLine(12, 6, 13, 11);
                    g2.drawLine(2, 11, 14, 11);
                    g2.draw(new java.awt.geom.Arc2D.Double(6.5, 12, 3, 2, 180, 180, java.awt.geom.Arc2D.OPEN));
                    break;
                case SEARCH:
                    g2.drawOval(2, 2, 9, 9);
                    g2.drawLine(10, 10, 14, 14);
                    break;
                case PLUS:
                    g2.drawLine(8, 3, 8, 13);
                    g2.drawLine(3, 8, 13, 8);
                    break;
                case REFRESH:
                    g2.draw(new java.awt.geom.Arc2D.Double(2.5, 2.5, 11, 11, 35, 290, java.awt.geom.Arc2D.OPEN));
                    g2.draw(new java.awt.geom.Line2D.Double(13.5, 2.5, 13.5, 6));
                    g2.draw(new java.awt.geom.Line2D.Double(10, 6, 13.5, 6));
                    break;
                case EDIT:
                    g2.draw(new java.awt.geom.Line2D.Double(10.5, 2.5, 13.5, 5.5));
                    g2.draw(new java.awt.geom.Line2D.Double(4, 12, 13.5, 2.5));
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 13.5, 4, 12));
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 13.5, 6, 13));
                    break;
                case TRASH:
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 4, 13.5, 4));
                    g2.drawLine(6, 2, 10, 2);
                    g2.drawRect(4, 4, 8, 10);
                    g2.draw(new java.awt.geom.Line2D.Double(6.5, 7, 6.5, 11));
                    g2.draw(new java.awt.geom.Line2D.Double(9.5, 7, 9.5, 11));
                    break;
                case DOWNLOAD:
                    g2.drawLine(8, 2, 8, 10);
                    g2.draw(new java.awt.geom.Line2D.Double(5, 7.5, 8, 10.5));
                    g2.draw(new java.awt.geom.Line2D.Double(11, 7.5, 8, 10.5));
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 13.5, 13.5, 13.5));
                    break;
                case LOGOUT:
                    g2.draw(new java.awt.geom.Line2D.Double(6, 2.5, 2.5, 2.5));
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 2.5, 2.5, 13.5));
                    g2.draw(new java.awt.geom.Line2D.Double(2.5, 13.5, 6, 13.5));
                    g2.draw(new java.awt.geom.Line2D.Double(6.5, 8, 13.5, 8));
                    g2.draw(new java.awt.geom.Line2D.Double(10.5, 5, 13.5, 8));
                    g2.draw(new java.awt.geom.Line2D.Double(10.5, 11, 13.5, 8));
                    break;
                case SUN:
                    g2.drawOval(5, 5, 6, 6);
                    g2.drawLine(8, 1, 8, 3);
                    g2.drawLine(8, 13, 8, 15);
                    g2.drawLine(1, 8, 3, 8);
                    g2.drawLine(13, 8, 15, 8);
                    break;
                case MOON:
                    g2.drawArc(3, 2, 10, 12, 70, 210);
                    g2.drawArc(6, 3, 7, 10, 100, 150);
                    break;
                case CHECK:
                    g2.draw(new java.awt.geom.Line2D.Double(3, 8.5, 6.5, 12));
                    g2.draw(new java.awt.geom.Line2D.Double(6.5, 12, 13, 4.5));
                    break;
            }
            g2.dispose();
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            final String st = value != null ? value.toString() : "";
            final boolean isAct = "Active".equalsIgnoreCase(st);
            final boolean isExpSoon = st.toLowerCase(Locale.ROOT).contains("expiring");
            JLabel lbl = new JLabel(isAct ? "Active" : (isExpSoon ? "Expiring" : "Expired"), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? TABLE_SELECT : (row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW));
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    Color pillBg;
                    if (isAct) {
                        pillBg = darkMode ? new Color(16, 185, 129, 36) : new Color(236, 253, 245);
                    } else if (isExpSoon) {
                        pillBg = darkMode ? new Color(245, 158, 11, 38) : new Color(254, 243, 199);
                    } else {
                        pillBg = darkMode ? new Color(244, 63, 94, 38) : new Color(255, 241, 242);
                    }
                    g2.setColor(pillBg);
                    g2.fillRoundRect(10, 7, Math.max(0, getWidth() - 20), Math.max(0, getHeight() - 14), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(isAct ? NEON_EMERALD : (isExpSoon ? AMBER_GOLD : CORAL_RED));
            return lbl;
        }
    }

    private static class MachineBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            final String text = value != null ? value.toString() : "";
            final boolean isMachine = text.toLowerCase(Locale.ROOT).contains("machine") || text.startsWith("⚙");
            String clean = text.replace("⚙ ", "").replace("✓ ", "");
            JLabel lbl = new JLabel(clean, SwingConstants.LEFT) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? TABLE_SELECT : (row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    Color bg = isMachine
                            ? (darkMode ? new Color(56, 139, 253, 30) : new Color(239, 246, 255))
                            : (darkMode ? new Color(16, 185, 129, 30) : new Color(236, 253, 245));
                    g2.setColor(bg);
                    g2.fillRoundRect(6, 7, Math.max(0, getWidth() - 12), Math.max(0, getHeight() - 14), 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setBorder(new EmptyBorder(0, 14, 0, 12));
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(isMachine ? ELECTRIC_INDIGO : NEON_EMERALD);
            return lbl;
        }
    }

    /**
     * Real Swing-component Member Inspector Panel with embedded quick actions.
     */
    private class MemberInspectorPanel extends JPanel {
        private final JPanel avatarCircle;
        private final JLabel lblInitials = new JLabel("--", SwingConstants.CENTER);
        private final JLabel lblName = new JLabel("Select a Member");
        private final JLabel lblMeta = new JLabel("Click any row to inspect");
        private final JLabel lblStatusPill = new JLabel("NO SELECTION", SwingConstants.CENTER);
        private final JLabel lblCycleDays = new JLabel("--");
        private final JProgressBar cycleBar = new JProgressBar(0, 30);

        private final JLabel valLevel = new JLabel("-");
        private final JLabel valGender = new JLabel("-");
        private final JLabel valContact = new JLabel("-");
        private final JLabel valEmergency = new JLabel("-");
        private final JLabel valPeriod = new JLabel("-");
        private final JLabel valNextRenewal = new JLabel("-");

        private final JButton btnRenew;
        private final JButton btnEdit;
        private final JButton btnDelete;
        private User activeMember;

        MemberInspectorPanel(boolean allowDelete, java.util.function.Consumer<User> onRenew,
                             java.util.function.Consumer<User> onEdit, java.util.function.Consumer<User> onDelete) {
            setLayout(new BorderLayout(0, 14));
            setBackground(CARD_SURFACE);
            setBorder(new EmptyBorder(18, 18, 18, 18));
            setPreferredSize(new Dimension(340, 0));
            putClientProperty("FlatLaf.style",
                    "background: " + hex(CARD_SURFACE) + "; border: 1,1,1,1," + hex(BORDER_SUBTLE) + ",1,10");

            // Header: Avatar + Name + Status
            JPanel header = new JPanel(new BorderLayout(12, 8));
            header.setOpaque(false);

            avatarCircle = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            avatarCircle.setOpaque(false);
            avatarCircle.setPreferredSize(new Dimension(46, 46));
            avatarCircle.setBackground(CARD_HIGHLIGHT);
            lblInitials.setFont(new Font("SansSerif", Font.BOLD, 16));
            lblInitials.setForeground(TEXT_PRIMARY);
            avatarCircle.add(lblInitials, BorderLayout.CENTER);

            JPanel nameStack = new JPanel(new GridLayout(2, 1, 0, 2));
            nameStack.setOpaque(false);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 16));
            lblName.setForeground(TEXT_PRIMARY);
            lblMeta.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblMeta.setForeground(TEXT_MUTED);
            nameStack.add(lblName);
            nameStack.add(lblMeta);

            JPanel topIdentity = new JPanel(new BorderLayout(12, 0));
            topIdentity.setOpaque(false);
            topIdentity.add(avatarCircle, BorderLayout.WEST);
            topIdentity.add(nameStack, BorderLayout.CENTER);

            lblStatusPill.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblStatusPill.setOpaque(true);
            lblStatusPill.setBorder(new EmptyBorder(5, 10, 5, 10));

            header.add(topIdentity, BorderLayout.NORTH);
            header.add(lblStatusPill, BorderLayout.SOUTH);

            // Body: Cycle Progress + Hairline Metadata
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            JPanel cycleHead = new JPanel(new BorderLayout());
            cycleHead.setOpaque(false);
            cycleHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            JLabel lblCycleTitle = new JLabel("MEMBERSHIP CYCLE");
            lblCycleTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblCycleTitle.setForeground(TEXT_MUTED);
            lblCycleDays.setFont(new Font("SansSerif", Font.BOLD, 11));
            cycleHead.add(lblCycleTitle, BorderLayout.WEST);
            cycleHead.add(lblCycleDays, BorderLayout.EAST);

            cycleBar.setStringPainted(false);
            cycleBar.setPreferredSize(new Dimension(0, 8));
            cycleBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));

            body.add(cycleHead);
            body.add(Box.createVerticalStrut(6));
            body.add(cycleBar);
            body.add(Box.createVerticalStrut(14));
            body.add(makeTwoColMeta("FITNESS LEVEL", valLevel, "GENDER", valGender));
            body.add(makeDivider());
            body.add(makeSingleMeta("CONTACT & ADDRESS", valContact));
            body.add(makeDivider());
            body.add(makeSingleMeta("EMERGENCY CONTACT", valEmergency));
            body.add(makeDivider());
            body.add(makeSingleMeta("CURRENT 1-MONTH PERIOD", valPeriod));
            body.add(makeDivider());
            body.add(makeSingleMeta("NEXT RENEWAL WINDOW", valNextRenewal));

            // Embedded Quick Actions Footer
            JPanel footer = new JPanel(new GridLayout(allowDelete ? 2 : 1, 1, 0, 8));
            footer.setOpaque(false);
            JPanel primaryRow = new JPanel(new GridLayout(1, 2, 8, 0));
            primaryRow.setOpaque(false);

            btnRenew = createPrimaryButton("Renew +1 Mo", VectorIcon.Kind.REFRESH);
            btnEdit = createSecondaryButton("Edit Profile", VectorIcon.Kind.EDIT, TEXT_SECONDARY);
            btnDelete = createDangerButton("Delete Member", VectorIcon.Kind.TRASH);

            btnRenew.addActionListener(e -> { if (activeMember != null) onRenew.accept(activeMember); });
            btnEdit.addActionListener(e -> { if (activeMember != null) onEdit.accept(activeMember); });
            btnDelete.addActionListener(e -> { if (activeMember != null) onDelete.accept(activeMember); });

            primaryRow.add(btnRenew);
            primaryRow.add(btnEdit);
            footer.add(primaryRow);
            if (allowDelete) {
                footer.add(btnDelete);
            }

            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
            setMemberData(null, null);
        }

        private JPanel makeDivider() {
            JPanel d = new JPanel();
            d.setBackground(BORDER_SUBTLE);
            d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            d.setPreferredSize(new Dimension(0, 1));
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(8, 0, 8, 0));
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
            wrap.add(d, BorderLayout.CENTER);
            return wrap;
        }

        private JPanel makeSingleMeta(String title, JLabel val) {
            JPanel p = new JPanel(new BorderLayout(0, 2));
            p.setOpaque(false);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            JLabel t = new JLabel(title);
            t.setFont(new Font("SansSerif", Font.BOLD, 10));
            t.setForeground(TEXT_MUTED);
            val.setFont(new Font("SansSerif", Font.PLAIN, 13));
            val.setForeground(TEXT_PRIMARY);
            p.add(t, BorderLayout.NORTH);
            p.add(val, BorderLayout.CENTER);
            return p;
        }

        private JPanel makeTwoColMeta(String t1, JLabel v1, String t2, JLabel v2) {
            JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            row.add(makeSingleMeta(t1, v1));
            row.add(makeSingleMeta(t2, v2));
            return row;
        }

        void setMemberData(User member, Membership latest) {
            this.activeMember = member;
            boolean hasMember = member != null;
            btnRenew.setEnabled(hasMember);
            btnEdit.setEnabled(hasMember);
            btnDelete.setEnabled(hasMember);

            if (!hasMember) {
                lblInitials.setText("--");
                avatarCircle.setBackground(CARD_HIGHLIGHT);
                lblName.setText("No Member Selected");
                lblMeta.setText("Select a row in the directory");
                lblStatusPill.setText("READY");
                lblStatusPill.setBackground(CARD_HIGHLIGHT);
                lblStatusPill.setForeground(TEXT_MUTED);
                lblCycleDays.setText("--");
                lblCycleDays.setForeground(TEXT_MUTED);
                cycleBar.setValue(0);
                valLevel.setText("-");
                valGender.setText("-");
                valContact.setText("-");
                valEmergency.setText("-");
                valPeriod.setText("-");
                valNextRenewal.setText("-");
                return;
            }

            LocalDate today = LocalDate.now();
            String status = latest != null ? latest.getComputedStatus(today) : "Expired";
            boolean isActive = "Active".equalsIgnoreCase(status);
            long daysLeft = latest != null ? latest.getDaysRemaining(today) : -999;

            lblInitials.setText(member.getInitials());
            lblInitials.setForeground(isActive ? NEON_EMERALD : CORAL_RED);
            avatarCircle.setBackground(isActive
                    ? (darkMode ? new Color(16, 185, 129, 40) : new Color(236, 253, 245))
                    : (darkMode ? new Color(244, 63, 94, 40) : new Color(255, 241, 242)));
            lblName.setText(member.getFullName());
            lblMeta.setText("@" + member.username + "  •  Member #" + member.userId);

            lblStatusPill.setText(isActive ? "ACTIVE PASS — ENTRY CLEARED" : "EXPIRED PASS — RENEWAL NEEDED");
            lblStatusPill.setForeground(isActive ? NEON_EMERALD : CORAL_RED);
            lblStatusPill.setBackground(isActive
                    ? (darkMode ? new Color(16, 185, 129, 32) : new Color(236, 253, 245))
                    : (darkMode ? new Color(244, 63, 94, 32) : new Color(255, 241, 242)));

            lblCycleDays.setText(latest != null
                    ? (daysLeft >= 0 ? daysLeft + " of 30 days left" : Math.abs(daysLeft) + " days overdue")
                    : "No Record");
            lblCycleDays.setForeground(isActive ? NEON_EMERALD : CORAL_RED);
            cycleBar.setValue(isActive ? (int) Math.max(2, Math.min(30, daysLeft)) : 30);
            cycleBar.setForeground(isActive ? NEON_EMERALD : CORAL_RED);

            valLevel.setText(member.fitnessLevel != null ? member.fitnessLevel : "Beginner");
            valGender.setText(member.gender != null ? member.gender : "Unspecified");
            valContact.setText(member.contactNo + ((member.address != null && !member.address.isEmpty()) ? (" • " + member.address) : ""));
            valEmergency.setText((member.emergencyContactName != null ? member.emergencyContactName : "N/A") +
                    " (" + (member.emergencyContactNo != null ? member.emergencyContactNo : "N/A") + ")");
            valPeriod.setText(latest != null ? (latest.startDate + "  ->  " + latest.endDate) : "No active period");
            LocalDate[] nextR = BusinessRules.calculateRenewalDates(latest != null ? latest.endDate : null, today);
            valNextRenewal.setText(nextR[0] + "  ->  " + nextR[1]);
        }
    }

    /**
     * Animated Workout Demonstration & Target Muscle Anatomy Card (Light & Dark aware).
     */
    private class WorkoutVisualCard extends JPanel {
        private Exercise exercise;
        private int animTick = 0;
        private final javax.swing.Timer animTimer;
        private ImageIcon loadedGifIcon;
        private String loadedSourceLabel = "";

        public WorkoutVisualCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 0));
            setLayout(new BorderLayout());

            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
            bottomBar.setOpaque(false);
            JButton btnLocalGif = createSecondaryButton("Local GIF / File", VectorIcon.Kind.DOWNLOAD, TEXT_SECONDARY);
            JButton btnUrlGif   = createSecondaryButton("Sync GIF URL", VectorIcon.Kind.REFRESH, TEXT_SECONDARY);
            btnLocalGif.addActionListener(e -> {
                if (exercise == null) return;
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select Animated Workout GIF or PNG Image");
                if (fc.showOpenDialog(gymsyncapp.this) == JFileChooser.APPROVE_OPTION) {
                    exercise.customGifOrImagePath = fc.getSelectedFile().getAbsolutePath();
                    loadGifForExercise(exercise);
                }
            });
            btnUrlGif.addActionListener(e -> {
                if (exercise == null) return;
                String urlStr = JOptionPane.showInputDialog(gymsyncapp.this,
                        "Paste direct GIF/Image URL to download and cache offline for '" + exercise.name + "':",
                        "Sync Online GIF to Offline Cache", JOptionPane.QUESTION_MESSAGE);
                if (urlStr == null || urlStr.trim().isEmpty()) return;
                final String trimmedUrl = urlStr.trim();
                new SwingWorker<java.io.File, Void>() {
                    @Override
                    protected java.io.File doInBackground() throws Exception {
                        java.io.File imgDir = new java.io.File("images");
                        if (!imgDir.exists()) imgDir.mkdirs();
                        java.io.File dest = new java.io.File(imgDir, exercise.getDefaultGifFilename());
                        try (java.io.InputStream in = java.net.URI.create(trimmedUrl).toURL().openStream()) {
                            java.nio.file.Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        return dest;
                    }
                    @Override
                    protected void done() {
                        try {
                            java.io.File dest = get();
                            exercise.customGifOrImagePath = dest.getAbsolutePath();
                            loadGifForExercise(exercise);
                            JOptionPane.showMessageDialog(gymsyncapp.this,
                                    "Cached offline to:\n" + dest.getPath(),
                                    "GIF Synced", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            JOptionPane.showMessageDialog(gymsyncapp.this,
                                    "Could not download URL (" + cause.getMessage() + "). Using built-in motion preview.",
                                    "Offline Fallback", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }.execute();
            });
            bottomBar.add(btnLocalGif);
            bottomBar.add(btnUrlGif);
            add(bottomBar, BorderLayout.SOUTH);

            animTimer = new javax.swing.Timer(45, e -> {
                animTick = (animTick + 1) % 360;
                repaint();
            });
            animTimer.start();
        }

        public void setExercise(Exercise ex) {
            this.exercise = ex;
            loadGifForExercise(ex);
            repaint();
        }

        private void loadGifForExercise(Exercise ex) {
            loadedGifIcon = null;
            loadedSourceLabel = "";
            if (ex == null) return;
            java.util.List<java.io.File> candidates = new ArrayList<>();
            if (ex.customGifOrImagePath != null && !ex.customGifOrImagePath.isEmpty()) {
                candidates.add(new java.io.File(ex.customGifOrImagePath));
            }
            candidates.add(new java.io.File("images", ex.getDefaultGifFilename()));
            for (java.io.File f : candidates) {
                if (f.exists() && f.isFile() && f.length() > 0) {
                    ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                    if (icon.getIconWidth() > 0) {
                        loadedGifIcon = icon;
                        loadedSourceLabel = f.getName();
                        return;
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(CARD_SURFACE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            if (exercise == null) {
                g2.setColor(NEON_EMERALD);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString("WORKOUT & MUSCLE TARGET PREVIEW", 20, 34);
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.drawString("Select any exercise row to preview", 20, 66);
                g2.drawString("muscle targeting, form cues, and", 20, 86);
                g2.drawString("machine requirements.", 20, 106);
                g2.dispose();
                return;
            }

            boolean needsMachine = exercise.requiresMachine();
            Color headerAccent = needsMachine ? ELECTRIC_INDIGO : NEON_EMERALD;

            g2.setColor(darkMode ? BG_OBSIDIAN : new Color(248, 250, 252));
            g2.fillRoundRect(12, 12, w - 24, 78, 10, 10);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(12, 12, w - 24, 78, 10, 10);

            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            String title = exercise.name.length() > 32 ? exercise.name.substring(0, 29) + "..." : exercise.name;
            g2.drawString(title, 22, 38);

            String machTag = needsMachine
                    ? "MACHINE: " + exercise.equipment.replace("Machine: ", "")
                    : "NO MACHINE (" + exercise.equipment.toUpperCase(Locale.ROOT) + ")";
            g2.setColor(headerAccent);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(machTag.length() > 44 ? machTag.substring(0, 41) + "..." : machTag, 22, 62);
            g2.setColor(TEXT_MUTED);
            g2.drawString(exercise.muscleGroup + "  •  " + exercise.difficulty, 22, 79);

            int canvasY = 102;
            int canvasH = 215;
            g2.setColor(darkMode ? new Color(13, 17, 23) : new Color(248, 250, 252));
            g2.fillRoundRect(12, canvasY, w - 24, canvasH, 10, 10);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(12, canvasY, w - 24, canvasH, 10, 10);

            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("MUSCLE MAP", 24, canvasY + 18);
            g2.drawString("FORM MOTION", w / 2 + 8, canvasY + 18);
            g2.setColor(BORDER_SUBTLE);
            g2.drawLine(w / 2, canvasY + 10, w / 2, canvasY + canvasH - 10);

            drawMuscleAnatomyDiagram(g2, 24, canvasY + 26, (w / 2) - 34, canvasH - 36, exercise.muscleGroup);

            if (loadedGifIcon != null && loadedGifIcon.getIconWidth() > 0) {
                int imgX = w / 2 + 12;
                int imgY = canvasY + 26;
                int imgW = (w / 2) - 32;
                int imgH = canvasH - 52;
                g2.drawImage(loadedGifIcon.getImage(), imgX, imgY, imgW, imgH, this);
                g2.setColor(NEON_EMERALD);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString(loadedSourceLabel, imgX, canvasY + canvasH - 10);
            } else {
                drawLiveExerciseAnimation(g2, w / 2 + 10, canvasY + 26, (w / 2) - 28, canvasH - 36, exercise.name);
            }

            int infoY = canvasY + canvasH + 12;
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("EXECUTION CUES", 16, infoY + 12);
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            drawWrappedText(g2, exercise.description, 16, infoY + 30, w - 32, 17);
            g2.dispose();
        }

        private void drawMuscleAnatomyDiagram(Graphics2D g2, int x, int y, int w, int h, String muscleGroup) {
            int cx = x + w / 2;
            int top = y + 10;
            double pulse = 0.55 + 0.45 * Math.sin(animTick * 0.14);
            Color glow = new Color(CORAL_RED.getRed(), CORAL_RED.getGreen(), CORAL_RED.getBlue(), (int) (130 + 115 * pulse));
            Color baseBody = darkMode ? new Color(56, 66, 82) : new Color(203, 213, 225);

            g2.setColor(baseBody);
            g2.fillOval(cx - 10, top, 20, 20);
            g2.fillRoundRect(cx - 17, top + 24, 34, 48, 12, 12);
            g2.fillRoundRect(cx - 28, top + 26, 9, 44, 6, 6);
            g2.fillRoundRect(cx + 19, top + 26, 9, 44, 6, 6);
            g2.fillRoundRect(cx - 15, top + 76, 12, 54, 8, 8);
            g2.fillRoundRect(cx + 3, top + 76, 12, 54, 8, 8);

            String mg = muscleGroup != null ? muscleGroup.toLowerCase(Locale.ROOT) : "";
            g2.setColor(glow);
            if (mg.contains("chest")) {
                g2.fillRoundRect(cx - 15, top + 27, 13, 15, 5, 5);
                g2.fillRoundRect(cx + 2, top + 27, 13, 15, 5, 5);
            } else if (mg.contains("back")) {
                g2.fillPolygon(new int[]{cx - 17, cx + 17, cx + 9, cx - 9},
                        new int[]{top + 26, top + 26, top + 58, top + 58}, 4);
            } else if (mg.contains("shoulder")) {
                g2.fillOval(cx - 27, top + 22, 14, 14);
                g2.fillOval(cx + 13, top + 22, 14, 14);
            } else if (mg.contains("arm")) {
                g2.fillRoundRect(cx - 28, top + 30, 10, 24, 6, 6);
                g2.fillRoundRect(cx + 18, top + 30, 10, 24, 6, 6);
            } else if (mg.contains("leg")) {
                g2.fillRoundRect(cx - 16, top + 76, 14, 38, 8, 8);
                g2.fillRoundRect(cx + 2, top + 76, 14, 38, 8, 8);
            } else if (mg.contains("core")) {
                g2.fillRoundRect(cx - 9, top + 40, 18, 28, 6, 6);
            } else {
                g2.fillRoundRect(cx - 14, top + 27, 28, 42, 10, 10);
                g2.fillRoundRect(cx - 14, top + 78, 11, 34, 6, 6);
                g2.fillRoundRect(cx + 3, top + 78, 11, 34, 6, 6);
            }
            g2.setColor(CORAL_RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String tag = "TARGET: " + (muscleGroup != null ? muscleGroup.toUpperCase(Locale.ROOT) : "BODY");
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tag, cx - fm.stringWidth(tag) / 2, top + 148);
        }

        private void drawLiveExerciseAnimation(Graphics2D g2, int x, int y, int w, int h, String exName) {
            int cx = x + w / 2;
            int my = y + h / 2 - 14;
            double phase = 0.5 * (1.0 - Math.cos(animTick * 0.11));
            int off = (int) (phase * 22);

            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            String n = exName != null ? exName.toLowerCase(Locale.ROOT) : "";
            if (n.contains("squat") || n.contains("leg") || n.contains("lunge") || n.contains("calf")) {
                int drop = (int) (phase * 22);
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 9, my - 36 + drop, 18, 18);
                g2.setColor(NEON_EMERALD);
                g2.drawLine(cx, my - 18 + drop, cx, my + 12 + drop);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx, my + 12 + drop, cx - 18, my + 24 + drop / 2);
                g2.drawLine(cx - 18, my + 24 + drop / 2, cx - 14, my + 48);
                g2.drawLine(cx, my + 12 + drop, cx + 18, my + 24 + drop / 2);
                g2.drawLine(cx + 18, my + 24 + drop / 2, cx + 14, my + 48);
            } else if (n.contains("pulldown") || n.contains("row") || n.contains("pull") || n.contains("dip")) {
                int hy = my - 36 + off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 9, my - 30, 18, 18);
                g2.setColor(ELECTRIC_INDIGO);
                g2.fillRoundRect(cx - 12, my - 12, 24, 30, 6, 6);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx - 12, my - 6, cx - 22, hy);
                g2.drawLine(cx + 12, my - 6, cx + 22, hy);
                g2.setColor(NEON_EMERALD);
                g2.drawLine(cx - 28, hy, cx + 28, hy);
            } else {
                int by = my - 12 - off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 9, my - 14, 18, 18);
                g2.setColor(CORAL_RED);
                g2.fillRoundRect(cx - 14, my + 6, 28, 32, 6, 6);
                g2.drawLine(cx - 14, my + 12, cx - 26, by);
                g2.drawLine(cx + 14, my + 12, cx + 26, by);
                g2.setColor(ELECTRIC_INDIGO);
                g2.drawLine(cx - 34, by, cx + 34, by);
                g2.setColor(NEON_EMERALD);
                g2.fillRoundRect(cx - 38, by - 7, 6, 14, 3, 3);
                g2.fillRoundRect(cx + 32, by - 7, 6, 14, 3, 3);
            }

            g2.setColor(BORDER_SUBTLE);
            g2.fillRoundRect(cx - 46, my + 58, 92, 8, 6, 6);
            g2.setColor(NEON_EMERALD);
            g2.fillRoundRect(cx - 46, my + 58, Math.max(10, (int) (92 * (1.0 - phase))), 8, 6, 6);
        }

        private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxW, int lineH) {
            if (text == null) return;
            FontMetrics fm = g2.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int curY = y;
            for (String w : words) {
                String test = line.length() == 0 ? w : line + " " + w;
                if (fm.stringWidth(test) > maxW) {
                    g2.drawString(line.toString(), x, curY);
                    line = new StringBuilder(w);
                    curY += lineH;
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (line.length() > 0) g2.drawString(line.toString(), x, curY);
        }
    }

    // ========================================================================
    // 5. SPLIT-SCREEN LOGIN & SIDEBAR WORKSPACE SHELL
    // ========================================================================

    private void showLoginScreen() {
        currentUser = null;
        rootContainer.removeAll();
        rootContainer.add(buildLoginPanel(), "LOGIN");
        rootCardLayout.show(rootContainer, "LOGIN");
        rootContainer.revalidate();
        rootContainer.repaint();
    }

    private void routeToRoleDashboard(User user) {
        if ("Admin".equalsIgnoreCase(user.role)) {
            this.currentUser = user;
            rootContainer.removeAll();
            rootContainer.add(buildAdminDashboard(), "DASHBOARD");
        } else if ("Staff".equalsIgnoreCase(user.role)) {
            this.currentUser = user;
            rootContainer.removeAll();
            rootContainer.add(buildStaffDashboard(), "DASHBOARD");
        } else {
            JOptionPane.showMessageDialog(this,
                    "Desk Management Access Only.\nMember accounts are managed by Front Desk Staff — please sign in as Admin or Staff.",
                    "Operator Access Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        rootCardLayout.show(rootContainer, "DASHBOARD");
        rootContainer.revalidate();
        rootContainer.repaint();
    }

    private JPanel buildLoginPanel() {
        JPanel split = new JPanel(new GridLayout(1, 2));
        split.setBackground(BG_OBSIDIAN);

        // Left Editorial Showcase Panel (Clean Deep Slate)
        JPanel leftHero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(15, 23, 42));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(30, 41, 59));
                g2.drawLine(0, getHeight() - 120, getWidth(), getHeight() - 220);
                g2.drawLine(0, getHeight() - 80, getWidth(), getHeight() - 180);
                g2.dispose();
            }
        };
        leftHero.setBorder(new EmptyBorder(48, 52, 42, 52));

        JPanel brandHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandHeader.setOpaque(false);
        JLabel markLbl = new JLabel(new VectorIcon(VectorIcon.Kind.DUMBBELL, 22, new Color(16, 185, 129)));
        JLabel brandName = new JLabel("GYMSYNC");
        brandName.setFont(new Font("SansSerif", Font.BOLD, 20));
        brandName.setForeground(Color.WHITE);
        brandHeader.add(markLbl);
        brandHeader.add(brandName);

        JPanel heroCopy = new JPanel();
        heroCopy.setOpaque(false);
        heroCopy.setLayout(new BoxLayout(heroCopy, BoxLayout.Y_AXIS));
        heroCopy.setBorder(new EmptyBorder(64, 0, 0, 0));

        JLabel headline = new JLabel("<html>Front Desk &amp; Gym<br>Operations Console</html>");
        headline.setFont(new Font("SansSerif", Font.BOLD, 34));
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lead = new JLabel("<html>Manage member passes, 1-month renewals, expiry alerts, and equipment-aware workout plans from one unified desk console.</html>");
        lead.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lead.setForeground(new Color(148, 163, 184));
        lead.setAlignmentX(Component.LEFT_ALIGNMENT);

        heroCopy.add(headline);
        heroCopy.add(Box.createVerticalStrut(14));
        heroCopy.add(lead);
        heroCopy.add(Box.createVerticalStrut(32));
        heroCopy.add(makeHeroBullet("Instant Active / Expired pass check at front desk"));
        heroCopy.add(Box.createVerticalStrut(12));
        heroCopy.add(makeHeroBullet("Automated 1-month renewal date calculations"));
        heroCopy.add(Box.createVerticalStrut(12));
        heroCopy.add(makeHeroBullet("Rule-based & custom workout planning with muscle map"));

        JLabel heroFoot = new JLabel("DIT 2-3 Group 3  •  Java Swing + MySQL Case Study");
        heroFoot.setFont(new Font("SansSerif", Font.PLAIN, 12));
        heroFoot.setForeground(new Color(100, 116, 139));

        leftHero.add(brandHeader, BorderLayout.NORTH);
        leftHero.add(heroCopy, BorderLayout.CENTER);
        leftHero.add(heroFoot, BorderLayout.SOUTH);

        // Right Clean Sign-In Panel
        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setBackground(BG_SURFACE);

        JPanel formBox = new JPanel();
        formBox.setOpaque(false);
        formBox.setLayout(new BoxLayout(formBox, BoxLayout.Y_AXIS));
        formBox.setPreferredSize(new Dimension(380, 440));

        JPanel topControls = new JPanel(new BorderLayout());
        topControls.setOpaque(false);
        topControls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel roleChip = new JLabel("OPERATOR SIGN IN");
        roleChip.setFont(new Font("SansSerif", Font.BOLD, 11));
        roleChip.setForeground(NEON_EMERALD);
        JButton btnTheme = createSecondaryButton(darkMode ? "Light Mode" : "Dark Mode",
                darkMode ? VectorIcon.Kind.SUN : VectorIcon.Kind.MOON, TEXT_SECONDARY);
        btnTheme.addActionListener(e -> toggleTheme());
        topControls.add(roleChip, BorderLayout.WEST);
        topControls.add(btnTheme, BorderLayout.EAST);

        JLabel signInTitle = new JLabel("Welcome back");
        signInTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        signInTitle.setForeground(TEXT_PRIMARY);
        signInTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel signInSub = new JLabel("Sign in with your Admin or Front Desk Staff account.");
        signInSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        signInSub.setForeground(TEXT_MUTED);
        signInSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        userLbl.setForeground(TEXT_PRIMARY);
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField userField = createStyledTextField("");
        userField.putClientProperty("JTextField.placeholderText", "e.g. admin or staff");
        userField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        passLbl.setForeground(TEXT_PRIMARY);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPasswordField passField = createStyledPasswordField();
        passField.putClientProperty("JTextField.placeholderText", "Enter password");
        passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginBtn = createPrimaryButton("Sign in to Console", null);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        ActionListener doLogin = e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                        "Missing Credentials", JOptionPane.WARNING_MESSAGE);
                return;
            }
            User auth = repo.authenticate(u, p);
            if (auth == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Sign-In Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            routeToRoleDashboard(auth);
        };
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);

        JLabel quickLbl = new JLabel("QUICK DEMO OPERATOR ACCESS");
        quickLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        quickLbl.setForeground(TEXT_MUTED);
        quickLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel demoBtns = new JPanel(new GridLayout(1, 2, 10, 0));
        demoBtns.setOpaque(false);
        demoBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        demoBtns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnAdminDemo = createSecondaryButton("Admin Console", VectorIcon.Kind.SHIELD, TEXT_SECONDARY);
        JButton btnStaffDemo = createSecondaryButton("Front Desk Staff", VectorIcon.Kind.USERS, NEON_EMERALD);
        btnAdminDemo.addActionListener(e -> { userField.setText("admin"); passField.setText("admin123"); doLogin.actionPerformed(null); });
        btnStaffDemo.addActionListener(e -> { userField.setText("staff"); passField.setText("staff123"); doLogin.actionPerformed(null); });
        demoBtns.add(btnAdminDemo);
        demoBtns.add(btnStaffDemo);

        formBox.add(topControls);
        formBox.add(Box.createVerticalStrut(18));
        formBox.add(signInTitle);
        formBox.add(Box.createVerticalStrut(4));
        formBox.add(signInSub);
        formBox.add(Box.createVerticalStrut(24));
        formBox.add(userLbl);
        formBox.add(Box.createVerticalStrut(6));
        formBox.add(userField);
        formBox.add(Box.createVerticalStrut(14));
        formBox.add(passLbl);
        formBox.add(Box.createVerticalStrut(6));
        formBox.add(passField);
        formBox.add(Box.createVerticalStrut(20));
        formBox.add(loginBtn);
        formBox.add(Box.createVerticalStrut(24));
        formBox.add(quickLbl);
        formBox.add(Box.createVerticalStrut(8));
        formBox.add(demoBtns);

        rightWrap.add(formBox);

        split.add(leftHero);
        split.add(rightWrap);
        return split;
    }

    private JPanel makeHeroBullet(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel ico = new JLabel(new VectorIcon(VectorIcon.Kind.CHECK, 15, new Color(16, 185, 129)));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(new Color(226, 232, 240));
        p.add(ico);
        p.add(lbl);
        return p;
    }

    private JPanel buildAppShell(String consoleSubtitle, LinkedHashMap<String, JPanel> navScreens) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG_OBSIDIAN);

        CardLayout contentLayout = new CardLayout();
        JPanel contentArea = new JPanel(contentLayout);
        contentArea.setOpaque(false);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SURFACE);
        sidebar.setPreferredSize(new Dimension(232, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_SUBTLE));

        // Sidebar Brand Header
        JPanel brandPanel = new JPanel(new BorderLayout(0, 4));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_SUBTLE),
                new EmptyBorder(16, 18, 14, 18)
        ));
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        JLabel logoIcon = new JLabel(new VectorIcon(VectorIcon.Kind.DUMBBELL, 18, NEON_EMERALD));
        JLabel logoTitle = new JLabel("GYMSYNC");
        logoTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        logoTitle.setForeground(TEXT_PRIMARY);
        logoRow.add(logoIcon);
        logoRow.add(logoTitle);

        JLabel subTitleLbl = new JLabel(consoleSubtitle);
        subTitleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subTitleLbl.setForeground(TEXT_MUTED);
        subTitleLbl.setBorder(new EmptyBorder(2, 2, 0, 0));
        brandPanel.add(logoRow, BorderLayout.NORTH);
        brandPanel.add(subTitleLbl, BorderLayout.SOUTH);

        // Sidebar Nav List
        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.setOpaque(false);
        navWrap.setBorder(new EmptyBorder(14, 12, 14, 12));
        JLabel navHdr = new JLabel("OPERATIONS");
        navHdr.setFont(new Font("SansSerif", Font.BOLD, 10));
        navHdr.setForeground(TEXT_MUTED);
        navHdr.setBorder(new EmptyBorder(0, 10, 8, 10));

        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));

        String initialTitle = navScreens.isEmpty() ? consoleSubtitle : navScreens.keySet().iterator().next();
        JLabel pageTitleLbl = new JLabel(initialTitle);
        pageTitleLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        pageTitleLbl.setForeground(TEXT_PRIMARY);

        List<JButton> navButtons = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, JPanel> entry : navScreens.entrySet()) {
            String key = entry.getKey();
            contentArea.add(entry.getValue(), key);
            final int tabIdx = idx;
            JButton navBtn = new JButton(key);
            navBtn.putClientProperty("active", idx == 0);
            navBtn.setIcon(new VectorIcon(VectorIcon.forTab(key), 15, idx == 0 ? NEON_EMERALD : TEXT_MUTED));
            navBtn.setIconTextGap(10);
            navBtn.setHorizontalAlignment(SwingConstants.LEFT);
            navBtn.setFont(new Font("SansSerif", idx == 0 ? Font.BOLD : Font.PLAIN, 13));
            navBtn.setFocusPainted(false);
            navBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            navBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            navBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            applyNavBtnStyle(navBtn, idx == 0);

            navBtn.addActionListener(e -> {
                for (int i = 0; i < navButtons.size(); i++) {
                    JButton b = navButtons.get(i);
                    boolean isThis = (i == tabIdx);
                    b.putClientProperty("active", isThis);
                    b.setFont(new Font("SansSerif", isThis ? Font.BOLD : Font.PLAIN, 13));
                    b.setIcon(new VectorIcon(VectorIcon.forTab(b.getText()), 15, isThis ? NEON_EMERALD : TEXT_MUTED));
                    applyNavBtnStyle(b, isThis);
                }
                pageTitleLbl.setText(key);
                Object onTabSelected = entry.getValue().getClientProperty("onTabSelected");
                if (onTabSelected instanceof Runnable) {
                    ((Runnable) onTabSelected).run();
                }
                contentLayout.show(contentArea, key);
            });

            navButtons.add(navBtn);
            navList.add(navBtn);
            navList.add(Box.createVerticalStrut(4));
            idx++;
        }
        navWrap.add(navHdr, BorderLayout.NORTH);
        navWrap.add(navList, BorderLayout.CENTER);

        // Sidebar Footer: Theme Toggle + User + Sign Out
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_SUBTLE),
                new EmptyBorder(12, 14, 14, 14)
        ));

        JLabel userNameLbl = new JLabel(currentUser.getFullName());
        userNameLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        userNameLbl.setForeground(TEXT_PRIMARY);
        userNameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        String dbTag = repo.isJdbcConnected() ? (repo.isMySql() ? "MySQL Live" : "SQLite Live") : "Memory DB";
        JLabel userRoleLbl = new JLabel(currentUser.role.toUpperCase(Locale.ROOT) + "  •  " + dbTag);
        userRoleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        userRoleLbl.setForeground(repo.isJdbcConnected() ? NEON_EMERALD : TEXT_MUTED);
        userRoleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel footerBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        footerBtns.setOpaque(false);
        footerBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        footerBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JButton btnTheme = createSecondaryButton(darkMode ? "Light" : "Dark",
                darkMode ? VectorIcon.Kind.SUN : VectorIcon.Kind.MOON, TEXT_SECONDARY);
        btnTheme.addActionListener(e -> toggleTheme());
        JButton btnLogout = createSecondaryButton("Sign Out", VectorIcon.Kind.LOGOUT, TEXT_SECONDARY);
        btnLogout.addActionListener(e -> showLoginScreen());

        footerBtns.add(btnTheme);
        footerBtns.add(btnLogout);

        footer.add(userNameLbl);
        footer.add(Box.createVerticalStrut(2));
        footer.add(userRoleLbl);
        footer.add(Box.createVerticalStrut(10));
        footer.add(footerBtns);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navWrap, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        // Main Workspace
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(BG_OBSIDIAN);

        JPanel pageHeader = new JPanel(new BorderLayout());
        pageHeader.setBackground(BG_SURFACE);
        pageHeader.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_SUBTLE),
                new EmptyBorder(14, 22, 14, 22)
        ));
        pageHeader.add(pageTitleLbl, BorderLayout.WEST);

        JLabel dbPill = new JLabel(dbTag);
        dbPill.setFont(new Font("SansSerif", Font.BOLD, 11));
        dbPill.setForeground(repo.isJdbcConnected() ? NEON_EMERALD : TEXT_MUTED);
        pageHeader.add(dbPill, BorderLayout.EAST);

        workspace.add(pageHeader, BorderLayout.NORTH);
        workspace.add(contentArea, BorderLayout.CENTER);

        shell.add(sidebar, BorderLayout.WEST);
        shell.add(workspace, BorderLayout.CENTER);
        return shell;
    }

    private void applyNavBtnStyle(JButton b, boolean active) {
        if (active) {
            Color activeBg = darkMode ? new Color(16, 185, 129, 28) : new Color(236, 253, 245);
            b.setForeground(darkMode ? TEXT_PRIMARY : new Color(6, 95, 70));
            b.putClientProperty("FlatLaf.style",
                    "background: " + hex(activeBg) + "; foreground: " + hex(b.getForeground()) + "; " +
                    "borderWidth: 0; arc: 8; margin: 8,12,8,12");
        } else {
            b.setForeground(TEXT_SECONDARY);
            b.putClientProperty("FlatLaf.style",
                    "background: " + hex(BG_SURFACE) + "; foreground: " + hex(TEXT_SECONDARY) + "; " +
                    "hoverBackground: " + hex(CARD_HIGHLIGHT) + "; borderWidth: 0; arc: 8; margin: 8,12,8,12");
        }
    }

    private JPanel buildStaffDashboard() {
        LinkedHashMap<String, JPanel> screens = new LinkedHashMap<>();
        screens.put("Member Check-In", buildFrontDeskMemberTab(false));
        screens.put("Expiry Alerts", buildAlertListTab());
        screens.put("Workout Coach", buildStaffWorkoutHelperTab());
        return buildAppShell("Front Desk Operator", screens);
    }

    private JPanel buildAdminDashboard() {
        LinkedHashMap<String, JPanel> screens = new LinkedHashMap<>();
        screens.put("Member Check-In & Renewals", buildFrontDeskMemberTab(true));
        screens.put("Analytics & Reports", buildReportsTab());
        screens.put("Exercise Library", buildExerciseLibraryTab());
        screens.put("Staff Accounts", buildAdminUserManagementTab());
        screens.put("Expiry Alerts", buildAlertListTab());
        return buildAppShell("Administrator Console", screens);
    }

    // ========================================================================
    // 6. FRONT DESK CHECK-IN (KPI Strip + Filter Pills + Split Master/Detail)
    // ========================================================================

    private JPanel buildFrontDeskMemberTab(boolean allowDelete) {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setBackground(BG_OBSIDIAN);
        container.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Live 4-KPI Summary Strip
        JPanel kpiStrip = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiStrip.setOpaque(false);

        // Toolbar: Search + Segmented Status Filter Pills + New Member CTA
        JPanel toolbar = createCardPanel(new BorderLayout(12, 0));
        toolbar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);
        JTextField searchField = createStyledTextField("");
        searchField.setColumns(22);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, @username, phone...");
        searchField.putClientProperty("JTextField.leadingIcon", new VectorIcon(VectorIcon.Kind.SEARCH, 15, TEXT_MUTED));
        searchField.putClientProperty("JTextField.showClearButton", true);

        ButtonGroup filterGroup = new ButtonGroup();
        JToggleButton pillAll = createFilterPill("All", true);
        JToggleButton pillActive = createFilterPill("Active", false);
        JToggleButton pillExpiring = createFilterPill("Expiring", false);
        JToggleButton pillExpired = createFilterPill("Expired", false);
        for (JToggleButton p : new JToggleButton[]{pillAll, pillActive, pillExpiring, pillExpired}) {
            filterGroup.add(p);
        }

        leftTools.add(searchField);
        leftTools.add(Box.createHorizontalStrut(4));
        leftTools.add(pillAll);
        leftTools.add(pillActive);
        leftTools.add(pillExpiring);
        leftTools.add(pillExpired);

        JButton btnRegister = createPrimaryButton("New Member", VectorIcon.Kind.PLUS);
        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(btnRegister, BorderLayout.EAST);

        JPanel northWrap = new JPanel(new BorderLayout(0, 12));
        northWrap.setOpaque(false);
        northWrap.add(kpiStrip, BorderLayout.NORTH);
        northWrap.add(toolbar, BorderLayout.SOUTH);

        // Table + Inspector Split
        String[] cols = {"ID", "Username", "Member Name", "Contact No.", "Level", "End Date", "Balance", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleModernTable(table);
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());
        int[] colWidths = {44, 100, 145, 112, 108, 96, 102, 92};
        for (int c = 0; c < colWidths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(colWidths[c]);
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        final Runnable[] refreshRef = new Runnable[1];

        java.util.function.Consumer<User> doRenewMember = target -> {
            Membership latest = repo.getLatestMembership(target.userId);
            LocalDate today = LocalDate.now();
            String currentStatus = latest != null ? latest.getComputedStatus(today) : "Expired";
            LocalDate[] renewalDates = BusinessRules.calculateRenewalDates(latest != null ? latest.endDate : null, today);
            String ruleExplanation = "Active".equalsIgnoreCase(currentStatus)
                    ? "Member is currently ACTIVE (expires " + latest.endDate + ").\nNew 1-month period starts on current end date."
                    : "Member is currently EXPIRED.\nNew 1-month period starts today (" + today + ").";
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Confirm 1-Month Renewal for " + target.getFullName() + "?\n\n" +
                    ruleExplanation + "\n\nNew Period: " + renewalDates[0] + " -> " + renewalDates[1],
                    "Confirm Monthly Renewal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repo.addMembership(target.userId, renewalDates[0], renewalDates[1]);
                refreshRef[0].run();
                JOptionPane.showMessageDialog(this,
                        "Membership renewed for " + target.getFullName() + " through " + renewalDates[1] + ".",
                        "Renewal Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        java.util.function.Consumer<User> doEditMember = target -> showMemberRegistrationDialog(target, refreshRef[0]);

        java.util.function.Consumer<User> doDeleteMember = target -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete Member #" + target.userId + " (" + target.getFullName() + ") and all memberships/plans?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repo.deleteUser(target.userId);
                refreshRef[0].run();
            }
        };

        MemberInspectorPanel inspector = new MemberInspectorPanel(allowDelete, doRenewMember, doEditMember, doDeleteMember);

        Runnable applyFilters = () -> {
            String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
            String mode = pillActive.isSelected() ? "active"
                    : pillExpiring.isSelected() ? "expiring"
                    : pillExpired.isSelected() ? "expired" : "all";
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    String status = String.valueOf(entry.getValue(7));
                    String bal = String.valueOf(entry.getValue(6));
                    if ("active".equals(mode) && !"Active".equalsIgnoreCase(status)) return false;
                    if ("expired".equals(mode) && !"Expired".equalsIgnoreCase(status)) return false;
                    if ("expiring".equals(mode)) {
                        if (!"Active".equalsIgnoreCase(status)) return false;
                        try {
                            int d = Integer.parseInt(bal.replaceAll("[^0-9]", ""));
                            if (d > 7) return false;
                        } catch (NumberFormatException ex) { return false; }
                    }
                    if (!q.isEmpty()) {
                        for (int c = 0; c < entry.getValueCount(); c++) {
                            if (String.valueOf(entry.getValue(c)).toLowerCase(Locale.ROOT).contains(q)) return true;
                        }
                        return false;
                    }
                    return true;
                }
            });
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            } else {
                inspector.setMemberData(null, null);
            }
        };

        refreshRef[0] = () -> {
            int selectedUserId = -1;
            int prevViewRow = table.getSelectedRow();
            if (prevViewRow >= 0) {
                selectedUserId = (int) model.getValueAt(table.convertRowIndexToModel(prevViewRow), 0);
            }
            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            int total = 0, active = 0, expiring = 0, expired = 0;
            for (User m : repo.getMembers()) {
                total++;
                Membership latest = repo.getLatestMembership(m.userId);
                String endStr = latest != null ? latest.endDate.toString() : "N/A";
                String status = latest != null ? latest.getComputedStatus(today) : "Expired";
                long daysLeft = latest != null ? latest.getDaysRemaining(today) : -999;
                if ("Active".equalsIgnoreCase(status)) {
                    active++;
                    if (daysLeft >= 0 && daysLeft <= 7) expiring++;
                } else {
                    expired++;
                }
                String daysStr = latest != null
                        ? (daysLeft >= 0 ? daysLeft + "d left" : Math.abs(daysLeft) + "d overdue")
                        : "None";
                model.addRow(new Object[]{
                        m.userId, "@" + m.username, m.getFullName(), m.contactNo,
                        m.fitnessLevel != null ? m.fitnessLevel : "Beginner",
                        endStr, daysStr, status
                });
            }

            kpiStrip.removeAll();
            kpiStrip.add(createKpiStatCard("TOTAL MEMBERS", String.valueOf(total), "Registered athletes", TEXT_MUTED, VectorIcon.Kind.USERS));
            kpiStrip.add(createKpiStatCard("ACTIVE PASSES", String.valueOf(active), "Cleared for entry", NEON_EMERALD, VectorIcon.Kind.CHECK));
            kpiStrip.add(createKpiStatCard("EXPIRING <= 7D", String.valueOf(expiring), "Due for renewal", AMBER_GOLD, VectorIcon.Kind.BELL));
            kpiStrip.add(createKpiStatCard("EXPIRED", String.valueOf(expired), "Access blocked", CORAL_RED, VectorIcon.Kind.REFRESH));
            kpiStrip.revalidate();
            kpiStrip.repaint();

            applyFilters.run();
            if (selectedUserId != -1) {
                for (int vr = 0; vr < table.getRowCount(); vr++) {
                    int mr = table.convertRowIndexToModel(vr);
                    if ((int) model.getValueAt(mr, 0) == selectedUserId) {
                        table.setRowSelectionInterval(vr, vr);
                        break;
                    }
                }
            }
        };

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters.run(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters.run(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters.run(); }
        });
        for (JToggleButton p : new JToggleButton[]{pillAll, pillActive, pillExpiring, pillExpired}) {
            p.addActionListener(e -> applyFilters.run());
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int userId = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
            User selected = repo.getMembers().stream().filter(u -> u.userId == userId).findFirst().orElse(null);
            if (selected != null) {
                inspector.setMemberData(selected, repo.getLatestMembership(userId));
            }
        });

        btnRegister.addActionListener(e -> showMemberRegistrationDialog(null, refreshRef[0]));
        refreshRef[0].run();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapTableScrollPane(table), inspector);
        splitPane.setResizeWeight(1.0);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        container.putClientProperty("onTabSelected", refreshRef[0]);
        container.add(northWrap, BorderLayout.NORTH);
        container.add(splitPane, BorderLayout.CENTER);
        return container;
    }

    private void showMemberRegistrationDialog(User existing, Runnable onComplete) {
        boolean isNew = (existing == null);
        JDialog dlg = new JDialog(this, isNew
                ? "Register New Gym Member (+ Automatic 1-Month Pass)"
                : "Edit Member Profile — " + existing.getFullName(), true);
        dlg.setSize(580, 640);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_OBSIDIAN);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(11, 2, 12, 10));
        form.setBackground(BG_SURFACE);
        form.setBorder(new EmptyBorder(18, 24, 14, 24));

        JTextField tfUsername  = createStyledTextField(isNew ? "" : existing.username);
        JPasswordField pfPass  = createStyledPasswordField();
        JTextField tfFirst     = createStyledTextField(isNew ? "" : existing.firstName);
        JTextField tfLast      = createStyledTextField(isNew ? "" : existing.lastName);
        JTextField tfContact   = createStyledTextField(isNew ? "09" : existing.contactNo);
        JTextField tfBirthdate = createStyledTextField(isNew ? "" : (existing.birthdate != null ? existing.birthdate.toString() : ""));
        JComboBox<String> cbGender = createStyledComboBox(new String[]{"Male", "Female", "Other", "Prefer not to say"});
        if (!isNew && existing.gender != null) cbGender.setSelectedItem(existing.gender);
        JTextField tfAddress   = createStyledTextField(isNew ? "" : (existing.address != null ? existing.address : ""));
        JTextField tfEmName    = createStyledTextField(isNew ? "" : (existing.emergencyContactName != null ? existing.emergencyContactName : ""));
        JTextField tfEmPhone   = createStyledTextField(isNew ? "09" : (existing.emergencyContactNo != null ? existing.emergencyContactNo : ""));
        JComboBox<String> cbFitness = createStyledComboBox(new String[]{"Beginner", "Intermediate", "Advanced"});
        if (!isNew && existing.fitnessLevel != null) cbFitness.setSelectedItem(existing.fitnessLevel);

        addFormRow(form, "Username *:", tfUsername);
        addFormRow(form, isNew ? "Password *:" : "New Password (optional):", pfPass);
        addFormRow(form, "First Name *:", tfFirst);
        addFormRow(form, "Last Name *:", tfLast);
        addFormRow(form, "Contact No. *:", tfContact);
        addFormRow(form, "Emergency Contact Name *:", tfEmName);
        addFormRow(form, "Emergency Contact No. *:", tfEmPhone);
        addFormRow(form, "Fitness Level *:", cbFitness);
        addFormRow(form, "Gender (optional):", cbGender);
        addFormRow(form, "Birthdate (YYYY-MM-DD, optional):", tfBirthdate);
        addFormRow(form, "Address (optional):", tfAddress);

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        JLabel membershipNote = new JLabel(isNew
                ? "Automatic 1-Month Pass will be activated: " + start + " -> " + end
                : "Updating member profile details.");
        membershipNote.setFont(new Font("SansSerif", Font.BOLD, 12));
        membershipNote.setForeground(NEON_EMERALD);
        membershipNote.setBorder(new EmptyBorder(8, 24, 4, 24));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(BG_OBSIDIAN);
        JButton btnCancel = createSecondaryButton("Cancel", null, null);
        JButton btnSave = createPrimaryButton(isNew ? "Activate Member Pass" : "Save Changes", VectorIcon.Kind.CHECK);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            String uname = tfUsername.getText().trim();
            String rawPass = new String(pfPass.getPassword()).trim();
            String fn = tfFirst.getText().trim();
            String ln = tfLast.getText().trim();
            String contact = tfContact.getText().trim();
            String bdateStr = tfBirthdate.getText().trim();
            String addr = tfAddress.getText().trim();
            String emName = tfEmName.getText().trim();
            String emPhone = tfEmPhone.getText().trim();

            if (uname.isEmpty() || fn.isEmpty() || ln.isEmpty() || contact.isEmpty() || contact.equals("09")) {
                JOptionPane.showMessageDialog(dlg, "Username, First Name, Last Name, and a valid Contact No. are required (*).",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (emName.isEmpty() || emPhone.isEmpty() || emPhone.equals("09")) {
                JOptionPane.showMessageDialog(dlg, "Emergency Contact Name and Phone No. are mandatory (*).",
                        "Emergency Contact Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isNew && rawPass.length() < 4) {
                JOptionPane.showMessageDialog(dlg, "Password is required and must be at least 4 characters.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (repo.usernameExists(uname, isNew ? 0 : existing.userId)) {
                JOptionPane.showMessageDialog(dlg, "Username '" + uname + "' is already taken.",
                        "Duplicate Username", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate parsedBirthdate = null;
            if (!bdateStr.isEmpty()) {
                try {
                    parsedBirthdate = LocalDate.parse(bdateStr);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(dlg, "Birthdate must be in YYYY-MM-DD format, or left blank.",
                            "Invalid Date Format", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            User u = isNew ? new User() : existing;
            u.username = uname;
            u.role = "Member";
            u.firstName = fn;
            u.lastName = ln;
            u.contactNo = contact;
            u.birthdate = parsedBirthdate;
            u.gender = (String) cbGender.getSelectedItem();
            u.address = addr;
            u.emergencyContactName = emName;
            u.emergencyContactNo = emPhone;
            u.fitnessLevel = (String) cbFitness.getSelectedItem();

            repo.saveUser(u, rawPass);
            if (isNew) repo.addMembership(u.userId, start, end);
            onComplete.run();
            dlg.dispose();
            JOptionPane.showMessageDialog(this,
                    isNew ? "Member " + u.getFullName() + " registered!\n1-Month Pass active: " + start + " to " + end + "."
                          : "Profile updated for " + u.getFullName() + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        JPanel southBox = new JPanel(new BorderLayout());
        southBox.setBackground(BG_OBSIDIAN);
        southBox.add(membershipNote, BorderLayout.NORTH);
        southBox.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(southBox, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void addFormRow(JPanel form, String labelText, JComponent field) {
        JLabel l = new JLabel(labelText);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT_SECONDARY);
        form.add(l);
        form.add(field);
    }

    private JPanel buildAlertListTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel banner = createCardPanel(new BorderLayout());
        banner.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, CORAL_RED),
                new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel info = new JLabel("Expiry & Renewal Radar — Expired & Expiring Within 7 Days");
        info.setFont(new Font("SansSerif", Font.BOLD, 14));
        info.setForeground(TEXT_PRIMARY);

        String[] cols = {"ID", "Member Name", "Contact No.", "Emergency Contact", "End Date", "Alert Status", "Timeline"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleModernTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        Runnable loadAlerts = () -> {
            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (User m : repo.getMembers()) {
                Membership latest = repo.getLatestMembership(m.userId);
                String status = latest != null ? latest.getComputedStatus(today) : "Expired";
                long days = latest != null ? latest.getDaysRemaining(today) : -999;

                if ("Expired".equalsIgnoreCase(status)) {
                    model.addRow(new Object[]{
                            m.userId, m.getFullName(), m.contactNo,
                            m.emergencyContactName + " (" + m.emergencyContactNo + ")",
                            latest != null ? latest.endDate.toString() : "None",
                            "Expired",
                            latest != null ? Math.abs(days) + " days overdue" : "No pass"
                    });
                } else if (days >= 0 && days <= 7) {
                    model.addRow(new Object[]{
                            m.userId, m.getFullName(), m.contactNo,
                            m.emergencyContactName + " (" + m.emergencyContactNo + ")",
                            latest.endDate.toString(),
                            "Expiring Soon",
                            days + " days remaining"
                    });
                }
            }
        };
        loadAlerts.run();

        JButton btnRefresh = createSecondaryButton("Refresh Radar", VectorIcon.Kind.REFRESH, TEXT_SECONDARY);
        btnRefresh.addActionListener(e -> loadAlerts.run());

        banner.add(info, BorderLayout.WEST);
        banner.add(btnRefresh, BorderLayout.EAST);

        panel.putClientProperty("onTabSelected", loadAlerts);
        panel.add(banner, BorderLayout.NORTH);
        panel.add(wrapTableScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminUserManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] cols = {"User ID", "Username", "Role", "First Name", "Last Name", "Contact No.", "Date Registered"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleModernTable(table);

        Runnable refresh = () -> {
            model.setRowCount(0);
            for (User u : repo.getAllUsers()) {
                model.addRow(new Object[]{
                        u.userId, "@" + u.username, u.role, u.firstName, u.lastName, u.contactNo, u.dateRegistered
                });
            }
        };
        refresh.run();

        JPanel top = createCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAddStaff = createPrimaryButton("Add Admin / Staff Account", VectorIcon.Kind.PLUS);
        JButton btnDeleteUser = createDangerButton("Delete Selected Account", VectorIcon.Kind.TRASH);
        top.add(btnAddStaff);
        top.add(btnDeleteUser);

        btnAddStaff.addActionListener(e -> {
            JTextField tfUser = createStyledTextField("");
            JPasswordField pfPass = createStyledPasswordField();
            JComboBox<String> cbRole = createStyledComboBox(new String[]{"Staff", "Admin"});
            JTextField tfFirst = createStyledTextField("");
            JTextField tfLast = createStyledTextField("");
            JTextField tfContact = createStyledTextField("09");

            Object[] fields = {
                    "Username *:", tfUser,
                    "Password (min 4 chars) *:", pfPass,
                    "Role *:", cbRole,
                    "First Name *:", tfFirst,
                    "Last Name *:", tfLast,
                    "Contact No. *:", tfContact
            };
            int res = JOptionPane.showConfirmDialog(this, fields, "Create Staff / Admin Account", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String un = tfUser.getText().trim();
                String pw = new String(pfPass.getPassword()).trim();
                String fn = tfFirst.getText().trim();
                String ln = tfLast.getText().trim();
                String contact = tfContact.getText().trim();
                if (un.isEmpty() || fn.isEmpty() || ln.isEmpty() || contact.isEmpty() || contact.equals("09")) {
                    JOptionPane.showMessageDialog(this, "Username, First Name, Last Name, and Contact No. are required.");
                    return;
                }
                if (pw.length() < 4) {
                    JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.");
                    return;
                }
                if (repo.usernameExists(un, 0)) {
                    JOptionPane.showMessageDialog(this, "Username already exists.");
                    return;
                }
                User u = new User();
                u.username = un;
                u.role = (String) cbRole.getSelectedItem();
                u.firstName = fn;
                u.lastName = ln;
                u.contactNo = contact;
                u.dateRegistered = LocalDate.now();
                repo.saveUser(u, pw);
                refresh.run();
            }
        });

        btnDeleteUser.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a user account to delete.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            int id = (int) model.getValueAt(modelRow, 0);
            if (id == currentUser.userId) {
                JOptionPane.showMessageDialog(this, "You cannot delete your own active Admin account.");
                return;
            }
            String username = String.valueOf(model.getValueAt(modelRow, 1));
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete user account " + username + " (#" + id + ")?",
                    "Confirm Account Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repo.deleteUser(id);
                refresh.run();
            }
        });

        panel.putClientProperty("onTabSelected", refresh);
        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapTableScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildExerciseLibraryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] cols = {"ID", "Workout Name", "Target Muscle", "Level", "Equipment", "Form Cues"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleModernTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new MachineBadgeRenderer());

        WorkoutVisualCard visualCard = new WorkoutVisualCard();

        JCheckBox chkShowMachines = new JCheckBox("Show Gym Machine Workouts", true);
        chkShowMachines.setOpaque(false);
        chkShowMachines.setForeground(TEXT_PRIMARY);
        chkShowMachines.setFont(new Font("SansSerif", Font.BOLD, 12));

        JCheckBox chkShowFree = new JCheckBox("Show Free-Weight / Bodyweight", true);
        chkShowFree.setOpaque(false);
        chkShowFree.setForeground(TEXT_PRIMARY);
        chkShowFree.setFont(new Font("SansSerif", Font.BOLD, 12));

        Runnable refresh = () -> {
            model.setRowCount(0);
            boolean showM = chkShowMachines.isSelected();
            boolean showF = chkShowFree.isSelected();
            for (Exercise ex : repo.getAllExercises()) {
                if (ex.requiresMachine() && !showM) continue;
                if (!ex.requiresMachine() && !showF) continue;
                model.addRow(new Object[]{
                        ex.exerciseId, ex.name, ex.muscleGroup, ex.difficulty, ex.getMachineBadgeText(), ex.description
                });
            }
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            } else {
                visualCard.setExercise(null);
            }
        };

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            Exercise selected = repo.getAllExercises().stream().filter(x -> x.exerciseId == id).findFirst().orElse(null);
            visualCard.setExercise(selected);
        });

        chkShowMachines.addActionListener(e -> refresh.run());
        chkShowFree.addActionListener(e -> refresh.run());
        refresh.run();

        JPanel top = createCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnAdd = createPrimaryButton("Add Workout / Machine", VectorIcon.Kind.PLUS);
        JButton btnEdit = createSecondaryButton("Edit Selected", VectorIcon.Kind.EDIT, TEXT_SECONDARY);
        JButton btnDel = createDangerButton("Delete", VectorIcon.Kind.TRASH);
        top.add(btnAdd);
        top.add(btnEdit);
        top.add(btnDel);
        top.add(Box.createHorizontalStrut(12));
        top.add(chkShowMachines);
        top.add(chkShowFree);

        ActionListener addOrEdit = e -> {
            boolean isEdit = (e.getSource() == btnEdit);
            Exercise target = null;
            if (isEdit) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Select a workout first.");
                    return;
                }
                int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
                target = repo.getAllExercises().stream().filter(x -> x.exerciseId == id).findFirst().orElse(null);
            }
            JTextField tfName = createStyledTextField(target != null ? target.name : "");
            JComboBox<String> cbGroup = createStyledComboBox(new String[]{"Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body"});
            if (target != null) cbGroup.setSelectedItem(target.muscleGroup);
            JComboBox<String> cbDiff = createStyledComboBox(new String[]{"Beginner", "Intermediate", "Advanced"});
            if (target != null) cbDiff.setSelectedItem(target.difficulty);

            JCheckBox chkNeedsMachine = new JCheckBox("This workout requires a Gym Machine", target == null || target.requiresMachine());
            String[] equipOptions = new String[]{
                    "Machine: Lat Pulldown Station", "Machine: Seated Cable Row Station",
                    "Machine: Assisted Dip & Chin-Up Machine", "Machine: Converging Chest Press Machine",
                    "Machine: Pec Deck Fly Station", "Machine: Angled Smith Machine",
                    "Machine: 45-Degree Leg Press Sled", "Machine: Quad Extension Machine",
                    "Machine: Prone Leg Curl Machine", "Machine: Standing Calf Machine",
                    "Machine: Linear Hack Squat Sled", "Machine: Overhead Press Machine",
                    "Machine: Dual Adjustable Pulley Tower", "Machine: High Pulley Cable Station",
                    "Machine: Bicep Preacher Curl Machine", "Machine: Captain's Chair / Roman Chair",
                    "Machine: Abdominal Crunch Machine", "Machine: Motorized Gym Treadmill",
                    "Machine: Indoor Concept Rower", "Bodyweight", "Dumbbells", "Barbell"
            };
            JComboBox<String> cbEquip = createStyledComboBox(equipOptions);
            cbEquip.setEditable(true);
            if (target != null) cbEquip.setSelectedItem(target.equipment);

            chkNeedsMachine.addActionListener(ev -> {
                Object selItem = cbEquip.getSelectedItem();
                String selStr = selItem != null ? selItem.toString() : "";
                if (!chkNeedsMachine.isSelected()) {
                    cbEquip.setSelectedItem("Dumbbells");
                } else if (!Exercise.isMachineEquipment(selStr)) {
                    cbEquip.setSelectedItem("Machine: Lat Pulldown Station");
                }
            });

            JTextArea taDesc = new JTextArea(target != null ? target.description : "", 3, 24);
            taDesc.setLineWrap(true);
            taDesc.setWrapStyleWord(true);

            Object[] fields = {
                    "Workout Name *:", tfName,
                    "Target Muscle Group *:", cbGroup,
                    "Difficulty Level *:", cbDiff,
                    "Machine Requirement Toggle:", chkNeedsMachine,
                    "Gym Machine / Equipment *:", cbEquip,
                    "Form & Execution Cues *:", new JScrollPane(taDesc)
            };
            int res = JOptionPane.showConfirmDialog(this, fields,
                    isEdit ? "Edit Workout & Machine Settings" : "Add New Workout / Gym Machine", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String exName = tfName.getText().trim();
                String exDesc = taDesc.getText().trim();
                if (exName.isEmpty() || exDesc.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Workout name and instructions are required.");
                    return;
                }
                int excludeId = (isEdit && target != null) ? target.exerciseId : 0;
                if (repo.exerciseNameExists(exName, excludeId)) {
                    JOptionPane.showMessageDialog(this, "An exercise named '" + exName + "' already exists.",
                            "Duplicate Exercise", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Exercise ex = isEdit && target != null ? target : new Exercise();
                ex.name = exName;
                ex.muscleGroup = (String) cbGroup.getSelectedItem();
                ex.difficulty = (String) cbDiff.getSelectedItem();
                String chosenEq = cbEquip.getSelectedItem() != null ? cbEquip.getSelectedItem().toString().trim() : "Bodyweight";
                if (chosenEq.isEmpty()) chosenEq = chkNeedsMachine.isSelected() ? "Machine: Gym Station" : "Bodyweight";
                if (chkNeedsMachine.isSelected() && !Exercise.isMachineEquipment(chosenEq)) {
                    chosenEq = "Machine: " + chosenEq;
                } else if (!chkNeedsMachine.isSelected() && Exercise.isMachineEquipment(chosenEq)) {
                    chosenEq = "Dumbbells";
                }
                ex.equipment = chosenEq;
                ex.description = exDesc;
                repo.saveExercise(ex);
                refresh.run();
            }
        };
        btnAdd.addActionListener(addOrEdit);
        btnEdit.addActionListener(addOrEdit);

        btnDel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an exercise to delete.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            int id = (int) model.getValueAt(modelRow, 0);
            String exName = String.valueOf(model.getValueAt(modelRow, 1));
            if (repo.isExerciseUsedInPlans(id)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete '" + exName + "' because it is assigned to one or more Workout Plans.",
                        "Exercise In Use", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete exercise '" + exName + "' (#" + id + ") from the library?",
                    "Confirm Exercise Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repo.deleteExercise(id);
                refresh.run();
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapTableScrollPane(table), visualCard);
        split.setResizeWeight(1.0);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setOpaque(false);

        panel.putClientProperty("onTabSelected", refresh);
        panel.add(top, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel wrapReportTable(String title, JTable table) {
        styleModernTable(table);
        JPanel box = createCardPanel(new BorderLayout(0, 10));
        box.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_PRIMARY);
        box.add(lbl, BorderLayout.NORTH);
        box.add(wrapTableScrollPane(table), BorderLayout.CENTER);
        return box;
    }

    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        JPanel tablesGrid = new JPanel(new GridLayout(1, 3, 12, 12));
        tablesGrid.setOpaque(false);

        Runnable buildReportContent = () -> {
            kpiRow.removeAll();
            tablesGrid.removeAll();

            LocalDate today = LocalDate.now();
            int activeCount = 0, expiredCount = 0, expiring7Count = 0;

            DefaultTableModel expSoonModel = new DefaultTableModel(new String[]{"Member", "End Date", "Remaining"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            for (User m : repo.getMembers()) {
                Membership latest = repo.getLatestMembership(m.userId);
                String st = latest != null ? latest.getComputedStatus(today) : "Expired";
                long days = latest != null ? latest.getDaysRemaining(today) : -999;
                if ("Active".equalsIgnoreCase(st)) {
                    activeCount++;
                    if (days >= 0 && days <= 7) {
                        expiring7Count++;
                        expSoonModel.addRow(new Object[]{m.getFullName(), latest.endDate, days + " days"});
                    }
                } else {
                    expiredCount++;
                }
            }

            int totalMembers = activeCount + expiredCount;
            DefaultTableModel r1Model = new DefaultTableModel(new String[]{"Pass Status", "Members", "Share"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            r1Model.addRow(new Object[]{"Active Passes", activeCount,
                    totalMembers > 0 ? String.format(Locale.ROOT, "%.1f%%", (activeCount * 100.0) / totalMembers) : "0%"});
            r1Model.addRow(new Object[]{"Expired Passes", expiredCount,
                    totalMembers > 0 ? String.format(Locale.ROOT, "%.1f%%", (expiredCount * 100.0) / totalMembers) : "0%"});
            r1Model.addRow(new Object[]{"Total Registered", totalMembers, "100.0%"});

            Map<YearMonth, Integer> monthlyCounts = new TreeMap<>(Comparator.reverseOrder());
            for (Membership m : repo.getAllMemberships()) {
                YearMonth ym = YearMonth.from(m.startDate);
                monthlyCounts.put(ym, monthlyCounts.getOrDefault(ym, 0) + 1);
            }
            DefaultTableModel r3Model = new DefaultTableModel(new String[]{"Month (YYYY-MM)", "Renewals"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (Map.Entry<YearMonth, Integer> entry : monthlyCounts.entrySet()) {
                r3Model.addRow(new Object[]{entry.getKey().toString(), entry.getValue()});
            }

            kpiRow.add(createKpiStatCard("TOTAL MEMBERS", String.valueOf(totalMembers), "Registered Athletes", TEXT_MUTED, VectorIcon.Kind.USERS));
            kpiRow.add(createKpiStatCard("ACTIVE PASSES", String.valueOf(activeCount), "Cleared for Entry", NEON_EMERALD, VectorIcon.Kind.CHECK));
            kpiRow.add(createKpiStatCard("EXPIRED PASSES", String.valueOf(expiredCount), "Needs Renewal", CORAL_RED, VectorIcon.Kind.REFRESH));
            kpiRow.add(createKpiStatCard("EXPIRING <= 7 DAYS", String.valueOf(expiring7Count), "Upcoming Renewals", AMBER_GOLD, VectorIcon.Kind.BELL));

            tablesGrid.add(wrapReportTable("1. Active vs. Expired Summary", new JTable(r1Model)));
            tablesGrid.add(wrapReportTable("2. Expiring Within 7 Days", new JTable(expSoonModel)));
            tablesGrid.add(wrapReportTable("3. Monthly Renewals Log", new JTable(r3Model)));

            panel.revalidate();
            panel.repaint();
        };
        buildReportContent.run();

        JButton btnRefresh = createSecondaryButton("Refresh Analytics", VectorIcon.Kind.REFRESH, TEXT_SECONDARY);
        btnRefresh.addActionListener(e -> buildReportContent.run());
        JButton btnExportCsv = createPrimaryButton("Export Reports CSV", VectorIcon.Kind.DOWNLOAD);
        btnExportCsv.addActionListener(e -> exportReportsCsv());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(btnExportCsv);
        south.add(btnRefresh);

        panel.putClientProperty("onTabSelected", buildReportContent);
        panel.add(kpiRow, BorderLayout.NORTH);
        panel.add(tablesGrid, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void exportReportsCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export GymSync Management Reports (CSV)");
        fc.setSelectedFile(new java.io.File("gymsync_reports_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File target = fc.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            target = new java.io.File(target.getParentFile(), target.getName() + ".csv");
        }

        LocalDate today = LocalDate.now();
        int activeCount = 0, expiredCount = 0;
        List<String[]> expiringRows = new ArrayList<>();
        for (User m : repo.getMembers()) {
            Membership latest = repo.getLatestMembership(m.userId);
            String st = latest != null ? latest.getComputedStatus(today) : "Expired";
            long days = latest != null ? latest.getDaysRemaining(today) : -999;
            if ("Active".equalsIgnoreCase(st)) {
                activeCount++;
                if (days >= 0 && days <= 7) expiringRows.add(new String[]{m.getFullName(), latest.endDate.toString(), String.valueOf(days)});
            } else {
                expiredCount++;
            }
        }
        int totalMembers = activeCount + expiredCount;

        Map<YearMonth, Integer> monthlyCounts = new TreeMap<>(Comparator.reverseOrder());
        for (Membership m : repo.getAllMemberships()) {
            YearMonth ym = YearMonth.from(m.startDate);
            monthlyCounts.put(ym, monthlyCounts.getOrDefault(ym, 0) + 1);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Report 1: Active vs. Expired Summary\nPass Status,Members,Share\n");
        csv.append("Active Passes,").append(activeCount).append(',')
                .append(totalMembers > 0 ? String.format(Locale.ROOT, "%.1f%%", (activeCount * 100.0) / totalMembers) : "0%").append('\n');
        csv.append("Expired Passes,").append(expiredCount).append(',')
                .append(totalMembers > 0 ? String.format(Locale.ROOT, "%.1f%%", (expiredCount * 100.0) / totalMembers) : "0%").append('\n');
        csv.append("Total Registered,").append(totalMembers).append(",100.0%\n\n");
        csv.append("Report 2: Memberships Expiring Within 7 Days\nMember,End Date,Days Remaining\n");
        for (String[] r : expiringRows) {
            csv.append(escapeCsv(r[0])).append(',').append(r[1]).append(',').append(r[2]).append('\n');
        }
        csv.append("\nReport 3: Monthly Renewals Log\nMonth (YYYY-MM),Renewals\n");
        for (Map.Entry<YearMonth, Integer> entry : monthlyCounts.entrySet()) {
            csv.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
        }

        try {
            java.nio.file.Files.writeString(target.toPath(), csv.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Exported management summary reports to:\n" + target.getAbsolutePath(),
                    "CSV Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not export CSV file: " + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private JPanel buildMemberWorkoutTab(User targetMember) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_OBSIDIAN);

        JPanel topControls = createCardPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JComboBox<String> cbGoal = createStyledComboBox(new String[]{"Weight Loss", "Muscle Gain", "General Fitness", "Strength Foundation"});
        JComboBox<String> cbLevel = createStyledComboBox(new String[]{"Beginner", "Intermediate", "Advanced"});
        if (targetMember.fitnessLevel != null) cbLevel.setSelectedItem(targetMember.fitnessLevel);
        JComboBox<Integer> cbDays = createStyledComboBox(new Integer[]{2, 3, 4, 5});
        cbDays.setSelectedItem(3);
        JComboBox<String> cbEquip = createStyledComboBox(new String[]{"Full Gym", "Gym Machines Only", "Dumbbells", "Bodyweight"});

        JCheckBox chkUseMachines = new JCheckBox("Include Gym Machines", true);
        chkUseMachines.setOpaque(false);
        chkUseMachines.setForeground(TEXT_PRIMARY);
        chkUseMachines.setFont(new Font("SansSerif", Font.BOLD, 12));

        JButton btnGenerate = createPrimaryButton("Generate Plan", VectorIcon.Kind.PLUS);
        JButton btnCustom = createSecondaryButton("Custom Builder", VectorIcon.Kind.EDIT, TEXT_SECONDARY);

        topControls.add(createChipLabel("GOAL"));
        topControls.add(cbGoal);
        topControls.add(createChipLabel("LEVEL"));
        topControls.add(cbLevel);
        topControls.add(createChipLabel("DAYS"));
        topControls.add(cbDays);
        topControls.add(createChipLabel("ZONE"));
        topControls.add(cbEquip);
        topControls.add(chkUseMachines);
        topControls.add(btnGenerate);
        topControls.add(btnCustom);

        JLabel planHeaderLbl = new JLabel("Current Active Workout Plan:");
        planHeaderLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        planHeaderLbl.setForeground(NEON_EMERALD);

        JComboBox<WorkoutPlan> cbSavedPlans = createStyledComboBox(new WorkoutPlan[0]);
        cbSavedPlans.setMaximumRowCount(8);
        JButton btnDeletePlan = createDangerButton("Delete Plan", VectorIcon.Kind.TRASH);

        JPanel planHeaderBar = new JPanel(new BorderLayout(10, 6));
        planHeaderBar.setOpaque(false);
        JPanel planSelectorBar = new JPanel(new BorderLayout(8, 0));
        planSelectorBar.setOpaque(false);
        planSelectorBar.add(createChipLabel("SAVED PLANS"), BorderLayout.WEST);
        planSelectorBar.add(cbSavedPlans, BorderLayout.CENTER);
        planSelectorBar.add(btnDeletePlan, BorderLayout.EAST);
        planHeaderBar.add(planHeaderLbl, BorderLayout.NORTH);
        planHeaderBar.add(planSelectorBar, BorderLayout.SOUTH);

        String[] cols = {"Day", "Workout Name", "Target Muscle", "Equipment", "Sets", "Reps"};
        DefaultTableModel exModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable exTable = new JTable(exModel);
        styleModernTable(exTable);
        exTable.getColumnModel().getColumn(3).setCellRenderer(new MachineBadgeRenderer());

        WorkoutVisualCard visualCard = new WorkoutVisualCard();
        final boolean[] updatingPlanCombo = {false};

        Runnable renderSelectedPlan = () -> {
            exModel.setRowCount(0);
            WorkoutPlan selected = (WorkoutPlan) cbSavedPlans.getSelectedItem();
            if (selected == null) {
                planHeaderLbl.setText("No workout plan yet — use Generate Plan or Custom Builder above.");
                visualCard.setExercise(null);
                return;
            }
            String typeTag = selected.planType != null ? selected.planType.toUpperCase(Locale.ROOT) : "PLAN";
            planHeaderLbl.setText("ACTIVE PLAN #" + selected.workoutPlanId + " [" + typeTag + "]  •  " + selected.goal);
            boolean showMachines = chkUseMachines.isSelected();
            for (WorkoutPlanExercise item : selected.exercises) {
                boolean isMach = Exercise.isMachineEquipment(item.equipment);
                if (!showMachines && isMach) continue;
                String badge = isMach ? item.equipment.replace("Machine: ", "") : ("No Machine (" + item.equipment + ")");
                exModel.addRow(new Object[]{item.dayOfWeek, item.exerciseName, item.muscleGroup, badge, item.sets + " sets", item.reps});
            }
            if (exTable.getRowCount() > 0) {
                exTable.setRowSelectionInterval(0, 0);
            } else {
                visualCard.setExercise(null);
            }
        };

        java.util.function.Consumer<Boolean> refreshPlans = selectNewest -> {
            WorkoutPlan prev = (WorkoutPlan) cbSavedPlans.getSelectedItem();
            int prevId = (!selectNewest && prev != null) ? prev.workoutPlanId : -1;
            List<WorkoutPlan> plans = repo.getWorkoutPlansByUser(targetMember.userId);
            updatingPlanCombo[0] = true;
            try {
                cbSavedPlans.removeAllItems();
                int restoreIdx = 0;
                for (int i = 0; i < plans.size(); i++) {
                    WorkoutPlan wp = plans.get(i);
                    cbSavedPlans.addItem(wp);
                    if (wp.workoutPlanId == prevId) restoreIdx = i;
                }
                boolean hasPlans = !plans.isEmpty();
                cbSavedPlans.setEnabled(hasPlans);
                btnDeletePlan.setEnabled(hasPlans);
                if (hasPlans) cbSavedPlans.setSelectedIndex(restoreIdx);
            } finally {
                updatingPlanCombo[0] = false;
            }
            renderSelectedPlan.run();
        };

        Runnable refreshPlansKeepSelection = () -> refreshPlans.accept(false);
        Runnable refreshPlansSelectNewest = () -> refreshPlans.accept(true);

        cbSavedPlans.addActionListener(e -> { if (!updatingPlanCombo[0]) renderSelectedPlan.run(); });
        btnDeletePlan.addActionListener(e -> {
            WorkoutPlan selected = (WorkoutPlan) cbSavedPlans.getSelectedItem();
            if (selected == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete workout plan #" + selected.workoutPlanId + " (" + selected.goal + ")?",
                    "Confirm Delete Plan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repo.deleteWorkoutPlan(selected.workoutPlanId);
                refreshPlansSelectNewest.run();
            }
        });

        exTable.getSelectionModel().addListSelectionListener(e -> {
            int row = exTable.getSelectedRow();
            if (row < 0) return;
            String exName = exModel.getValueAt(exTable.convertRowIndexToModel(row), 1).toString();
            Exercise matched = repo.getAllExercises().stream().filter(x -> x.name.equalsIgnoreCase(exName)).findFirst().orElse(null);
            visualCard.setExercise(matched);
        });

        cbEquip.addActionListener(e -> {
            if ("Gym Machines Only".equals(cbEquip.getSelectedItem()) && !chkUseMachines.isSelected()) {
                chkUseMachines.setSelected(true);
                renderSelectedPlan.run();
            }
        });
        chkUseMachines.addActionListener(e -> {
            if (!chkUseMachines.isSelected() && "Gym Machines Only".equals(cbEquip.getSelectedItem())) {
                cbEquip.setSelectedItem("Dumbbells");
            }
            renderSelectedPlan.run();
        });

        refreshPlansSelectNewest.run();

        btnGenerate.addActionListener(e -> {
            String eqPref = (String) cbEquip.getSelectedItem();
            boolean useMach = chkUseMachines.isSelected();
            if (!useMach && ("Full Gym".equals(eqPref) || "Gym Machines Only".equals(eqPref))) eqPref = "Dumbbells";
            WorkoutPlan generated = BusinessRules.generateRuleBasedPlan(
                    targetMember.userId, (String) cbGoal.getSelectedItem(), (String) cbLevel.getSelectedItem(),
                    (Integer) cbDays.getSelectedItem(), eqPref, useMach, repo.getAllExercises());
            repo.saveWorkoutPlan(generated);
            refreshPlansSelectNewest.run();
            JOptionPane.showMessageDialog(this, "Workout plan generated: " + generated.goal,
                    "Workout Plan Ready", JOptionPane.INFORMATION_MESSAGE);
        });
        btnCustom.addActionListener(e -> showCustomPlanBuilderDialog(targetMember, refreshPlansSelectNewest));

        JPanel centerBox = new JPanel(new BorderLayout(0, 10));
        centerBox.setOpaque(false);
        centerBox.add(planHeaderBar, BorderLayout.NORTH);
        centerBox.add(wrapTableScrollPane(exTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerBox, visualCard);
        split.setResizeWeight(1.0);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setOpaque(false);

        panel.putClientProperty("onTabSelected", refreshPlansKeepSelection);
        panel.add(topControls, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createChipLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JPanel buildStaffWorkoutHelperTab() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setBackground(BG_OBSIDIAN);
        wrapper.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel selectorBar = createCardPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JLabel lbl = new JLabel("Member Workout Plan:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_PRIMARY);
        JComboBox<String> memberCombo = createStyledComboBox(new String[]{});
        List<User> members = new ArrayList<>();

        selectorBar.add(lbl);
        selectorBar.add(memberCombo);

        JPanel dynamicHolder = new JPanel(new BorderLayout());
        dynamicHolder.setOpaque(false);

        Runnable loadSelectedMemberPlanner = () -> {
            dynamicHolder.removeAll();
            int idx = memberCombo.getSelectedIndex();
            if (idx >= 0 && idx < members.size()) {
                dynamicHolder.add(buildMemberWorkoutTab(members.get(idx)), BorderLayout.CENTER);
            }
            dynamicHolder.revalidate();
            dynamicHolder.repaint();
        };

        Runnable refreshMembers = () -> {
            int selectedUserId = -1;
            int prevIdx = memberCombo.getSelectedIndex();
            if (prevIdx >= 0 && prevIdx < members.size()) selectedUserId = members.get(prevIdx).userId;
            members.clear();
            members.addAll(repo.getMembers());
            ActionListener[] listeners = memberCombo.getActionListeners();
            for (ActionListener al : listeners) memberCombo.removeActionListener(al);
            memberCombo.removeAllItems();
            int restoreIdx = 0;
            for (int i = 0; i < members.size(); i++) {
                User m = members.get(i);
                memberCombo.addItem(m.userId + " - " + m.getFullName() + " (" + m.fitnessLevel + ")");
                if (m.userId == selectedUserId) restoreIdx = i;
            }
            if (!members.isEmpty()) memberCombo.setSelectedIndex(restoreIdx);
            for (ActionListener al : listeners) memberCombo.addActionListener(al);
            loadSelectedMemberPlanner.run();
        };

        memberCombo.addActionListener(e -> loadSelectedMemberPlanner.run());
        refreshMembers.run();

        wrapper.putClientProperty("onTabSelected", refreshMembers);
        wrapper.add(selectorBar, BorderLayout.NORTH);
        wrapper.add(dynamicHolder, BorderLayout.CENTER);
        return wrapper;
    }

    private void showCustomPlanBuilderDialog(User member, Runnable onSaved) {
        JDialog dlg = new JDialog(this, "Custom Workout Builder — " + member.getFullName(), true);
        dlg.setSize(820, 560);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_OBSIDIAN);
        dlg.setLayout(new BorderLayout(12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        top.setBackground(BG_OBSIDIAN);
        JLabel goalLbl = new JLabel("Custom Routine Title *:");
        goalLbl.setForeground(TEXT_PRIMARY);
        goalLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        JTextField tfGoal = createStyledTextField("Custom Hypertrophy & Conditioning Split");
        tfGoal.setColumns(28);
        top.add(goalLbl);
        top.add(tfGoal);

        String[] cols = {"Day of Week", "Exercise ID", "Exercise Name", "Muscle Group", "Sets", "Reps"};
        DefaultTableModel draftModel = new DefaultTableModel(cols, 0);
        JTable draftTable = new JTable(draftModel);
        styleModernTable(draftTable);

        JPanel adder = createCardPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JComboBox<String> cbDay = createStyledComboBox(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
        JComboBox<Exercise> cbEx = createStyledComboBox(repo.getAllExercises().toArray(new Exercise[0]));
        JSpinner spSets = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JTextField tfReps = createStyledTextField("10-12");
        tfReps.setColumns(6);
        JButton btnAddRow = createPrimaryButton("Add Exercise", VectorIcon.Kind.PLUS);
        JButton btnRemoveRow = createDangerButton("Remove", VectorIcon.Kind.TRASH);

        adder.add(createChipLabel("DAY"));
        adder.add(cbDay);
        adder.add(createChipLabel("EXERCISE"));
        adder.add(cbEx);
        adder.add(createChipLabel("SETS"));
        adder.add(spSets);
        adder.add(createChipLabel("REPS"));
        adder.add(tfReps);
        adder.add(btnAddRow);
        adder.add(btnRemoveRow);

        List<WorkoutPlanExercise> draftItems = new ArrayList<>();
        btnAddRow.addActionListener(e -> {
            Exercise ex = (Exercise) cbEx.getSelectedItem();
            if (ex == null) return;
            String reps = tfReps.getText().trim();
            if (reps.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter target reps (e.g., 10-12).");
                return;
            }
            WorkoutPlanExercise item = new WorkoutPlanExercise();
            item.exerciseId = ex.exerciseId;
            item.exerciseName = ex.name;
            item.muscleGroup = ex.muscleGroup;
            item.equipment = ex.equipment;
            item.dayOfWeek = (String) cbDay.getSelectedItem();
            item.sets = (Integer) spSets.getValue();
            item.reps = reps;
            draftItems.add(item);
            draftModel.addRow(new Object[]{item.dayOfWeek, ex.exerciseId, ex.name, ex.muscleGroup, item.sets, item.reps});
        });

        btnRemoveRow.addActionListener(e -> {
            int row = draftTable.getSelectedRow();
            if (row < 0) return;
            int modelRow = draftTable.convertRowIndexToModel(row);
            draftItems.remove(modelRow);
            draftModel.removeRow(modelRow);
        });

        JButton btnSavePlan = createPrimaryButton("Save Custom Workout Plan", VectorIcon.Kind.CHECK);
        btnSavePlan.addActionListener(e -> {
            if (tfGoal.getText().trim().isEmpty() || draftItems.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter a plan title and add at least one exercise.");
                return;
            }
            WorkoutPlan custom = new WorkoutPlan();
            custom.userId = member.userId;
            custom.planType = "custom";
            custom.goal = tfGoal.getText().trim();
            custom.dateCreated = LocalDate.now();
            custom.exercises.addAll(draftItems);
            repo.saveWorkoutPlan(custom);
            onSaved.run();
            dlg.dispose();
        });

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBackground(BG_OBSIDIAN);
        south.setBorder(new EmptyBorder(0, 12, 12, 12));
        south.add(adder, BorderLayout.CENTER);
        JPanel saveBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        saveBar.setOpaque(false);
        saveBar.add(btnSavePlan);
        south.add(saveBar, BorderLayout.SOUTH);

        dlg.add(top, BorderLayout.NORTH);
        dlg.add(wrapTableScrollPane(draftTable), BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    public static void main(String[] args) {
        installLookAndFeel(false);
        SwingUtilities.invokeLater(() -> {
            gymsyncapp app = new gymsyncapp();
            app.setVisible(true);
        });
    }
}
