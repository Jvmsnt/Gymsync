import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * GymSync Integrative Web Server & REST API
 * Serves the React + Tailwind + shadcn-style frontend from frontend/dist
 * and exposes JSON REST endpoints backed by gymsyncapp.DataRepository (MySQL/SQLite/Memory).
 */
public final class GymsyncServer {
    private static final int PORT = Integer.parseInt(System.getProperty("gymsync.port", "8080"));
    private static final Path DIST_DIR = Paths.get("frontend", "dist").toAbsolutePath().normalize();

    private final gymsyncapp.DataRepository repo;

    public GymsyncServer() {
        this.repo = new gymsyncapp.DataRepository();
        this.repo.initializeDatabase();
    }

    public static void main(String[] args) throws Exception {
        GymsyncServer app = new GymsyncServer();
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        } catch (java.net.BindException busy) {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }
        int boundPort = server.getAddress().getPort();
        server.createContext("/api/", app::handleApi);
        server.createContext("/", app::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        String url = "http://127.0.0.1:" + boundPort;
        System.out.println("====================================================");
        System.out.println(" GymSync Desktop App running (" + url + ")");
        System.out.println(" Database: " + (app.repo.isJdbcConnected()
                ? (app.repo.isMySql() ? "MySQL Live" : "SQLite Live")
                : "In-Memory Fallback"));
        System.out.println("====================================================");

        Process appWindow = Boolean.getBoolean("gymsync.headless") ? null : launchDesktopAppWindow(url);
        if (appWindow != null) {
            appWindow.waitFor();
            server.stop(0);
            System.exit(0);
        } else if (!Boolean.getBoolean("gymsync.headless") && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {}
        }
    }

