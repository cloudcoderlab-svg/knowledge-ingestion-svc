# Database Changelog Directory

## Overview

This directory contains database schema definitions and migration scripts for the knowledge-ingestion service. The schema implements a comprehensive knowledge graph architecture that extracts, stores, and indexes structured knowledge from documents using AI-powered analysis and vector embeddings.

**Current Version:** 1.0 (Consolidated)
**Last Updated:** 2026-05-26
**Total Tables:** 20
**Database Engine:** PostgreSQL 14+ with pgvector extension

## Active Files

### Primary Changelog

**changelog-master.xml** - Active consolidated changelog
- Single source of truth for complete schema
- Aligned with all 20 entity classes
- Includes all fixes and improvements
- **Fully idempotent** - Safe to run multiple times
- Uses IF NOT EXISTS for all CREATE statements
- Use this for new deployments and development

### Schema Definition

**sql/consolidated-schema.sql** - Complete database schema
- All 20 tables defined
- Proper indexes and constraints
- Entity-aligned column names
- Pgvector support

### Documentation

**MIGRATION_GUIDE.md** - Comprehensive migration guide
- Migration paths for fresh and existing installations
- Validation steps
- Rollback procedures
- Entity-to-table mapping reference

## Archived Files

The `/archive` directory contains incremental migration files from previous versions:
- 001-initial-schema.sql
- 002-add-version-columns.sql
- 003-add-document-knowledge-fields.sql
- 004-create-document-processing-tracking.sql
- 005-remove-unused-doc-process-columns.sql

**Note:** These files are kept for historical reference only. They are NOT executed in new deployments.

### Legacy Changelog

**db.changelog-master.xml** - Legacy incremental changelog
- Contains all historical migrations
- Use only for existing production databases that need incremental updates
- Not recommended for new deployments

## Usage

### Development (Fresh Start)

1. Set up dev profile:
   ```yaml
   spring.profiles.active: dev
   ```

2. Clean database (optional):
   ```bash
   psql -U kengine-app -d kengine-db -f db/cleanup-and-recreate.sql
   ```

3. Run application:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

4. Liquibase will create all tables from consolidated-schema.sql

### Production Deployment

**For Fresh Installations:**
- Use `db.changelog-master-consolidated.xml`
- All tables created from clean state

**For Existing Databases:**
- Review MIGRATION_GUIDE.md
- May need incremental migration scripts
- Test in staging first

## Idempotent Configuration

The schema is designed to be **fully idempotent** - safe to run multiple times without errors or data loss.

### IF NOT EXISTS Clauses

All CREATE statements use IF NOT EXISTS:

| Statement Type | Count | Behavior |
|---|---|---|
| CREATE SCHEMA | 1 | Creates knowledge schema only if it doesn't exist |
| CREATE EXTENSION | 1 | Enables pgvector only if not already enabled |
| CREATE TABLE | 20 | Creates tables only if they don't exist |
| CREATE INDEX | 70 | Creates indexes only if they don't exist |
| CREATE UNIQUE INDEX | 2 | Creates unique indexes only if they don't exist |

**Total IF NOT EXISTS clauses: 94**

### Safe Execution

This configuration ensures:
- ✅ **First Run**: All schema objects are created
- ✅ **Subsequent Runs**: No errors, existing objects are preserved
- ✅ **Partial State**: Missing objects are created, existing ones unchanged
- ✅ **No Data Loss**: Existing data is never dropped or modified
- ✅ **Application Restart**: Safe to restart application multiple times

### Liquibase Configuration

```yaml
spring:
  liquibase:
    change-log: classpath:/db/changelog/changelog-master.xml
    liquibase-schema: public
    default-schema: knowledge
    drop-first: false  # Never drops schema automatically
    enabled: true      # Runs on application startup
```

### Example: Running Multiple Times

```bash
# First run - creates all objects
./gradlew bootRun --args='--spring.profiles.active=dev'
# Application starts successfully, all tables created

# Second run - no errors, objects already exist
./gradlew bootRun --args='--spring.profiles.active=dev'
# Application starts successfully, no changes made

# After manual table deletion - recreates missing tables only
psql -U kengine-app -d kengine-db -c "DROP TABLE knowledge.projects;"
./gradlew bootRun --args='--spring.profiles.active=dev'
# Application starts successfully, projects table recreated
```

## Schema Validation

After deployment, verify schema:

```sql
-- Count tables (should be 20)
SELECT COUNT(*) FROM information_schema.tables
WHERE table_schema = 'knowledge';

-- List all tables
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'knowledge'
ORDER BY table_name;

-- Verify pgvector extension
SELECT * FROM pg_extension WHERE extname = 'vector';
```

