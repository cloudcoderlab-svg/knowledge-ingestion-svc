# Knowledge Extractor Solution Capabilities

This service is the generic knowledge-base extractor for software delivery and migration programs.

It is intentionally not tied only to TIBCO MDM. It can ingest source material from normal development projects and from legacy/BPM migration projects, extract generic knowledge, persist it in Spanner, and expose that knowledge to downstream MCP tools, AGENTS, and specialized extractor services.

## Core Positioning

```text
+-----------------------------+
| knowledge-ingestion-svc     |
| Generic knowledge extractor |
+-------------+---------------+
              |
              v
+-------------+---------------+
| Spanner generic knowledge   |
| Chunks, embeddings, facts   |
+-------------+---------------+
              |
              +-------------------------------+
              |                               |
              v                               v
+-------------+---------------+   +-----------+-------------------+
| MCP / AGENT layer           |   | Specialized extractors        |
| Normal development          |   | TIBCO, BPMN, Pega, etc.       |
+-------------+---------------+   +-----------+-------------------+
              |                               |
              v                               v
+-------------+---------------+   +-----------+-------------------+
| LLD, tests, code, stories   |   | Precise migration facts       |
| for normal development      |   | store / API                   |
+-----------------------------+   +-----------+-------------------+
                                              |
                                              v
                                  +-----------+-------------------+
                                  | MCP / AGENT migration flows   |
                                  +-------------------------------+
```

## What This Service Does

The current service supports the first-stage knowledge extraction pipeline:

```text
Source artifacts
  |
  |-- XML
  |-- requirements
  |-- design documents
  |-- architecture notes
  |-- diagrams with extractable text
  |-- estimates / sizing documents
  v
GCS bucket
  v
knowledge-ingestion-svc
  |
  |-- parse source content
  |-- chunk source content
  |-- classify chunks
  |-- create embeddings
  |-- extract generic solution knowledge using LLM
  v
Cloud Spanner
```

## Generic Knowledge Extracted

The service extracts and stores:

```text
+----------------------------+------------------------------------------+
| Knowledge type             | Purpose                                  |
+----------------------------+------------------------------------------+
| Source chunks              | Evidence and RAG context                 |
| Embeddings                 | Semantic retrieval                       |
| Business rules             | Functional logic and validations         |
| Business flows             | End-to-end business processes            |
| Business flow steps        | Ordered activities within a flow         |
| Business components        | Capabilities and domain-owned modules    |
| Technical components       | APIs, services, jobs, adapters, modules  |
| Deployment resources       | Cloud and on-prem runtime dependencies   |
| Relationships              | Component, flow, resource dependencies   |
| Knowledge notes            | Architecture and migration observations  |
| Usage profiles             | Volume, traffic, retention, DR details   |
| Cost estimates             | Resource cost and sizing assumptions     |
+----------------------------+------------------------------------------+
```

## Current Storage Model

```text
knowledge.projects
  |
  +-- knowledge.artifacts
        |
        +-- knowledge.ingestion_documents
        |
        +-- knowledge.semantic_chunks
        |     |
        |     +-- chunk text
        |     +-- embeddings
        |     +-- source metadata
        |
        +-- knowledge.business_rules
        +-- knowledge.business_flows
        |     |
        |     +-- knowledge.business_flow_steps
        |
        +-- knowledge.solution_components
        +-- knowledge.deployment_resources
        |     |
        |     +-- knowledge.deployment_resource_configs
        |
        +-- knowledge.knowledge_relationships
        +-- knowledge.knowledge_notes
        +-- knowledge.usage_profiles
        +-- knowledge.resource_cost_estimates
```

## Current Query Surface

The service exposes retrieval endpoints for downstream services and MCP wrappers:

```text
GET /api/v1/projects/{projectId}/knowledge/sources

GET /api/v1/projects/{projectId}/knowledge/chunks
    ?sourceObject={sourceObject}
    &q={query}
    &limit={limit}

GET /api/v1/projects/{projectId}/knowledge
    ?sourceObject={sourceObject}
```

These endpoints allow downstream services to retrieve:

```text
+---------------------+-----------------------------------------------+
| Endpoint             | Use                                           |
+---------------------+-----------------------------------------------+
| /sources             | Discover ingested source artifacts            |
| /chunks              | Retrieve source evidence and RAG text         |
| /knowledge           | Retrieve extracted generic facts              |
+---------------------+-----------------------------------------------+
```

## Normal Development Flow

