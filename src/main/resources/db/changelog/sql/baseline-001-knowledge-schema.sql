-- liquibase formatted sql

-- changeset kengine:baseline-001-knowledge-schema

-- ============================================================================
-- Extensions
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- Schema
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS knowledge;

-- ============================================================================
-- Core Structural Tables
-- ============================================================================

-- Projects table
CREATE TABLE knowledge.projects (
    project_id VARCHAR(255) PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    source_bucket VARCHAR(255) NOT NULL,
    gcs_prefix VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Domains table
CREATE TABLE knowledge.domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Subdomains table
CREATE TABLE knowledge.subdomains (
    subdomain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_id UUID,
    project_id VARCHAR(255) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    subdomain VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Artifacts table
CREATE TABLE knowledge.artifacts (
    artifact_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    subdomain VARCHAR(255),
    source_bucket VARCHAR(255) NOT NULL,
    source_object VARCHAR(2048) NOT NULL,
    source_generation BIGINT,
    source_checksum VARCHAR(255),
    content_hash VARCHAR(64) NOT NULL,
    artifact_type VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    title VARCHAR(1024) NOT NULL,
    version VARCHAR(255),
    is_current BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Document Processing Tables
-- ============================================================================

-- Ingestion documents table
CREATE TABLE knowledge.ingestion_documents (
    document_id VARCHAR(36) PRIMARY KEY,
    artifact_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(255) NOT NULL,
    source_bucket VARCHAR(255) NOT NULL,
    source_object VARCHAR(2048) NOT NULL,
    source_generation BIGINT,
    source_checksum VARCHAR(255),
    content_hash VARCHAR(64) NOT NULL,
    chunk_count BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Semantic chunks table
CREATE TABLE knowledge.semantic_chunks (
    chunk_id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    artifact_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(255) NOT NULL,
    source_bucket VARCHAR(255) NOT NULL,
    source_object VARCHAR(2048) NOT NULL,
    source_generation BIGINT,
    source_checksum VARCHAR(255),
    document_content_hash VARCHAR(64) NOT NULL,
    chunk_index BIGINT NOT NULL,
    total_chunks BIGINT NOT NULL,
    char_start BIGINT NOT NULL,
    char_end BIGINT NOT NULL,
    chunk_content_hash VARCHAR(64) NOT NULL,
    domain VARCHAR(255),
    subdomain VARCHAR(255),
    content TEXT,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Knowledge Entity Tables
-- ============================================================================

-- Knowledge components table
CREATE TABLE knowledge.knowledge_components (
    component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    project_id VARCHAR(255) NOT NULL,
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

-- Knowledge APIs table
CREATE TABLE knowledge.knowledge_apis (
    api_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID,
    artifact_id VARCHAR(36),
    project_id VARCHAR(255) NOT NULL,
    api_name VARCHAR(500) NOT NULL,
    api_type VARCHAR(100),
    http_method VARCHAR(10),
    endpoint_path VARCHAR(2000),
    description TEXT,
    request_schema JSONB,
    response_schema JSONB,
    authentication VARCHAR(100),
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge business rules table
CREATE TABLE knowledge.knowledge_business_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    component_id UUID,
    project_id VARCHAR(255) NOT NULL,
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

-- Knowledge workflows table
CREATE TABLE knowledge.knowledge_workflows (
    workflow_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    project_id VARCHAR(255) NOT NULL,
    domain_id UUID,
    workflow_name VARCHAR(500) NOT NULL,
    trigger_text TEXT,
    outcome_text TEXT,
    owner VARCHAR(255),
    confidence DOUBLE PRECISION,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge workflow steps table
CREATE TABLE knowledge.knowledge_workflow_steps (
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge data models table
CREATE TABLE knowledge.knowledge_data_models (
    data_model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    project_id VARCHAR(255) NOT NULL,
    domain_id UUID,
    model_name VARCHAR(500) NOT NULL,
    model_type VARCHAR(100),
    description TEXT,
    schema_definition JSONB,
    embedding vector(768),
    business_name VARCHAR(500),
    business_definition TEXT,
    business_owner VARCHAR(255),
    data_sensitivity VARCHAR(100),
    data_quality_requirements TEXT,
    business_usage TEXT,
    master_data BOOLEAN,
    reference_data BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge data fields table
CREATE TABLE knowledge.knowledge_data_fields (
    field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_model_id UUID NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_type VARCHAR(100),
    is_required BOOLEAN,
    description TEXT,
    constraints JSONB,
    embedding vector(768),
    business_name VARCHAR(500),
    business_definition TEXT,
    business_examples TEXT,
    business_rules JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge integrations table
CREATE TABLE knowledge.knowledge_integrations (
    integration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    component_id UUID,
    project_id VARCHAR(255) NOT NULL,
    integration_name VARCHAR(500) NOT NULL,
    integration_type VARCHAR(100),
    source_system VARCHAR(255),
    target_system VARCHAR(255),
    protocol VARCHAR(100),
    description TEXT,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge resources table
CREATE TABLE knowledge.knowledge_resources (
    resource_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36),
    component_id UUID,
    project_id VARCHAR(255) NOT NULL,
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

-- Knowledge relationships table
CREATE TABLE knowledge.knowledge_relationships (
    relationship_id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    source_name VARCHAR(500) NOT NULL,
    source_type VARCHAR(255) NOT NULL,
    source_ref_id VARCHAR(36),
    target_name VARCHAR(500) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_ref_id VARCHAR(36),
    relationship_type VARCHAR(255) NOT NULL,
    context TEXT,
    source_artifact_id VARCHAR(36),
    source_chunk_id VARCHAR(36),
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

-- Document knowledge table
CREATE TABLE knowledge.document_knowledge (
    doc_knowledge_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id VARCHAR(36) NOT NULL,
    overall_architecture TEXT,
    system_summary TEXT,
    key_patterns JSONB,
    technologies JSONB,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================

-- Domains
ALTER TABLE knowledge.domains
    ADD CONSTRAINT fk_domains_project FOREIGN KEY (project_id)
    REFERENCES knowledge.projects(project_id) ON DELETE CASCADE;

-- Subdomains
ALTER TABLE knowledge.subdomains
    ADD CONSTRAINT fk_subdomains_domain FOREIGN KEY (domain_id)
    REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL;

ALTER TABLE knowledge.subdomains
    ADD CONSTRAINT fk_subdomains_project FOREIGN KEY (project_id)
    REFERENCES knowledge.projects(project_id) ON DELETE CASCADE;

-- Artifacts
ALTER TABLE knowledge.artifacts
    ADD CONSTRAINT fk_artifacts_project FOREIGN KEY (project_id)
    REFERENCES knowledge.projects(project_id) ON DELETE CASCADE;

-- Ingestion documents
ALTER TABLE knowledge.ingestion_documents
    ADD CONSTRAINT fk_ingestion_documents_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE CASCADE;

-- Semantic chunks
ALTER TABLE knowledge.semantic_chunks
    ADD CONSTRAINT fk_semantic_chunks_document FOREIGN KEY (document_id)
    REFERENCES knowledge.ingestion_documents(document_id) ON DELETE CASCADE;

ALTER TABLE knowledge.semantic_chunks
    ADD CONSTRAINT fk_semantic_chunks_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE CASCADE;

-- Knowledge components
ALTER TABLE knowledge.knowledge_components
    ADD CONSTRAINT fk_knowledge_components_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_components
    ADD CONSTRAINT fk_knowledge_components_domain FOREIGN KEY (domain_id)
    REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_components
    ADD CONSTRAINT fk_knowledge_components_subdomain FOREIGN KEY (subdomain_id)
    REFERENCES knowledge.subdomains(subdomain_id) ON DELETE SET NULL;

-- Knowledge APIs
ALTER TABLE knowledge.knowledge_apis
    ADD CONSTRAINT fk_knowledge_apis_component FOREIGN KEY (component_id)
    REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_apis
    ADD CONSTRAINT fk_knowledge_apis_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

-- Knowledge business rules
ALTER TABLE knowledge.knowledge_business_rules
    ADD CONSTRAINT fk_knowledge_business_rules_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_business_rules
    ADD CONSTRAINT fk_knowledge_business_rules_component FOREIGN KEY (component_id)
    REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_business_rules
    ADD CONSTRAINT fk_knowledge_business_rules_domain FOREIGN KEY (domain_id)
    REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL;

-- Knowledge workflows
ALTER TABLE knowledge.knowledge_workflows
    ADD CONSTRAINT fk_knowledge_workflows_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_workflows
    ADD CONSTRAINT fk_knowledge_workflows_domain FOREIGN KEY (domain_id)
    REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL;

-- Knowledge workflow steps
ALTER TABLE knowledge.knowledge_workflow_steps
    ADD CONSTRAINT fk_knowledge_workflow_steps_workflow FOREIGN KEY (workflow_id)
    REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE;

-- Knowledge data models
ALTER TABLE knowledge.knowledge_data_models
    ADD CONSTRAINT fk_knowledge_data_models_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_data_models
    ADD CONSTRAINT fk_knowledge_data_models_domain FOREIGN KEY (domain_id)
    REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL;

-- Knowledge data fields
ALTER TABLE knowledge.knowledge_data_fields
    ADD CONSTRAINT fk_knowledge_data_fields_data_model FOREIGN KEY (data_model_id)
    REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE;

-- Knowledge integrations
ALTER TABLE knowledge.knowledge_integrations
    ADD CONSTRAINT fk_knowledge_integrations_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_integrations
    ADD CONSTRAINT fk_knowledge_integrations_component FOREIGN KEY (component_id)
    REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL;

-- Knowledge resources
ALTER TABLE knowledge.knowledge_resources
    ADD CONSTRAINT fk_knowledge_resources_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_resources
    ADD CONSTRAINT fk_knowledge_resources_component FOREIGN KEY (component_id)
    REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL;

-- Document knowledge
ALTER TABLE knowledge.document_knowledge
    ADD CONSTRAINT fk_document_knowledge_artifact FOREIGN KEY (artifact_id)
    REFERENCES knowledge.artifacts(artifact_id) ON DELETE CASCADE;

-- ============================================================================
-- Unique Constraints
-- ============================================================================

-- Domains: unique project + domain combination
ALTER TABLE knowledge.domains
    ADD CONSTRAINT uk_domains_project_domain UNIQUE (project_id, domain);

-- Subdomains: unique project + domain + subdomain combination
ALTER TABLE knowledge.subdomains
    ADD CONSTRAINT uk_subdomains_project_domain_subdomain UNIQUE (project_id, domain, subdomain);

-- Artifacts: unique source object per project
ALTER TABLE knowledge.artifacts
    ADD CONSTRAINT uk_artifacts_project_source UNIQUE (project_id, source_bucket, source_object, content_hash);

-- Document knowledge: unique per artifact
ALTER TABLE knowledge.document_knowledge
    ADD CONSTRAINT uk_document_knowledge_artifact UNIQUE (artifact_id);

-- ============================================================================
-- Standard Indexes
-- ============================================================================

-- Projects
CREATE INDEX idx_projects_project_name ON knowledge.projects(project_name);

-- Domains
CREATE INDEX idx_domains_project_id ON knowledge.domains(project_id);
CREATE INDEX idx_domains_domain ON knowledge.domains(domain);

-- Subdomains
CREATE INDEX idx_subdomains_project_id ON knowledge.subdomains(project_id);
CREATE INDEX idx_subdomains_domain_id ON knowledge.subdomains(domain_id);
CREATE INDEX idx_subdomains_domain ON knowledge.subdomains(domain);
CREATE INDEX idx_subdomains_subdomain ON knowledge.subdomains(subdomain);

-- Artifacts
CREATE INDEX idx_artifacts_project_id ON knowledge.artifacts(project_id);
CREATE INDEX idx_artifacts_source_object ON knowledge.artifacts(source_object);
CREATE INDEX idx_artifacts_artifact_type ON knowledge.artifacts(artifact_type);
CREATE INDEX idx_artifacts_domain ON knowledge.artifacts(domain);
CREATE INDEX idx_artifacts_subdomain ON knowledge.artifacts(subdomain);
CREATE INDEX idx_artifacts_is_current ON knowledge.artifacts(project_id, is_current);

-- Ingestion documents
CREATE INDEX idx_ingestion_documents_artifact_id ON knowledge.ingestion_documents(artifact_id);
CREATE INDEX idx_ingestion_documents_project_id ON knowledge.ingestion_documents(project_id);
CREATE INDEX idx_ingestion_documents_source_object ON knowledge.ingestion_documents(source_object);

-- Semantic chunks
CREATE INDEX idx_semantic_chunks_document_id ON knowledge.semantic_chunks(document_id);
CREATE INDEX idx_semantic_chunks_artifact_id ON knowledge.semantic_chunks(artifact_id);
CREATE INDEX idx_semantic_chunks_project_id ON knowledge.semantic_chunks(project_id);
CREATE INDEX idx_semantic_chunks_source_object ON knowledge.semantic_chunks(source_object);
CREATE INDEX idx_semantic_chunks_domain ON knowledge.semantic_chunks(domain);
CREATE INDEX idx_semantic_chunks_subdomain ON knowledge.semantic_chunks(subdomain);
CREATE INDEX idx_semantic_chunks_chunk_index ON knowledge.semantic_chunks(document_id, chunk_index);

-- Knowledge components
CREATE INDEX idx_knowledge_components_artifact_id ON knowledge.knowledge_components(artifact_id);
CREATE INDEX idx_knowledge_components_project_id ON knowledge.knowledge_components(project_id);
CREATE INDEX idx_knowledge_components_domain_id ON knowledge.knowledge_components(domain_id);
CREATE INDEX idx_knowledge_components_subdomain_id ON knowledge.knowledge_components(subdomain_id);
CREATE INDEX idx_knowledge_components_component_type ON knowledge.knowledge_components(component_type);
CREATE INDEX idx_knowledge_components_component_name ON knowledge.knowledge_components(component_name);

-- Knowledge APIs
CREATE INDEX idx_knowledge_apis_component_id ON knowledge.knowledge_apis(component_id);
CREATE INDEX idx_knowledge_apis_artifact_id ON knowledge.knowledge_apis(artifact_id);
CREATE INDEX idx_knowledge_apis_project_id ON knowledge.knowledge_apis(project_id);
CREATE INDEX idx_knowledge_apis_api_type ON knowledge.knowledge_apis(api_type);
CREATE INDEX idx_knowledge_apis_api_name ON knowledge.knowledge_apis(api_name);

-- Knowledge business rules
CREATE INDEX idx_knowledge_business_rules_artifact_id ON knowledge.knowledge_business_rules(artifact_id);
CREATE INDEX idx_knowledge_business_rules_component_id ON knowledge.knowledge_business_rules(component_id);
CREATE INDEX idx_knowledge_business_rules_project_id ON knowledge.knowledge_business_rules(project_id);
CREATE INDEX idx_knowledge_business_rules_domain_id ON knowledge.knowledge_business_rules(domain_id);
CREATE INDEX idx_knowledge_business_rules_rule_type ON knowledge.knowledge_business_rules(rule_type);

-- Knowledge workflows
CREATE INDEX idx_knowledge_workflows_artifact_id ON knowledge.knowledge_workflows(artifact_id);
CREATE INDEX idx_knowledge_workflows_project_id ON knowledge.knowledge_workflows(project_id);
CREATE INDEX idx_knowledge_workflows_domain_id ON knowledge.knowledge_workflows(domain_id);
CREATE INDEX idx_knowledge_workflows_workflow_name ON knowledge.knowledge_workflows(workflow_name);

-- Knowledge workflow steps
CREATE INDEX idx_knowledge_workflow_steps_workflow_id ON knowledge.knowledge_workflow_steps(workflow_id);
CREATE INDEX idx_knowledge_workflow_steps_sequence ON knowledge.knowledge_workflow_steps(workflow_id, sequence_number);

-- Knowledge data models
CREATE INDEX idx_knowledge_data_models_artifact_id ON knowledge.knowledge_data_models(artifact_id);
CREATE INDEX idx_knowledge_data_models_project_id ON knowledge.knowledge_data_models(project_id);
CREATE INDEX idx_knowledge_data_models_domain_id ON knowledge.knowledge_data_models(domain_id);
CREATE INDEX idx_knowledge_data_models_model_name ON knowledge.knowledge_data_models(model_name);
CREATE INDEX idx_knowledge_data_models_model_type ON knowledge.knowledge_data_models(model_type);

-- Knowledge data fields
CREATE INDEX idx_knowledge_data_fields_data_model_id ON knowledge.knowledge_data_fields(data_model_id);
CREATE INDEX idx_knowledge_data_fields_field_name ON knowledge.knowledge_data_fields(field_name);

-- Knowledge integrations
CREATE INDEX idx_knowledge_integrations_artifact_id ON knowledge.knowledge_integrations(artifact_id);
CREATE INDEX idx_knowledge_integrations_component_id ON knowledge.knowledge_integrations(component_id);
CREATE INDEX idx_knowledge_integrations_project_id ON knowledge.knowledge_integrations(project_id);
CREATE INDEX idx_knowledge_integrations_integration_type ON knowledge.knowledge_integrations(integration_type);

-- Knowledge resources
CREATE INDEX idx_knowledge_resources_artifact_id ON knowledge.knowledge_resources(artifact_id);
CREATE INDEX idx_knowledge_resources_component_id ON knowledge.knowledge_resources(component_id);
CREATE INDEX idx_knowledge_resources_project_id ON knowledge.knowledge_resources(project_id);
CREATE INDEX idx_knowledge_resources_resource_type ON knowledge.knowledge_resources(resource_type);
CREATE INDEX idx_knowledge_resources_provider ON knowledge.knowledge_resources(provider);

-- Knowledge relationships
CREATE INDEX idx_knowledge_relationships_project_id ON knowledge.knowledge_relationships(project_id);
CREATE INDEX idx_knowledge_relationships_source_type ON knowledge.knowledge_relationships(source_type);
CREATE INDEX idx_knowledge_relationships_target_type ON knowledge.knowledge_relationships(target_type);
CREATE INDEX idx_knowledge_relationships_relationship_type ON knowledge.knowledge_relationships(relationship_type);
CREATE INDEX idx_knowledge_relationships_source_artifact_id ON knowledge.knowledge_relationships(source_artifact_id);
CREATE INDEX idx_knowledge_relationships_source_entity_id ON knowledge.knowledge_relationships(source_entity_id);
CREATE INDEX idx_knowledge_relationships_target_entity_id ON knowledge.knowledge_relationships(target_entity_id);

-- Document knowledge
CREATE INDEX idx_document_knowledge_artifact_id ON knowledge.document_knowledge(artifact_id);

-- ============================================================================
-- Vector Indexes (HNSW for semantic search)
-- ============================================================================

-- Semantic chunks embedding
CREATE INDEX idx_semantic_chunks_embedding ON knowledge.semantic_chunks
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge components embedding
CREATE INDEX idx_knowledge_components_embedding ON knowledge.knowledge_components
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge APIs embedding
CREATE INDEX idx_knowledge_apis_embedding ON knowledge.knowledge_apis
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge business rules embedding
CREATE INDEX idx_knowledge_business_rules_embedding ON knowledge.knowledge_business_rules
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge workflows embedding
CREATE INDEX idx_knowledge_workflows_embedding ON knowledge.knowledge_workflows
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge workflow steps embedding
CREATE INDEX idx_knowledge_workflow_steps_embedding ON knowledge.knowledge_workflow_steps
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge data models embedding
CREATE INDEX idx_knowledge_data_models_embedding ON knowledge.knowledge_data_models
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge data fields embedding
CREATE INDEX idx_knowledge_data_fields_embedding ON knowledge.knowledge_data_fields
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge integrations embedding
CREATE INDEX idx_knowledge_integrations_embedding ON knowledge.knowledge_integrations
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge resources embedding
CREATE INDEX idx_knowledge_resources_embedding ON knowledge.knowledge_resources
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Knowledge relationships embedding
CREATE INDEX idx_knowledge_relationships_embedding ON knowledge.knowledge_relationships
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Document knowledge embedding
CREATE INDEX idx_document_knowledge_embedding ON knowledge.document_knowledge
    USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- rollback DROP SCHEMA knowledge CASCADE;
