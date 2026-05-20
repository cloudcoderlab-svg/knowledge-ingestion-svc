# Knowledge Ingestion Service Setup

This service ingests documents from GCS, chunks and classifies content with Vertex AI, creates per-chunk embeddings, extracts generic solution knowledge, and stores the knowledge in Cloud Spanner.

It is intended to be the first-stage knowledge base for both normal development and migration workflows. Downstream MCP/AGENT services can query source chunks and generic facts, then perform task-specific generation such as LLD, test cases, code scaffolding, epic/story generation, or TIBCO-specific migration extraction.

## Prerequisites

- Java 21
- Gradle wrapper from this project
- Google Cloud service account key:
  - `C:\root\lab\ai-lab\knowledge-engine\sa-knowledge-engine.json`
- GCS bucket:
  - `knowledge-ingestion-bucket-app-lab-001`
- Cloud Spanner:
  - Project: `app-lab-001`
  - Instance: `inst-knowledge-engine`
  - Database: `knowledge-engine-db`
- Vertex AI model project:
  - Project: `ai-lab-001-494218`
  - Location: `us-central1`

## Application Configuration

The service config is in `src/main/resources/application.yml`.

Expected Vertex configuration:

```yaml
vertex:
  project-id: ai-lab-001-494218
  location: us-central1
  model-name: gemini-2.5-flash
  classification-model-name: gemini-2.5-flash
  embedding-model-name: gemini-embedding-001
```

Expected ingestion chunking configuration:

```yaml
ingestion:
  chunk:
    max-chars: 3000
    overlap-chars: 300
```

Expected Spanner configuration:

```yaml
spanner:
  project-id: app-lab-001
  instance-id: inst-knowledge-engine
  database-id: knowledge-engine-db
```

The Spanner schema includes:

- `knowledge.ingestion_documents`
- `knowledge.semantic_chunks`
- `knowledge.business_rules`
- `knowledge.business_flows`
- `knowledge.business_flow_steps`
- `knowledge.solution_components`
- `knowledge.deployment_resources`
- `knowledge.knowledge_relationships`
- `knowledge.knowledge_notes`

Duplicate GCS notifications for the same bucket, object, and parsed content hash are skipped before classification, embedding, and extraction calls.

Likely TIBCO/MDM XML files are tagged as `artifact_type = tibco_mdm_xml`. Generic XML files are tagged as `xml_doc`.

## IAM Requirements

The runtime service account is:

```text
sa-knowledge-engine@app-lab-001.iam.gserviceaccount.com
```

It needs access in both projects.

In `app-lab-001`, grant permissions for:

- GCS read/write on `knowledge-ingestion-bucket-app-lab-001`
- Cloud Spanner writes and reads on `knowledge-engine-db`

In `ai-lab-001-494218`, grant Vertex AI invocation:

```bash
gcloud projects add-iam-policy-binding ai-lab-001-494218 \
  --member="serviceAccount:sa-knowledge-engine@app-lab-001.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"
```

If IAM edits fail because Cloud Resource Manager is disabled, a project admin must first run:

```bash
gcloud services enable cloudresourcemanager.googleapis.com --project=ai-lab-001-494218
```

## Local Auth

Use the service account key for local runs:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS='C:\root\lab\ai-lab\knowledge-engine\sa-knowledge-engine.json'
```

On this workstation, Google HTTPS traffic is intercepted by Norton TLS inspection. Java test workers need the Windows trust store:

```powershell
$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

Without this setting, calls to Google OAuth can fail with:

```text
PKIX path building failed
unable to find valid certification path to requested target
```

## Run Unit Tests

From `knowledge-engine/knowledge-ingestion-svc`:

```powershell
.\gradlew test
```

The real GCP integration test is skipped by default unless explicitly enabled.

## Run Real GCP Integration Test

This test makes real calls to GCS, Vertex AI, and Spanner. It uploads a dummy document, ingests it, chunks and classifies it, creates embeddings, saves to Spanner, then verifies the Spanner row has a non-empty embedding.

From `knowledge-engine/knowledge-ingestion-svc`:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS='C:\root\lab\ai-lab\knowledge-engine\sa-knowledge-engine.json'
$env:RUN_REAL_GCP_INTEGRATION_TESTS='true'
$env:INTEGRATION_GCS_BUCKET='knowledge-ingestion-bucket-app-lab-001'
$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'

.\gradlew test --rerun-tasks --tests com.kengine.ingestion.service.KnowledgeIngestionRealGcpIntegrationTest
```

Expected result:

```text
BUILD SUCCESSFUL
KnowledgeIngestionRealGcpIntegrationTest
tests=1, skipped=0, failures=0, errors=0
```

## Run the Service

From `knowledge-engine/knowledge-ingestion-svc`:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS='C:\root\lab\ai-lab\knowledge-engine\sa-knowledge-engine.json'
$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'

.\gradlew bootRun
```

The service listens on port `8080`.

## Knowledge Query Endpoints

These endpoints are intended for MCP wrappers and downstream extraction services:

```text
GET /api/v1/projects/{projectId}/knowledge/sources
GET /api/v1/projects/{projectId}/knowledge/chunks?sourceObject={sourceObject}&q={query}&limit={limit}
GET /api/v1/projects/{projectId}/knowledge?sourceObject={sourceObject}
```

Use these endpoints to retrieve source metadata, source chunks with evidence text, and generic extracted facts. See `MCP_TOOLS.md` for the recommended MCP tool contracts.

## Troubleshooting

### Vertex returns permission denied

Error:

```text
PERMISSION_DENIED: Permission 'aiplatform.endpoints.predict' denied
```

Fix: grant `roles/aiplatform.user` on `ai-lab-001-494218` to:

```text
serviceAccount:sa-knowledge-engine@app-lab-001.iam.gserviceaccount.com
```

### Vertex model not found

Error:

```text
Publisher Model ... was not found or your project does not have access to it
```

Check:

- `vertex.project-id` is `ai-lab-001-494218`
- `vertex.location` is `us-central1`
- `classification-model-name` is `gemini-2.5-flash`
- The service account has `roles/aiplatform.user` in the model project

### Vertex generation returns Markdown-wrapped JSON

Gemini may return JSON inside Markdown fences even when prompted for JSON. The service handles this through `JsonResponseExtractor`, which extracts the JSON object or array before Jackson parsing.

### Classification fields come back as arrays

Real model responses may return arrays for `businessCapability` or `technicalCapability`. `SemanticClassificationService` normalizes those fields to comma-separated strings before mapping to `ClassificationResult`.

### Spanner query parameter errors

The Spanner database uses PostgreSQL dialect. Avoid GoogleSQL-style named parameters in ad hoc verification queries unless tested against this dialect.

### GCS cleanup

The real integration test deletes the uploaded dummy GCS object after the run. It does not delete Spanner rows, so test rows remain available for audit/debugging.