## Key Features

### AI-Powered Knowledge Extraction
- **Multimodal Document Processing**: Supports PDF, DOCX, PPTX, images using Gemini 2.5 Flash
- **Document-Level Analysis**: Extracts high-level knowledge, themes, and summaries
- **Entity Extraction**: Identifies components, APIs, business rules, workflows, and data models
- **Semantic Chunking**: Breaks documents into meaningful chunks with context preservation
- **Vector Embeddings**: Generates 768-dimensional embeddings for semantic search

### Pgvector Support
- Vector similarity search using PostgreSQL pgvector extension
- 768-dimensional embeddings using Vertex AI text-embedding-005 model
- Cosine similarity search on semantic_chunks and knowledge entities
- Enables "find similar" queries across all knowledge types
- Supports hybrid search combining vector similarity with metadata filters

### Entity-Schema Alignment
- **100% Coverage**: All 20 JPA entities have corresponding database tables
- **Column Name Matching**: Column names exactly match @Column annotations in entities
- **Type Safety**: SQL types aligned with JPA field types (e.g., JSONB for @JdbcTypeCode(SqlTypes.JSON))
- **Foreign Key Integrity**: Cascading deletes and referential integrity enforced
- **Index Optimization**: Indexes on foreign keys, search columns, and vector embeddings

### Content Deduplication
- **Hash-Based Deduplication**: SHA-256 content hashing on artifacts prevents duplicate storage
- **Unique Constraints**: Unique indexes on (project_id, file_name, content_hash)
- **Storage Optimization**: Identical documents uploaded multiple times stored once
- **Link Preservation**: Multiple ingestion_documents can reference same artifact

### Parallel Processing Architecture
- **Batch-Level Tracking**: knowledge_ingestion_process tracks overall batch status
- **Document-Level Tracking**: knowledge_ingestion_document_process tracks individual file status
- **Virtual Thread Support**: Designed for Java 21 virtual threads for massive parallelism
- **Fine-Grained Status**: Per-document status (PENDING, PROCESSING, COMPLETED, FAILED)
- **Fault Tolerance**: Failed documents don't block batch completion; can be retried independently

## Architecture and Data Model

### Architectural Patterns

The schema implements a **multi-layered knowledge extraction architecture**:

1. **Organization Layer**: Hierarchical project/domain structure for knowledge categorization
2. **Ingestion Layer**: Parallel batch processing with document-level tracking
3. **Document Layer**: Artifact storage, chunking, and vector indexing
4. **Knowledge Layer**: AI-extracted entities representing system components, business logic, and data models
5. **Graph Layer**: Relationships connecting knowledge entities into a queryable graph

### Tables by Category

#### 1. Organization Hierarchy (4 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **projects** | ProjectEntity | Top-level projects containing domains | project_id, project_name, source_bucket |
| **domains** | DomainEntity | Business/technical domains within projects | domain_id, project_id, domain_name |
| **subdomains** | SubdomainEntity | Fine-grained domain subdivisions | subdomain_id, domain_id, subdomain_name |
| **subjects** | SubjectEntity | Specific topics/subjects for classification | subject_id, subject_name, description |

**Hierarchy:** Project → Domain → Subdomain → Documents

#### 2. Ingestion Processing (2 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **knowledge_ingestion_process** | KnowledgeIngestionProcessEntity | Batch-level tracking | process_id, project_id, status, parallel_enabled |
| **knowledge_ingestion_document_process** | KnowledgeIngestionDocumentProcessEntity | Document-level tracking within batches | doc_process_id, process_id, file_name, status |

**Pattern:** Enables parallel document processing using virtual threads with fine-grained status tracking.

#### 3. Document Storage and Chunking (3 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **artifacts** | ArtifactEntity | Original documents with deduplication | artifact_id, file_name, content_hash, gcs_uri |
| **ingestion_documents** | IngestionDocumentEntity | Processed documents linked to projects | document_id, artifact_id, project_id, domain_id |
| **semantic_chunks** | SemanticChunkEntity | Text chunks with vector embeddings | chunk_id, document_id, chunk_text, embedding (768d) |

**Pattern:** Content hash-based deduplication prevents re-processing of identical documents.

