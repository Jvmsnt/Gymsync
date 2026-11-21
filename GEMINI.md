# GymSync Project Guidelines

- **Scope**: Front Desk & Gym Operations desktop app (`Admin` and `Staff` desk operators only; `Member` accounts are managed by desk staff and do not sign into the desktop console).
- **Tech Stack**: Java, Java Swing, JDBC (MySQL primary via `-Dgymsync.db.url` / `gymsync_database_schema_and_seed.sql`, with SQLite/in-memory fallback).
- **UI Design Rules (Ban the AI Look)**:
  - Use the slate surface hierarchy (`#111318` canvas, `#171A21` sidebar/headers, `#1F242D` cards, `#2A303C` controls, `1px` `#343B49` borders) with `#10B981` emerald as the single primary accent.
  - Never use purple/indigo/rainbow gradients, glowing background blobs, or Unicode emojis inside Swing buttons/tabs.
  - Keep left sidebar navigation (`buildAppShell`), hairline inspector rows, and explicit table column widths so text never truncates to `...`.
- **Build Check**: Always verify `javac -Xlint:deprecation` compiles with 0 errors and 0 warnings before committing.
