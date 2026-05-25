-- liquibase formatted sql

-- changeset kengine:001-initial-schema

-- ============================================================================
-- Knowledge Schema - Complete Initial Setup
-- ============================================================================

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS knowledge;

-- Enable pgvector extension for embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- Subjects Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.subjects (
    subject_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_name VARCHAR(500) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    source_bucket VARCHAR(255) NOT NULL,
    gcs_folder_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subjects_subject_name ON knowledge.subjects(subject_name);
CREATE INDEX IF NOT EXISTS idx_subjects_created_at ON knowledge.subjects(created_at);

-- ============================================================================
-- Knowledge Ingestion Process Tracking Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.knowledge_ingestion_process (
    process_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    process_type VARCHAR(50) DEFAULT 'DOCUMENT_INGESTION',
    status VARCHAR(50) NOT NULL DEFAULT 'INIT',
    total_files INTEGER DEFAULT 0,
    processed_files INTEGER DEFAULT 0,
    failed_files INTEGER DEFAULT 0,
    file_list JSONB,
    current_file VARCHAR(2048),
    failure_cause TEXT,
    error_details JSONB,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_ingestion_process_subject
        FOREIGN KEY (subject_id) REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_ingestion_process_subject_id
    ON knowledge.knowledge_ingestion_process(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_ingestion_process_status
    ON knowledge.knowledge_ingestion_process(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_ingestion_process_created_at
    ON knowledge.knowledge_ingestion_process(created_at);

-- ============================================================================
-- Artifacts Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.artifacts (
    artifact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    artifact_name VARCHAR(500),
    artifact_type VARCHAR(100),
    description TEXT,
    gcs_url TEXT,
    file_size BIGINT,
    mime_type VARCHAR(255),
    source_bucket VARCHAR(255),
    source_object TEXT,
    source_checksum VARCHAR(64),
    source_generation BIGINT,
    content_hash VARCHAR(64),
    domain VARCHAR(500),
    subdomain VARCHAR(500),
    file_type VARCHAR(100),
    title VARCHAR(1000),
    version INTEGER DEFAULT 1,
    is_current BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_artifacts_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_artifacts_subject_id ON knowledge.artifacts(subject_id);
CREATE INDEX IF NOT EXISTS idx_artifacts_artifact_type ON knowledge.artifacts(artifact_type);
CREATE INDEX IF NOT EXISTS idx_artifacts_created_at ON knowledge.artifacts(created_at);
CREATE INDEX IF NOT EXISTS idx_artifacts_source_object ON knowledge.artifacts(source_object);
CREATE INDEX IF NOT EXISTS idx_artifacts_content_hash ON knowledge.artifacts(content_hash);
CREATE INDEX IF NOT EXISTS idx_artifacts_domain ON knowledge.artifacts(domain);
CREATE INDEX IF NOT EXISTS idx_artifacts_subdomain ON knowledge.artifacts(subdomain);
CREATE INDEX IF NOT EXISTS idx_artifacts_is_current ON knowledge.artifacts(is_current);
CREATE INDEX IF NOT EXISTS idx_artifacts_dedup ON knowledge.artifacts(subject_id, source_bucket, source_object, content_hash);

-- ============================================================================
-- Domains Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    domain VARCHAR(500) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_domains_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT unique_domain_per_subject UNIQUE (subject_id, domain)
);

CREATE INDEX IF NOT EXISTS idx_domains_subject_id ON knowledge.domains(subject_id);
CREATE INDEX IF NOT EXISTS idx_domains_domain ON knowledge.domains(domain);

-- ============================================================================
-- Subdomains Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.subdomains (
    subdomain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    domain_id UUID NOT NULL,
    subdomain VARCHAR(500) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subdomains_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_subdomains_domain FOREIGN KEY (domain_id)
        REFERENCES knowledge.domains(domain_id) ON DELETE CASCADE,
    CONSTRAINT unique_subdomain_per_domain UNIQUE (domain_id, subdomain)
);

CREATE INDEX IF NOT EXISTS idx_subdomains_subject_id ON knowledge.subdomains(subject_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_domain_id ON knowledge.subdomains(domain_id);
CREATE INDEX IF NOT EXISTS idx_subdomains_subdomain ON knowledge.subdomains(subdomain);

-- ============================================================================
-- Ingestion Documents Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.ingestion_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    artifact_id UUID,
    document_name VARCHAR(500),
    document_type VARCHAR(100),
    gcs_url TEXT,
    file_size BIGINT,
    mime_type VARCHAR(255),
    source_bucket VARCHAR(255),
    source_object TEXT,
    content_hash VARCHAR(64),
    source_checksum VARCHAR(64),
    source_generation BIGINT,
    chunk_count INTEGER DEFAULT 0,
    extraction_status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ingestion_documents_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_ingestion_documents_artifact FOREIGN KEY (artifact_id)
        REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_ingestion_documents_subject_id ON knowledge.ingestion_documents(subject_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_artifact_id ON knowledge.ingestion_documents(artifact_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_extraction_status ON knowledge.ingestion_documents(extraction_status);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_created_at ON knowledge.ingestion_documents(created_at);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_source_object ON knowledge.ingestion_documents(source_object);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_content_hash ON knowledge.ingestion_documents(content_hash);
CREATE INDEX IF NOT EXISTS idx_ingestion_documents_dedup ON knowledge.ingestion_documents(subject_id, source_bucket, source_object, content_hash);

-- ============================================================================
-- Semantic Chunks Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.semantic_chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    document_id UUID NOT NULL,
    artifact_id UUID,
    domain_id UUID,
    subdomain_id UUID,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    total_chunks BIGINT,
    chunk_size INTEGER,
    char_start BIGINT,
    char_end BIGINT,
    domain VARCHAR(500),
    subdomain VARCHAR(500),
    source_bucket VARCHAR(255),
    source_object TEXT,
    source_generation BIGINT,
    source_checksum VARCHAR(64),
    document_content_hash VARCHAR(64),
    chunk_content_hash VARCHAR(64),
    embedding vector(768),
    embedding_status VARCHAR(50) DEFAULT 'PENDING',
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_semantic_chunks_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_semantic_chunks_document FOREIGN KEY (document_id)
        REFERENCES knowledge.ingestion_documents(document_id) ON DELETE CASCADE,
    CONSTRAINT fk_semantic_chunks_artifact FOREIGN KEY (artifact_id)
        REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    CONSTRAINT fk_semantic_chunks_domain FOREIGN KEY (domain_id)
        REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL,
    CONSTRAINT fk_semantic_chunks_subdomain FOREIGN KEY (subdomain_id)
        REFERENCES knowledge.subdomains(subdomain_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_semantic_chunks_subject_id ON knowledge.semantic_chunks(subject_id);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_document_id ON knowledge.semantic_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_artifact_id ON knowledge.semantic_chunks(artifact_id);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_domain_id ON knowledge.semantic_chunks(domain_id);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_subdomain_id ON knowledge.semantic_chunks(subdomain_id);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_embedding_status ON knowledge.semantic_chunks(embedding_status);
CREATE INDEX IF NOT EXISTS idx_semantic_chunks_chunk_index ON knowledge.semantic_chunks(document_id, chunk_index);

-- ============================================================================
-- Entities Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.entities (
    entity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    entity_name VARCHAR(500) NOT NULL,
    entity_type VARCHAR(100),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entities_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT unique_entity_per_subject UNIQUE (subject_id, entity_name, entity_type)
);

CREATE INDEX IF NOT EXISTS idx_entities_subject_id ON knowledge.entities(subject_id);
CREATE INDEX IF NOT EXISTS idx_entities_entity_name ON knowledge.entities(entity_name);
CREATE INDEX IF NOT EXISTS idx_entities_entity_type ON knowledge.entities(entity_type);

-- ============================================================================
-- Relationships Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.relationships (
    relationship_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    source_entity_id UUID NOT NULL,
    target_entity_id UUID NOT NULL,
    relationship_type VARCHAR(100) NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_relationships_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_relationships_source_entity FOREIGN KEY (source_entity_id)
        REFERENCES knowledge.entities(entity_id) ON DELETE CASCADE,
    CONSTRAINT fk_relationships_target_entity FOREIGN KEY (target_entity_id)
        REFERENCES knowledge.entities(entity_id) ON DELETE CASCADE,
    CONSTRAINT unique_relationship UNIQUE (subject_id, source_entity_id, target_entity_id, relationship_type)
);

CREATE INDEX IF NOT EXISTS idx_relationships_subject_id ON knowledge.relationships(subject_id);
CREATE INDEX IF NOT EXISTS idx_relationships_source_entity_id ON knowledge.relationships(source_entity_id);
CREATE INDEX IF NOT EXISTS idx_relationships_target_entity_id ON knowledge.relationships(target_entity_id);
CREATE INDEX IF NOT EXISTS idx_relationships_relationship_type ON knowledge.relationships(relationship_type);

-- ============================================================================
-- Entity Chunks Table (Maps entities to chunks where they appear)
-- ============================================================================

CREATE TABLE IF NOT EXISTS knowledge.entity_chunks (
    entity_chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    relevance_score FLOAT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entity_chunks_subject FOREIGN KEY (subject_id)
        REFERENCES knowledge.subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_chunks_entity FOREIGN KEY (entity_id)
        REFERENCES knowledge.entities(entity_id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_chunks_chunk FOREIGN KEY (chunk_id)
        REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE CASCADE,
    CONSTRAINT unique_entity_chunk UNIQUE (entity_id, chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_entity_chunks_subject_id ON knowledge.entity_chunks(subject_id);
CREATE INDEX IF NOT EXISTS idx_entity_chunks_entity_id ON knowledge.entity_chunks(entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_chunks_chunk_id ON knowledge.entity_chunks(chunk_id);
CREATE INDEX IF NOT EXISTS idx_entity_chunks_relevance_score ON knowledge.entity_chunks(relevance_score);

-- ============================================================================
-- Knowledge Graph Tables
-- ============================================================================

-- Document Knowledge Table
CREATE TABLE IF NOT EXISTS knowledge.document_knowledge (
    doc_knowledge_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    title VARCHAR(1000),
    summary TEXT,
    domain VARCHAR(500),
    subdomain VARCHAR(500),
    document_type VARCHAR(100),
    key_entities TEXT[],
    key_concepts TEXT[],
    technologies TEXT[],
    extracted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_knowledge_artifact_id ON knowledge.document_knowledge(artifact_id);
CREATE INDEX IF NOT EXISTS idx_document_knowledge_subject_id ON knowledge.document_knowledge(subject_id);

-- Knowledge Components Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_components (
    component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    subject_id UUID NOT NULL,
    domain_id UUID,
    subdomain_id UUID,
    component_name VARCHAR(500) NOT NULL,
    component_type VARCHAR(100),
    category VARCHAR(50),
    description TEXT,
    responsibility TEXT,
    technology VARCHAR(255),
    capability VARCHAR(500),
    owner VARCHAR(255),
    lifecycle VARCHAR(50),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_components_artifact_id ON knowledge.knowledge_components(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_components_subject_id ON knowledge.knowledge_components(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_components_domain_id ON knowledge.knowledge_components(domain_id);

-- Knowledge APIs Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_apis (
    api_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID,
    artifact_id UUID,
    subject_id UUID NOT NULL,
    api_name VARCHAR(500) NOT NULL,
    api_type VARCHAR(100),
    http_method VARCHAR(10),
    endpoint_path VARCHAR(2048),
    description TEXT,
    request_schema JSONB,
    response_schema JSONB,
    authentication VARCHAR(255),
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_apis_artifact_id ON knowledge.knowledge_apis(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_apis_subject_id ON knowledge.knowledge_apis(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_apis_component_id ON knowledge.knowledge_apis(component_id);

-- Knowledge Business Rules Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_business_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    component_id UUID,
    subject_id UUID NOT NULL,
    domain_id UUID,
    rule_name VARCHAR(500) NOT NULL,
    rule_type VARCHAR(100),
    condition_text TEXT,
    outcome_text TEXT,
    priority VARCHAR(50),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_business_rules_artifact_id ON knowledge.knowledge_business_rules(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_business_rules_subject_id ON knowledge.knowledge_business_rules(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_business_rules_component_id ON knowledge.knowledge_business_rules(component_id);

-- Knowledge Workflows Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflows (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    subject_id UUID NOT NULL,
    domain_id UUID,
    workflow_name VARCHAR(500) NOT NULL,
    trigger_text TEXT,
    outcome_text TEXT,
    owner VARCHAR(255),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_workflows_artifact_id ON knowledge.knowledge_workflows(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_workflows_subject_id ON knowledge.knowledge_workflows(subject_id);

-- Knowledge Workflow Steps Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_workflow_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    sequence_number INTEGER,
    step_name VARCHAR(500),
    actor VARCHAR(255),
    action_text TEXT,
    input_data TEXT,
    output_data TEXT,
    next_step VARCHAR(500),
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workflow_steps_workflow FOREIGN KEY (workflow_id)
        REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_workflow_steps_workflow_id ON knowledge.knowledge_workflow_steps(workflow_id);

-- Knowledge Resources Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_resources (
    resource_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    component_id UUID,
    subject_id UUID NOT NULL,
    resource_name VARCHAR(500) NOT NULL,
    resource_type VARCHAR(100),
    provider VARCHAR(100),
    hosting_model VARCHAR(50),
    environment VARCHAR(50),
    region VARCHAR(100),
    criticality VARCHAR(50),
    lifecycle VARCHAR(50),
    configs JSONB,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_resources_artifact_id ON knowledge.knowledge_resources(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_resources_subject_id ON knowledge.knowledge_resources(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_resources_component_id ON knowledge.knowledge_resources(component_id);

-- Knowledge Relationships Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_relationships (
    relationship_id VARCHAR(36) PRIMARY KEY,
    subject_id UUID NOT NULL,
    source_name VARCHAR(500) NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    source_ref_id VARCHAR(36),
    target_name VARCHAR(500) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_ref_id VARCHAR(36),
    relationship_type VARCHAR(100) NOT NULL,
    context TEXT,
    source_artifact_id UUID,
    source_chunk_id UUID,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    source_entity_type VARCHAR(100),
    target_entity_type VARCHAR(100),
    source_entity_id UUID,
    target_entity_id UUID,
    business_relationship_type VARCHAR(100),
    business_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_relationships_subject_id ON knowledge.knowledge_relationships(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_relationships_source_type ON knowledge.knowledge_relationships(source_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_relationships_target_type ON knowledge.knowledge_relationships(target_type);

-- Knowledge Data Models Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_models (
    data_model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    component_id UUID,
    subject_id UUID NOT NULL,
    domain_id UUID,
    model_name VARCHAR(500) NOT NULL,
    model_type VARCHAR(100),
    description TEXT,
    database_type VARCHAR(100),
    schema_name VARCHAR(255),
    confidence DOUBLE PRECISION,
    schema_definition JSONB,
    business_name VARCHAR(500),
    business_definition TEXT,
    business_owner VARCHAR(255),
    data_sensitivity VARCHAR(100),
    data_quality_requirements TEXT,
    business_usage TEXT,
    master_data BOOLEAN,
    reference_data BOOLEAN,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_data_models_domain FOREIGN KEY (domain_id)
        REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_knowledge_data_models_artifact_id ON knowledge.knowledge_data_models(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_data_models_subject_id ON knowledge.knowledge_data_models(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_data_models_domain_id ON knowledge.knowledge_data_models(domain_id);

-- Knowledge Data Fields Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_data_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_model_id UUID NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(100),
    is_primary_key BOOLEAN DEFAULT FALSE,
    is_nullable BOOLEAN DEFAULT TRUE,
    description TEXT,
    constraints TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_fields_data_model FOREIGN KEY (data_model_id)
        REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_data_fields_data_model_id ON knowledge.knowledge_data_fields(data_model_id);

-- Knowledge Integrations Table
CREATE TABLE IF NOT EXISTS knowledge.knowledge_integrations (
    integration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id UUID,
    component_id UUID,
    subject_id UUID NOT NULL,
    integration_name VARCHAR(500) NOT NULL,
    integration_type VARCHAR(100),
    source_system VARCHAR(255),
    target_system VARCHAR(255),
    protocol VARCHAR(100),
    description TEXT,
    confidence DOUBLE PRECISION,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_integrations_artifact_id ON knowledge.knowledge_integrations(artifact_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_integrations_subject_id ON knowledge.knowledge_integrations(subject_id);

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON SCHEMA knowledge IS 'Knowledge ingestion and management schema';

COMMENT ON TABLE knowledge.subjects IS 'Subjects representing knowledge domains';
COMMENT ON TABLE knowledge.knowledge_ingestion_process IS 'Tracks document ingestion processes for subjects';
COMMENT ON TABLE knowledge.artifacts IS 'Binary artifacts and files associated with subjects';
COMMENT ON TABLE knowledge.domains IS 'Top-level knowledge domains within subjects';
COMMENT ON TABLE knowledge.subdomains IS 'Sub-categories within domains';
COMMENT ON TABLE knowledge.ingestion_documents IS 'Documents being processed for knowledge extraction';
COMMENT ON TABLE knowledge.semantic_chunks IS 'Text chunks for semantic processing and embedding';
COMMENT ON TABLE knowledge.entities IS 'Named entities extracted from documents';
COMMENT ON TABLE knowledge.relationships IS 'Relationships between entities';
COMMENT ON TABLE knowledge.entity_chunks IS 'Maps entities to chunks where they appear';
COMMENT ON TABLE knowledge.document_knowledge IS 'Document-level knowledge extraction results';
COMMENT ON TABLE knowledge.knowledge_components IS 'System components extracted from documents';
COMMENT ON TABLE knowledge.knowledge_apis IS 'API definitions extracted from documents';
COMMENT ON TABLE knowledge.knowledge_business_rules IS 'Business rules extracted from documents';
COMMENT ON TABLE knowledge.knowledge_workflows IS 'Workflow definitions extracted from documents';
COMMENT ON TABLE knowledge.knowledge_workflow_steps IS 'Individual steps within workflows';
COMMENT ON TABLE knowledge.knowledge_resources IS 'Infrastructure and cloud resources';
COMMENT ON TABLE knowledge.knowledge_relationships IS 'Relationships between knowledge graph entities';
COMMENT ON TABLE knowledge.knowledge_data_models IS 'Data models and schemas';
COMMENT ON TABLE knowledge.knowledge_data_fields IS 'Fields within data models';
COMMENT ON TABLE knowledge.knowledge_integrations IS 'System integrations and interfaces';