#### 4. Extracted Knowledge - System Architecture (4 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **knowledge_components** | KnowledgeComponentEntity | System building blocks | component_id, component_name, component_type, embedding |
| **knowledge_apis** | KnowledgeAPIEntity | API endpoints and contracts | api_id, api_name, endpoint, method, request_format |
| **knowledge_resources** | KnowledgeResourceEntity | External dependencies | resource_id, resource_name, resource_type, connection_details |
| **knowledge_integrations** | KnowledgeIntegrationEntity | System integrations | integration_id, integration_name, integration_type |

**Pattern:** Represents the technical architecture extracted from design documents and code.

#### 5. Extracted Knowledge - Business Logic (3 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **knowledge_business_rules** | KnowledgeBusinessRuleEntity | Business rules and validations | rule_id, rule_name, rule_logic, applies_to |
| **knowledge_workflows** | KnowledgeWorkflowEntity | End-to-end business processes | workflow_id, workflow_name, trigger_events |
| **knowledge_workflow_steps** | KnowledgeWorkflowStepEntity | Individual workflow steps | step_id, workflow_id, step_number, step_description |

**Pattern:** Captures business processes and logic from requirements and process documents.

#### 6. Extracted Knowledge - Data Models (3 tables)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **document_knowledge** | DocumentKnowledgeEntity | Document-level extracted knowledge | knowledge_id, document_id, knowledge_type, embedding |
| **knowledge_data_models** | KnowledgeDataModelEntity | Data schemas and structures | data_model_id, model_name, schema_type, fields_summary |
| **knowledge_data_fields** | KnowledgeDataFieldEntity | Individual data fields | field_id, data_model_id, field_name, field_type, constraints |

**Pattern:** Represents data models, schemas, and field definitions extracted from data dictionaries and schemas.

#### 7. Knowledge Graph (1 table)

| Table | Entity | Purpose | Key Fields |
|---|---|---|---|
| **knowledge_relationships** | KnowledgeRelationshipEntity | Edges connecting entities | relationship_id, source_id, target_id, relationship_type, confidence |

**Pattern:** Creates a queryable graph where entities are nodes and relationships are edges. Supports graph traversal queries.

### Entity-to-Table Mapping Reference

| JPA Entity | Database Table | Primary Key | Schema |
|---|---|---|---|
| ProjectEntity | projects | project_id (VARCHAR) | knowledge |
| DomainEntity | domains | domain_id (UUID) | knowledge |
| SubdomainEntity | subdomains | subdomain_id (UUID) | knowledge |
| SubjectEntity | subjects | subject_id (UUID) | knowledge |
| KnowledgeIngestionProcessEntity | knowledge_ingestion_process | process_id (UUID) | knowledge |
| KnowledgeIngestionDocumentProcessEntity | knowledge_ingestion_document_process | doc_process_id (UUID) | knowledge |
| ArtifactEntity | artifacts | artifact_id (UUID) | knowledge |
| IngestionDocumentEntity | ingestion_documents | document_id (UUID) | knowledge |
| SemanticChunkEntity | semantic_chunks | chunk_id (UUID) | knowledge |
| DocumentKnowledgeEntity | document_knowledge | knowledge_id (UUID) | knowledge |
| KnowledgeComponentEntity | knowledge_components | component_id (UUID) | knowledge |
| KnowledgeAPIEntity | knowledge_apis | api_id (UUID) | knowledge |
| KnowledgeResourceEntity | knowledge_resources | resource_id (UUID) | knowledge |
| KnowledgeIntegrationEntity | knowledge_integrations | integration_id (UUID) | knowledge |
| KnowledgeBusinessRuleEntity | knowledge_business_rules | rule_id (UUID) | knowledge |
| KnowledgeWorkflowEntity | knowledge_workflows | workflow_id (UUID) | knowledge |
| KnowledgeWorkflowStepEntity | knowledge_workflow_steps | step_id (UUID) | knowledge |
| KnowledgeDataModelEntity | knowledge_data_models | data_model_id (UUID) | knowledge |
| KnowledgeDataFieldEntity | knowledge_data_fields | field_id (UUID) | knowledge |
| KnowledgeRelationshipEntity | knowledge_relationships | relationship_id (UUID) | knowledge |

## Knowledge Graph Architecture

### Graph Structure

The knowledge graph is built from three types of elements:

1. **Nodes (Entities)**: Any knowledge entity can be a graph node
   - Components, APIs, Resources, Integrations
   - Business Rules, Workflows, Data Models
   - Document Knowledge entries

2. **Edges (Relationships)**: knowledge_relationships table stores directed edges
   - source_id → target_id connections
   - relationship_type (e.g., "depends_on", "implements", "calls", "contains")
   - confidence scores for AI-extracted relationships