    private static Process launchDesktopAppWindow(String url) {
        String pf = System.getenv("ProgramFiles");
        String pf86 = System.getenv("ProgramFiles(x86)");
        String localApp = System.getenv("LOCALAPPDATA");
        String[] candidates = new String[]{
                pf86 != null ? pf86 + "\\Microsoft\\Edge\\Application\\msedge.exe" : "",
                pf != null ? pf + "\\Microsoft\\Edge\\Application\\msedge.exe" : "",
                pf != null ? pf + "\\Google\\Chrome\\Application\\chrome.exe" : "",
                pf86 != null ? pf86 + "\\Google\\Chrome\\Application\\chrome.exe" : "",
                pf != null ? pf + "\\BraveSoftware\\Brave-Browser\\Application\\brave.exe" : "",
                localApp != null ? localApp + "\\BraveSoftware\\Brave-Browser\\Application\\brave.exe" : ""
        };
        for (String exe : candidates) {
            if (exe != null && !exe.isEmpty() && Files.exists(Paths.get(exe))) {
                try {
                    Path profileDir = Paths.get(System.getProperty("java.io.tmpdir"), "gymsync-desktop-profile");
                    Files.createDirectories(profileDir);
                    return new ProcessBuilder(
                            exe,
                            "--app=" + url,
                            "--user-data-dir=" + profileDir.toAbsolutePath(),
                            "--window-size=1400,880",
                            "--no-first-run",
                            "--no-default-browser-check"
                    ).start();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    private synchronized void handleApi(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase(Locale.ROOT);
        URI uri = ex.getRequestURI();
        String path = uri.getPath();
        String query = uri.getRawQuery();

        if ("OPTIONS".equals(method)) {
            sendJson(ex, 204, "");
            return;
        }

        try {
            if ("GET".equals(method) && "/api/health".equals(path)) {
                String dbLabel = repo.isJdbcConnected() ? (repo.isMySql() ? "MySQL Live" : "SQLite Live") : "Memory DB";
                sendJson(ex, 200, obj(
                        "jdbcConnected", repo.isJdbcConnected(),
                        "isMySql", repo.isMySql(),
                        "dbLabel", dbLabel
                ));
                return;
            }

            if ("POST".equals(method) && "/api/login".equals(path)) {
                Map<String, String> body = parseFlatJson(readBody(ex));
                String u = body.getOrDefault("username", "").trim();
                String p = body.getOrDefault("password", "");
                if (u.isEmpty() || p.isEmpty()) {
                    sendError(ex, 400, "Username and password are required.");
                    return;
                }
                gymsyncapp.User auth = repo.authenticate(u, p);
                if (auth == null) {
                    sendError(ex, 401, "Invalid username or password.");
                    return;
                }
                if (!"Admin".equalsIgnoreCase(auth.role) && !"Staff".equalsIgnoreCase(auth.role)) {
                    sendError(ex, 403, "Desk Management Access Only. Please sign in with an Admin or Staff account.");
                    return;
                }
                sendJson(ex, 200, obj("user", userJson(auth)));
                return;
            }

            if ("/api/members".equals(path)) {
                if ("GET".equals(method)) {
                    LocalDate today = LocalDate.now();
                    List<String> items = new ArrayList<>();
                    for (gymsyncapp.User m : repo.getMembers()) {
                        items.add(memberDetailJson(m, today));
                    }
                    sendJson(ex, 200, obj("members", arr(items)));
                    return;
                }
                if ("POST".equals(method)) {
                    Map<String, String> b = parseFlatJson(readBody(ex));
                    String err = validateMember(b, true, 0);
                    if (err != null) { sendError(ex, 400, err); return; }
                    gymsyncapp.User u = applyMemberFields(new gymsyncapp.User(), b);
                    repo.saveUser(u, b.getOrDefault("password", "").trim());
                    LocalDate start = LocalDate.now();
                    repo.addMembership(u.userId, start, start.plusMonths(1));
                    sendJson(ex, 201, obj("member", memberDetailJson(u, start)));
                    return;
                }
            }

            if (path.startsWith("/api/members/")) {
                String rest = path.substring("/api/members/".length());
                if (rest.endsWith("/memberships") && "GET".equals(method)) {
                    int id = parsePositiveInt(rest.substring(0, rest.length() - "/memberships".length()), "member ID");
                    gymsyncapp.User target = findMember(id);
                    if (target == null) { sendError(ex, 404, "Member not found."); return; }
                    LocalDate today = LocalDate.now();
                    List<gymsyncapp.Membership> history = repo.getMembershipsByUser(id);
                    List<String> items = new ArrayList<>();
                    for (int i = 0; i < history.size(); i++) {
                        gymsyncapp.Membership ms = history.get(i);
                        items.add(obj(
                                "membershipId", ms.membershipId,
                                "userId", ms.userId,
                                "startDate", ms.startDate.toString(),
                                "endDate", ms.endDate.toString(),
                                "status", ms.getComputedStatus(today),
                                "daysRemaining", ms.getDaysRemaining(today),
                                "isCurrent", i == 0
                        ));
                    }
                    sendJson(ex, 200, obj("memberId", id, "memberships", arr(items)));
                    return;
                }
                if (rest.endsWith("/renew") && "POST".equals(method)) {
                    int id = parsePositiveInt(rest.substring(0, rest.length() - "/renew".length()), "member ID");
                    gymsyncapp.User target = findMember(id);
                    if (target == null) { sendError(ex, 404, "Member not found."); return; }
                    gymsyncapp.Membership latest = repo.getLatestMembership(id);
                    LocalDate[] dates = gymsyncapp.BusinessRules.calculateRenewalDates(
                            latest != null ? latest.endDate : null, LocalDate.now());
                    repo.addMembership(id, dates[0], dates[1]);
                    sendJson(ex, 200, obj("startDate", dates[0].toString(), "endDate", dates[1].toString()));
                    return;
                }
                int id = parsePositiveInt(rest, "member ID");
                gymsyncapp.User existing = findMember(id);
                if (existing == null) { sendError(ex, 404, "Member not found."); return; }
                if ("GET".equals(method)) {
                    sendJson(ex, 200, obj("member", memberDetailJson(existing, LocalDate.now())));
                    return;
                }
                if ("PUT".equals(method)) {
                    Map<String, String> b = parseFlatJson(readBody(ex));
                    String err = validateMember(b, false, id);
                    if (err != null) { sendError(ex, 400, err); return; }
                    applyMemberFields(existing, b);
                    repo.saveUser(existing, b.getOrDefault("password", "").trim());
                    sendJson(ex, 200, obj("member", memberDetailJson(existing, LocalDate.now())));
                    return;
                }
                if ("DELETE".equals(method)) {
                    repo.deleteUser(id);
                    sendJson(ex, 200, obj("deleted", true));
                    return;
                }
            }

            if ("GET".equals(method) && "/api/alerts".equals(path)) {
                LocalDate today = LocalDate.now();
                List<String> items = new ArrayList<>();
                for (gymsyncapp.User m : repo.getMembers()) {
                    gymsyncapp.Membership latest = repo.getLatestMembership(m.userId);
                    String status = latest != null ? latest.getComputedStatus(today) : "Expired";
                    long days = latest != null ? latest.getDaysRemaining(today) : -999;
                    if ("Expired".equalsIgnoreCase(status)) {
                        items.add(obj(
                                "userId", m.userId,
                                "fullName", m.getFullName(),
                                "contactNo", m.contactNo,
                                "emergencyContact", (m.emergencyContactName != null ? m.emergencyContactName : "N/A")
                                        + " (" + (m.emergencyContactNo != null ? m.emergencyContactNo : "N/A") + ")",
                                "endDate", latest != null ? latest.endDate.toString() : "None",
                                "alertStatus", "Expired",
                                "timeline", latest != null ? Math.abs(days) + " days overdue" : "No pass"
                        ));
                    } else if (days >= 0 && days <= 7) {
                        items.add(obj(
                                "userId", m.userId,
                                "fullName", m.getFullName(),
                                "contactNo", m.contactNo,
                                "emergencyContact", (m.emergencyContactName != null ? m.emergencyContactName : "N/A")
                                        + " (" + (m.emergencyContactNo != null ? m.emergencyContactNo : "N/A") + ")",
                                "endDate", latest.endDate.toString(),
                                "alertStatus", "Expiring Soon",
                                "timeline", days + " days remaining"
                        ));
                    }
                }
                sendJson(ex, 200, obj("alerts", arr(items)));
                return;
            }

            if ("/api/staff".equals(path)) {
                if ("GET".equals(method)) {
                    List<String> items = new ArrayList<>();
                    for (gymsyncapp.User u : repo.getAllUsers()) items.add(userJson(u));
                    sendJson(ex, 200, obj("users", arr(items)));
                    return;
                }
                if ("POST".equals(method)) {
                    Map<String, String> b = parseFlatJson(readBody(ex));
                    String un = b.getOrDefault("username", "").trim();
                    String pw = b.getOrDefault("password", "").trim();
                    String fn = b.getOrDefault("firstName", "").trim();
                    String ln = b.getOrDefault("lastName", "").trim();
                    String ct = b.getOrDefault("contactNo", "").trim();
                    if (un.isEmpty() || fn.isEmpty() || ln.isEmpty() || ct.isEmpty() || "09".equals(ct)) {
                        sendError(ex, 400, "Username, First Name, Last Name, and Contact No. are required.");
                        return;
                    }
                    if (pw.length() < 4) { sendError(ex, 400, "Password must be at least 4 characters."); return; }
                    if (repo.usernameExists(un, 0)) { sendError(ex, 400, "Username already exists."); return; }
                    String requestedRole = b.getOrDefault("role", "Staff").trim();
                    String normalizedRole;
                    if ("Admin".equalsIgnoreCase(requestedRole)) normalizedRole = "Admin";
                    else if ("Staff".equalsIgnoreCase(requestedRole)) normalizedRole = "Staff";
                    else { sendError(ex, 400, "Operator role must be Admin or Staff."); return; }
                    gymsyncapp.User u = new gymsyncapp.User();
                    u.username = un;
                    u.role = normalizedRole;
                    u.firstName = fn;
                    u.lastName = ln;
                    u.contactNo = ct;
                    u.dateRegistered = LocalDate.now();
                    repo.saveUser(u, pw);
                    sendJson(ex, 201, obj("user", userJson(u)));
                    return;
                }
            }

            if (path.startsWith("/api/staff/") && "DELETE".equals(method)) {
                int id = parsePositiveInt(path.substring("/api/staff/".length()), "user ID");
                gymsyncapp.User target = findUser(id);
                if (target == null || "Member".equalsIgnoreCase(target.role)) {
                    sendError(ex, 404, "Operator account not found.");
                    return;
                }
                if ("Admin".equalsIgnoreCase(target.role)) {
                    long adminCount = repo.getAllUsers().stream()
                            .filter(u -> "Admin".equalsIgnoreCase(u.role))
                            .count();
                    if (adminCount <= 1) {
                        sendError(ex, 400, "Cannot delete the last Admin account.");
                        return;
                    }
                }
                repo.deleteUser(id);
                sendJson(ex, 200, obj("deleted", true));
                return;
            }

            if ("/api/exercises".equals(path)) {
                if ("GET".equals(method)) {
                    List<String> items = new ArrayList<>();
                    for (gymsyncapp.Exercise x : repo.getAllExercises()) items.add(exerciseJson(x));
                    sendJson(ex, 200, obj("exercises", arr(items)));
                    return;
                }
                if ("POST".equals(method)) {
                    Map<String, String> b = parseFlatJson(readBody(ex));
                    String name = b.getOrDefault("name", "").trim();
                    String desc = b.getOrDefault("description", "").trim();
                    if (name.isEmpty() || desc.isEmpty()) { sendError(ex, 400, "Name and instructions are required."); return; }
                    if (repo.exerciseNameExists(name, 0)) { sendError(ex, 400, "Exercise already exists."); return; }
                    gymsyncapp.Exercise x = new gymsyncapp.Exercise();
                    x.name = name;
                    x.muscleGroup = b.getOrDefault("muscleGroup", "Chest");
                    x.difficulty = b.getOrDefault("difficulty", "Beginner");
                    x.equipment = b.getOrDefault("equipment", "Bodyweight");
                    x.description = desc;
                    repo.saveExercise(x);
                    sendJson(ex, 201, obj("exercise", exerciseJson(x)));
                    return;
                }
            }

            if (path.startsWith("/api/exercises/")) {
                int id = parsePositiveInt(path.substring("/api/exercises/".length()), "exercise ID");
                if ("PUT".equals(method)) {
                    gymsyncapp.Exercise x = repo.getAllExercises().stream().filter(i -> i.exerciseId == id).findFirst().orElse(null);
                    if (x == null) { sendError(ex, 404, "Exercise not found."); return; }
                    Map<String, String> b = parseFlatJson(readBody(ex));
                    String name = b.getOrDefault("name", "").trim();
                    String desc = b.getOrDefault("description", "").trim();
                    if (name.isEmpty() || desc.isEmpty()) { sendError(ex, 400, "Name and instructions are required."); return; }
                    if (repo.exerciseNameExists(name, id)) { sendError(ex, 400, "Exercise name already exists."); return; }
                    x.name = name;
                    x.muscleGroup = b.getOrDefault("muscleGroup", x.muscleGroup);
                    x.difficulty = b.getOrDefault("difficulty", x.difficulty);
                    x.equipment = b.getOrDefault("equipment", x.equipment);
                    x.description = desc;
                    repo.saveExercise(x);
                    sendJson(ex, 200, obj("exercise", exerciseJson(x)));
                    return;
                }
                if ("DELETE".equals(method)) {
                    if (repo.isExerciseUsedInPlans(id)) {
                        sendError(ex, 400, "Cannot delete exercise because it is used in one or more workout plans.");
                        return;
                    }
                    repo.deleteExercise(id);
                    sendJson(ex, 200, obj("deleted", true));
                    return;
                }
            }

            if ("GET".equals(method) && "/api/workout-plans".equals(path)) {
                int userId = 0;
                String rawUid = extractQueryParam(query, "userId");
                if (!rawUid.isEmpty()) userId = parsePositiveInt(rawUid, "userId");
                List<String> items = new ArrayList<>();
                for (gymsyncapp.WorkoutPlan wp : repo.getWorkoutPlansByUser(userId)) {
                    items.add(workoutPlanJson(wp));
                }
                sendJson(ex, 200, obj("plans", arr(items)));
                return;
            }

            if ("POST".equals(method) && "/api/workout-plans".equals(path)) {
                Map<String, Object> body = parseJsonObject(readBody(ex));
                int userId = parsePositiveInt(jsonAsString(body.get("userId")), "member ID");
                if (findMember(userId) == null) {
                    sendError(ex, 404, "Member not found.");
                    return;
                }
                String goal = jsonAsString(body.get("goal")).trim();
                if (goal.isEmpty()) {
                    sendError(ex, 400, "Custom routine title (goal) is required.");
                    return;
                }
                if (goal.length() > 120) {
                    sendError(ex, 400, "Routine title must be 120 characters or fewer.");
                    return;
                }
                Object rawExercises = body.get("exercises");
                if (!(rawExercises instanceof List)) {
                    sendError(ex, 400, "exercises must be a non-empty array.");
                    return;
                }
                List<?> rawList = (List<?>) rawExercises;
                if (rawList.isEmpty() || rawList.size() > 40) {
                    sendError(ex, 400, "Custom workout plan must include between 1 and 40 exercises.");
                    return;
                }

                Map<Integer, gymsyncapp.Exercise> exerciseById = new HashMap<>();
                for (gymsyncapp.Exercise item : repo.getAllExercises()) {
                    exerciseById.put(item.exerciseId, item);
                }

                gymsyncapp.WorkoutPlan custom = new gymsyncapp.WorkoutPlan();
                custom.userId = userId;
                custom.planType = "custom";
                custom.goal = goal;
                custom.dateCreated = LocalDate.now();

                for (int idx = 0; idx < rawList.size(); idx++) {
                    Object entry = rawList.get(idx);
                    if (!(entry instanceof Map)) {
                        sendError(ex, 400, "Exercise item #" + (idx + 1) + " must be an object.");
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> itemMap = (Map<String, Object>) entry;
                    int exerciseId = parsePositiveInt(jsonAsString(itemMap.get("exerciseId")), "exerciseId");
                    gymsyncapp.Exercise matched = exerciseById.get(exerciseId);
                    if (matched == null) {
                        sendError(ex, 400, "Exercise #" + exerciseId + " was not found in the library.");
                        return;
                    }
                    String day = normalizeDayOfWeek(jsonAsString(itemMap.get("dayOfWeek")));
                    if (day == null) {
                        sendError(ex, 400, "dayOfWeek must be a valid weekday (Monday through Sunday).");
                        return;
                    }
                    int sets = parsePositiveInt(jsonAsString(itemMap.get("sets")), "sets");
                    if (sets < 1 || sets > 20) {
                        sendError(ex, 400, "sets must be between 1 and 20.");
                        return;
                    }
                    String reps = jsonAsString(itemMap.get("reps")).trim();
                    if (reps.isEmpty() || reps.length() > 25) {
                        sendError(ex, 400, "reps is required and must be 25 characters or fewer.");
                        return;
                    }

                    gymsyncapp.WorkoutPlanExercise wpe = new gymsyncapp.WorkoutPlanExercise();
                    wpe.exerciseId = matched.exerciseId;
                    wpe.exerciseName = matched.name;
                    wpe.muscleGroup = matched.muscleGroup;
                    wpe.equipment = matched.equipment;
                    wpe.dayOfWeek = day;
                    wpe.sets = sets;
                    wpe.reps = reps;
                    custom.exercises.add(wpe);
                }

                repo.saveWorkoutPlan(custom);
                sendJson(ex, 201, obj("plan", workoutPlanJson(custom)));
                return;
            }

            if ("POST".equals(method) && "/api/workout-plans/generate".equals(path)) {
                Map<String, String> b = parseFlatJson(readBody(ex));
                int userId = parsePositiveInt(b.getOrDefault("userId", ""), "member ID");
                if (findMember(userId) == null) {
                    sendError(ex, 404, "Member not found.");
                    return;
                }
                String goal = b.getOrDefault("goal", "General Fitness").trim();
                String level = normalizeFitnessLevel(b.getOrDefault("fitnessLevel", "Beginner"));
                if (level == null) {
                    sendError(ex, 400, "Fitness level must be Beginner, Intermediate, or Advanced.");
                    return;
                }
                int days = parsePositiveInt(b.getOrDefault("daysPerWeek", "3"), "daysPerWeek");
                if (days < 2 || days > 5) {
                    sendError(ex, 400, "daysPerWeek must be between 2 and 5.");
                    return;
                }
                String equip = b.getOrDefault("equipmentPref", "Full Gym");
                boolean includeMachines = !"false".equalsIgnoreCase(b.getOrDefault("includeMachines", "true"));
                gymsyncapp.WorkoutPlan plan = gymsyncapp.BusinessRules.generateRuleBasedPlan(
                        userId, goal, level, days, equip, includeMachines, repo.getAllExercises());
                repo.saveWorkoutPlan(plan);
                sendJson(ex, 201, obj("plan", workoutPlanJson(plan)));
                return;
            }

            if (path.startsWith("/api/workout-plans/") && "DELETE".equals(method)) {
                int id = parsePositiveInt(path.substring("/api/workout-plans/".length()), "workout plan ID");
                repo.deleteWorkoutPlan(id);
                sendJson(ex, 200, obj("deleted", true));
                return;
            }

            if ("GET".equals(method) && "/api/reports".equals(path)) {
                LocalDate today = LocalDate.now();
                int activeCount = 0, expiredCount = 0, expiring7Count = 0;
                List<String> expSoon = new ArrayList<>();
                for (gymsyncapp.User m : repo.getMembers()) {
                    gymsyncapp.Membership latest = repo.getLatestMembership(m.userId);
                    String st = latest != null ? latest.getComputedStatus(today) : "Expired";
                    long days = latest != null ? latest.getDaysRemaining(today) : -999;
                    if ("Active".equalsIgnoreCase(st)) {
                        activeCount++;
                        if (days >= 0 && days <= 7) {
                            expiring7Count++;
                            expSoon.add(obj("name", m.getFullName(), "endDate", latest.endDate.toString(), "days", days));
                        }
                    } else {
                        expiredCount++;
                    }
                }
                int total = activeCount + expiredCount;
                String activeShare = total > 0 ? String.format(Locale.ROOT, "%.1f%%", (activeCount * 100.0) / total) : "0%";
                String expiredShare = total > 0 ? String.format(Locale.ROOT, "%.1f%%", (expiredCount * 100.0) / total) : "0%";
                Map<YearMonth, Integer> monthly = new TreeMap<>(Comparator.reverseOrder());
                for (gymsyncapp.Membership m : repo.getAllMemberships()) {
                    YearMonth ym = YearMonth.from(m.startDate);
                    monthly.put(ym, monthly.getOrDefault(ym, 0) + 1);
                }
                List<String> monthlyArr = new ArrayList<>();
                for (Map.Entry<YearMonth, Integer> e : monthly.entrySet()) {
                    monthlyArr.add(obj("month", e.getKey().toString(), "count", e.getValue()));
                }
                sendJson(ex, 200, obj(
                        "totalMembers", total,
                        "activeCount", activeCount,
                        "expiredCount", expiredCount,
                        "expiring7Count", expiring7Count,
                        "activeShare", activeShare,
                        "expiredShare", expiredShare,
                        "expiringSoon", arr(expSoon),
                        "monthlyRenewals", arr(monthlyArr)
                ));
                return;
            }

            if ("GET".equals(method) && "/api/reports/csv".equals(path)) {
                String csv = buildReportsCsv();
                byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
                ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"gymsync_reports_" + LocalDate.now() + ".csv\"");
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
                return;
            }

            sendError(ex, 404, "Endpoint not found: " + path);
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        String reqPath = ex.getRequestURI().getPath();
        if (reqPath == null || "/".equals(reqPath)) reqPath = "/index.html";
        Path candidate = DIST_DIR.resolve(reqPath.substring(1)).normalize();
        if (!candidate.startsWith(DIST_DIR) || !Files.exists(candidate) || Files.isDirectory(candidate)) {
            candidate = DIST_DIR.resolve("index.html");
        }
        if (!Files.exists(candidate)) {
            byte[] msg = "Frontend not built. Run: npm --prefix frontend run build".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(msg); }
            return;
        }
        String name = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
        String mime = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".js") ? "application/javascript; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".svg") ? "image/svg+xml"
                : name.endsWith(".png") ? "image/png"
                : "application/octet-stream";
        byte[] bytes = Files.readAllBytes(candidate);
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private gymsyncapp.User findUser(int id) {
        return repo.getAllUsers().stream().filter(u -> u.userId == id).findFirst().orElse(null);
    }

    private gymsyncapp.User findMember(int id) {
        gymsyncapp.User u = findUser(id);
        return (u != null && "Member".equalsIgnoreCase(u.role)) ? u : null;
    }

    private static int parsePositiveInt(String raw, String fieldLabel) {
        try {
            int v = Integer.parseInt(raw == null ? "" : raw.trim());
            if (v <= 0) throw new IllegalArgumentException(fieldLabel + " must be a positive integer.");
            return v;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + fieldLabel + ".");
        }
    }

    private static String extractQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return "";
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static String normalizeDayOfWeek(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String val = raw.trim();
        for (String day : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}) {
            if (day.equalsIgnoreCase(val)) return day;
        }
        return null;
    }

    private static String jsonAsString(Object val) {
        return val == null ? "" : String.valueOf(val);
    }

    private static String normalizeFitnessLevel(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Beginner";
        String val = raw.trim();
        if ("Beginner".equalsIgnoreCase(val)) return "Beginner";
        if ("Intermediate".equalsIgnoreCase(val)) return "Intermediate";
        if ("Advanced".equalsIgnoreCase(val)) return "Advanced";
        return null;
    }

    private String validateMember(Map<String, String> b, boolean isNew, int excludeId) {
        String un = b.getOrDefault("username", "").trim();
        String pw = b.getOrDefault("password", "").trim();
        String fn = b.getOrDefault("firstName", "").trim();
        String ln = b.getOrDefault("lastName", "").trim();
        String ct = b.getOrDefault("contactNo", "").trim();
        String emName = b.getOrDefault("emergencyContactName", "").trim();
        String emPhone = b.getOrDefault("emergencyContactNo", "").trim();
        String bd = b.getOrDefault("birthdate", "").trim();
        String level = normalizeFitnessLevel(b.getOrDefault("fitnessLevel", "Beginner"));
        if (un.isEmpty() || fn.isEmpty() || ln.isEmpty() || ct.isEmpty() || "09".equals(ct)) {
            return "Username, First Name, Last Name, and a valid Contact No. are required.";
        }
        if (emName.isEmpty() || emPhone.isEmpty() || "09".equals(emPhone)) {
            return "Emergency Contact Name and Phone No. are required.";
        }
        if ((isNew || !pw.isEmpty()) && pw.length() < 4) return "Password must be at least 4 characters.";
        if (level == null) return "Fitness level must be Beginner, Intermediate, or Advanced.";
        if (repo.usernameExists(un, excludeId)) return "Username '" + un + "' is already taken.";
        if (!bd.isEmpty()) {
            try { LocalDate.parse(bd); } catch (DateTimeParseException ex) {
                return "Birthdate must be in YYYY-MM-DD format.";
            }
        }
        return null;
    }

    private gymsyncapp.User applyMemberFields(gymsyncapp.User u, Map<String, String> b) {
        u.username = b.getOrDefault("username", "").trim();
        u.role = "Member";
        u.firstName = b.getOrDefault("firstName", "").trim();
        u.lastName = b.getOrDefault("lastName", "").trim();
        u.contactNo = b.getOrDefault("contactNo", "").trim();
        String bd = b.getOrDefault("birthdate", "").trim();
        u.birthdate = bd.isEmpty() ? null : LocalDate.parse(bd);
        u.gender = b.getOrDefault("gender", "Male");
        u.address = b.getOrDefault("address", "").trim();
        u.emergencyContactName = b.getOrDefault("emergencyContactName", "").trim();
        u.emergencyContactNo = b.getOrDefault("emergencyContactNo", "").trim();
        String level = normalizeFitnessLevel(b.getOrDefault("fitnessLevel", "Beginner"));
        u.fitnessLevel = level != null ? level : "Beginner";
        return u;
    }

    private String memberDetailJson(gymsyncapp.User m, LocalDate today) {
        gymsyncapp.Membership latest = repo.getLatestMembership(m.userId);
        String status = latest != null ? latest.getComputedStatus(today) : "Expired";
        long days = latest != null ? latest.getDaysRemaining(today) : -999;
        String bal = latest != null ? (days >= 0 ? days + "d left" : Math.abs(days) + "d overdue") : "None";
        LocalDate[] nextR = gymsyncapp.BusinessRules.calculateRenewalDates(latest != null ? latest.endDate : null, today);
        return obj(
                "userId", m.userId,
                "username", m.username,
                "firstName", m.firstName,
                "lastName", m.lastName,
                "fullName", m.getFullName(),
                "initials", m.getInitials(),
                "contactNo", m.contactNo,
                "birthdate", m.birthdate != null ? m.birthdate.toString() : "",
                "gender", m.gender != null ? m.gender : "",
                "address", m.address != null ? m.address : "",
                "emergencyContactName", m.emergencyContactName != null ? m.emergencyContactName : "",
                "emergencyContactNo", m.emergencyContactNo != null ? m.emergencyContactNo : "",
                "fitnessLevel", m.fitnessLevel != null ? m.fitnessLevel : "Beginner",
                "startDate", latest != null ? latest.startDate.toString() : "N/A",
                "endDate", latest != null ? latest.endDate.toString() : "N/A",
                "status", status,
                "daysRemaining", days,
                "balanceLabel", bal,
                "nextRenewalStart", nextR[0].toString(),
                "nextRenewalEnd", nextR[1].toString()
        );
    }

    private String userJson(gymsyncapp.User u) {
        return obj(
                "userId", u.userId,
                "username", u.username,
                "role", u.role,
                "firstName", u.firstName,
                "lastName", u.lastName,
                "fullName", u.getFullName(),
                "contactNo", u.contactNo
        );
    }

    private String exerciseJson(gymsyncapp.Exercise x) {
        return obj(
                "exerciseId", x.exerciseId,
                "name", x.name,
                "muscleGroup", x.muscleGroup,
                "difficulty", x.difficulty,
                "equipment", x.equipment,
                "description", x.description
        );
    }

    private String workoutPlanJson(gymsyncapp.WorkoutPlan wp) {
        List<String> exArr = new ArrayList<>();
        for (gymsyncapp.WorkoutPlanExercise item : wp.exercises) {
            exArr.add(obj(
                    "exerciseId", item.exerciseId,
                    "exerciseName", item.exerciseName,
                    "muscleGroup", item.muscleGroup,
                    "equipment", item.equipment,
                    "dayOfWeek", item.dayOfWeek,
                    "sets", item.sets,
                    "reps", item.reps
            ));
        }
        return obj(
                "workoutPlanId", wp.workoutPlanId,
                "userId", wp.userId,
                "planType", wp.planType != null ? wp.planType : "suggested",
                "goal", wp.goal,
                "dateCreated", wp.dateCreated != null ? wp.dateCreated.toString() : "",
                "exercises", arr(exArr)
        );
    }

    private String buildReportsCsv() {
        LocalDate today = LocalDate.now();
        int active = 0, expired = 0;
        List<String[]> expSoon = new ArrayList<>();
        for (gymsyncapp.User m : repo.getMembers()) {
            gymsyncapp.Membership latest = repo.getLatestMembership(m.userId);
            String st = latest != null ? latest.getComputedStatus(today) : "Expired";
            long days = latest != null ? latest.getDaysRemaining(today) : -999;
            if ("Active".equalsIgnoreCase(st)) {
                active++;
                if (days >= 0 && days <= 7) expSoon.add(new String[]{m.getFullName(), latest.endDate.toString(), String.valueOf(days)});
            } else {
                expired++;
            }
        }
        int total = active + expired;
        StringBuilder sb = new StringBuilder();
        sb.append("Report 1: Active vs Expired\nStatus,Members,Share\n");
        sb.append("Active,").append(active).append(',').append(total > 0 ? String.format(Locale.ROOT, "%.1f%%", (active * 100.0) / total) : "0%").append('\n');
        sb.append("Expired,").append(expired).append(',').append(total > 0 ? String.format(Locale.ROOT, "%.1f%%", (expired * 100.0) / total) : "0%").append('\n');
        sb.append("\nReport 2: Expiring Within 7 Days\nMember,End Date,Days\n");
        for (String[] r : expSoon) sb.append('"').append(r[0].replace("\"", "\"\"")).append("\",").append(r[1]).append(',').append(r[2]).append('\n');
        return sb.toString();
    }

    // Minimal zero-dependency JSON helpers
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseFlatJson(String raw) {
        Map<String, Object> parsed = parseJsonObject(raw);
        Map<String, String> flat = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            Object val = entry.getValue();
            flat.put(entry.getKey(), val == null ? "" : String.valueOf(val));
        }
        return flat;
    }

    private static Map<String, Object> parseJsonObject(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new LinkedHashMap<>();
        Object parsed = new JsonParser(raw).parseValue();
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Request body must be a JSON object.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) parsed;
        return obj;
    }