For normal development, this service can support AGENT workflows for epics, stories, LLD, test cases, and code generation.

```text
Requirements / diagrams / design docs
  v
knowledge-ingestion-svc
  v
Spanner knowledge base
  v
MCP tools
  v
AGENT
  |
  |-- generate epics
  |-- generate user stories
  |-- generate LLD
  |-- generate test cases
  |-- generate Java / Spring Boot code
```

Recommended MCP tools for normal development:

```text
+-----------------------------+-----------------------------------------+
| MCP tool                    | Purpose                                 |
+-----------------------------+-----------------------------------------+
| list_sources                | List available documents and diagrams   |
| get_source_chunks           | Retrieve evidence text                  |
| get_generic_knowledge       | Retrieve extracted facts                |
| get_generation_context      | Build complete generation context       |
| get_lld_context             | Build LLD-specific context              |
| get_testcase_context        | Build test-case-specific context        |
| get_code_generation_context | Build code-generation context           |
+-----------------------------+-----------------------------------------+
```

## Migration Flow

For migration, this service acts as the generic first-stage extractor. It should not contain every platform-specific parser. Specialized extractors should be implemented as separate services.

```text
Legacy / BPM / MDM artifacts
  |
  |-- TIBCO MDM XML
  |-- BPMN
  |-- Pega exports
  |-- IBM BPM exports
  |-- Camunda BPMN
  |-- Appian packages
  |-- design docs
  |-- diagrams
  v
knowledge-ingestion-svc
  |
  |-- chunks
  |-- embeddings
  |-- generic business rules
  |-- generic business flows
  |-- generic components
  |-- generic relationships
  v
Spanner knowledge base
  v
Specialized extractor service
  |
  |-- precise platform-specific parsing
  |-- exact workflow model
  |-- exact rule model
  |-- exact data mappings
  |-- exact dependency model
  v
Precise migration facts store / API
  |
  |-- platform-specific entities
  |-- process definitions
  |-- rule definitions
  |-- workflow states and transitions
  |-- service bindings
  |-- data mappings
  |-- migration gaps and assumptions
  v
MCP / AGENT
  |
  |-- migration epics
  |-- migration stories
  |-- target architecture
  |-- LLD
  |-- test cases
  |-- Java microservice code
```

## Specialized Extractor Services

Specialized extractors should be separate services that consume the generic knowledge base.

```text
+--------------------------+
| Spanner generic KB       |
| chunks and generic facts |
+------------+-------------+
             |
             v
+------------+-------------+
| Specialized extractors   |
+------------+-------------+
             |
             +-----------------------------+
             |                             |
             v                             v
+------------+-------------+   +-----------+--------------+
| TIBCO MDM extractor      |   | BPMN extractor           |
+------------+-------------+   +-----------+--------------+
             |                             |
             v                             v
+------------+-------------+   +-----------+--------------+
| Precise TIBCO facts      |   | Precise BPMN facts       |
+------------+-------------+   +-----------+--------------+
             |                             |
             +--------------+--------------+
                            |
                            v
              +-------------+---------------+
              | Precise migration facts     |
              | store / API                 |
              +-------------+---------------+
                            |
                            v
              +-------------+---------------+
              | MCP migration tools         |
              | Migration AGENTS            |
              +-----------------------------+

Other extractor services can plug into the same pattern:

+-------------------+     +-------------------+     +-------------------+
| Pega extractor    |     | IBM BPM extractor |     | Camunda extractor |
+-------------------+     +-------------------+     +-------------------+

+-------------------+
| Appian extractor  |
+-------------------+
```

## Why Specialized Extractors Are Separate

This separation keeps the design clean:

```text
+-----------------------------+------------------------------------------+
| Generic service             | Specialized extractor                    |
+-----------------------------+------------------------------------------+
| Ingests many document types  | Understands one platform deeply          |
| Extracts broad knowledge     | Extracts precise platform facts          |
| Reusable for normal dev      | Used for migration-specific precision    |
| Stores source evidence       | Produces target migration model          |
| Exposes query APIs           | Exposes platform-specific facts to MCP   |
+-----------------------------+------------------------------------------+
```

The generic service should not become a large collection of unrelated BPM parsers. Each platform has different export formats, rule semantics, workflow structures, and migration concerns.

## Example TIBCO MDM Migration Flow

