-- ============================================================================
-- GYMSYNC: Gym Membership and Workout Plan Management System
-- Database Schema, Views, Seed Data, and Report Queries (SQLite & MySQL Ready)
-- Prepared for:
--   Musnit, Jhan Vince Aaron (Project Manager / DB Schema & Exercise Data)
--   Teston, Christian (Developer / JDBC & Business Logic)
--   Barizo, MikeAndrew (QA Tester / Verification & Reporting)
--   Texon, Broderick (UI/UX Designer / Java Swing Layouts)
-- ============================================================================

PRAGMA foreign_keys = ON;

-- Drop existing tables in reverse dependency order for clean re-initialization
DROP VIEW IF EXISTS vw_member_status;
DROP TABLE IF EXISTS WorkoutPlanExercise;
DROP TABLE IF EXISTS WorkoutPlan;
DROP TABLE IF EXISTS Membership;
DROP TABLE IF EXISTS Exercise;
DROP TABLE IF EXISTS User;

-- ============================================================================
-- 1. TABLE DEFINITIONS (5 Core Entities from Proposal Section 7)
-- ============================================================================

-- 1.1 User Entity
-- Stores Admin, Staff, and Member accounts in a single unified table.
-- Member-only fields (birthdate, emergency_contact_name, emergency_contact_no,
-- fitness_level) are populated when role = 'Member'.
CREATE TABLE User (
    user_id                INTEGER PRIMARY KEY AUTOINCREMENT,
    username               VARCHAR(50)  NOT NULL UNIQUE,
    password_hash          VARCHAR(64)  NOT NULL, -- SHA-256 hex digest
    role                   VARCHAR(15)  NOT NULL CHECK (role IN ('Admin', 'Staff', 'Member')),
    first_name             VARCHAR(60)  NOT NULL,
    last_name              VARCHAR(60)  NOT NULL,
    contact_no             VARCHAR(25)  NOT NULL,
    birthdate              DATE         NULL,
    gender                 VARCHAR(20)  NULL,
    address                VARCHAR(255) NULL,
    emergency_contact_name VARCHAR(100) NULL,
    emergency_contact_no   VARCHAR(25)  NULL,
    fitness_level          VARCHAR(20)  NULL CHECK (fitness_level IN ('Beginner', 'Intermediate', 'Advanced') OR fitness_level IS NULL),
    date_registered        DATE         NOT NULL DEFAULT (CURRENT_DATE)
);

-- 1.2 Membership Entity
-- Tracks month-to-month membership periods for each Member.
-- Active/Expired status is dynamically computed from the latest end_date.
CREATE TABLE Membership (
    membership_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                INTEGER NOT NULL,
    start_date             DATE    NOT NULL,
    end_date               DATE    NOT NULL,
    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id) REFERENCES User(user_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_membership_dates
        CHECK (end_date >= start_date)
);

-- 1.3 Exercise Entity
-- Master exercise library managed by Administrators and used by Workout Plans.
CREATE TABLE Exercise (
    exercise_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   VARCHAR(100) NOT NULL UNIQUE,
    muscle_group           VARCHAR(40)  NOT NULL, -- Chest, Back, Legs, Shoulders, Arms, Core, Full Body
    difficulty             VARCHAR(20)  NOT NULL CHECK (difficulty IN ('Beginner', 'Intermediate', 'Advanced')),
    equipment              VARCHAR(50)  NOT NULL, -- Bodyweight, Dumbbells, Barbell, Machine, Cable
    description            TEXT         NOT NULL
);

-- 1.4 WorkoutPlan Entity
-- Header table for either rule-based ('suggested') or member-built ('custom') plans.
CREATE TABLE WorkoutPlan (
    workout_plan_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                INTEGER     NOT NULL,
    plan_type              VARCHAR(20) NOT NULL CHECK (plan_type IN ('suggested', 'custom')),
    goal                   VARCHAR(60) NOT NULL, -- Weight Loss, Muscle Gain, General Fitness, Strength
    date_created           DATE        NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_workoutplan_user
        FOREIGN KEY (user_id) REFERENCES User(user_id)
        ON DELETE CASCADE
);

