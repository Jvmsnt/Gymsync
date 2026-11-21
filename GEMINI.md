# GymSync Project Guidelines

- **Scope**: Front Desk & Gym Operations app (`Admin` and `Staff` desk operators only; `Member` accounts are managed by desk staff and do not sign into the operator console).
- **Integrative Stack**:
  - **Backend**: Java (`GymsyncServer.java` REST API + `gymsyncapp.java` `DataRepository`, supporting MySQL JDBC via `-Dgymsync.db.url` and `gymsync_database_schema_and_seed.sql` with SQLite/in-memory fallback).
  - **Frontend**: React + Vite + Tailwind CSS + Lucide Icons (`frontend/`), built to `frontend/dist/` and served by `GymsyncServer.java` at `http://localhost:8080`.
- **UI Design Rules (Ban the AI Look)**:
  - Use clean `shadcn/ui` / `AlignUI` zinc surfaces (`bg-zinc-50`/`bg-white` Light, `bg-zinc-950` Dark) with `1px` `border-zinc-200`/`dark:border-zinc-800` borders and `#059669` emerald primary accent.
  - Never use generic purple/indigo gradients, glowing ambient blobs, or Unicode emojis inside UI buttons/tabs (use `lucide-react` SVG icons).
- **Build Check**: Always verify `javac -Xlint:deprecation -cp ".;lib/*" GymsyncServer.java gymsyncapp.java` compiles cleanly and `npm --prefix frontend run build` succeeds before committing.
