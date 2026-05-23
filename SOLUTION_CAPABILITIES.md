# Knowledge Ingestion Service Capabilities

This service is the generic knowledge-ingestion and knowledge-graph builder for software delivery, modernization, and migration programs.

It ingests source artifacts from GCS, extracts document and chunk-level knowledge with Vertex AI, persists source evidence and structured facts in Cloud SQL for PostgreSQL, and stores embeddings in pgvector-backed columns for semantic retrieval and downstream RAG workflows.

## Current Architecture

```text
+-----------------------------+
| GCS Bucket:                 |
| kengine-knowledge-artifacts |
|                             |
| staged/<project_id>/        |
|   <timestamp>/              |
|                             |
| processed/<project_id>/     |
|   <timestamp>/              |
+-------------+---------------+
              |
              v (Every 5 mins)
+-------------+---------------+
| Scheduler Service           |
| Picks files from staged     |
| folders and queues them     |
+-------------+---------------+
              |
              v
+-------------+---------------+
| knowledge-ingestion-svc     |
| Spring Boot + Jetty         |
| ThreadPool Executor (10)    |
+-------------+---------------+
              |
              v
+-------------+---------------+
| Vertex AI                   |
| classification, extraction, |
| embeddings, multimodal      |
+-------------+---------------+
              |
              v
+-------------+---------------+
| Cloud SQL PostgreSQL        |
| schema: knowledge           |
| pgvector vector(768)        |
+-------------+---------------+
              |
              v
+-------------+---------------+
| MCP / AGENT / extractor     |
| consumers                   |
+-----------------------------+
```

## Runtime Stack

```text
Java 21
Spring Boot 3.4.2
Spring MVC + Jetty
Spring Scheduler
Spring Data JPA
Liquibase
Cloud SQL for PostgreSQL
pgvector
Google Cloud Storage
Vertex AI
Apache Tika
```

The service currently listens on port `8086`.

## Cloud SQL Configuration

The active persistence target is:

```text
Cloud SQL instance: appdata-inst-001
Database:           kengine-db
Application schema: knowledge
Liquibase schema:   public
Application user:   kengine-app
```

Configuration is environment-driven:

```text
KENGINE_DB_HOST
KENGINE_DB_PORT
KENGINE_DB_NAME
KENGINE_DB_USER
KENGINE_DB_PASSWORD
```

Liquibase uses `classpath:/db/changelog/db.changelog-master.xml`.

## Schema Management

Liquibase manages the database schema through a regenerated baseline changelog:

```text
db.changelog-master.xml
sql/baseline-001-knowledge-schema.sql
```

The Liquibase-created schema includes:

```text
Core ingestion tables:
- knowledge.projects
- knowledge.domains
- knowledge.subdomains
- knowledge.artifacts
- knowledge.ingestion_documents
- knowledge.semantic_chunks

Generic extraction tables:
- knowledge.solution_components
- knowledge.business_rules
- knowledge.business_flows
- knowledge.business_flow_steps
- knowledge.deployment_resources
- knowledge.deployment_resource_configs
- knowledge.knowledge_relationships
- knowledge.knowledge_notes
- knowledge.usage_profiles
- knowledge.resource_cost_estimates
- knowledge.knowledge_data_models
- knowledge.knowledge_data_fields

Business layer tables:
- knowledge.business_roles
- knowledge.business_users
- knowledge.business_capabilities
- knowledge.business_role_workflows
- knowledge.business_decisions
- knowledge.business_terms
- knowledge.business_policies
- knowledge.business_metrics
```

Embedding columns use `vector(768)`, matching the configured Vertex AI embedding model `text-embedding-005`. The index changelog creates HNSW indexes with `vector_cosine_ops` for semantic-search-ready tables.

The Java entity/repository layer also targets these knowledge graph tables:

```text
- knowledge.document_knowledge
- knowledge.knowledge_components
- knowledge.knowledge_apis
- knowledge.knowledge_business_rules
- knowledge.knowledge_workflows
- knowledge.knowledge_workflow_steps
- knowledge.knowledge_integrations
- knowledge.knowledge_resources
```

