# knowledge_engine_svc

Spring Boot service for the project-centered Knowledge Engine redesign.

## What This Service Contains

- Project creation and ingestion management APIs under `/api/v1/projects`
- GCS upload URL and project file listing using `projects/{projectName}/...`
- Process tracking endpoints for ingestion, consolidation, and planning generation
- Project-scoped JPA entities and repositories for source evidence, normalized graph knowledge, relationships, and delivery planning
- Liquibase baseline schema in `src/main/resources/db/changelog/sql/project-centered-schema.sql`
- Graph-level finalized knowledge stored as `knowledge_chunks`
- Query APIs moved to sibling service `knowledge-query-svc`
- Ported ingestion logic from `knowledge-ingestion-svc`: GCS reads, Tika/Gemini parsing, document-level analysis, semantic classification, embeddings, enhanced chunk extraction, graph persistence, cross-document relationship inference, golden chunk generation, and basic delivery planning generation

## Package Layout

- `com.kengine.knowledge.service`: project-facing services used by REST controllers
- `com.kengine.knowledge.ingestion`: ingestion-specific parser, AI, extraction, storage, graph, golden chunk, and planning generation internals

## Local Commands

```powershell
.\gradlew test
.\gradlew bootRun
```

The service defaults to port `8087`.