-- 1.5 WorkoutPlanExercise Entity
-- Associative detail rows mapping Exercises to specific days, sets, and reps in a WorkoutPlan.
CREATE TABLE WorkoutPlanExercise (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    workout_plan_id        INTEGER     NOT NULL,
    exercise_id            INTEGER     NOT NULL,
    day_of_week            VARCHAR(20) NOT NULL, -- Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    sets                   INTEGER     NOT NULL CHECK (sets >= 1 AND sets <= 20),
    reps                   VARCHAR(25) NOT NULL, -- e.g. '10-12', '15', '30 sec'
    CONSTRAINT fk_wpe_plan
        FOREIGN KEY (workout_plan_id) REFERENCES WorkoutPlan(workout_plan_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_wpe_exercise
        FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id)
        ON DELETE RESTRICT
);

-- Indexes for fast (<5 second) Front Desk lookups and reporting
CREATE INDEX idx_user_role_name ON User(role, last_name, first_name);
CREATE INDEX idx_membership_user_end ON Membership(user_id, end_date DESC);
CREATE INDEX idx_exercise_filter ON Exercise(difficulty, equipment, muscle_group);
CREATE INDEX idx_workoutplan_user ON WorkoutPlan(user_id, date_created DESC);

-- ============================================================================
-- 2. DYNAMIC STATUS VIEW (vw_member_status)
-- Computes Active vs. Expired from the latest Membership.end_date
-- ============================================================================
CREATE VIEW vw_member_status AS
SELECT
    u.user_id,
    u.username,
    u.first_name,
    u.last_name,
    u.contact_no,
    u.fitness_level,
    u.emergency_contact_name,
    u.emergency_contact_no,
    u.date_registered,
    m.membership_id AS latest_membership_id,
    m.start_date    AS latest_start_date,
    m.end_date      AS latest_end_date,
    CASE
        WHEN m.end_date IS NOT NULL AND m.end_date >= DATE('now') THEN 'Active'
        ELSE 'Expired'
    END AS computed_status,
    CAST(JULIANDAY(m.end_date) - JULIANDAY(DATE('now')) AS INTEGER) AS days_remaining
FROM User u
LEFT JOIN Membership m
    ON m.membership_id = (
        SELECT m2.membership_id
        FROM Membership m2
        WHERE m2.user_id = u.user_id
        ORDER BY m2.end_date DESC, m2.membership_id DESC
        LIMIT 1
    )
WHERE u.role = 'Member';

-- ============================================================================
-- 3. SEED DATA: USERS (Admin, Staff, and Sample Gym Members)
-- Default Credentials (SHA-256 hashed):
--   Admin  -> username: admin      | password: admin123
--   Staff  -> username: staff      | password: staff123
--   Member -> username: carlos_m   | password: member123 (Active)
--   Member -> username: angela_r   | password: member123 (Expiring in 4 days)
--   Member -> username: mark_d     | password: member123 (Expired)
--   Member -> username: sofia_l    | password: member123 (Active Beginner)
--   Member -> username: kevin_t    | password: member123 (Expired Beginner)
--   Member -> username: hannah_g   | password: member123 (Expiring in 2 days)
-- ============================================================================

INSERT INTO User (
    user_id, username, password_hash, role, first_name, last_name, contact_no,
    birthdate, gender, address, emergency_contact_name, emergency_contact_no,
    fitness_level, date_registered
) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Admin', 'Jhan Vince', 'Musnit', '0917-555-0101',
 '2001-04-12', 'Male', 'Imus, Cavite', 'Elena Musnit', '0917-555-0199', NULL, '2026-01-10'),