These graph tables are included in the regenerated baseline SQL changelog.

## Ingestion Trigger

The service uses a scheduler-based approach with the following configuration:

```text
gcp.project-id=app-lab-001
gcp.storage.bucket-name=kengine-knowledge-artifacts
scheduler.enabled=true
```

### Folder Structure

The GCS bucket has two main folders:
- `staged/<project_id>/<ISO_TIMESTAMP>/` - Files awaiting processing
- `processed/<project_id>/<ISO_TIMESTAMP>/` - Successfully processed files

### Default Project

A default project folder is created on startup: `staged/default-project/`

### Scheduler Behavior

1. **Folder Creation Scheduler** (cron: `0 0 2 * * *` - daily at 2:00 AM):
   - Queries all projects from `knowledge.projects` table
   - Creates timestamped folders for each project: `staged/<project_id>/<ISO_TIMESTAMP>`
   - Creates corresponding processed folders: `processed/<project_id>/<ISO_TIMESTAMP>`
   - Falls back to "default-project" if no projects found

2. **File Processing Scheduler** (cron: `0 */5 * * * *` - every 5 minutes):
   - Lists all timestamped folders for each project
   - For each folder, lists all files in staged folder
   - Submits each file to processing queue (ThreadPool of 10 executors)
   - After successful processing, moves file to corresponding processed folder

The first path segment of the GCS object name becomes the project id. Root-level objects are rejected.

### Project Management Endpoints

```text
POST /api/v1/projects/{projectId}/folders
  Creates project-specific folder in staged directory

POST /api/v1/projects/{projectId}/timestamped-folders
  Creates timestamped folders for a project

GET /api/v1/projects/{projectId}/timestamped-folders
  Lists all timestamped folders for a project

GET /api/v1/projects/{projectId}/timestamped-folders/{timestamp}/files
  Lists files in a specific timestamped staged folder
```

## Parsing Capabilities

The active parser is `DocumentParserOrchestrator`.

It routes configured multimodal file types through Gemini multimodal extraction:

```text
pdf, png, jpg, jpeg, docx, pptx
```

Other files use Apache Tika text extraction.

The normalized parser output is `DocumentContent`, which can include:

```text
- text content
- diagram descriptions
- table content
- metadata
```

## Extraction Pipeline

The current ingestion flow is:

```text
GCS object
  v
parse document
  v
document-level analysis
  v
semantic chunking
  v
per-chunk classification
  v
per-chunk embedding
  v
context-aware chunk extraction
  v
persist source artifact and chunks
  v
build knowledge graph
  v
persist graph entities with embeddings
```

Document-level analysis runs before chunking so later chunk extraction has broader context. It extracts high-level domain, subdomain, architecture summary, key patterns, technologies, components, APIs, and workflows.

Chunk-level extraction then uses that document context to improve entity linking and confidence.

## Vertex AI Usage

The configured Vertex AI settings are:

```text
vertex.project-id=ai-lab-001-494218
vertex.location=us-central1
vertex.classification-model-name=gemini-2.5-flash
vertex.embedding-model-name=text-embedding-005
```

Vertex AI is used for:

```text
- document-level analysis
- chunk classification
- enhanced knowledge extraction
- multimodal document extraction
- source chunk embeddings
- entity embeddings for graph nodes
```

## XML Platform Detection

XML documents are analyzed by `XMLPlatformDetector`.

Detected platforms include:

```text
TIBCO MDM
TIBCO BPM
TIBCO BusinessWorks
Informatica MDM
SAP MDM
Oracle MDM
IBM InfoSphere MDM
Reltio MDM
Semarchy MDM
Pega BPM
Camunda BPMN
Activiti BPMN
jBPM
Flowable BPMN
BPMN 2.0 generic
Oracle BPEL
IBM BPM
Appian
SAP Workflow
Generic XML
```

Currently present platform-specific chunk prompts:

```text
- tibco-mdm-extraction-prompt.txt
- camunda-bpmn-extraction-prompt.txt
- pega-bpm-extraction-prompt.txt
```

