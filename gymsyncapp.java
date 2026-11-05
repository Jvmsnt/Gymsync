import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
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
public class gymsyncapp extends JFrame {
    private static final long serialVersionUID = 1L;

    // ── Play Store Fitness App Dark & Neon Theme Palette ──
    public static final Color BG_OBSIDIAN     = new Color(11, 15, 25);    // #0B0F19 Main App Canvas
    public static final Color BG_SURFACE      = new Color(18, 25, 38);    // #121926 Secondary Surface
    public static final Color CARD_SURFACE    = new Color(28, 37, 54);    // #1C2536 Elevated Card
    public static final Color CARD_HIGHLIGHT  = new Color(37, 49, 71);    // #253147 Hover/Inner Card
    public static final Color BORDER_SUBTLE   = new Color(51, 65, 85);    // #334155 Card Outline
    public static final Color INPUT_BG        = new Color(15, 23, 42);    // #0F172A Dark Input Pill

    public static final Color TEXT_PRIMARY    = new Color(248, 250, 252); // #F8FAFC Crisp White
    public static final Color TEXT_SECONDARY  = new Color(148, 163, 184); // #94A3B8 Muted Slate
    public static final Color TEXT_MUTED      = new Color(100, 116, 139); // #64748B Sub-label

    // Play Store Vibrant Accents & Gradients
    public static final Color NEON_EMERALD    = new Color(16, 185, 129);  // #10B981 Active / Primary CTA
    public static final Color CYBER_CYAN      = new Color(6, 182, 212);   // #06B6D4 Secondary Gradient
    public static final Color ELECTRIC_INDIGO = new Color(99, 102, 241);  // #6366F1 Brand Accent
    public static final Color VIOLET_GLOW     = new Color(139, 92, 246);  // #8B5CF6 Secondary Accent
    public static final Color CORAL_RED       = new Color(244, 63, 94);   // #F43F5E Expired / Danger
    public static final Color AMBER_GOLD      = new Color(245, 158, 11);  // #F59E0B Expiring Soon Warning

    public static final Color TABLE_EVEN_ROW  = new Color(24, 33, 48);
    public static final Color TABLE_ALT_ROW   = new Color(19, 27, 40);
    public static final Color TABLE_SELECT    = new Color(55, 48, 163);

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