3. **Attributes**: Entity tables store node properties
   - Names, descriptions, types
   - Vector embeddings for semantic similarity
   - Metadata (JSONB fields for flexible attributes)

### Relationship Types

Common relationship types extracted from documents:

| Relationship Type | Description | Example |
|---|---|---|
| **depends_on** | Component/API dependency | Component A depends_on API B |
| **implements** | Implementation relationship | Component A implements Business Rule B |
| **calls** | API invocation | API A calls API B |
| **contains** | Containment hierarchy | Workflow A contains Step B |
| **validates** | Validation rule | Business Rule A validates Data Model B |
| **processes** | Data processing | Component A processes Data Model B |
| **triggers** | Event triggering | Workflow A triggers Workflow B |
| **references** | General reference | Any entity references any other |

### Query Patterns

The schema supports multiple query patterns:

#### 1. Vector Similarity Search
```sql
-- Find semantically similar components
SELECT component_id, component_name,
       1 - (embedding <=> '[0.1, 0.2, ...]'::vector) AS similarity
FROM knowledge.knowledge_components
WHERE embedding IS NOT NULL
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;
```

#### 2. Graph Traversal
```sql
-- Find all APIs called by a component (one hop)
SELECT ka.api_name, ka.endpoint, kr.relationship_type
FROM knowledge.knowledge_components kc
JOIN knowledge.knowledge_relationships kr ON kc.component_id = kr.source_id
JOIN knowledge.knowledge_apis ka ON kr.target_id = ka.api_id
WHERE kc.component_name = 'OrderService'
  AND kr.relationship_type = 'calls';
```

#### 3. Multi-Hop Graph Traversal
```sql
-- Find data models affected by a component (two hops)
WITH RECURSIVE component_deps AS (
  -- Base case: direct relationships
  SELECT source_id, target_id, relationship_type, 1 AS depth
  FROM knowledge.knowledge_relationships
  WHERE source_id = 'component-uuid'

  UNION ALL

  -- Recursive case: follow relationships
  SELECT kr.source_id, kr.target_id, kr.relationship_type, cd.depth + 1
  FROM knowledge.knowledge_relationships kr
  JOIN component_deps cd ON kr.source_id = cd.target_id
  WHERE cd.depth < 3
)
SELECT DISTINCT kdm.model_name, kdm.schema_type, cd.depth
FROM component_deps cd
JOIN knowledge.knowledge_data_models kdm ON cd.target_id = kdm.data_model_id;
```

#### 4. Hierarchical Organization Queries
```sql
-- Find all documents in a domain hierarchy
SELECT p.project_name, d.domain_name, sd.subdomain_name,
       id.file_name, id.classification
FROM knowledge.projects p
JOIN knowledge.domains d ON p.project_id = d.project_id
LEFT JOIN knowledge.subdomains sd ON d.domain_id = sd.domain_id
JOIN knowledge.ingestion_documents id ON
  id.project_id = p.project_id AND id.domain_id = d.domain_id
WHERE p.project_name = 'MyProject';
```

#### 5. Hybrid Search (Vector + Metadata)
```sql
-- Find APIs similar to query with metadata filters
SELECT ka.api_name, ka.endpoint, ka.method,
       1 - (ka.embedding <=> $1::vector) AS similarity
FROM knowledge.knowledge_apis ka
WHERE ka.embedding IS NOT NULL
  AND ka.method = 'POST'
  AND ka.endpoint LIKE '/api/v1/%'
ORDER BY ka.embedding <=> $1::vector
LIMIT 10;
```

### Data Flow

1. **Document Upload** → artifacts table (with content hash)
2. **Document Processing** → ingestion_documents table (linked to project/domain)
3. **Semantic Chunking** → semantic_chunks table (with vector embeddings)
4. **AI Analysis** → document_knowledge table (document-level insights)
5. **Entity Extraction** → knowledge_* tables (components, APIs, rules, workflows, etc.)
6. **Relationship Extraction** → knowledge_relationships table (graph edges)
7. **Vector Indexing** → Embeddings generated for all entities and chunks

## Troubleshooting

### Liquibase Lock Issues

If Liquibase is locked:
```sql
DELETE FROM public.databasechangeloglock;
```

### Schema Mismatch

If JPA validation fails:
1. Check entity @Column annotations
2. Compare with SQL schema
3. Review MIGRATION_GUIDE.md for known issues

### Clean Slate (Dev Only)

⚠️ **WARNING: Deletes all data!**

```bash
psql -U kengine-app -d kengine-db -f db/cleanup-and-recreate.sql
```