Other detected platforms fall back to the generic enhanced extraction prompt until their specialized prompts are added.

## Knowledge Extracted

The service extracts and persists:

```text
- source artifacts and ingestion documents
- semantic chunks with source evidence and embeddings
- document-level architecture knowledge
- business and technical components
- APIs
- business rules
- workflows and workflow steps
- deployment resources and configuration
- data models and data fields
- integrations
- relationships
- usage profiles
- cost estimates
- business roles, users, capabilities, decisions, terms, policies, and metrics
```

## Knowledge Graph Persistence

`KnowledgeGraphService` is wired to build graph entities after source chunks have been saved.

It currently:

```text
- ensures domain and subdomain records exist
- saves document-level knowledge
- extracts business and technical components
- extracts business rules
- extracts workflows and workflow steps
- extracts deployment resources
- extracts relationships
- generates embeddings for graph entities
- persists graph entities through Spring Data repositories
```

Knowledge graph persistence is controlled by:

```text
ingestion.knowledge-graph.enable-persistence=true
ingestion.knowledge-graph.batch-size=100
```

Entity embedding generation is controlled by:

```text
ingestion.extraction.enable-entity-embeddings=true
```

## Duplicate Handling

Before processing, the service checks for an existing ingestion document by:

```text
project_id
source_bucket
source_object
content_hash
```

If the same source content was already ingested, classification, embedding, extraction, and persistence are skipped.

## Public Query Surface

The public controller exposes these endpoints:

```text
GET /api/v1/projects/{projectId}/knowledge/sources

GET /api/v1/projects/{projectId}/knowledge/chunks
    ?sourceObject={sourceObject}
    &q={query}
    &limit={limit}

GET /api/v1/projects/{projectId}/knowledge
    ?sourceObject={sourceObject}
```

Endpoint behavior:

```text
/sources
  Lists ingested artifacts for a project.

/chunks
  Returns source chunks for evidence/RAG context.
  Supports optional sourceObject filtering, text search, and limit.
  Default limit: 100
  Max limit: 1000

/knowledge
  Returns a GenericKnowledgeSnapshot containing extracted facts.
  Supports optional sourceObject filtering.
```

The public API is still retrieval-oriented. It does not yet expose dedicated semantic-search endpoints or full CRUD over graph entities, although the database schema and indexes are prepared for vector search.

## Downstream Uses

The service can feed:

```text
- MCP tools
- agent workflows
- LLD generation
- test-case generation
- code-generation context builders
- migration discovery workflows
- specialized platform extractors
- RAG retrieval pipelines
```

Recommended MCP-facing tool shapes remain:

```text
- list_sources
- get_source_chunks
- get_generic_knowledge
- get_generation_context
- get_lld_context
- get_testcase_context
- get_code_generation_context
```

## Migration And Modernization Role

The service now performs more than generic text extraction: it builds a pgvector-backed knowledge graph from documents and chunks.

For migration programs, it can identify platform type, extract generic and platform-assisted facts, persist evidence, and provide context for downstream migration agents.

It is still not intended to be the final precise parser for every platform export. Exact platform semantics should still be handled by specialized services when precision matters.

Examples:

```text
TIBCO MDM extractor:
  parse repository XML, entities, attributes, rulebases, workflows

BPMN extractor:
  parse process definitions, gateways, events, sequence flows

Pega extractor:
  parse case types, stages, flows, rules, decision tables

Integration extractor:
  parse service bindings, mappings, protocols, endpoints
```

Those specialized extractors can consume this service's chunks, graph entities, embeddings, and extracted facts.

## Current Boundaries

The current implementation does not yet provide:

```text
- a public vector similarity search endpoint
- a public graph traversal API
- exact structural parsers for every detected XML platform
- automatic deletion or replacement of stale rows from older ingestions
- a UI
- a full migration planning API
```

These are natural extension points on top of the current schema and graph persistence layer.

## Design Principle

The service should remain the reusable knowledge-ingestion layer.

Platform-specific precision, migration planning, and code generation should sit in downstream MCP tools, agents, or specialized extractor services that consume this knowledge base.
