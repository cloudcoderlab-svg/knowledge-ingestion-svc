DROP TABLE IF EXISTS knowledge.resource_cost_estimates;
DROP TABLE IF EXISTS knowledge.usage_profiles;
DROP TABLE IF EXISTS knowledge.knowledge_notes;
DROP TABLE IF EXISTS knowledge.knowledge_relationships;
DROP TABLE IF EXISTS knowledge.deployment_resource_configs;
DROP TABLE IF EXISTS knowledge.deployment_resources;
DROP TABLE IF EXISTS knowledge.business_flow_steps;
DROP TABLE IF EXISTS knowledge.business_flows;
DROP TABLE IF EXISTS knowledge.business_rules;
DROP TABLE IF EXISTS knowledge.solution_components;
DROP TABLE IF EXISTS knowledge.semantic_chunks;
DROP TABLE IF EXISTS knowledge.ingestion_documents;
DROP TABLE IF EXISTS knowledge.artifacts;
DROP TABLE IF EXISTS knowledge.subdomains;
DROP TABLE IF EXISTS knowledge.domains;
DROP TABLE IF EXISTS knowledge.projects;
CREATE SCHEMA IF NOT EXISTS knowledge;

CREATE TABLE knowledge.projects (
    project_id text NOT NULL,
    project_name text NOT NULL,
    source_bucket text NOT NULL,
    gcs_prefix text NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (project_id)
);

CREATE TABLE knowledge.domains (
    project_id text NOT NULL,
    domain text NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (project_id, domain),
    CONSTRAINT fk_domain_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id)
);

CREATE TABLE knowledge.subdomains (
    project_id text NOT NULL,
    domain text NOT NULL,
    subdomain text NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (project_id, domain, subdomain),
    CONSTRAINT fk_subdomain_domain FOREIGN KEY (project_id, domain)
        REFERENCES knowledge.domains (project_id, domain)
);

CREATE TABLE knowledge.artifacts (
    artifact_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    domain text,
    subdomain text,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation int8,
    source_checksum text,
    content_hash varchar(64) NOT NULL,
    artifact_type text NOT NULL,
    file_type text NOT NULL,
    title text NOT NULL,
    version text,
    is_current bool NOT NULL,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (artifact_id),
    CONSTRAINT fk_artifact_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_artifact_subdomain FOREIGN KEY (project_id, domain, subdomain)
        REFERENCES knowledge.subdomains (project_id, domain, subdomain)
);

CREATE TABLE knowledge.ingestion_documents (
    document_id varchar(36) NOT NULL,
    artifact_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation int8,
    source_checksum text,
    content_hash varchar(64) NOT NULL,
    chunk_count int8 NOT NULL,
    created_at timestamptz,
    PRIMARY KEY (document_id),
    CONSTRAINT fk_document_artifact FOREIGN KEY (artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_document_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id)
);