(2, 'staff', '10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6', 'Staff', 'Christian', 'Teston', '0918-555-0102',
 '2002-08-23', 'Male', 'Bacoor, Cavite', 'Roberto Teston', '0918-555-0198', NULL, '2026-01-15'),
(3, 'carlos_m', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Carlos', 'Mendoza', '0919-555-0201',
 '1999-05-14', 'Male', 'Bayan Luma, Imus, Cavite', 'Maria Mendoza', '0919-555-0299', 'Beginner', '2026-08-10'),
(4, 'angela_r', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Angela', 'Reyes', '0920-555-0202',
 '2003-11-02', 'Female', 'Tanzang Luma, Imus, Cavite', 'Liza Reyes', '0920-555-0298', 'Intermediate', '2026-09-01'),
(5, 'mark_d', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Mark', 'Dela Cruz', '0921-555-0203',
 '1997-02-19', 'Male', 'Anabu I-C, Imus, Cavite', 'Ramon Dela Cruz', '0921-555-0297', 'Beginner', '2026-07-15'),
(6, 'sofia_l', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Sofia', 'Lim', '0922-555-0204',
 '2004-07-30', 'Female', 'Bucandala, Imus, Cavite', 'Grace Lim', '0922-555-0296', 'Beginner', '2026-10-12'),
(7, 'kevin_t', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Kevin', 'Torres', '0923-555-0205',
 '1995-12-08', 'Male', 'Palico IV, Imus, Cavite', 'diana Torres', '0923-555-0295', 'Advanced', '2026-06-20'),
(8, 'hannah_g', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'Member', 'Hannah', 'Garcia', '0924-555-0206',
 '2001-09-25', 'Female', 'Medicion I-A, Imus, Cavite', 'Noel Garcia', '0924-555-0294', 'Beginner', '2026-09-28');

-- ============================================================================
-- 4. SEED DATA: MEMBERSHIPS (Historical + Current/Expiring/Expired Records)
-- Relative to current date so Active, Expiring Soon (<=7 days), and Expired
-- statuses are always demonstrable during QA testing.
-- ============================================================================

INSERT INTO Membership (membership_id, user_id, start_date, end_date) VALUES
-- Carlos Mendoza: renewed twice, currently ACTIVE (expires in 18 days)
(1,  3, DATE('now', '-72 days'), DATE('now', '-42 days')),
(2,  3, DATE('now', '-42 days'), DATE('now', '-12 days')),
(3,  3, DATE('now', '-12 days'), DATE('now', '+18 days')),
-- Angela Reyes: renewed once, EXPIRING SOON (expires in 4 days)
(4,  4, DATE('now', '-56 days'), DATE('now', '-26 days')),
(5,  4, DATE('now', '-26 days'), DATE('now', '+4 days')),
-- Mark Dela Cruz: EXPIRED 9 days ago (appears in Front Desk Expired Alert List)
(6,  5, DATE('now', '-69 days'), DATE('now', '-39 days')),
(7,  5, DATE('now', '-39 days'), DATE('now', '-9 days')),
-- Sofia Lim: New beginner member, ACTIVE (expires in 22 days)
(8,  6, DATE('now', '-8 days'),  DATE('now', '+22 days')),
-- Kevin Torres: EXPIRED 15 days ago (appears in Front Desk Expired Alert List)
(9,  7, DATE('now', '-45 days'), DATE('now', '-15 days')),
-- Hannah Garcia: EXPIRING SOON (expires in 2 days)
(10, 8, DATE('now', '-28 days'), DATE('now', '+2 days'));

-- ============================================================================
-- 5. SEED DATA: EXERCISE LIBRARY (24 Structured Exercises Across Muscle Groups)
-- ============================================================================

