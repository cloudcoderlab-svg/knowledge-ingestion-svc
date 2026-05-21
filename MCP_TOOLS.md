# MCP Tools for Knowledge-Driven Generation

This document defines the recommended MCP tools that AGENTS should use to interact with the knowledge base for both normal software development and TIBCO MDM migration.

The current service is the generic ingestion and knowledge source. It ingests source documents, extracts generic knowledge, stores chunks and facts in Cloud SQL for PostgreSQL with pgvector, and exposes retrieval endpoints. MCP tools can wrap these endpoints and add task-specific orchestration.

## High-Level Architecture

```text
Docs / requirements / diagrams / XML
  -> knowledge-ingestion-svc
  -> Cloud SQL PostgreSQL knowledge base
      -> semantic_chunks
      -> business_rules
      -> business_flows
      -> business_flow_steps
      -> solution_components
      -> deployment_resources
      -> knowledge_relationships
      -> knowledge_notes
  -> MCP tools
  -> AGENTS
      -> epics / stories
      -> LLD
      -> test cases
      -> Java / Spring Boot code
      -> migration artifacts
```

## Current Service REST Endpoints

The MCP layer can initially wrap these endpoints:

```text
GET /api/v1/projects/{projectId}/knowledge/sources
GET /api/v1/projects/{projectId}/knowledge/chunks?sourceObject={sourceObject}&q={query}&limit={limit}
GET /api/v1/projects/{projectId}/knowledge?sourceObject={sourceObject}
```

## Core MCP Tools

### `list_sources`

Lists ingested artifacts for a project.

Use for:
- Discovering available source documents.
- Finding TIBCO XML files.
- Finding requirements, diagrams, and design docs.

Input:

```json
{
  "projectId": "project-a"
}
```

Output:

```json
{
  "sources": [
    {
      "artifactId": "...",
      "sourceObject": "project-a/tibco/customer-workflow.xml",
      "artifactType": "tibco_mdm_xml",
      "fileType": "xml",
      "title": "customer-workflow.xml",
      "contentHash": "...",
      "current": true
    }
  ]
}
```

### `get_source_chunks`

Retrieves source chunks and evidence text.

Use for:
- Supplying citations to AGENTS.
- Re-reading source evidence before generating artifacts.
- Retrieving chunks matching business terms, workflow names, entities, APIs, or resources.

Input:

```json
{
  "projectId": "project-a",
  "sourceObject": "project-a/requirements/customer-onboarding.md",
  "query": "customer onboarding validation",
  "limit": 50
}
```

Output:

```json
{
  "chunks": [
    {
      "chunkId": "...",
      "sourceObject": "...",
      "chunkIndex": 0,
      "charStart": 0,
      "charEnd": 2100,
      "content": "...",
      "domain": "MDM",
      "subdomain": "Workflow"
    }
  ]
}
```

### `get_generic_knowledge`

Returns all generic extracted facts for a project or source object.

Use for:
- LLD generation.
- Test-case generation.
- Story generation.
- Migration context assembly.

Input:

```json
{
  "projectId": "project-a",
  "sourceObject": "project-a/requirements/customer-onboarding.md"
}
```

Output sections:

```json
{
  "businessRules": [],
  "businessFlows": [],
  "businessFlowSteps": [],
  "solutionComponents": [],
  "deploymentResources": [],
  "deploymentResourceConfigs": [],
  "knowledgeRelationships": [],
  "knowledgeNotes": [],
  "usageProfiles": [],
  "resourceCostEstimates": []
}
```

## Normal Development Tools

### `get_generation_context`

Builds a curated context package for AGENTS generating LLD, tests, or code.

Use for:
- Normal development.
- Feature design.
- API design.
- Test planning.
- Code scaffolding.

Input:

```json
{
  "projectId": "project-a",
  "targetCapability": "Customer Onboarding",
  "artifactType": "lld",
  "includeEvidence": true
}
```

Recommended behavior:
- Find relevant sources.
- Retrieve matching chunks.
- Include related business rules.
- Include flows and steps.
- Include business and technical components.
- Include dependency relationships.
- Include deployment resources and NFR-related notes.
- Return citations using `sourceObject`, `chunkId`, and `chunkIndex`.

Output:

```json
{
  "targetCapability": "Customer Onboarding",
  "businessRules": [],
  "businessFlows": [],
  "solutionComponents": [],
  "relationships": [],
  "deploymentResources": [],
  "evidence": []
}
```

### `get_lld_context`

Specialized context builder for low-level design.

Input:

```json
{
  "projectId": "project-a",
  "componentName": "Customer API"
}
```

Should return:
- Component responsibility.
- Business capabilities.
- APIs or operations inferred from docs.
- Dependencies.
- Business rules to implement.
- Workflows involving the component.
- Data/resource dependencies.
- Source evidence.

### `get_testcase_context`

Specialized context builder for test-case generation.

Input:

```json
{
  "projectId": "project-a",
  "targetCapability": "Customer Onboarding",
  "testLevel": "functional"
}
```

Should return:
- Business rules.
- Positive and negative flow paths.
- Workflow steps.
- Validation conditions.
- Inputs and expected outcomes.
- Dependencies and mocks.
- NFR assumptions when relevant.
- Source evidence.

### `get_code_generation_context`

Specialized context builder for code generation.

Input:

```json
{
  "projectId": "project-a",
  "componentName": "Customer API",
  "targetStack": "spring_boot"
}
```

Should return:
- Component responsibility.
- Business rules.
- Workflow interactions.
- Dependencies.
- Runtime resources.
- Suggested package/module boundaries.
- Source evidence.

## TIBCO Migration Tools

The current service provides generic knowledge. TIBCO-specific extraction should be implemented in a separate service that reads chunks and generic facts from this service.

### `list_tibco_sources`

Lists candidate TIBCO artifacts from the generic source list.

Selection rules:
- `artifactType = tibco_mdm_xml`
- `fileType = xml`
- source path contains `tibco`, `mdm`, `workflow`, `rulebase`, `datamodel`, or `repository`

Input:

```json
{
  "projectId": "project-a"
}
```

Output:

```json
{
  "sources": []
}
```

### `get_tibco_extraction_input`

Returns source chunks and generic facts needed by the TIBCO-specific extractor.

Input:

```json
{
  "projectId": "project-a",
  "sourceObject": "project-a/tibco/customer-workflow.xml",
  "includeGenericFacts": true
}
```

Should return:
- Source chunks.
- Generic business rules.
- Generic business flows.
- Generic components.
- Generic relationships.
- Source metadata.

### `get_migration_context`

Builds AGENT-ready context for migration planning and artifact generation.

Input:

```json
{
  "projectId": "project-a",
  "targetDomain": "Customer",
  "migrationTarget": "spring_boot_microservices"
}
```

Should return:
- TIBCO XML source evidence.
- Generic business rules and flows.
- TIBCO-specific extracted facts from the migration service.
- Target architecture assumptions.
- Component and resource relationships.
- Open gaps and ambiguity notes.

### `get_epic_story_context`

Builds context for epic and story generation.

Input:

```json
{
  "projectId": "project-a",
  "capability": "Customer Master Data Management",
  "mode": "migration"
}
```

Should return:
- Business capabilities.
- Workflows.
- Business rules.
- Affected technical components.
- Dependencies.
- Migration notes.
- Evidence chunks.

## Agent Usage Patterns

### Normal Development

```text
AGENT
  -> list_sources
  -> get_generation_context
  -> get_lld_context
  -> get_testcase_context
  -> get_code_generation_context
  -> generate LLD / tests / code
```

### TIBCO Migration

```text
AGENT
  -> list_tibco_sources
  -> get_tibco_extraction_input
  -> TIBCO-specific extractor service
  -> get_migration_context
  -> get_epic_story_context
  -> generate epics / stories / LLD / code / tests
```

## Evidence Requirements

Every MCP response used for generation should include evidence references:

```json
{
  "sourceObject": "project-a/tibco/customer-workflow.xml",
  "chunkId": "...",
  "chunkIndex": 3,
  "contentHash": "...",
  "excerpt": "..."
}
```

Agents should prefer facts with source evidence and should flag generated assumptions when evidence is missing.

## Recommended Tool Priority

For normal development:

1. `get_generation_context`
2. `get_lld_context`
3. `get_testcase_context`
4. `get_code_generation_context`
5. `get_source_chunks`

For TIBCO migration:

1. `list_tibco_sources`
2. `get_tibco_extraction_input`
3. TIBCO-specific extractor service
4. `get_migration_context`
5. `get_epic_story_context`
6. `get_source_chunks`

## Future Enhancements

- Vector search over `semantic_chunks.embedding`.
- Graph traversal endpoint for `knowledge_relationships`.
- Dedicated retrieval endpoint by business capability.
- Dedicated retrieval endpoint by workflow.
- Dedicated retrieval endpoint by component.
- OCR or vision extraction for diagrams.
- Manual correction/versioning for extracted facts.
- MCP tools that return citations in a standard format.