CREATE TABLE knowledge.semantic_chunks (
    chunk_id varchar(36) NOT NULL,
    document_id varchar(36) NOT NULL,
    artifact_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation int8,
    source_checksum text,
    document_content_hash varchar(64) NOT NULL,
    chunk_index int8 NOT NULL,
    total_chunks int8 NOT NULL,
    char_start int8 NOT NULL,
    char_end int8 NOT NULL,
    chunk_content_hash varchar(64) NOT NULL,
    domain text,
    subdomain text,
    content text,
    embedding double precision[],
    created_at timestamptz,
    PRIMARY KEY (chunk_id),
    CONSTRAINT fk_chunk_artifact FOREIGN KEY (artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge.ingestion_documents (document_id),
    CONSTRAINT fk_chunk_subdomain FOREIGN KEY (project_id, domain, subdomain)
        REFERENCES knowledge.subdomains (project_id, domain, subdomain)
);

CREATE UNIQUE INDEX ux_ingestion_documents_source_hash
    ON knowledge.ingestion_documents (project_id, source_bucket, source_object, content_hash);

CREATE UNIQUE INDEX ux_artifacts_source_hash
    ON knowledge.artifacts (project_id, source_bucket, source_object, content_hash);

CREATE UNIQUE INDEX ux_semantic_chunks_source_hash_index
    ON knowledge.semantic_chunks (project_id, source_bucket, source_object, document_content_hash, chunk_index);

CREATE INDEX ix_artifacts_project_source
    ON knowledge.artifacts (project_id, source_object);

CREATE INDEX ix_semantic_chunks_project_source
    ON knowledge.semantic_chunks (project_id, source_object, chunk_index);

CREATE TABLE knowledge.solution_components (
    component_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    domain text,
    subdomain text,
    component_layer text NOT NULL,
    component_name text NOT NULL,
    component_type text NOT NULL,
    capability text,
    responsibility text,
    technology text,
    owner text,
    lifecycle text NOT NULL,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (component_id),
    CONSTRAINT fk_solution_component_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_solution_component_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_solution_component_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE INDEX ix_solution_components_project_artifact
    ON knowledge.solution_components (project_id, source_artifact_id);

CREATE TABLE knowledge.business_rules (
    rule_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    rule_name text NOT NULL,
    rule_type text NOT NULL,
    condition_text text,
    outcome_text text,
    source_business_component_name text,
    priority text,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (rule_id),
    CONSTRAINT fk_business_rule_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_business_rule_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_business_rule_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE INDEX ix_business_rules_project_artifact
    ON knowledge.business_rules (project_id, source_artifact_id);

CREATE TABLE knowledge.business_flows (
    flow_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    flow_name text NOT NULL,
    trigger_text text,
    outcome_text text,
    owner text,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (flow_id),
    CONSTRAINT fk_business_flow_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_business_flow_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_business_flow_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE INDEX ix_business_flows_project_artifact
    ON knowledge.business_flows (project_id, source_artifact_id);

CREATE TABLE knowledge.business_flow_steps (
    step_id varchar(36) NOT NULL,
    flow_id varchar(36) NOT NULL,
    sequence_number int8,
    step_name text,
    actor text,
    action_text text,
    input_text text,
    output_text text,
    next_step text,
    created_at timestamptz,
    PRIMARY KEY (step_id),
    CONSTRAINT fk_business_flow_step_flow FOREIGN KEY (flow_id) REFERENCES knowledge.business_flows (flow_id)
);

CREATE TABLE knowledge.deployment_resources (
    resource_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    domain text,
    subdomain text,
    resource_name text NOT NULL,
    resource_type text NOT NULL,
    provider text NOT NULL,
    hosting_model text NOT NULL,
    environment text NOT NULL,
    region text,
    criticality text,
    lifecycle text NOT NULL,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (resource_id),
    CONSTRAINT fk_deployment_resource_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_deployment_resource_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_deployment_resource_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE INDEX ix_deployment_resources_project_artifact
    ON knowledge.deployment_resources (project_id, source_artifact_id);

CREATE TABLE knowledge.deployment_resource_configs (
    config_id varchar(36) NOT NULL,
    resource_id varchar(36) NOT NULL,
    config_key text NOT NULL,
    config_value text NOT NULL,
    unit text,
    source_chunk_id varchar(36),
    created_at timestamptz,
    PRIMARY KEY (config_id),
    CONSTRAINT fk_deployment_resource_config_resource FOREIGN KEY (resource_id)
        REFERENCES knowledge.deployment_resources (resource_id),
    CONSTRAINT fk_deployment_resource_config_chunk FOREIGN KEY (source_chunk_id)
        REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE TABLE knowledge.knowledge_relationships (
    relationship_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    source_name text NOT NULL,
    source_type text NOT NULL,
    source_ref_id varchar(36),
    target_name text NOT NULL,
    target_type text NOT NULL,
    target_ref_id varchar(36),
    relationship_type text NOT NULL,
    context text,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    PRIMARY KEY (relationship_id),
    CONSTRAINT fk_knowledge_relationship_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_knowledge_relationship_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_knowledge_relationship_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE INDEX ix_knowledge_relationships_project_artifact
    ON knowledge.knowledge_relationships (project_id, source_artifact_id);

CREATE TABLE knowledge.knowledge_notes (
    note_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    note_type text NOT NULL,
    note_text text NOT NULL,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    created_at timestamptz,
    PRIMARY KEY (note_id),
    CONSTRAINT fk_knowledge_note_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_knowledge_note_artifact FOREIGN KEY (source_artifact_id) REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_knowledge_note_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE TABLE knowledge.usage_profiles (
    profile_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    environment text NOT NULL,
    users_count int8,
    requests_per_day int8,
    peak_rps double precision,
    data_ingest_gb_per_day double precision,
    data_retention_days int8,
    storage_growth_gb_per_month double precision,
    availability_target text,
    dr_required bool,
    source_chunk_id varchar(36),
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (profile_id),
    CONSTRAINT fk_usage_profile_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_usage_profile_chunk FOREIGN KEY (source_chunk_id) REFERENCES knowledge.semantic_chunks (chunk_id)
);

CREATE TABLE knowledge.resource_cost_estimates (
    estimate_id varchar(36) NOT NULL,
    project_id text NOT NULL,
    resource_id varchar(36),
    resource_name text,
    environment text NOT NULL,
    provider text NOT NULL,
    billing_model text NOT NULL,
    quantity double precision,
    unit text,
    unit_cost double precision,
    estimated_monthly_cost double precision,
    currency text NOT NULL,
    pricing_source text,
    pricing_date date,
    assumptions text,
    source_artifact_id varchar(36),
    source_chunk_id varchar(36),
    confidence double precision,
    created_at timestamptz,
    updated_at timestamptz,
    PRIMARY KEY (estimate_id),
    CONSTRAINT fk_cost_estimate_project FOREIGN KEY (project_id) REFERENCES knowledge.projects (project_id),
    CONSTRAINT fk_cost_estimate_resource FOREIGN KEY (resource_id)
        REFERENCES knowledge.deployment_resources (resource_id),
    CONSTRAINT fk_cost_estimate_artifact FOREIGN KEY (source_artifact_id)
        REFERENCES knowledge.artifacts (artifact_id),
    CONSTRAINT fk_cost_estimate_chunk FOREIGN KEY (source_chunk_id)
        REFERENCES knowledge.semantic_chunks (chunk_id)
);