        public boolean requiresMachine() {
            if (equipment == null) return false;
            String eq = equipment.toLowerCase();
            return eq.contains("machine") || eq.contains("cable") || eq.contains("pulley")
                    || eq.contains("sled") || eq.contains("station") || eq.contains("tower")
                    || eq.contains("treadmill") || eq.contains("rower") || eq.contains("chair")
                    || eq.contains("pec deck") || eq.contains("smith");
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
                default: return muscleGroup.toLowerCase().replace(" ", "_") + ".gif";
            }
        }

        public String getPrimaryMuscles() {
            String n = name != null ? name.toLowerCase() : "";
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
            String mg = muscleGroup != null ? muscleGroup.toLowerCase() : "";
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
                pool = new ArrayList<>(allExercises);
            }

            int defaultSets;
            String defaultReps;
            if (goal.toLowerCase().contains("weight loss")) {
                defaultSets = 3;
                defaultReps = "12-15";
            } else if (goal.toLowerCase().contains("muscle")) {
                defaultSets = 4;
                defaultReps = "8-12";
            } else if (goal.toLowerCase().contains("strength")) {
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
            String eq = exEquip != null ? exEquip.toLowerCase() : "";
            boolean isMachine = eq.contains("machine") || eq.contains("station") || eq.contains("sled")
                    || eq.contains("tower") || eq.contains("treadmill") || eq.contains("rower") || eq.contains("chair");
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
        private static final String JDBC_URL = System.getProperty("gymsync.db.url", "jdbc:sqlite:gymsync.db");
        private boolean jdbcAvailable = false;

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
            try {
                Connection conn = DriverManager.getConnection(JDBC_URL);
                jdbcAvailable = true;
                createTablesIfNotExist(conn);
                syncIfDatabaseEmpty(conn);
                loadFromJdbc(conn);
                conn.close();
            } catch (SQLException ex) {
                jdbcAvailable = false;
            }
        }

        public boolean isJdbcConnected() {
            return jdbcAvailable;
        }

        private void createTablesIfNotExist(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
                st.execute("CREATE TABLE IF NOT EXISTS User (" +
                        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
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
                        "date_registered DATE NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS Membership (" +
                        "membership_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INTEGER NOT NULL," +
                        "start_date DATE NOT NULL," +
                        "end_date DATE NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE)");
                st.execute("CREATE TABLE IF NOT EXISTS Exercise (" +
                        "exercise_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name VARCHAR(100) NOT NULL UNIQUE," +
                        "muscle_group VARCHAR(40) NOT NULL," +
                        "difficulty VARCHAR(20) NOT NULL," +
                        "equipment VARCHAR(50) NOT NULL," +
                        "description TEXT NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS WorkoutPlan (" +
                        "workout_plan_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INTEGER NOT NULL," +
                        "plan_type VARCHAR(20) NOT NULL," +
                        "goal VARCHAR(60) NOT NULL," +
                        "date_created DATE NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE)");
                st.execute("CREATE TABLE IF NOT EXISTS WorkoutPlanExercise (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "workout_plan_id INTEGER NOT NULL," +
                        "exercise_id INTEGER NOT NULL," +
                        "day_of_week VARCHAR(20) NOT NULL," +
                        "sets INTEGER NOT NULL," +
                        "reps VARCHAR(25) NOT NULL," +
                        "FOREIGN KEY (workout_plan_id) REFERENCES WorkoutPlan(workout_plan_id) ON DELETE CASCADE," +
                        "FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id))");
            }
        }

        private void syncIfDatabaseEmpty(Connection conn) throws SQLException {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM User")) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
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
                    String bdate = rs.getString("birthdate");
                    u.birthdate = (bdate != null && !bdate.isEmpty()) ? LocalDate.parse(bdate) : null;
                    u.gender = rs.getString("gender");
                    u.address = rs.getString("address");
                    u.emergencyContactName = rs.getString("emergency_contact_name");
                    u.emergencyContactNo = rs.getString("emergency_contact_no");
                    u.fitnessLevel = rs.getString("fitness_level");
                    String dreg = rs.getString("date_registered");
                    u.dateRegistered = (dreg != null && !dreg.isEmpty()) ? LocalDate.parse(dreg) : LocalDate.now();
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
                    m.startDate = LocalDate.parse(rs.getString("start_date"));
                    m.endDate = LocalDate.parse(rs.getString("end_date"));
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
                    wp.dateCreated = LocalDate.parse(rs.getString("date_created"));
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

            addSeedMembership(uCarlos.userId, today.minusDays(42), today.minusDays(12));
            addSeedMembership(uCarlos.userId, today.minusDays(12), today.plusDays(18));
            addSeedMembership(uAngela.userId, today.minusDays(26), today.plusDays(4));
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

            WorkoutPlan initialPlan = BusinessRules.generateRuleBasedPlan(
                    uCarlos.userId, "Weight Loss", "Beginner", 3, "Dumbbells", exercises);
            initialPlan.workoutPlanId = nextPlanId++;
            for (WorkoutPlanExercise wpe : initialPlan.exercises) {
                wpe.id = nextPlanExId++;
                wpe.workoutPlanId = initialPlan.workoutPlanId;
            }
            workoutPlans.add(initialPlan);
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
                    try (Connection conn = DriverManager.getConnection(JDBC_URL);
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
                    } catch (SQLException ignored) {}
                }
            } else {
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).userId == u.userId) {
                        users.set(i, u);
                        break;
                    }
                }
                if (jdbcAvailable) {
                    try (Connection conn = DriverManager.getConnection(JDBC_URL);
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
                    } catch (SQLException ignored) {}
                }
            }
            return u;
        }

        public void deleteUser(int userId) {
            users.removeIf(u -> u.userId == userId);
            memberships.removeIf(m -> m.userId == userId);
            workoutPlans.removeIf(wp -> wp.userId == userId);
            if (jdbcAvailable) {
                try (Connection conn = DriverManager.getConnection(JDBC_URL);
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM User WHERE user_id=?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                } catch (SQLException ignored) {}
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
                try (Connection conn = DriverManager.getConnection(JDBC_URL);
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO Membership (membership_id, user_id, start_date, end_date) VALUES (?,?,?,?)")) {
                    ps.setInt(1, m.membershipId);
                    ps.setInt(2, m.userId);
                    ps.setString(3, m.startDate.toString());
                    ps.setString(4, m.endDate.toString());
                    ps.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return m;
        }

        public void saveExercise(Exercise ex) {
            if (ex.exerciseId == 0) {
                ex.exerciseId = nextExerciseId++;
                exercises.add(ex);
                if (jdbcAvailable) {
                    try (Connection conn = DriverManager.getConnection(JDBC_URL);
                         PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO Exercise (exercise_id, name, muscle_group, difficulty, equipment, description) VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, ex.exerciseId);
                        ps.setString(2, ex.name);
                        ps.setString(3, ex.muscleGroup);
                        ps.setString(4, ex.difficulty);
                        ps.setString(5, ex.equipment);
                        ps.setString(6, ex.description);
                        ps.executeUpdate();
                    } catch (SQLException ignored) {}
                }
            } else {
                for (int i = 0; i < exercises.size(); i++) {
                    if (exercises.get(i).exerciseId == ex.exerciseId) {
                        exercises.set(i, ex);
                        break;
                    }
                }
                if (jdbcAvailable) {
                    try (Connection conn = DriverManager.getConnection(JDBC_URL);
                         PreparedStatement ps = conn.prepareStatement(
                                 "UPDATE Exercise SET name=?, muscle_group=?, difficulty=?, equipment=?, description=? WHERE exercise_id=?")) {
                        ps.setString(1, ex.name);
                        ps.setString(2, ex.muscleGroup);
                        ps.setString(3, ex.difficulty);
                        ps.setString(4, ex.equipment);
                        ps.setString(5, ex.description);
                        ps.setInt(6, ex.exerciseId);
                        ps.executeUpdate();
                    } catch (SQLException ignored) {}
                }
            }
        }

        public void deleteExercise(int exerciseId) {
            exercises.removeIf(e -> e.exerciseId == exerciseId);
            if (jdbcAvailable) {
                try (Connection conn = DriverManager.getConnection(JDBC_URL);
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM Exercise WHERE exercise_id=?")) {
                    ps.setInt(1, exerciseId);
                    ps.executeUpdate();
                } catch (SQLException ignored) {}
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
                try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
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
                } catch (SQLException ignored) {}
            }
        }
    }

    // ========================================================================
    // 4. PLAY STORE MOBILE-APP CUSTOM UI COMPONENTS (Graphics2D)
    // ========================================================================

    /**
     * Rounded elevated card container with optional gradient fill or subtle border.
     */
    private static class RoundedCardPanel extends JPanel {
        private final Color c1;
        private final Color c2;
        private final Color borderColor;
        private final int radius;

        public RoundedCardPanel(Color bg, Color border, int radius) {
            this(bg, bg, border, radius);
        }

        public RoundedCardPanel(Color gradStart, Color gradEnd, Color border, int radius) {
            this.c1 = gradStart;
            this.c2 = gradEnd;
            this.borderColor = border;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (!c1.equals(c2)) {
                g2.setPaint(new GradientPaint(0, 0, c1, w, h, c2));
            } else {
                g2.setColor(c1);
            }
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Play Store style Digital Gym Membership Pass Card (like Google Wallet / Nike Training Club).
     */
    private static class DigitalMemberPassCard extends JPanel {
        private User member;
        private Membership latest;

        public DigitalMemberPassCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(350, 0));
        }

        public void setMemberData(User m, Membership latestMembership) {
            this.member = m;
            this.latest = latestMembership;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Outer Card Background
            g2.setColor(CARD_SURFACE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);

            LocalDate today = LocalDate.now();
            boolean isActive = (latest != null && "Active".equalsIgnoreCase(latest.getComputedStatus(today)));
            long daysLeft = latest != null ? latest.getDaysRemaining(today) : -999;

            // Top Gradient Wallet Pass Banner
            Color bannerStart = (member == null) ? ELECTRIC_INDIGO : (isActive ? new Color(13, 148, 136) : new Color(190, 18, 60));
            Color bannerEnd   = (member == null) ? VIOLET_GLOW     : (isActive ? NEON_EMERALD : CORAL_RED);
            g2.setPaint(new GradientPaint(0, 0, bannerStart, w, 135, bannerEnd));
            g2.fillRoundRect(10, 10, w - 20, 135, 20, 20);

            // Subtle decorative circles on the wallet banner
            g2.setColor(new Color(255, 255, 255, 22));
            g2.fillOval(w - 95, -15, 110, 110);
            g2.fillOval(w - 55, 55, 80, 80);

            if (member == null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.drawString("DIGITAL GYM PASS CHECK-IN", 26, 40);
                g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                g2.drawString("Tap Any Member Row", 26, 78);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.setColor(new Color(235, 245, 255));
                g2.drawString("Instant Active / Expired pass check (<5s)", 26, 104);

                g2.setColor(TEXT_SECONDARY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.drawString("Select a member from the directory", 26, 190);
                g2.drawString("or type in the search bar above to", 26, 212);
                g2.drawString("inspect their live membership pass,", 26, 234);
                g2.drawString("remaining days, and emergency info.", 26, 256);
                g2.dispose();
                return;
            }

            // Avatar Circle with Initials
            g2.setColor(new Color(15, 23, 42, 190));
            g2.fillOval(24, 26, 56, 56);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(24, 26, 56, 56);
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            FontMetrics fm = g2.getFontMetrics();
            String initials = member.getInitials();
            g2.drawString(initials, 24 + (56 - fm.stringWidth(initials)) / 2, 26 + 35);

            // Member Name & Handle on Banner
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString(member.getFullName(), 92, 48);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(new Color(240, 248, 255));
            g2.drawString("@" + member.username + "  •  ID #" + member.userId, 92, 68);

            // Status Pill inside Banner
            String pillText = isActive ? "● ACTIVE — ENTRY ALLOWED" : "● EXPIRED — RENEW NOW";
            g2.setColor(new Color(11, 15, 25, 175));
            g2.fillRoundRect(24, 96, w - 48, 34, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics pfm = g2.getFontMetrics();
            g2.drawString(pillText, (w - pfm.stringWidth(pillText)) / 2, 118);

            // Progress Bar Section (Days Remaining)
            int y = 168;
            g2.setColor(TEXT_SECONDARY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("MEMBERSHIP CYCLE PROGRESS", 22, y);

            String dayLabel = (latest != null)
                    ? (daysLeft >= 0 ? daysLeft + " of 30 Days Left" : Math.abs(daysLeft) + " Days Overdue")
                    : "No Record";
            g2.setColor(isActive ? NEON_EMERALD : CORAL_RED);
            FontMetrics dfm = g2.getFontMetrics();
            g2.drawString(dayLabel, w - 22 - dfm.stringWidth(dayLabel), y);

            y += 10;
            g2.setColor(INPUT_BG);
            g2.fillRoundRect(22, y, w - 44, 12, 10, 10);
            double ratio = isActive ? Math.max(0.08, Math.min(1.0, daysLeft / 30.0)) : 1.0;
            int fillW = (int) ((w - 44) * ratio);
            g2.setPaint(new GradientPaint(22, y, bannerStart, 22 + fillW, y, bannerEnd));
            g2.fillRoundRect(22, y, fillW, 12, 10, 10);

            // Info Tiles
            y += 28;
            y = drawDetailTile(g2, 18, y, w - 36, "FITNESS LEVEL & PROFILE",
                    (member.fitnessLevel != null ? member.fitnessLevel : "Beginner") + "  •  " +
                    (member.gender != null ? member.gender : "Unspecified") +
                    (member.birthdate != null ? ("  •  Born: " + member.birthdate) : ""));

            y = drawDetailTile(g2, 18, y, w - 36, "CONTACT & ADDRESS",
                    member.contactNo + ((member.address != null && !member.address.isEmpty()) ? ("  •  " + member.address) : "  •  Address optional"));

            y = drawDetailTile(g2, 18, y, w - 36, "EMERGENCY CONTACT",
                    (member.emergencyContactName != null ? member.emergencyContactName : "N/A") +
                    " (" + (member.emergencyContactNo != null ? member.emergencyContactNo : "N/A") + ")");

            y = drawDetailTile(g2, 18, y, w - 36, "CURRENT 1-MONTH PERIOD",
                    (latest != null ? (latest.startDate + "  →  " + latest.endDate) : "No active period"));

            LocalDate[] nextRenewal = BusinessRules.calculateRenewalDates(
                    latest != null ? latest.endDate : null, today);
            drawDetailTile(g2, 18, y, w - 36, "NEXT 1-MONTH RENEWAL PREVIEW",
                    nextRenewal[0] + "  →  " + nextRenewal[1]);

            g2.dispose();
        }

        private int drawDetailTile(Graphics2D g2, int x, int y, int w, String label, String val) {
            g2.setColor(CARD_HIGHLIGHT);
            g2.fillRoundRect(x, y, w, 50, 14, 14);
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString(label, x + 14, y + 18);
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(val, x + 14, y + 38);
            return y + 58;
        }
    }


    /**
     * Play Store style Animated Workout Demonstration & Target Muscle Anatomy Card.
     * Displays:
     *   1. Pulsing Front & Back Human Muscle Anatomy Map highlighting the exact target muscle group.
     *   2. Live animated workout motion demonstration (or attached GIF/PNG image via ImageIcon).
     *   3. Machine Requirement Badge ("⚙ REQUIRES GYM MACHINE" vs "✓ NO MACHINE NEEDED").
     *   4. Coaching cues & "Attach Custom GIF / Image" button.
     */
    private class WorkoutVisualCard extends JPanel {
        private Exercise exercise;
        private int animTick = 0;
        private final javax.swing.Timer animTimer;
        private ImageIcon activeGifIcon;
        private String loadedSourceLabel = "Built-In 60FPS Engine";

        public WorkoutVisualCard() {
            setOpaque(false);
            setPreferredSize(new Dimension(385, 0));
            setLayout(new BorderLayout());

            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
            bottomBar.setOpaque(false);

            JButton btnLocalGif = createStyledButton("🖼 Local GIF / File", CARD_HIGHLIGHT, CYBER_CYAN);
            JButton btnUrlGif   = createStyledButton("🌐 Sync GIF URL (Offline Cache)", ELECTRIC_INDIGO, Color.WHITE);

            btnLocalGif.addActionListener(e -> {
                if (exercise == null) return;
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select Animated GIF or Image for " + exercise.name);
                if (fc.showOpenDialog(gymsyncapp.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        java.io.File src = fc.getSelectedFile();
                        java.io.File imgDir = new java.io.File("images");
                        if (!imgDir.exists()) imgDir.mkdirs();
                        java.io.File dest = new java.io.File(imgDir, exercise.getDefaultGifFilename());
                        java.nio.file.Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        exercise.customGifOrImagePath = dest.getAbsolutePath();
                        loadGifForExercise(exercise);
                        JOptionPane.showMessageDialog(gymsyncapp.this,
                                "Saved to local offline cache:\n" + dest.getPath() + "\nThis workout GIF will now load 100% offline!",
                                "Offline GIF Cached", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        exercise.customGifOrImagePath = fc.getSelectedFile().getAbsolutePath();
                        loadGifForExercise(exercise);
                    }
                }
            });

            btnUrlGif.addActionListener(e -> {
                if (exercise == null) return;
                String urlStr = JOptionPane.showInputDialog(gymsyncapp.this,
                        "Paste any direct .gif or .png URL (from Fitness Programer, ExerciseDB, or GitHub)\n" +
                        "to download and cache offline for '" + exercise.name + "':",
                        "Sync Online GIF to Offline Cache", JOptionPane.QUESTION_MESSAGE);
                if (urlStr == null || urlStr.trim().isEmpty()) return;
                try {
                    java.io.File imgDir = new java.io.File("images");
                    if (!imgDir.exists()) imgDir.mkdirs();
                    java.io.File dest = new java.io.File(imgDir, exercise.getDefaultGifFilename());
                    try (java.io.InputStream in = new java.net.URL(urlStr.trim()).openStream()) {
                        java.nio.file.Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    exercise.customGifOrImagePath = dest.getAbsolutePath();
                    loadGifForExercise(exercise);
                    JOptionPane.showMessageDialog(gymsyncapp.this,
                            "Downloaded and cached offline to:\n" + dest.getPath() + "\nWorks 100% offline even without Wi-Fi!",
                            "GIF Synced & Cached Offline", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(gymsyncapp.this,
                            "Could not download URL (" + ex.getMessage() + ").\nUsing built-in offline biomechanical animation instead.",
                            "Offline Fallback Active", JOptionPane.WARNING_MESSAGE);
                }
            });

            bottomBar.add(btnLocalGif);
            bottomBar.add(btnUrlGif);
            add(bottomBar, BorderLayout.SOUTH);

            animTimer = new javax.swing.Timer(75, e -> {
                animTick = (animTick + 1) % 24;
                if (isShowing()) {
                    repaint();
                }
            });
            animTimer.start();
        }

        private void loadGifForExercise(Exercise ex) {
            activeGifIcon = null;
            loadedSourceLabel = "Offline Biomechanical Engine";
            if (ex == null) return;

            String[] candidatePaths = new String[]{
                    ex.customGifOrImagePath,
                    "images/" + ex.getDefaultGifFilename(),
                    "GymSync_NetBeans_Project/images/" + ex.getDefaultGifFilename(),
                    "images/" + ex.muscleGroup.toLowerCase().replace(" ", "_") + ".gif"
            };
            for (String path : candidatePaths) {
                if (path != null && !path.isEmpty()) {
                    java.io.File f = new java.io.File(path);
                    if (f.exists() && f.length() > 0) {
                        activeGifIcon = new ImageIcon(f.getAbsolutePath());
                        activeGifIcon.setImageObserver(this);
                        loadedSourceLabel = "Offline GIF (" + f.getName() + ")";
                        break;
                    }
                }
            }
            repaint();
        }

        public void setExercise(Exercise ex) {
            this.exercise = ex;
            loadGifForExercise(ex);
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
            g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);

            if (exercise == null) {
                g2.setColor(CYBER_CYAN);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.drawString("🏋️ WORKOUT & MUSCLE TARGET PREVIEW", 22, 36);
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.drawString("Tap any workout row to view its", 22, 72);
                g2.drawString("anatomical muscle map, animated GIF,", 22, 94);
                g2.drawString("and machine requirement details.", 22, 116);
                g2.dispose();
                return;
            }

            boolean needsMachine = exercise.requiresMachine();
            Color headerStart = needsMachine ? ELECTRIC_INDIGO : new Color(5, 150, 105);
            Color headerEnd   = needsMachine ? VIOLET_GLOW     : NEON_EMERALD;

            // Top Gradient Header Banner
            g2.setPaint(new GradientPaint(0, 0, headerStart, w, 90, headerEnd));
            g2.fillRoundRect(10, 10, w - 20, 90, 18, 18);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            String title = exercise.name.length() > 30 ? exercise.name.substring(0, 28) + "…" : exercise.name;
            g2.drawString(title, 20, 36);

            String machTag = needsMachine
                    ? "⚙ MACHINE: " + exercise.equipment.replace("Machine: ", "")
                    : "✓ NO MACHINE (" + exercise.equipment.toUpperCase() + ")";
            g2.setColor(new Color(11, 15, 25, 185));
            g2.fillRoundRect(18, 48, w - 36, 26, 12, 12);
            g2.setColor(needsMachine ? AMBER_GOLD : NEON_EMERALD);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(machTag.length() > 44 ? machTag.substring(0, 42) + "…" : machTag, 28, 65);

            // Visual Canvas (210px tall)
            int canvasY = 108;
            int canvasH = 212;
            g2.setColor(BG_OBSIDIAN);
            g2.fillRoundRect(12, canvasY, w - 24, canvasH, 18, 18);
            g2.setColor(BORDER_SUBTLE);
            g2.drawRoundRect(12, canvasY, w - 24, canvasH, 18, 18);

            // If user attached a custom external GIF/PNG, show it; otherwise render crisp vector Anatomical Red Muscle Map + Exercise-Specific Motion
            if (exercise.customGifOrImagePath != null && activeGifIcon != null && activeGifIcon.getIconWidth() > 0) {
                g2.drawImage(activeGifIcon.getImage(), 18, canvasY + 6, w - 36, canvasH - 12, this);
            } else {
                double phase = Math.abs(12 - animTick) / 12.0;
                boolean pulse = (animTick % 4 < 2);

                // Left Half: Anatomical Body Map with Red Primary & Orange Secondary Muscle Highlights (like Fitness Programer)
                g2.setColor(CORAL_RED);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("● PRIMARY MUSCLE MAP", 24, canvasY + 18);
                drawAnatomicalMuscleMap(g2, 96, canvasY + 108, exercise.muscleGroup, pulse);

                g2.setColor(BORDER_SUBTLE);
                g2.drawLine(w / 2 - 4, canvasY + 10, w / 2 - 4, canvasY + canvasH - 26);

                // Right Half: Exercise-Specific Biomechanical Animation
                g2.setColor(CYBER_CYAN);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("⚡ LIVE WORKOUT MOTION", w / 2 + 8, canvasY + 18);
                drawExerciseSpecificMotion(g2, (w * 3) / 4 - 2, canvasY + 105, exercise, phase);

                // Legend Strip at bottom of visual canvas
                g2.setColor(BG_SURFACE);
                g2.fillRoundRect(16, canvasY + canvasH - 24, w - 32, 20, 8, 8);
                g2.setColor(CORAL_RED);
                g2.fillRect(24, canvasY + canvasH - 18, 8, 8);
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString("Primary", 36, canvasY + canvasH - 10);

                g2.setColor(AMBER_GOLD);
                g2.fillRect(88, canvasY + canvasH - 18, 8, 8);
                g2.setColor(TEXT_SECONDARY);
                g2.drawString("Secondary", 100, canvasY + canvasH - 10);

                g2.setColor(NEON_EMERALD);
                g2.drawString("● " + loadedSourceLabel, 175, canvasY + canvasH - 10);
            }

            // Primary & Secondary Muscles Tile
            int ty = canvasY + canvasH + 10;
            g2.setColor(CARD_HIGHLIGHT);
            g2.fillRoundRect(12, ty, w - 24, 52, 14, 14);
            g2.setColor(CORAL_RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("PRIMARY TARGET: " + exercise.getPrimaryMuscles().toUpperCase(), 22, ty + 18);
            g2.setColor(AMBER_GOLD);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("Secondary: " + exercise.getSecondaryMuscles(), 22, ty + 38);

            // Step-by-Step Cues Tile
            ty += 60;
            g2.setColor(CARD_HIGHLIGHT);
            g2.fillRoundRect(12, ty, w - 24, 78, 14, 14);
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("FORM & MACHINE SETUP CUES (" + exercise.difficulty.toUpperCase() + ")", 22, ty + 16);
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            drawWrappedText(g2, exercise.description, 22, ty + 34, w - 44, 15);

            g2.dispose();
        }

        private void drawAnatomicalMuscleMap(Graphics2D g2, int cx, int cy, String mg, boolean pulse) {
            Color baseBody = new Color(45, 58, 78);
            Color outline  = new Color(148, 163, 184);
            Color primCol  = pulse ? new Color(244, 63, 94) : new Color(225, 29, 72); // Glowing Red Primary Muscle
            Color secCol   = new Color(251, 146, 60, 210);                             // Orange Secondary Muscle

            String m = mg != null ? mg.toLowerCase() : "";

            // Head & Neck
            g2.setColor(baseBody);
            g2.fillOval(cx - 11, cy - 68, 22, 24);
            g2.setColor(outline);
            g2.drawOval(cx - 11, cy - 68, 22, 24);
            g2.setColor(baseBody);
            g2.fillRect(cx - 4, cy - 44, 8, 7);

            // Deltoids (Shoulders)
            Color deltCol = m.contains("shoulder") ? primCol : ((m.contains("chest") || m.contains("back") || m.contains("full")) ? secCol : baseBody);
            g2.setColor(deltCol);
            g2.fillOval(cx - 33, cy - 38, 16, 18);
            g2.fillOval(cx + 17, cy - 38, 16, 18);

            // Torso Base
            g2.setColor(baseBody);
            g2.fillRoundRect(cx - 21, cy - 37, 42, 48, 10, 10);

            // Back Lats (V-Taper Wings)
            if (m.contains("back") || m.contains("full")) {
                g2.setColor(primCol);
                int[] lx = {cx - 21, cx - 29, cx - 18};
                int[] ly = {cy - 32, cy - 12, cy + 4};
                g2.fillPolygon(lx, ly, 3);
                int[] rx = {cx + 21, cx + 29, cx + 18};
                int[] ry = {cy - 32, cy - 12, cy + 4};
                g2.fillPolygon(rx, ry, 3);
                g2.fillRoundRect(cx - 17, cy - 34, 34, 24, 6, 6);
            }

            // Pectorals (Chest)
            Color chestCol = (m.contains("chest") || m.contains("full")) ? primCol : (m.contains("shoulder") ? secCol : new Color(62, 76, 99));
            g2.setColor(chestCol);
            g2.fillRoundRect(cx - 19, cy - 35, 17, 17, 5, 5);
            g2.fillRoundRect(cx + 2, cy - 35, 17, 17, 5, 5);

            // Core / 6-Pack Abs
            Color abCol = (m.contains("core") || m.contains("full")) ? primCol : ((m.contains("leg") || m.contains("back")) ? secCol : new Color(55, 68, 90));
            g2.setColor(abCol);
            for (int r = 0; r < 3; r++) {
                int ay = cy - 15 + r * 8;
                g2.fillRoundRect(cx - 11, ay, 9, 6, 3, 3);
                g2.fillRoundRect(cx + 2, ay, 9, 6, 3, 3);
            }

            // Arms (Biceps & Triceps)
            Color armCol = m.contains("arm") ? primCol : ((m.contains("chest") || m.contains("back") || m.contains("shoulder") || m.contains("full")) ? secCol : baseBody);
            g2.setColor(armCol);
            g2.fillRoundRect(cx - 35, cy - 20, 11, 34, 6, 6);
            g2.fillRoundRect(cx + 24, cy - 20, 11, 34, 6, 6);

            // Legs (Quads, Hamstrings & Calves)
            Color legCol = (m.contains("leg") || m.contains("full")) ? primCol : baseBody;
            g2.setColor(legCol);
            g2.fillRoundRect(cx - 19, cy + 12, 16, 32, 6, 6);
            g2.fillRoundRect(cx + 3, cy + 12, 16, 32, 6, 6);
            g2.fillRoundRect(cx - 17, cy + 45, 13, 26, 5, 5);
            g2.fillRoundRect(cx + 4, cy + 45, 13, 26, 5, 5);
        }

        private void drawExerciseSpecificMotion(Graphics2D g2, int mx, int my, Exercise ex, double phase) {
            int off = (int) (phase * 26);
            String n = ex.name != null ? ex.name.toLowerCase() : "";
            boolean isMach = ex.requiresMachine();

            if (isMach) {
                g2.setColor(new Color(71, 85, 105));
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawRoundRect(mx - 58, my - 60, 116, 112, 10, 10);
                // Animated selectorized weight stack on left of machine frame
                for (int p = 0; p < 5; p++) {
                    int py = my + 34 - p * 9 - (p >= 2 ? off / 2 : 0);
                    g2.setColor(p >= 2 ? AMBER_GOLD : new Color(71, 85, 105));
                    g2.fillRoundRect(mx - 52, py, 18, 6, 3, 3);
                }
            }

            int cx = mx + (isMach ? 10 : 0);
            g2.setStroke(new BasicStroke(4f));

            if (n.contains("pulldown") || n.contains("pull-up")) {
                int barY = my - 44 + off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 8, my - 14, 16, 16);
                g2.setColor(CORAL_RED);
                g2.fillRoundRect(cx - 13, my + 2, 26, 30, 6, 6);
                g2.drawLine(cx - 12, my + 6, cx - 24, barY);
                g2.drawLine(cx + 12, my + 6, cx + 24, barY);
                g2.setColor(CYBER_CYAN);
                g2.drawLine(cx - 34, barY, cx + 34, barY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx, my - 58, cx, barY);
            } else if (n.contains("row") && !n.contains("ergometer")) {
                int hx = cx - 24 + off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx + 4, my - 24, 15, 15);
                g2.setColor(CORAL_RED);
                g2.fillRoundRect(cx, my - 8, 20, 30, 6, 6);
                g2.setColor(TEXT_SECONDARY);
                g2.drawLine(cx + 4, my + 22, cx - 22, my + 26);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx + 4, my + 2, hx, my + 4);
                g2.setColor(CYBER_CYAN);
                g2.drawLine(cx - 36, my + 4, hx, my + 4);
            } else if (n.contains("leg press") || n.contains("hack squat")) {
                int sx = cx + 6 + off / 2;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 28, my + 2, 14, 14);
                g2.setColor(TEXT_SECONDARY);
                g2.drawLine(cx - 22, my + 16, cx - 8, my + 32);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx - 8, my + 32, cx, my + 10);
                g2.drawLine(cx, my + 10, sx, my - 6);
                g2.setColor(CYBER_CYAN);
                g2.drawLine(sx - 8, my - 20, sx + 8, my + 10);
            } else if (n.contains("extension") || n.contains("hamstring")) {
                int py = my + 32 - off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 14, my - 26, 14, 14);
                g2.setColor(TEXT_SECONDARY);
                g2.fillRoundRect(cx - 16, my - 10, 16, 24, 5, 5);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx - 4, my + 12, cx + 12, my + 12);
                g2.drawLine(cx + 12, my + 12, cx + 24, py);
                g2.setColor(NEON_EMERALD);
                g2.fillOval(cx + 18, py - 5, 10, 10);
            } else if (n.contains("squat") || n.contains("lunge") || n.contains("calf")) {
                int hy = my - 8 + (n.contains("calf") ? -off / 3 : off / 2);
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 8, hy - 32, 16, 16);
                g2.setColor(TEXT_SECONDARY);
                g2.fillRoundRect(cx - 11, hy - 15, 22, 24, 5, 5);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx - 5, hy + 10, cx - 16, hy + 26);
                g2.drawLine(cx + 5, hy + 10, cx + 16, hy + 26);
                g2.drawLine(cx - 16, hy + 26, cx - 8, my + 44);
                g2.drawLine(cx + 16, hy + 26, cx + 8, my + 44);
            } else if (n.contains("curl") || n.contains("pushdown") || n.contains("tricep")) {
                int hy = n.contains("curl") ? (my + 20 - off) : (my - 6 + off);
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 8, my - 30, 16, 16);
                g2.setColor(TEXT_SECONDARY);
                g2.fillRoundRect(cx - 12, my - 12, 24, 30, 6, 6);
                g2.setColor(CORAL_RED);
                g2.drawLine(cx - 12, my - 6, cx - 22, hy);
                g2.drawLine(cx + 12, my - 6, cx + 22, hy);
                g2.setColor(NEON_EMERALD);
                g2.drawLine(cx - 28, hy, cx + 28, hy);
            } else if (n.contains("treadmill") || n.contains("ergometer")) {
                int stride = (int) ((phase - 0.5) * 24);
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 8, my - 32, 16, 16);
                g2.setColor(CORAL_RED);
                g2.fillRoundRect(cx - 10, my - 15, 20, 26, 5, 5);
                g2.drawLine(cx - 4, my + 11, cx - 14 + stride, my + 38);
                g2.drawLine(cx + 4, my + 11, cx + 14 - stride, my + 38);
                g2.setColor(CYBER_CYAN);
                g2.drawLine(cx - 32, my + 42, cx + 32, my + 42);
            } else {
                // Pressing / Fly / Core movements
                int by = my - 12 - off;
                g2.setColor(TEXT_PRIMARY);
                g2.fillOval(cx - 9, my - 14, 18, 18);
                g2.setColor(CORAL_RED);
                g2.fillRoundRect(cx - 14, my + 6, 28, 32, 6, 6);
                g2.drawLine(cx - 14, my + 12, cx - 26, by);
                g2.drawLine(cx + 14, my + 12, cx + 26, by);
                g2.setColor(CYBER_CYAN);
                g2.drawLine(cx - 36, by, cx + 36, by);
                g2.setColor(NEON_EMERALD);
                g2.fillRoundRect(cx - 40, by - 8, 7, 16, 3, 3);
                g2.fillRoundRect(cx + 33, by - 8, 7, 16, 3, 3);
            }

            // Rep contraction meter bar
            g2.setColor(INPUT_BG);
            g2.fillRoundRect(mx - 48, my + 56, 96, 10, 6, 6);
            g2.setColor(NEON_EMERALD);
            g2.fillRoundRect(mx - 48, my + 56, Math.max(12, (int) (96 * (1.0 - phase))), 10, 6, 6);
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
            if (line.length() > 0) {
                g2.drawString(line.toString(), x, curY);
            }
        }
    }

    private static class MachineBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            final String text = value != null ? value.toString() : "";
            final boolean isMachine = text.startsWith("⚙") || text.toLowerCase().contains("machine");
            JLabel lbl = new JLabel(text, SwingConstants.LEFT) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? TABLE_SELECT : (row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW));
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    g2.setColor(isMachine ? new Color(99, 102, 241, 55) : new Color(16, 185, 129, 40));
                    g2.fillRoundRect(6, 5, getWidth() - 12, getHeight() - 10, 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setBorder(new EmptyBorder(0, 14, 0, 10));
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(isMachine ? new Color(165, 180, 252) : NEON_EMERALD);
            return lbl;
        }
    }

    // ========================================================================
    // 5. NAVIGATION & PLAY STORE LOGIN SCREEN
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
        this.currentUser = user;
        rootContainer.removeAll();
        if ("Admin".equalsIgnoreCase(user.role)) {
            rootContainer.add(buildAdminDashboard(), "DASHBOARD");
        } else if ("Staff".equalsIgnoreCase(user.role)) {
            rootContainer.add(buildStaffDashboard(), "DASHBOARD");
        } else {
            rootContainer.add(buildMemberDashboard(), "DASHBOARD");
        }
        rootCardLayout.show(rootContainer, "DASHBOARD");
        rootContainer.revalidate();
        rootContainer.repaint();
    }

    private JPanel buildLoginPanel() {
        // Custom painted dark fitness background with subtle ambient glow circles
        JPanel outer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, BG_OBSIDIAN, w, h, new Color(17, 24, 39)));
                g2.fillRect(0, 0, w, h);

                // Ambient neon glow orbs like Play Store fitness apps
                g2.setColor(new Color(99, 102, 241, 28));
                g2.fillOval(-120, -120, 480, 480);
                g2.setColor(new Color(16, 185, 129, 24));
                g2.fillOval(w - 380, h - 380, 500, 500);
                g2.dispose();
            }
        };

        RoundedCardPanel phoneCard = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 32);
        phoneCard.setLayout(new BoxLayout(phoneCard, BoxLayout.Y_AXIS));
        phoneCard.setBorder(new EmptyBorder(28, 36, 28, 36));
        phoneCard.setPreferredSize(new Dimension(440, 580));

        // Top Gradient App Hero Banner inside Login Card
        RoundedCardPanel heroBanner = new RoundedCardPanel(ELECTRIC_INDIGO, NEON_EMERALD, null, 24);
        heroBanner.setLayout(new BoxLayout(heroBanner, BoxLayout.Y_AXIS));
        heroBanner.setBorder(new EmptyBorder(20, 20, 20, 20));
        heroBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel badgeLbl = new JLabel("⚡ FITNESS & GYM MANAGEMENT APP");
        badgeLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        badgeLbl.setForeground(new Color(224, 242, 254));
        badgeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("GYMSYNC");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Train Smarter • Instant Check-In • Custom Plans");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(240, 253, 244));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        heroBanner.add(badgeLbl);
        heroBanner.add(Box.createVerticalStrut(4));
        heroBanner.add(title);
        heroBanner.add(Box.createVerticalStrut(4));
        heroBanner.add(subtitle);

        // Input Form
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 8));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(22, 0, 18, 0));

        JLabel userLbl = new JLabel("USERNAME");
        userLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        userLbl.setForeground(TEXT_SECONDARY);
        JTextField userField = createDarkTextField("");

        JLabel passLbl = new JLabel("PASSWORD");
        passLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        passLbl.setForeground(TEXT_SECONDARY);
        JPasswordField passField = createDarkPasswordField();

        form.add(userLbl);
        form.add(userField);
        form.add(passLbl);
        form.add(passField);

        JButton loginBtn = createGradientButton("SIGN IN TO GYMSYNC  →", NEON_EMERALD, CYBER_CYAN);
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        ActionListener doLogin = e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter both your username and password.",
                        "Missing Credentials", JOptionPane.WARNING_MESSAGE);
                return;
            }
            User authenticated = repo.authenticate(u, p);
            if (authenticated == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password. Please try again.",
                        "Sign-In Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            routeToRoleDashboard(authenticated);
        };
        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);

        // One-Tap Play Store Role Switcher Chips
        RoundedCardPanel demoBox = new RoundedCardPanel(BG_SURFACE, BORDER_SUBTLE, 18);
        demoBox.setLayout(new BorderLayout(0, 8));
        demoBox.setBorder(new EmptyBorder(12, 14, 12, 14));
        demoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        JLabel demoTitle = new JLabel("1-TAP DEMO PROFILES (QA INSTANT SIGN-IN):");
        demoTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        demoTitle.setForeground(TEXT_SECONDARY);

        JPanel demoBtns = new JPanel(new GridLayout(1, 3, 8, 0));
        demoBtns.setOpaque(false);
        JButton btnAdminDemo = createStyledButton("🛡 Admin", ELECTRIC_INDIGO, Color.WHITE);
        JButton btnStaffDemo = createStyledButton("⚡ Staff", CYBER_CYAN, Color.WHITE);
        JButton btnMemberDemo = createStyledButton("🔥 Member", NEON_EMERALD, Color.WHITE);
        for (JButton b : new JButton[]{btnAdminDemo, btnStaffDemo, btnMemberDemo}) {
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setBorder(new EmptyBorder(8, 6, 8, 6));
        }
        btnAdminDemo.addActionListener(e -> { userField.setText("admin"); passField.setText("admin123"); doLogin.actionPerformed(null); });
        btnStaffDemo.addActionListener(e -> { userField.setText("staff"); passField.setText("staff123"); doLogin.actionPerformed(null); });
        btnMemberDemo.addActionListener(e -> { userField.setText("carlos_m"); passField.setText("member123"); doLogin.actionPerformed(null); });
        demoBtns.add(btnAdminDemo);
        demoBtns.add(btnStaffDemo);
        demoBtns.add(btnMemberDemo);

        demoBox.add(demoTitle, BorderLayout.NORTH);
        demoBox.add(demoBtns, BorderLayout.CENTER);

        phoneCard.add(heroBanner);
        phoneCard.add(form);
        phoneCard.add(loginBtn);
        phoneCard.add(Box.createVerticalStrut(20));
        phoneCard.add(demoBox);

        outer.add(phoneCard);
        return outer;
    }

    // ========================================================================
    // 6. PLAY STORE APP SHELL WITH PILL NAVIGATION BAR (Admin, Staff, Member)
    // ========================================================================

    private JPanel buildAppShell(String consoleSubtitle, LinkedHashMap<String, JPanel> navScreens) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG_OBSIDIAN);

        CardLayout contentLayout = new CardLayout();
        JPanel contentArea = new JPanel(contentLayout);
        contentArea.setOpaque(false);

        // Top App Bar + Modern Pill Navigation Bar
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setBackground(BG_SURFACE);
        headerWrapper.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_SUBTLE));

        // Row 1: Brand Title + User Profile Pill + Sign Out
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setBorder(new EmptyBorder(14, 22, 10, 22));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        JLabel logoBadge = new JLabel(" ⚡ GYMSYNC ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ELECTRIC_INDIGO, getWidth(), getHeight(), NEON_EMERALD));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoBadge.setFont(new Font("SansSerif", Font.BOLD, 15));
        logoBadge.setForeground(Color.WHITE);
        logoBadge.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel subTitleLbl = new JLabel(consoleSubtitle);
        subTitleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        subTitleLbl.setForeground(TEXT_PRIMARY);

        brandBox.add(logoBadge);
        brandBox.add(subTitleLbl);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightBox.setOpaque(false);

        String dbTag = repo.isJdbcConnected() ? "● SQLite JDBC Live" : "● Instant Memory DB";
        JLabel userChip = new JLabel(currentUser.getFullName() + "  •  " + currentUser.role.toUpperCase() + "  (" + dbTag + ")");
        userChip.setFont(new Font("SansSerif", Font.BOLD, 12));
        userChip.setForeground(TEXT_SECONDARY);

        JButton logoutBtn = createStyledButton("Sign Out", CORAL_RED, Color.WHITE);
        logoutBtn.addActionListener(e -> showLoginScreen());

        rightBox.add(userChip);
        rightBox.add(logoutBtn);

        topRow.add(brandBox, BorderLayout.WEST);
        topRow.add(rightBox, BorderLayout.EAST);

        // Row 2: Play Store Pill Navigation Tabs
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        navBar.setOpaque(false);
        navBar.setBorder(new EmptyBorder(0, 16, 8, 16));

        List<JButton> navButtons = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, JPanel> entry : navScreens.entrySet()) {
            String key = entry.getKey();
            contentArea.add(entry.getValue(), key);

            final int tabIdx = idx;
            JButton pillBtn = new JButton(key) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean active = Boolean.TRUE.equals(getClientProperty("active"));
                    if (active) {
                        g2.setPaint(new GradientPaint(0, 0, ELECTRIC_INDIGO, getWidth(), getHeight(), CYBER_CYAN));
                    } else if (getModel().isRollover()) {
                        g2.setColor(CARD_HIGHLIGHT);
                    } else {
                        g2.setColor(CARD_SURFACE);
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pillBtn.putClientProperty("active", idx == 0);
            pillBtn.setForeground(idx == 0 ? Color.WHITE : TEXT_SECONDARY);
            pillBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
            pillBtn.setContentAreaFilled(false);
            pillBtn.setBorderPainted(false);
            pillBtn.setFocusPainted(false);
            pillBtn.setBorder(new EmptyBorder(9, 18, 9, 18));
            pillBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            pillBtn.addActionListener(e -> {
                for (int i = 0; i < navButtons.size(); i++) {
                    JButton b = navButtons.get(i);
                    boolean isThis = (i == tabIdx);
                    b.putClientProperty("active", isThis);
                    b.setForeground(isThis ? Color.WHITE : TEXT_SECONDARY);
                    b.repaint();
                }
                contentLayout.show(contentArea, key);
            });

            navButtons.add(pillBtn);
            navBar.add(pillBtn);
            idx++;
        }

        headerWrapper.add(topRow, BorderLayout.NORTH);
        headerWrapper.add(navBar, BorderLayout.SOUTH);

        shell.add(headerWrapper, BorderLayout.NORTH);
        shell.add(contentArea, BorderLayout.CENTER);
        return shell;
    }

    private JPanel buildStaffDashboard() {
        LinkedHashMap<String, JPanel> screens = new LinkedHashMap<>();
        screens.put("⚡ Front Desk Check-In", buildFrontDeskMemberTab(false));
        screens.put("🔔 Expiry Alerts", buildAlertListTab());
        screens.put("🏋️ Workout Coach", buildStaffWorkoutHelperTab());
        return buildAppShell("Front Desk Staff App", screens);
    }

    private JPanel buildAdminDashboard() {
        LinkedHashMap<String, JPanel> screens = new LinkedHashMap<>();
        screens.put("⚡ Member Check-In & Renewals", buildFrontDeskMemberTab(true));
        screens.put("📊 Analytics & Reports", buildReportsTab());
        screens.put("🏋️ Exercise Library", buildExerciseLibraryTab());
        screens.put("👥 User Accounts", buildAdminUserManagementTab());
        screens.put("🔔 Expiry Alerts", buildAlertListTab());
        return buildAppShell("Administrator Control Center", screens);
    }

    private JPanel buildMemberDashboard() {
        LinkedHashMap<String, JPanel> screens = new LinkedHashMap<>();
        screens.put("🪪 My Digital Gym Pass", buildMemberStatusTab(currentUser));
        screens.put("🔥 My Workout Planner", buildMemberWorkoutTab(currentUser));
        screens.put("📚 Exercise Library", buildReadOnlyExerciseBrowser());
        return buildAppShell("Member Fitness Portal", screens);
    }

    // ========================================================================
    // 7. SCREEN BUILDERS (Play Store Dark Cards, Pill Badges, Wallet Pass)
    // ========================================================================

    private JPanel buildFrontDeskMemberTab(boolean allowDelete) {
        JPanel container = new JPanel(new BorderLayout(16, 16));
        container.setBackground(BG_OBSIDIAN);
        container.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Search & Action Pill Bar
        RoundedCardPanel topBar = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 20);
        topBar.setLayout(new BorderLayout(12, 0));
        topBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);
        JLabel searchLbl = new JLabel("🔍 Instant Check-In Search:");
        searchLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        searchLbl.setForeground(TEXT_PRIMARY);
        JTextField searchField = createDarkTextField("");
        searchField.setColumns(22);
        searchPanel.add(searchLbl);
        searchPanel.add(searchField);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);
        JButton btnRegister = createGradientButton("+ New Member (1-Mo)", NEON_EMERALD, new Color(5, 150, 105));
        JButton btnRenew = createGradientButton("↻ Renew +1 Month", ELECTRIC_INDIGO, CYBER_CYAN);
        JButton btnEdit = createStyledButton("✎ Edit Profile", CARD_HIGHLIGHT, TEXT_PRIMARY);
        actionBtns.add(btnRegister);
        actionBtns.add(btnRenew);
        actionBtns.add(btnEdit);
        if (allowDelete) {
            JButton btnDel = createStyledButton("🗑 Delete", CORAL_RED, Color.WHITE);
            actionBtns.add(btnDel);
        }

        topBar.add(searchPanel, BorderLayout.WEST);
        topBar.add(actionBtns, BorderLayout.EAST);

        // Center Directory Table + Right Digital Gym Pass Card
        String[] cols = {"ID", "Username", "Member Name", "Contact No.", "Level",
                "Start Date", "End Date", "Balance", "Pass Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(8).setCellRenderer(new StatusBadgeRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        DigitalMemberPassCard passCard = new DigitalMemberPassCard();

        Runnable refreshTable = () -> {
            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (User m : repo.getMembers()) {
                Membership latest = repo.getLatestMembership(m.userId);
                String startStr = latest != null ? latest.startDate.toString() : "N/A";
                String endStr   = latest != null ? latest.endDate.toString() : "N/A";
                String status   = latest != null ? latest.getComputedStatus(today) : "Expired";
                long daysLeft   = latest != null ? latest.getDaysRemaining(today) : -999;
                String daysStr  = latest != null
                        ? (daysLeft >= 0 ? daysLeft + "d left" : Math.abs(daysLeft) + "d overdue")
                        : "None";
                model.addRow(new Object[]{
                        m.userId, "@" + m.username, m.getFullName(), m.contactNo,
                        m.fitnessLevel != null ? m.fitnessLevel : "Beginner",
                        startStr, endStr, daysStr, status
                });
            }
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
        };

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String q = searchField.getText().trim();
                if (q.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
                }
                if (table.getRowCount() > 0) {
                    table.setRowSelectionInterval(0, 0);
                } else {
                    passCard.setMemberData(null, null);
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = table.convertRowIndexToModel(viewRow);
            int userId = (int) model.getValueAt(modelRow, 0);
            User selected = repo.getMembers().stream().filter(u -> u.userId == userId).findFirst().orElse(null);
            if (selected == null) return;
            Membership latest = repo.getLatestMembership(userId);
            passCard.setMemberData(selected, latest);
        });

        refreshTable.run();

        btnRegister.addActionListener(e -> showMemberRegistrationDialog(null, refreshTable));

        btnEdit.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a member from the directory first.");
                return;
            }
            int userId = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
            User target = repo.getMembers().stream().filter(u -> u.userId == userId).findFirst().orElse(null);
            if (target != null) showMemberRegistrationDialog(target, refreshTable);
        });

        btnRenew.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a member to renew.");
                return;
            }
            int userId = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
            User target = repo.getMembers().stream().filter(u -> u.userId == userId).findFirst().orElse(null);
            if (target == null) return;

            Membership latest = repo.getLatestMembership(userId);
            LocalDate today = LocalDate.now();
            String currentStatus = latest != null ? latest.getComputedStatus(today) : "Expired";
            LocalDate[] renewalDates = BusinessRules.calculateRenewalDates(
                    latest != null ? latest.endDate : null, today);

            String ruleExplanation = "Active".equalsIgnoreCase(currentStatus)
                    ? "Member is currently ACTIVE (expires " + latest.endDate + ").\n" +
                      "New 1-month period starts on current end date."
                    : "Member is currently EXPIRED.\n" +
                      "New 1-month period starts today (" + today + ").";

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Confirm 1-Month Membership Renewal for " + target.getFullName() + "?\n\n" +
                    ruleExplanation + "\n\n" +
                    "• New Start Date : " + renewalDates[0] + "\n" +
                    "• New End Date   : " + renewalDates[1] + " (1 Month)",
                    "Confirm Monthly Renewal",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                repo.addMembership(userId, renewalDates[0], renewalDates[1]);
                refreshTable.run();
                JOptionPane.showMessageDialog(this,
                        "Membership renewed for " + target.getFullName() + "!\n" +
                        "Active through: " + renewalDates[1],
                        "Renewal Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        if (allowDelete && actionBtns.getComponentCount() >= 4) {
            JButton btnDel = (JButton) actionBtns.getComponent(3);
            btnDel.addActionListener(e -> {
                int viewRow = table.getSelectedRow();
                if (viewRow < 0) {
                    JOptionPane.showMessageDialog(this, "Please select a member to delete.");
                    return;
                }
                int userId = (int) model.getValueAt(table.convertRowIndexToModel(viewRow), 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete Member #" + userId + " and all associated memberships and workout plans?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    repo.deleteUser(userId);
                    refreshTable.run();
                }
            });
        }

        container.add(topBar, BorderLayout.NORTH);
        container.add(wrapDarkScrollPane(table), BorderLayout.CENTER);
        container.add(passCard, BorderLayout.EAST);
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

        JPanel form = new JPanel(new GridLayout(11, 2, 10, 10));
        form.setBackground(BG_OBSIDIAN);
        form.setBorder(new EmptyBorder(18, 24, 10, 24));

        JTextField tfUsername  = createDarkTextField(isNew ? "" : existing.username);
        JPasswordField pfPass  = createDarkPasswordField();
        JTextField tfFirst     = createDarkTextField(isNew ? "" : existing.firstName);
        JTextField tfLast      = createDarkTextField(isNew ? "" : existing.lastName);
        JTextField tfContact   = createDarkTextField(isNew ? "09" : existing.contactNo);
        JTextField tfBirthdate = createDarkTextField(isNew ? "" : (existing.birthdate != null ? existing.birthdate.toString() : ""));
        JComboBox<String> cbGender = createDarkComboBox(new String[]{"Male", "Female", "Other", "Prefer not to say"});
        if (!isNew && existing.gender != null) cbGender.setSelectedItem(existing.gender);
        JTextField tfAddress   = createDarkTextField(isNew ? "" : (existing.address != null ? existing.address : ""));
        JTextField tfEmName    = createDarkTextField(isNew ? "" : (existing.emergencyContactName != null ? existing.emergencyContactName : ""));
        JTextField tfEmPhone   = createDarkTextField(isNew ? "09" : (existing.emergencyContactNo != null ? existing.emergencyContactNo : ""));
        JComboBox<String> cbFitness = createDarkComboBox(new String[]{"Beginner", "Intermediate", "Advanced"});
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
                ? "⚡ Automatic 1-Month Pass will be activated: " + start + " → " + end
                : "ℹ Updating member profile details.");
        membershipNote.setFont(new Font("SansSerif", Font.BOLD, 12));
        membershipNote.setForeground(NEON_EMERALD);
        membershipNote.setBorder(new EmptyBorder(4, 24, 4, 24));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(BG_OBSIDIAN);
        JButton btnCancel = createStyledButton("Cancel", CARD_HIGHLIGHT, TEXT_PRIMARY);
        JButton btnSave = createGradientButton(isNew ? "Activate Member Pass" : "Save Changes", NEON_EMERALD, CYBER_CYAN);
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

            if (uname.isEmpty() || fn.isEmpty() || ln.isEmpty() || contact.isEmpty()) {
                JOptionPane.showMessageDialog(dlg,
                        "Username, First Name, Last Name, and Contact No. are required (*).",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (emName.isEmpty() || emPhone.isEmpty() || emPhone.equals("09")) {
                JOptionPane.showMessageDialog(dlg,
                        "Emergency Contact Name and Phone No. are mandatory (*) for member safety in case of a gym accident.",
                        "Emergency Contact Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isNew && rawPass.length() < 4) {
                JOptionPane.showMessageDialog(dlg,
                        "Password is required and must be at least 4 characters.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (repo.usernameExists(uname, isNew ? 0 : existing.userId)) {
                JOptionPane.showMessageDialog(dlg,
                        "Username '" + uname + "' is already taken.",
                        "Duplicate Username", JOptionPane.ERROR_MESSAGE);
                return;
            }
            LocalDate parsedBirthdate = null;
            if (!bdateStr.isEmpty()) {
                try {
                    parsedBirthdate = LocalDate.parse(bdateStr);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(dlg,
                            "If provided, Birthdate must be in YYYY-MM-DD format (e.g., 2002-05-15), or leave it blank.",
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
            if (isNew) {
                repo.addMembership(u.userId, start, end);
            }
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
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        RoundedCardPanel banner = new RoundedCardPanel(new Color(153, 27, 27), CORAL_RED, null, 20);
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(16, 22, 16, 22));

        JLabel info = new JLabel("🔔 Automatic Expiry & Renewal Radar — Expired Members & Passes Expiring in ≤ 7 Days");
        info.setFont(new Font("SansSerif", Font.BOLD, 15));
        info.setForeground(Color.WHITE);

        String[] cols = {"ID", "Member Name", "Contact No.", "Emergency Contact", "End Date", "Alert Status", "Timeline"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);
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

        JButton btnRefresh = createStyledButton("↻ Refresh Radar", BG_OBSIDIAN, Color.WHITE);
        btnRefresh.addActionListener(e -> loadAlerts.run());

        banner.add(info, BorderLayout.WEST);
        banner.add(btnRefresh, BorderLayout.EAST);

        panel.add(banner, BorderLayout.NORTH);
        panel.add(wrapDarkScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminUserManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] cols = {"User ID", "Username", "Role", "First Name", "Last Name", "Contact No.", "Date Registered"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        Runnable refresh = () -> {
            model.setRowCount(0);
            for (User u : repo.getAllUsers()) {
                model.addRow(new Object[]{
                        u.userId, "@" + u.username, u.role, u.firstName, u.lastName, u.contactNo, u.dateRegistered
                });
            }
        };
        refresh.run();

        RoundedCardPanel top = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 20);
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JButton btnAddStaff = createGradientButton("+ Add Admin / Staff Account", ELECTRIC_INDIGO, VIOLET_GLOW);
        JButton btnDeleteUser = createStyledButton("🗑 Delete Selected Account", CORAL_RED, Color.WHITE);
        top.add(btnAddStaff);
        top.add(btnDeleteUser);

        btnAddStaff.addActionListener(e -> {
            JTextField tfUser = new JTextField();
            JPasswordField pfPass = new JPasswordField();
            JComboBox<String> cbRole = new JComboBox<>(new String[]{"Staff", "Admin"});
            JTextField tfFirst = new JTextField();
            JTextField tfLast = new JTextField();
            JTextField tfContact = new JTextField("09");

            Object[] fields = {
                    "Username *:", tfUser,
                    "Password *:", pfPass,
                    "Role *:", cbRole,
                    "First Name *:", tfFirst,
                    "Last Name *:", tfLast,
                    "Contact No. *:", tfContact
            };
            int res = JOptionPane.showConfirmDialog(this, fields, "Create Staff / Admin Account", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String un = tfUser.getText().trim();
                String pw = new String(pfPass.getPassword()).trim();
                if (un.isEmpty() || pw.isEmpty() || tfFirst.getText().trim().isEmpty() || tfLast.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }
                if (repo.usernameExists(un, 0)) {
                    JOptionPane.showMessageDialog(this, "Username already exists.");
                    return;
                }
                User u = new User();
                u.username = un;
                u.role = (String) cbRole.getSelectedItem();
                u.firstName = tfFirst.getText().trim();
                u.lastName = tfLast.getText().trim();
                u.contactNo = tfContact.getText().trim();
                u.dateRegistered = LocalDate.now();
                repo.saveUser(u, pw);
                refresh.run();
            }
        });

        btnDeleteUser.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int id = (int) model.getValueAt(row, 0);
            if (id == currentUser.userId) {
                JOptionPane.showMessageDialog(this, "You cannot delete your own active Admin account.");
                return;
            }
            repo.deleteUser(id);
            refresh.run();
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapDarkScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildExerciseLibraryTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] cols = {"ID", "Workout Name", "Target Muscle", "Level", "Machine / Equipment Indicator", "Form Cues"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new MachineBadgeRenderer());

        WorkoutVisualCard visualCard = new WorkoutVisualCard();

        JCheckBox chkShowMachines = new JCheckBox("⚙ Show Gym Machine Workouts", true);
        chkShowMachines.setOpaque(false);
        chkShowMachines.setForeground(CYBER_CYAN);
        chkShowMachines.setFont(new Font("SansSerif", Font.BOLD, 12));

        JCheckBox chkShowFree = new JCheckBox("✓ Show Free-Weight / Bodyweight", true);
        chkShowFree.setOpaque(false);
        chkShowFree.setForeground(NEON_EMERALD);
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

        RoundedCardPanel top = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 20);
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JButton btnAdd = createGradientButton("+ Add Workout / Machine", NEON_EMERALD, CYBER_CYAN);
        JButton btnEdit = createStyledButton("✎ Edit Selected", ELECTRIC_INDIGO, Color.WHITE);
        JButton btnDel = createStyledButton("🗑 Delete", CORAL_RED, Color.WHITE);
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
            JTextField tfName = new JTextField(target != null ? target.name : "");
            JComboBox<String> cbGroup = new JComboBox<>(new String[]{"Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body"});
            if (target != null) cbGroup.setSelectedItem(target.muscleGroup);
            JComboBox<String> cbDiff = new JComboBox<>(new String[]{"Beginner", "Intermediate", "Advanced"});
            if (target != null) cbDiff.setSelectedItem(target.difficulty);

            JCheckBox chkNeedsMachine = new JCheckBox("This workout requires a Gym Machine", target == null || target.requiresMachine());
            String[] equipOptions = new String[]{
                    "Machine: Lat Pulldown Station",
                    "Machine: Seated Cable Row Station",
                    "Machine: Assisted Dip & Chin-Up Machine",
                    "Machine: Converging Chest Press Machine",
                    "Machine: Pec Deck Fly Station",
                    "Machine: Angled Smith Machine",
                    "Machine: 45-Degree Leg Press Sled",
                    "Machine: Quad Extension Machine",
                    "Machine: Prone Leg Curl Machine",
                    "Machine: Standing Calf Machine",
                    "Machine: Linear Hack Squat Sled",
                    "Machine: Overhead Press Machine",
                    "Machine: Dual Adjustable Pulley Tower",
                    "Machine: High Pulley Cable Station",
                    "Machine: Bicep Preacher Curl Machine",
                    "Machine: Captain's Chair / Roman Chair",
                    "Machine: Abdominal Crunch Machine",
                    "Machine: Motorized Gym Treadmill",
                    "Machine: Indoor Concept Rower",
                    "Bodyweight",
                    "Dumbbells",
                    "Barbell"
            };
            JComboBox<String> cbEquip = new JComboBox<>(equipOptions);
            cbEquip.setEditable(true);
            if (target != null) cbEquip.setSelectedItem(target.equipment);

            chkNeedsMachine.addActionListener(ev -> {
                if (!chkNeedsMachine.isSelected()) {
                    cbEquip.setSelectedItem("Dumbbells");
                } else if (!cbEquip.getSelectedItem().toString().toLowerCase().contains("machine")) {
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
                if (tfName.getText().trim().isEmpty() || taDesc.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Workout name and instructions are required.");
                    return;
                }
                Exercise ex = isEdit && target != null ? target : new Exercise();
                ex.name = tfName.getText().trim();
                ex.muscleGroup = (String) cbGroup.getSelectedItem();
                ex.difficulty = (String) cbDiff.getSelectedItem();
                String chosenEq = cbEquip.getSelectedItem() != null ? cbEquip.getSelectedItem().toString().trim() : "Bodyweight";
                if (chkNeedsMachine.isSelected() && !chosenEq.toLowerCase().contains("machine")) {
                    chosenEq = "Machine: " + chosenEq;
                } else if (!chkNeedsMachine.isSelected() && chosenEq.toLowerCase().startsWith("machine:")) {
                    chosenEq = "Dumbbells";
                }
                ex.equipment = chosenEq;
                ex.description = taDesc.getText().trim();
                repo.saveExercise(ex);
                refresh.run();
            }
        };
        btnAdd.addActionListener(addOrEdit);
        btnEdit.addActionListener(addOrEdit);

        btnDel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            repo.deleteExercise(id);
            refresh.run();
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(wrapDarkScrollPane(table), BorderLayout.CENTER);
        panel.add(visualCard, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiRow.setOpaque(false);

        JPanel tablesGrid = new JPanel(new GridLayout(1, 3, 14, 14));
        tablesGrid.setOpaque(false);

        Runnable buildReportContent = () -> {
            kpiRow.removeAll();
            tablesGrid.removeAll();

            LocalDate today = LocalDate.now();
            int activeCount = 0;
            int expiredCount = 0;
            int expiring7Count = 0;

            DefaultTableModel expSoonModel = new DefaultTableModel(
                    new String[]{"Member", "End Date", "Remaining"}, 0);

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
            DefaultTableModel r1Model = new DefaultTableModel(
                    new String[]{"Pass Status", "Members", "Share"}, 0);
            r1Model.addRow(new Object[]{"Active Passes", activeCount,
                    totalMembers > 0 ? String.format("%.1f%%", (activeCount * 100.0) / totalMembers) : "0%"});
            r1Model.addRow(new Object[]{"Expired Passes", expiredCount,
                    totalMembers > 0 ? String.format("%.1f%%", (expiredCount * 100.0) / totalMembers) : "0%"});
            r1Model.addRow(new Object[]{"Total Registered", totalMembers, "100.0%"});

            Map<YearMonth, Integer> monthlyCounts = new TreeMap<>(Comparator.reverseOrder());
            for (Membership m : repo.getAllMemberships()) {
                YearMonth ym = YearMonth.from(m.startDate);
                monthlyCounts.put(ym, monthlyCounts.getOrDefault(ym, 0) + 1);
            }
            DefaultTableModel r3Model = new DefaultTableModel(
                    new String[]{"Month (YYYY-MM)", "Renewals"}, 0);
            for (Map.Entry<YearMonth, Integer> entry : monthlyCounts.entrySet()) {
                r3Model.addRow(new Object[]{entry.getKey().toString(), entry.getValue()});
            }

            kpiRow.add(createGradientKpiCard("TOTAL MEMBERS", String.valueOf(totalMembers), "Registered Athletes", ELECTRIC_INDIGO, VIOLET_GLOW));
            kpiRow.add(createGradientKpiCard("ACTIVE PASSES", String.valueOf(activeCount), "Cleared for Entry", new Color(5, 150, 105), NEON_EMERALD));
            kpiRow.add(createGradientKpiCard("EXPIRED PASSES", String.valueOf(expiredCount), "Needs Renewal", new Color(190, 18, 60), CORAL_RED));
            kpiRow.add(createGradientKpiCard("EXPIRING ≤ 7 DAYS", String.valueOf(expiring7Count), "Upcoming Renewals", new Color(180, 83, 9), AMBER_GOLD));

            tablesGrid.add(wrapReportTable("1. Active vs. Expired Summary", new JTable(r1Model)));
            tablesGrid.add(wrapReportTable("2. Expiring Within 7 Days", new JTable(expSoonModel)));
            tablesGrid.add(wrapReportTable("3. Monthly Renewals Log", new JTable(r3Model)));

            panel.revalidate();
            panel.repaint();
        };
        buildReportContent.run();

        JButton btnRefresh = createGradientButton("↻ Refresh Live Analytics", ELECTRIC_INDIGO, CYBER_CYAN);
        btnRefresh.addActionListener(e -> buildReportContent.run());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(btnRefresh);

        panel.add(kpiRow, BorderLayout.NORTH);
        panel.add(tablesGrid, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMemberStatusTab(User member) {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        Membership latest = repo.getLatestMembership(member.userId);
        DigitalMemberPassCard walletCard = new DigitalMemberPassCard();
        walletCard.setMemberData(member, latest);

        LocalDate today = LocalDate.now();
        String[] cols = {"Pass ID", "Start Date", "End Date (1 Month)", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Membership m : repo.getMembershipsByUser(member.userId)) {
            model.addRow(new Object[]{"#" + m.membershipId, m.startDate, m.endDate, m.getComputedStatus(today)});
        }
        JTable historyTable = new JTable(model);
        styleTable(historyTable);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());

        panel.add(walletCard, BorderLayout.WEST);
        panel.add(wrapReportTable("📅 My Monthly Membership & Renewal History", historyTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMemberWorkoutTab(User targetMember) {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Play Store Fitness Questionnaire Card with Machine Toggle
        RoundedCardPanel topControls = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 22);
        topControls.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JComboBox<String> cbGoal = createDarkComboBox(new String[]{
                "Weight Loss", "Muscle Gain", "General Fitness", "Strength Foundation"
        });
        JComboBox<String> cbLevel = createDarkComboBox(new String[]{
                "Beginner", "Intermediate", "Advanced"
        });
        if (targetMember.fitnessLevel != null) cbLevel.setSelectedItem(targetMember.fitnessLevel);
        JComboBox<Integer> cbDays = createDarkComboBox(new Integer[]{2, 3, 4, 5});
        cbDays.setSelectedItem(3);
        JComboBox<String> cbEquip = createDarkComboBox(new String[]{
                "Full Gym", "Gym Machines Only", "Dumbbells", "Bodyweight"
        });

        // Interactive Check / Uncheck Machine Toggle
        JCheckBox chkUseMachines = new JCheckBox("⚙ Include Gym Machines", true);
        chkUseMachines.setOpaque(false);
        chkUseMachines.setForeground(CYBER_CYAN);
        chkUseMachines.setFont(new Font("SansSerif", Font.BOLD, 12));
        chkUseMachines.setToolTipText("Check to include gym machines; uncheck for free-weights/bodyweight only");

        JButton btnGenerate = createGradientButton("⚡ Generate Plan", NEON_EMERALD, CYBER_CYAN);
        JButton btnCustom = createGradientButton("+ Custom Builder", ELECTRIC_INDIGO, VIOLET_GLOW);

        topControls.add(createChipLabel("GOAL:"));
        topControls.add(cbGoal);
        topControls.add(createChipLabel("LEVEL:"));
        topControls.add(cbLevel);
        topControls.add(createChipLabel("DAYS:"));
        topControls.add(cbDays);
        topControls.add(createChipLabel("ZONE:"));
        topControls.add(cbEquip);
        topControls.add(chkUseMachines);
        topControls.add(btnGenerate);
        topControls.add(btnCustom);

        JLabel planHeaderLbl = new JLabel("Current Active Workout Plan:");
        planHeaderLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        planHeaderLbl.setForeground(NEON_EMERALD);

        String[] cols = {"Day", "Workout Name", "Target Muscle", "Machine / Equipment Indicator", "Sets", "Reps"};
        DefaultTableModel exModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable exTable = new JTable(exModel);
        styleTable(exTable);
        exTable.getColumnModel().getColumn(3).setCellRenderer(new MachineBadgeRenderer());

        WorkoutVisualCard visualCard = new WorkoutVisualCard();

        Runnable loadLatestPlan = () -> {
            exModel.setRowCount(0);
            List<WorkoutPlan> plans = repo.getWorkoutPlansByUser(targetMember.userId);
            if (plans.isEmpty()) {
                planHeaderLbl.setText("No workout plan yet — use the Smart Generator or Custom Builder above!");
                visualCard.setExercise(null);
                return;
            }
            WorkoutPlan latest = plans.get(0);
            planHeaderLbl.setText("🔥 ACTIVE PLAN #" + latest.workoutPlanId + " [" + latest.planType.toUpperCase() + "]  •  " +
                    latest.goal + "  (Tap any row for animated muscle & machine demo)");
            boolean showMachines = chkUseMachines.isSelected();
            for (WorkoutPlanExercise item : latest.exercises) {
                boolean isMach = item.equipment != null && item.equipment.toLowerCase().contains("machine");
                if (!showMachines && isMach) continue;
                String badge = isMach ? ("⚙ " + item.equipment.replace("Machine: ", "")) : ("✓ No Machine (" + item.equipment + ")");
                exModel.addRow(new Object[]{
                        item.dayOfWeek, item.exerciseName, item.muscleGroup, badge, item.sets + " sets", item.reps
                });
            }
            if (exTable.getRowCount() > 0) {
                exTable.setRowSelectionInterval(0, 0);
            }
        };

        exTable.getSelectionModel().addListSelectionListener(e -> {
            int row = exTable.getSelectedRow();
            if (row < 0) return;
            String exName = exModel.getValueAt(exTable.convertRowIndexToModel(row), 1).toString();
            Exercise matched = repo.getAllExercises().stream()
                    .filter(x -> x.name.equalsIgnoreCase(exName))
                    .findFirst().orElse(null);
            visualCard.setExercise(matched);
        });

        chkUseMachines.addActionListener(e -> {
            if (!chkUseMachines.isSelected() && "Gym Machines Only".equals(cbEquip.getSelectedItem())) {
                cbEquip.setSelectedItem("Dumbbells");
            }
            loadLatestPlan.run();
        });

        loadLatestPlan.run();

        btnGenerate.addActionListener(e -> {
            String eqPref = (String) cbEquip.getSelectedItem();
            boolean useMach = chkUseMachines.isSelected();
            if (!useMach && ("Full Gym".equals(eqPref) || "Gym Machines Only".equals(eqPref))) {
                eqPref = "Dumbbells";
            }
            WorkoutPlan generated = BusinessRules.generateRuleBasedPlan(
                    targetMember.userId,
                    (String) cbGoal.getSelectedItem(),
                    (String) cbLevel.getSelectedItem(),
                    (Integer) cbDays.getSelectedItem(),
                    eqPref,
                    useMach,
                    repo.getAllExercises()
            );
            repo.saveWorkoutPlan(generated);
            loadLatestPlan.run();
            JOptionPane.showMessageDialog(this,
                    "Smart weekly workout plan generated in < 1 second!\n" +
                    "Routine: " + generated.goal + "\n" +
                    "Machine Toggle: " + (useMach ? "Gym Machines Included (⚙)" : "No Machines — Free Weight/Bodyweight Only (✓)"),
                    "Workout Plan Ready", JOptionPane.INFORMATION_MESSAGE);
        });

        btnCustom.addActionListener(e -> showCustomPlanBuilderDialog(targetMember, loadLatestPlan));

        JPanel centerBox = new JPanel(new BorderLayout(0, 10));
        centerBox.setOpaque(false);
        centerBox.add(planHeaderLbl, BorderLayout.NORTH);
        centerBox.add(wrapDarkScrollPane(exTable), BorderLayout.CENTER);

        panel.add(topControls, BorderLayout.NORTH);
        panel.add(centerBox, BorderLayout.CENTER);
        panel.add(visualCard, BorderLayout.EAST);
        return panel;
    }

    private JLabel createChipLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    private JPanel buildStaffWorkoutHelperTab() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 12));
        wrapper.setBackground(BG_OBSIDIAN);
        wrapper.setBorder(new EmptyBorder(14, 18, 14, 18));

        RoundedCardPanel selectorBar = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 18);
        selectorBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));
        JLabel lbl = new JLabel("🏋️ Select Member to Build Workout Plan For:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_PRIMARY);
        JComboBox<String> memberCombo = createDarkComboBox(new String[]{});
        List<User> members = repo.getMembers();
        for (User m : members) {
            memberCombo.addItem(m.userId + " - " + m.getFullName() + " (" + m.fitnessLevel + ")");
        }
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
        memberCombo.addActionListener(e -> loadSelectedMemberPlanner.run());
        loadSelectedMemberPlanner.run();

        wrapper.add(selectorBar, BorderLayout.NORTH);
        wrapper.add(dynamicHolder, BorderLayout.CENTER);
        return wrapper;
    }

    private void showCustomPlanBuilderDialog(User member, Runnable onSaved) {
        JDialog dlg = new JDialog(this, "Custom Workout Builder — " + member.getFullName(), true);
        dlg.setSize(800, 560);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG_OBSIDIAN);
        dlg.setLayout(new BorderLayout(12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        top.setBackground(BG_OBSIDIAN);
        JLabel goalLbl = new JLabel("Custom Routine Title *:");
        goalLbl.setForeground(TEXT_PRIMARY);
        goalLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        JTextField tfGoal = createDarkTextField("Custom Hypertrophy & Conditioning Split");
        tfGoal.setColumns(28);
        top.add(goalLbl);
        top.add(tfGoal);

        String[] cols = {"Day of Week", "Exercise ID", "Exercise Name", "Muscle Group", "Sets", "Reps"};
        DefaultTableModel draftModel = new DefaultTableModel(cols, 0);
        JTable draftTable = new JTable(draftModel);
        styleTable(draftTable);

        JPanel adder = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adder.setBackground(CARD_SURFACE);
        JComboBox<String> cbDay = createDarkComboBox(new String[]{
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        });
        JComboBox<Exercise> cbEx = createDarkComboBox(repo.getAllExercises().toArray(new Exercise[0]));
        JSpinner spSets = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JTextField tfReps = createDarkTextField("10-12");
        tfReps.setColumns(7);
        JButton btnAddRow = createGradientButton("+ Add Exercise", ELECTRIC_INDIGO, CYBER_CYAN);

        adder.add(createChipLabel("DAY:"));
        adder.add(cbDay);
        adder.add(createChipLabel("EXERCISE:"));
        adder.add(cbEx);
        adder.add(createChipLabel("SETS:"));
        adder.add(spSets);
        adder.add(createChipLabel("REPS:"));
        adder.add(tfReps);
        adder.add(btnAddRow);

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

        JButton btnSavePlan = createGradientButton("✓ Save Custom Workout Plan", NEON_EMERALD, CYBER_CYAN);
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

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG_OBSIDIAN);
        south.add(adder, BorderLayout.CENTER);
        JPanel saveBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        saveBar.setBackground(BG_OBSIDIAN);
        saveBar.add(btnSavePlan);
        south.add(saveBar, BorderLayout.SOUTH);

        dlg.add(top, BorderLayout.NORTH);
        dlg.add(wrapDarkScrollPane(draftTable), BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel buildReadOnlyExerciseBrowser() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_OBSIDIAN);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        RoundedCardPanel filterBar = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 20);
        filterBar.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));
        JLabel filterLbl = new JLabel("🏋️ Filter Gym Floor Workouts & Machines:");
        filterLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        filterLbl.setForeground(TEXT_PRIMARY);

        JCheckBox chkMachines = new JCheckBox("⚙ Needs Gym Machine", true);
        chkMachines.setOpaque(false);
        chkMachines.setForeground(CYBER_CYAN);
        chkMachines.setFont(new Font("SansSerif", Font.BOLD, 12));

        JCheckBox chkNoMachine = new JCheckBox("✓ No Machine (Free Weight / Bodyweight)", true);
        chkNoMachine.setOpaque(false);
        chkNoMachine.setForeground(NEON_EMERALD);
        chkNoMachine.setFont(new Font("SansSerif", Font.BOLD, 12));

        JComboBox<String> cbMuscleFilter = createDarkComboBox(new String[]{
                "All Muscle Groups", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body"
        });

        filterBar.add(filterLbl);
        filterBar.add(chkMachines);
        filterBar.add(chkNoMachine);
        filterBar.add(createChipLabel("MUSCLE:"));
        filterBar.add(cbMuscleFilter);

        String[] cols = {"ID", "Workout Name", "Target Muscle", "Difficulty", "Machine / Equipment Indicator", "Execution Cues"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        styleTable(t);
        t.getColumnModel().getColumn(4).setCellRenderer(new MachineBadgeRenderer());

        WorkoutVisualCard visualCard = new WorkoutVisualCard();

        Runnable populate = () -> {
            m.setRowCount(0);
            String mgFilter = (String) cbMuscleFilter.getSelectedItem();
            for (Exercise ex : repo.getAllExercises()) {
                if (ex.requiresMachine() && !chkMachines.isSelected()) continue;
                if (!ex.requiresMachine() && !chkNoMachine.isSelected()) continue;
                if (!"All Muscle Groups".equals(mgFilter) && !ex.muscleGroup.equalsIgnoreCase(mgFilter)) continue;
                m.addRow(new Object[]{ex.exerciseId, ex.name, ex.muscleGroup, ex.difficulty, ex.getMachineBadgeText(), ex.description});
            }
            if (t.getRowCount() > 0) {
                t.setRowSelectionInterval(0, 0);
            } else {
                visualCard.setExercise(null);
            }
        };

        t.getSelectionModel().addListSelectionListener(e -> {
            int row = t.getSelectedRow();
            if (row < 0) return;
            int id = (int) m.getValueAt(t.convertRowIndexToModel(row), 0);
            Exercise sel = repo.getAllExercises().stream().filter(x -> x.exerciseId == id).findFirst().orElse(null);
            visualCard.setExercise(sel);
        });

        chkMachines.addActionListener(e -> populate.run());
        chkNoMachine.addActionListener(e -> populate.run());
        cbMuscleFilter.addActionListener(e -> populate.run());
        populate.run();

        panel.add(filterBar, BorderLayout.NORTH);
        panel.add(wrapDarkScrollPane(t), BorderLayout.CENTER);
        panel.add(visualCard, BorderLayout.EAST);
        return panel;
    }

    // ========================================================================
    // 8. PLAY STORE DARK-MODE STYLING HELPERS & RENDERERS
    // ========================================================================

    private JTextField createDarkTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setBackground(INPUT_BG);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(CYBER_CYAN);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tf.setBorder(new CompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return tf;
    }

    private JPasswordField createDarkPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setBackground(INPUT_BG);
        pf.setForeground(TEXT_PRIMARY);
        pf.setCaretColor(CYBER_CYAN);
        pf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        pf.setBorder(new CompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return pf;
    }

    private <T> JComboBox<T> createDarkComboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setBackground(INPUT_BG);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(new Font("SansSerif", Font.BOLD, 12));
        cb.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        return cb;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        return createGradientButton(text, bg, bg, fg);
    }

    private JButton createGradientButton(String text, Color gradStart, Color gradEnd) {
        return createGradientButton(text, gradStart, gradEnd, Color.WHITE);
    }

    private JButton createGradientButton(String text, Color c1, Color c2, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel m = getModel();
                Color s = c1;
                Color e = c2;
                if (m.isPressed()) {
                    s = c1.darker();
                    e = c2.darker();
                } else if (m.isRollover()) {
                    s = brighten(c1, 22);
                    e = brighten(c2, 22);
                }
                g2.setPaint(new GradientPaint(0, 0, s, getWidth(), getHeight(), e));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static Color brighten(Color c, int delta) {
        return new Color(
                Math.min(255, c.getRed() + delta),
                Math.min(255, c.getGreen() + delta),
                Math.min(255, c.getBlue() + delta)
        );
    }

    private JScrollPane wrapDarkScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(TABLE_EVEN_ROW);
        sp.setBackground(CARD_SURFACE);
        sp.setBorder(new LineBorder(BORDER_SUBTLE, 1, true));
        return sp;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(TABLE_EVEN_ROW);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(TABLE_SELECT);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(38, 50, 72));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                lbl.setBackground(new Color(15, 23, 42));
                lbl.setForeground(CYBER_CYAN);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 2, 0, ELECTRIC_INDIGO),
                        new EmptyBorder(8, 10, 8, 10)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    lbl.setBackground(TABLE_SELECT);
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW);
                    lbl.setForeground(TEXT_PRIMARY);
                }
                lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, stripedRenderer);
        table.setDefaultRenderer(Integer.class, stripedRenderer);
    }

    private JPanel createGradientKpiCard(String title, String bigValue, String subtitle, Color c1, Color c2) {
        RoundedCardPanel card = new RoundedCardPanel(c1, c2, null, 22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setForeground(new Color(235, 245, 255));

        JLabel v = new JLabel(bigValue);
        v.setFont(new Font("SansSerif", Font.BOLD, 32));
        v.setForeground(Color.WHITE);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(new Color(230, 240, 255));

        card.add(t);
        card.add(Box.createVerticalStrut(4));
        card.add(v);
        card.add(Box.createVerticalStrut(2));
        card.add(s);
        return card;
    }

    private JPanel wrapReportTable(String title, JTable table) {
        styleTable(table);
        RoundedCardPanel box = new RoundedCardPanel(CARD_SURFACE, BORDER_SUBTLE, 20);
        box.setLayout(new BorderLayout(0, 10));
        box.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_PRIMARY);
        box.add(lbl, BorderLayout.NORTH);
        box.add(wrapDarkScrollPane(table), BorderLayout.CENTER);
        return box;
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            final String st = value != null ? value.toString() : "";
            JLabel lbl = new JLabel(st.toUpperCase(), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? TABLE_SELECT : (row % 2 == 0 ? TABLE_EVEN_ROW : TABLE_ALT_ROW));
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    Color pillBg;
                    if ("Active".equalsIgnoreCase(st)) {
                        pillBg = new Color(16, 185, 129, 45);
                    } else if (st.toLowerCase().contains("expiring")) {
                        pillBg = new Color(245, 158, 11, 50);
                    } else {
                        pillBg = new Color(244, 63, 94, 50);
                    }
                    g2.setColor(pillBg);
                    g2.fillRoundRect(8, 5, getWidth() - 16, getHeight() - 10, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            if ("Active".equalsIgnoreCase(st)) {
                lbl.setForeground(NEON_EMERALD);
                lbl.setText("● ACTIVE");
            } else if (st.toLowerCase().contains("expiring")) {
                lbl.setForeground(AMBER_GOLD);
                lbl.setText("● EXPIRING");
            } else {
                lbl.setForeground(CORAL_RED);
                lbl.setText("● EXPIRED");
            }
            return lbl;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("Panel.background", BG_OBSIDIAN);
            UIManager.put("OptionPane.background", CARD_SURFACE);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            UIManager.put("Label.foreground", TEXT_PRIMARY);
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            gymsyncapp app = new gymsyncapp();
            app.setVisible(true);
        });
    }
}