INSERT INTO Exercise (exercise_id, name, muscle_group, difficulty, equipment, description) VALUES
-- Bodyweight Exercises
(1,  'Push-Up', 'Chest', 'Beginner', 'Bodyweight', 'Keep body in a straight plank line, lower chest to floor, and press back up.'),
(2,  'Bodyweight Squat', 'Legs', 'Beginner', 'Bodyweight', 'Stand shoulder-width apart, sit hips back and down keeping chest upright, then stand.'),
(3,  'Forward Lunge', 'Legs', 'Beginner', 'Bodyweight', 'Step forward with one leg, lower hips until both knees form 90-degree angles, push back.'),
(4,  'Forearm Plank', 'Core', 'Beginner', 'Bodyweight', 'Hold a rigid straight-body position supported on forearms and toes with braced core.'),
(5,  'Glute Bridge', 'Legs', 'Beginner', 'Bodyweight', 'Lie on back with knees bent, drive through heels to lift hips until aligned with knees.'),
(6,  'Mountain Climbers', 'Full Body', 'Beginner', 'Bodyweight', 'From high plank position, alternate driving knees toward chest at a steady brisk pace.'),
(7,  'Bicycle Crunches', 'Core', 'Beginner', 'Bodyweight', 'Lie on back, alternate touching elbow to opposite knee while extending the other leg.'),
(8,  'Burpees', 'Full Body', 'Intermediate', 'Bodyweight', 'Drop into a squat with hands on floor, kick feet back to plank, return to squat, and jump.'),
(9,  'Pull-Up', 'Back', 'Advanced', 'Bodyweight', 'Grip overhead bar slightly wider than shoulders and pull chin cleanly above the bar.'),

-- Dumbbell Exercises
(10, 'Dumbbell Goblet Squat', 'Legs', 'Beginner', 'Dumbbells', 'Hold one dumbbell vertically at chest height and perform a controlled full squat.'),
(11, 'Dumbbell Chest Press', 'Chest', 'Beginner', 'Dumbbells', 'Lie on flat bench and press dumbbells from chest level up until arms are extended.'),
(12, 'One-Arm Dumbbell Row', 'Back', 'Beginner', 'Dumbbells', 'Support one hand on bench, keep back flat, and pull dumbbell toward hip crease.'),
(13, 'Seated Dumbbell Shoulder Press', 'Shoulders', 'Beginner', 'Dumbbells', 'Sit upright on bench and press dumbbells overhead without arching the lower back.'),
(14, 'Dumbbell Bicep Curl', 'Arms', 'Beginner', 'Dumbbells', 'Stand tall with elbows tucked at sides and curl dumbbells upward with full control.'),
(15, 'Overhead Dumbbell Tricep Extension', 'Arms', 'Beginner', 'Dumbbells', 'Hold dumbbell overhead with both hands, lower behind head by bending elbows, and extend.'),
(16, 'Dumbbell Romanian Deadlift', 'Legs', 'Intermediate', 'Dumbbells', 'Hinge at the hips with a slight knee bend, lowering dumbbells along shins, then squeeze glutes.'),
(17, 'Dumbbell Lateral Raise', 'Shoulders', 'Beginner', 'Dumbbells', 'Raise dumbbells out to the sides to shoulder height with a slight bend in the elbows.'),

-- Machine & Cable Exercises
(18, 'Lat Pulldown (Machine)', 'Back', 'Beginner', 'Machine', 'Sit at pulldown station, lean back slightly, and pull wide bar down to upper chest.'),
(19, 'Seated Cable Row', 'Back', 'Beginner', 'Machine', 'Sit tall with feet braced, pull handle to midsection while squeezing shoulder blades together.'),
(20, 'Leg Press (Machine)', 'Legs', 'Beginner', 'Machine', 'Place feet hip-width on sled platform, lower weight until knees reach 90 degrees, press up.'),
(21, 'Chest Press Machine', 'Chest', 'Beginner', 'Machine', 'Adjust seat height so handles align with mid-chest and press forward smoothly.'),
(22, 'Cable Tricep Pushdown', 'Arms', 'Beginner', 'Machine', 'Keep elbows pinned to sides and push rope or bar attachment down until arms lock out.'),