    private static final class JsonParser {
        private final String s;
        private int pos;

        JsonParser(String raw) {
            this.s = raw;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON.");
            char c = s.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            throw new IllegalArgumentException("Unexpected character in JSON at position " + pos + ".");
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < s.length()) {
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != '"') {
                    throw new IllegalArgumentException("Expected JSON string key at position " + pos + ".");
                }
                String key = parseString();
                skipWhitespace();
                if (pos >= s.length() || s.charAt(pos) != ':') {
                    throw new IllegalArgumentException("Expected ':' after JSON key at position " + pos + ".");
                }
                pos++;
                map.put(key, parseValue());
                skipWhitespace();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                } else if (pos < s.length() && s.charAt(pos) == '}') {
                    pos++;
                    return map;
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' in JSON object at position " + pos + ".");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON object.");
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWhitespace();
            if (pos < s.length() && s.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < s.length()) {
                list.add(parseValue());
                skipWhitespace();
                if (pos < s.length() && s.charAt(pos) == ',') {
                    pos++;
                } else if (pos < s.length() && s.charAt(pos) == ']') {
                    pos++;
                    return list;
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' in JSON array at position " + pos + ".");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON array.");
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            pos++; // opening quote
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\\') {
                    if (pos >= s.length()) throw new IllegalArgumentException("Unterminated JSON escape.");
                    char esc = s.charAt(pos++);
                    if (esc == 'n') sb.append('\n');
                    else if (esc == 'r') sb.append('\r');
                    else if (esc == 't') sb.append('\t');
                    else if (esc == 'b') sb.append('\b');
                    else if (esc == 'f') sb.append('\f');
                    else if (esc == 'u' && pos + 4 <= s.length()) {
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                    } else {
                        sb.append(esc);
                    }
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid JSON boolean at position " + pos + ".");
        }

        private Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid JSON literal at position " + pos + ".");
        }

        private String parseNumber() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
                else break;
            }
            return s.substring(start, pos);
        }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }
    }

    private static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(String.valueOf(kv[i]))).append("\":");
            Object v = kv[i + 1];
            if (v == null) sb.append("null");
            else if (v instanceof Boolean || v instanceof Number) sb.append(v);
            else {
                String sv = String.valueOf(v);
                if ((sv.startsWith("{") && sv.endsWith("}")) || (sv.startsWith("[") && sv.endsWith("]"))) {
                    sb.append(sv);
                } else {
                    sb.append('"').append(esc(sv)).append('"');
                }
            }
        }
        return sb.append('}').toString();
    }

    private static String arr(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void sendError(HttpExchange ex, int status, String msg) throws IOException {
        sendJson(ex, status, obj("error", msg));
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        if (status == 204) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
