# GymSync Project Guidelines

- **Scope**: Front Desk & Gym Operations app (`Admin` and `Staff` desk operators only; `Member` accounts are managed by desk staff and do not sign into the operator console).
- **Integrative Stack**:
  - **Backend**: Java (`GymsyncServer.java` REST API + `gymsyncapp.java` `DataRepository`, supporting MySQL JDBC via `-Dgymsync.db.url` and `gymsync_database_schema_and_seed.sql` with SQLite/in-memory fallback).
  - **Frontend**: React + Vite + Tailwind CSS v4 (`frontend/`), built with the curated libraries from `pick-ui-library` & `use-ui-libraries`: `@radix-ui/*` primitives, `cva`, `sonner` (toasts), `vaul` (slide-over drawer), `cmdk` (`Ctrl+K` command menu), `@number-flow/react` (animated counters), `recharts` (charts), `motion` (spring navigation & transitions), and `lucide-react`.
- **UI Design Rules (Ban the AI Look)**:
  - Never hand-write standard UI primitives when `Radix`/`Sonner`/`Vaul`/`cmdk`/`Recharts` handle them.
  - Keep a crisp, spacious layout (Top Command Bar + Bento Overview + Full-Width Directory with `Vaul` Slide-Over Drawer + One-Standout Digital Member Pass card).
  - Always use `tabular-nums` on numbers, `-0.02em` heading tracking, and `active:scale-[0.97]` button feedback.
- **Build Check**: Always verify `javac -Xlint:deprecation -cp ".;lib/*" GymsyncServer.java gymsyncapp.java` compiles cleanly and `npm --prefix frontend run build` succeeds before committing.