-- Barbell Exercises
(23, 'Barbell Bench Press', 'Chest', 'Intermediate', 'Barbell', 'Lie on flat bench, unrack barbell, lower with control to mid-chest, and press upward.'),
(24, 'Barbell Back Squat', 'Legs', 'Intermediate', 'Barbell', 'Rest barbell across upper traps, brace core, squat to parallel, and drive back up.');

-- ============================================================================
-- 6. SEED DATA: WORKOUT PLANS & WORKOUT PLAN EXERCISES
-- ============================================================================

INSERT INTO WorkoutPlan (workout_plan_id, user_id, plan_type, goal, date_created) VALUES
(1, 3, 'suggested', 'Weight Loss (Beginner - 3 Days - Dumbbells)', DATE('now', '-10 days')),
(2, 6, 'suggested', 'General Fitness (Beginner - 3 Days - Full Gym)', DATE('now', '-7 days')),
(3, 4, 'custom',    'Muscle Gain & Lower Body Focus', DATE('now', '-14 days'));

INSERT INTO WorkoutPlanExercise (workout_plan_id, exercise_id, day_of_week, sets, reps) VALUES
-- Plan 1: Carlos Mendoza (Beginner Suggested Plan - 3 Days)
(1, 10, 'Monday',    3, '12-15'),
(1, 11, 'Monday',    3, '12-15'),
(1, 4,  'Monday',    3, '30 sec'),
(1, 12, 'Wednesday', 3, '12-15'),
(1, 13, 'Wednesday', 3, '12-15'),
(1, 6,  'Wednesday', 3, '30 sec'),
(1, 3,  'Friday',    3, '12 each leg'),
(1, 14, 'Friday',    3, '12-15'),
(1, 7,  'Friday',    3, '15'),

-- Plan 2: Sofia Lim (Beginner Suggested Plan - 3 Days Full Gym)
(2, 20, 'Monday',    3, '10-12'),
(2, 21, 'Monday',    3, '10-12'),
(2, 4,  'Monday',    3, '30 sec'),
(2, 18, 'Wednesday', 3, '10-12'),
(2, 13, 'Wednesday', 3, '10-12'),
(2, 5,  'Wednesday', 3, '12-15'),
(2, 10, 'Friday',    3, '10-12'),
(2, 19, 'Friday',    3, '10-12'),
(2, 7,  'Friday',    3, '15'),

-- Plan 3: Angela Reyes (Custom Plan)
(3, 24, 'Monday',    4, '8-10'),
(3, 16, 'Monday',    4, '10-12'),
(3, 23, 'Wednesday', 4, '8-10'),
(3, 18, 'Wednesday', 4, '10-12'),
(3, 20, 'Friday',    4, '10-12'),
(3, 17, 'Friday',    3, '12-15');

-- ============================================================================
-- 7. STANDARD REPORT QUERIES (Proposal Section 4 & Section 6)
-- ============================================================================

-- REPORT A: Active vs. Expired Members Summary
-- SELECT computed_status, COUNT(*) AS member_count FROM vw_member_status GROUP BY computed_status;

-- REPORT B: Memberships Expiring Within 7 Days
-- SELECT user_id, first_name, last_name, contact_no, latest_end_date, days_remaining
-- FROM vw_member_status
-- WHERE computed_status = 'Active' AND days_remaining BETWEEN 0 AND 7
-- ORDER BY days_remaining ASC;

-- REPORT C: Monthly Membership Renewals Count
-- SELECT STRFTIME('%Y-%m', start_date) AS renewal_month, COUNT(*) AS total_memberships
-- FROM Membership
-- GROUP BY STRFTIME('%Y-%m', start_date)
-- ORDER BY renewal_month DESC;