## Technical Specifications

### Database Requirements
- **PostgreSQL**: Version 14 or higher
- **Extensions**: pgvector (for vector similarity search)
- **Schema**: knowledge (all tables) + public (Liquibase tracking)
- **Encoding**: UTF-8
- **Timezone**: UTC recommended for timestamp columns

### Performance Considerations

#### Indexes
All tables have optimized indexes:
- **Primary Keys**: B-tree indexes on all primary keys
- **Foreign Keys**: Indexes on all foreign key columns for join performance
- **Vector Columns**: IVFFlat indexes on embedding columns for similarity search
- **Search Columns**: Indexes on frequently queried columns (file_name, content_hash, etc.)

#### Vector Search Optimization
```sql
-- Create IVFFlat index for faster similarity search (recommended for >10K rows)
CREATE INDEX idx_semantic_chunks_embedding_ivfflat
ON knowledge.semantic_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- For smaller datasets, use default index (already created by schema)
-- The consolidated schema creates basic indexes suitable for development
```

#### Connection Pooling
Recommended HikariCP settings:
```yaml
spring.datasource.hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

### Data Types Reference

| SQL Type | Java/JPA Type | Purpose |
|---|---|---|
| UUID | UUID | Primary keys (except projects) |
| VARCHAR(255) | String | Names and identifiers |
| TEXT | String | Long text content |
| JSONB | Map<String, Object> | Flexible metadata |
| TIMESTAMP WITH TIME ZONE | Instant | Timestamps |
| BOOLEAN | Boolean | Flags |
| vector(768) | float[] | Embeddings for similarity search |
| REAL | Float | Confidence scores |

### Security Considerations

#### Database User Permissions
The kengine-app user should have:
- Full CRUD on knowledge schema
- CREATE/UPDATE on public schema (for Liquibase)
- USAGE on vector extension

```sql
-- Grant required permissions
GRANT ALL PRIVILEGES ON SCHEMA knowledge TO "kengine-app";
GRANT CREATE, USAGE ON SCHEMA public TO "kengine-app";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA knowledge TO "kengine-app";
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO "kengine-app";
```

#### Sensitive Data
The following fields may contain sensitive information:
- artifacts.gcs_uri (file locations)
- knowledge_resources.connection_details (connection strings)
- All JSONB metadata fields (may contain credentials)

Ensure proper access controls and encryption at rest.

## Support

For issues or questions:
- **Schema Issues**: Review MIGRATION_GUIDE.md for detailed entity-to-table mapping
- **Liquibase Issues**: Check application logs with liquibase logging level set to DEBUG
- **Performance Issues**: Review query plans with EXPLAIN ANALYZE, consider adding indexes
- **Vector Search Issues**: Verify pgvector extension is installed: `SELECT * FROM pg_extension WHERE extname = 'vector';`
- **JPA Validation Errors**: Compare entity @Column annotations with SQL schema in consolidated-schema.sql

### Common Issues

1. **Missing projects table**: Fixed in consolidated schema (was missing in earlier versions)
2. **knowledge_data_fields column mismatch**: Fixed - now uses field_type (not data_type)
3. **constraints field type**: Fixed - now JSONB (was TEXT)
4. **Vector operations failing**: Ensure pgvector extension is installed and vector type is properly mapped

## References

### Official Documentation
- [Liquibase Documentation](https://docs.liquibase.com/) - Database migration framework
- [PostgreSQL Vector Extension](https://github.com/pgvector/pgvector) - pgvector for similarity search
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/) - JPA entity mapping
- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/) - ORM framework details

### AI and Embeddings
- [Vertex AI Embeddings](https://cloud.google.com/vertex-ai/docs/generative-ai/embeddings/get-text-embeddings) - text-embedding-005 model
- [Gemini Models](https://cloud.google.com/vertex-ai/docs/generative-ai/learn/models) - Gemini 2.5 Flash for analysis

### Architecture Patterns
- [Knowledge Graph Design Patterns](https://neo4j.com/developer/graph-data-modeling/) - Graph modeling concepts
- [Vector Database Best Practices](https://www.pinecone.io/learn/vector-database/) - Vector search optimization

## Version History

| Version | Date | Changes |
|---|---|---|
| 1.0 (Consolidated) | 2026-05-26 | Consolidated all migrations into single schema. Fixed missing projects table, knowledge_data_fields alignment, and constraints field type. Complete entity-schema alignment for all 20 tables. |
| 0.5 (Incremental) | Earlier | Incremental migrations (001-005). See archive/ directory for historical files. |