```text
TIBCO MDM XML + docs + diagrams
  v
knowledge-ingestion-svc
  v
Spanner generic KB
  |
  |-- sources tagged as tibco_mdm_xml
  |-- chunks with evidence text
  |-- generic business rules
  |-- generic business flows
  |-- generic components
  |-- generic relationships
  v
TIBCO MDM extractor service
  |
  |-- parse TIBCO XML structure
  |-- extract entities
  |-- extract attributes
  |-- extract rulebases
  |-- extract validations
  |-- extract workflows
  |-- extract states and transitions
  |-- extract integration points
  v
Precise TIBCO migration facts
  |
  |-- MDM entities and attributes
  |-- MDM relationships
  |-- rulebases and validations
  |-- workflows
  |-- states and transitions
  |-- integration endpoints
  |-- Java microservice mapping hints
  v
MCP / AGENT
  |
  |-- epics
  |-- stories
  |-- LLD
  |-- Spring Boot services
  |-- entities and DTOs
  |-- validation logic
  |-- workflow orchestration
  |-- test cases
```

## Example BPMN Migration Flow

```text
BPMN files + process docs + diagrams
  v
knowledge-ingestion-svc
  v
Spanner generic KB
  v
BPMN extractor service
  |
  |-- parse BPMN XML
  |-- extract pools and lanes
  |-- extract tasks
  |-- extract events
  |-- extract gateways
  |-- extract sequence flows
  |-- extract message flows
  |-- extract subprocesses
  |-- extract timers and error paths
  v
Precise BPMN migration facts
  |
  |-- process model
  |-- lanes and actors
  |-- tasks and service tasks
  |-- events and gateways
  |-- sequence and message flows
  |-- subprocesses
  |-- Java orchestration mapping hints
  v
MCP / AGENT
  |
  |-- microservice boundaries
  |-- orchestration design
  |-- event contracts
  |-- LLD
  |-- tests
  |-- Java implementation plan
```

## Evidence and Traceability

Every generated artifact should be traceable to source evidence:

```text
Generated artifact
  |
  +-- sourceObject
  +-- artifactId
  +-- chunkId
  +-- chunkIndex
  +-- contentHash
  +-- excerpt
```

This is critical for:

- reviewing AGENT output
- validating migration logic
- explaining generated code
- debugging incorrect assumptions
- supporting manual corrections later

## What The Current Service Is Capable Of

The current service is capable of:

- generic document ingestion
- generic XML text ingestion
- chunking and embedding
- generic business rule extraction
- generic business flow extraction
- generic component and resource extraction
- generic relationship extraction
- source evidence persistence
- source/fact retrieval for MCP wrappers
- supporting normal development AGENT workflows
- supporting migration discovery and first-stage extraction
- feeding specialized BPM or platform extractors

## What The Current Service Does Not Try To Do

The current service does not try to be a complete parser for every BPM or legacy platform.

It does not yet provide precise structural extraction for:

- TIBCO MDM rulebase XML
- BPMN process semantics
- Pega rules and case types
- IBM BPM process applications
- Camunda deployment packages
- Appian application packages
- platform-specific forms, screens, rules, and service bindings

Those belong in specialized extractor services.

## Recommended Final Architecture

```text
+-----------------------------------+
| Source repositories / GCS         |
| docs, XML, BPM exports, diagrams  |
+----------------+------------------+
                 |
                 v
+----------------+------------------+
| knowledge-ingestion-svc           |
| generic knowledge extractor       |
+----------------+------------------+
                 |
                 v
+----------------+------------------+
| Spanner generic knowledge base    |
+----------------+------------------+
                 |
                 +-----------------------------------+
                 |                                   |
                 v                                   v
+----------------+------------------+   +------------+------------------+
| MCP normal-dev tools              |   | Specialized extractors        |
+----------------+------------------+   +------------+------------------+
                 |                                   |
                 v                                   v
+----------------+------------------+   +------------+------------------+
| LLD / tests / code generation     |   | Precise migration facts       |
+-----------------------------------+   | store / API                   |
                                        +------------+------------------+
                                                     |
                                                     v
                                        +------------+------------------+
                                        | MCP migration tools          |
                                        +------------+------------------+
                                                     |
                                                     v
                                        +------------+------------------+
                                        | epics, stories, migration    |
                                        | LLD, tests, code generation  |
                                        +-------------------------------+
```

## Design Principle

The service should remain a reusable generic knowledge-base extractor.

Platform-specific precision should be added through separate specialized extractor services that consume this knowledge base and expose precise facts to MCP/AGENT workflows.
