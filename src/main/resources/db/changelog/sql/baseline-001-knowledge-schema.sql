CREATE SCHEMA IF NOT EXISTS knowledge;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE knowledge.projects (
    project_id text PRIMARY KEY,
    project_name text NOT NULL,
    source_bucket text NOT NULL,
    gcs_prefix text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.domains (
    domain_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_domains_project_domain UNIQUE (project_id, domain)
);

CREATE TABLE knowledge.subdomains (
    subdomain_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_id uuid REFERENCES knowledge.domains(domain_id) ON DELETE CASCADE,
    project_id text NOT NULL,
    domain text NOT NULL,
    subdomain text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_subdomains_project_domain_subdomain UNIQUE (project_id, domain, subdomain),
    CONSTRAINT fk_subdomain_domain_name FOREIGN KEY (project_id, domain)
        REFERENCES knowledge.domains(project_id, domain) ON DELETE CASCADE
);

CREATE TABLE knowledge.artifacts (
    artifact_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain text,
    subdomain text,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation bigint,
    source_checksum text,
    content_hash varchar(64) NOT NULL,
    artifact_type text NOT NULL,
    file_type text NOT NULL,
    title text NOT NULL,
    version text,
    is_current boolean NOT NULL DEFAULT true,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_artifact_subdomain FOREIGN KEY (project_id, domain, subdomain)
        REFERENCES knowledge.subdomains(project_id, domain, subdomain) ON DELETE SET NULL
);

CREATE TABLE knowledge.ingestion_documents (
    document_id varchar(36) PRIMARY KEY,
    artifact_id varchar(36) NOT NULL REFERENCES knowledge.artifacts(artifact_id) ON DELETE CASCADE,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation bigint,
    source_checksum text,
    content_hash varchar(64) NOT NULL,
    chunk_count bigint NOT NULL DEFAULT 0,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.semantic_chunks (
    chunk_id varchar(36) PRIMARY KEY,
    document_id varchar(36) NOT NULL REFERENCES knowledge.ingestion_documents(document_id) ON DELETE CASCADE,
    artifact_id varchar(36) NOT NULL REFERENCES knowledge.artifacts(artifact_id) ON DELETE CASCADE,
    project_id text NOT NULL,
    source_bucket text NOT NULL,
    source_object text NOT NULL,
    source_generation bigint,
    source_checksum text,
    document_content_hash varchar(64) NOT NULL,
    chunk_index bigint NOT NULL,
    total_chunks bigint NOT NULL,
    char_start bigint NOT NULL,
    char_end bigint NOT NULL,
    chunk_content_hash varchar(64) NOT NULL,
    domain text,
    subdomain text,
    content text,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_subdomain FOREIGN KEY (project_id, domain, subdomain)
        REFERENCES knowledge.subdomains(project_id, domain, subdomain) ON DELETE SET NULL
);

CREATE TABLE knowledge.solution_components (
    component_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
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
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_rules (
    rule_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    rule_name text NOT NULL,
    rule_type text NOT NULL,
    condition_text text,
    outcome_text text,
    source_business_component_name text,
    priority text,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    embedding vector(768),
    business_rationale text,
    business_impact text,
    rule_category varchar(100),
    business_owner varchar(255),
    regulatory_basis text,
    exception_handling text,
    business_examples text,
    affects_business_capability uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_flows (
    flow_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    flow_name text NOT NULL,
    trigger_text text,
    outcome_text text,
    owner text,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    embedding vector(768),
    business_purpose text,
    business_capability_id uuid,
    business_value text,
    business_frequency varchar(50),
    business_volume varchar(50),
    business_criticality varchar(50),
    business_owner varchar(255),
    initiated_by_role uuid,
    success_criteria text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_flow_steps (
    step_id varchar(36) PRIMARY KEY,
    flow_id varchar(36) NOT NULL REFERENCES knowledge.business_flows(flow_id) ON DELETE CASCADE,
    sequence_number bigint,
    step_name text,
    actor text,
    action_text text,
    input_text text,
    output_text text,
    next_step text,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.deployment_resources (
    resource_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
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
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.deployment_resource_configs (
    config_id varchar(36) PRIMARY KEY,
    resource_id varchar(36) NOT NULL REFERENCES knowledge.deployment_resources(resource_id) ON DELETE CASCADE,
    config_key text NOT NULL,
    config_value text NOT NULL,
    unit text,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_relationships (
    relationship_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    source_name text NOT NULL,
    source_type text NOT NULL,
    source_ref_id varchar(36),
    target_name text NOT NULL,
    target_type text NOT NULL,
    target_ref_id varchar(36),
    relationship_type text NOT NULL,
    context text,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    embedding vector(768),
    source_entity_type varchar(100),
    target_entity_type varchar(100),
    source_entity_id uuid,
    target_entity_id uuid,
    business_relationship_type varchar(100),
    business_description text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_notes (
    note_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    note_type text NOT NULL,
    note_text text NOT NULL,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.usage_profiles (
    profile_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    environment text NOT NULL,
    users_count bigint,
    requests_per_day bigint,
    peak_rps double precision,
    data_ingest_gb_per_day double precision,
    data_retention_days bigint,
    storage_growth_gb_per_month double precision,
    availability_target text,
    dr_required boolean,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.resource_cost_estimates (
    estimate_id varchar(36) PRIMARY KEY,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    resource_id varchar(36) REFERENCES knowledge.deployment_resources(resource_id) ON DELETE SET NULL,
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
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    source_chunk_id varchar(36) REFERENCES knowledge.semantic_chunks(chunk_id) ON DELETE SET NULL,
    confidence double precision,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_data_models (
    data_model_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id uuid REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL,
    model_name varchar(500) NOT NULL,
    model_type varchar(100),
    description text,
    schema_definition jsonb,
    embedding vector(768),
    business_name varchar(500),
    business_definition text,
    business_owner varchar(255),
    data_sensitivity varchar(50),
    data_quality_requirements text,
    business_usage text,
    master_data boolean DEFAULT false,
    reference_data boolean DEFAULT false,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_data_fields (
    field_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    data_model_id uuid NOT NULL REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE CASCADE,
    field_name varchar(255) NOT NULL,
    field_type varchar(100),
    is_required boolean,
    description text,
    constraints jsonb,
    embedding vector(768),
    business_name varchar(255),
    business_definition text,
    business_examples text,
    business_rules jsonb,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.document_knowledge (
    doc_knowledge_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text NOT NULL,
    overall_architecture text,
    system_summary text,
    key_patterns jsonb,
    technologies jsonb,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_components (
    component_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id uuid REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL,
    subdomain_id uuid REFERENCES knowledge.subdomains(subdomain_id) ON DELETE SET NULL,
    component_name varchar(500) NOT NULL,
    component_type varchar(100),
    category varchar(50),
    description text,
    responsibility text,
    technology varchar(255),
    capability varchar(500),
    owner varchar(255),
    lifecycle varchar(50),
    confidence double precision,
    embedding vector(768),
    metadata jsonb,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_apis (
    api_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id uuid REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    artifact_id text,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    api_name varchar(500) NOT NULL,
    api_type varchar(100),
    http_method varchar(10),
    endpoint_path varchar(2000),
    description text,
    request_schema jsonb,
    response_schema jsonb,
    authentication varchar(100),
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_business_rules (
    rule_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    component_id uuid REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id uuid REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL,
    rule_name varchar(500) NOT NULL,
    rule_type varchar(100),
    condition_text text,
    outcome_text text,
    priority varchar(50),
    confidence double precision,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_workflows (
    workflow_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    domain_id uuid REFERENCES knowledge.domains(domain_id) ON DELETE SET NULL,
    workflow_name varchar(500) NOT NULL,
    trigger_text text,
    outcome_text text,
    owner varchar(255),
    confidence double precision,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_workflow_steps (
    step_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id uuid NOT NULL REFERENCES knowledge.knowledge_workflows(workflow_id) ON DELETE CASCADE,
    sequence_number integer,
    step_name varchar(500),
    actor varchar(255),
    action_text text,
    input_data text,
    output_data text,
    next_step varchar(500),
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_integrations (
    integration_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    component_id uuid REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    integration_name varchar(500) NOT NULL,
    integration_type varchar(100),
    source_system varchar(255),
    target_system varchar(255),
    protocol varchar(100),
    description text,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.knowledge_resources (
    resource_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    artifact_id text,
    component_id uuid REFERENCES knowledge.knowledge_components(component_id) ON DELETE SET NULL,
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    resource_name varchar(500) NOT NULL,
    resource_type varchar(100),
    provider varchar(100),
    hosting_model varchar(50),
    environment varchar(50),
    region varchar(100),
    criticality varchar(50),
    lifecycle varchar(50),
    configs jsonb,
    confidence double precision,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_roles (
    business_role_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    role_name varchar(255) NOT NULL,
    role_type varchar(100),
    description text,
    responsibilities text,
    department varchar(255),
    business_capability varchar(255),
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_users (
    business_user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    user_type varchar(100),
    persona varchar(255),
    needs jsonb,
    pain_points text,
    workflows_involved jsonb,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_capabilities (
    capability_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    capability_name varchar(500) NOT NULL,
    capability_type varchar(100),
    description text,
    business_value text,
    kpis jsonb,
    supported_by_workflows jsonb,
    supported_by_components jsonb,
    business_owner varchar(255),
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE knowledge.business_rules
    ADD CONSTRAINT fk_business_rules_capability FOREIGN KEY (affects_business_capability)
        REFERENCES knowledge.business_capabilities(capability_id) ON DELETE SET NULL;

ALTER TABLE knowledge.business_flows
    ADD CONSTRAINT fk_business_flows_capability FOREIGN KEY (business_capability_id)
        REFERENCES knowledge.business_capabilities(capability_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_business_flows_initiated_role FOREIGN KEY (initiated_by_role)
        REFERENCES knowledge.business_roles(business_role_id) ON DELETE SET NULL;

CREATE TABLE knowledge.business_role_workflows (
    assignment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    business_role_id uuid NOT NULL REFERENCES knowledge.business_roles(business_role_id) ON DELETE CASCADE,
    workflow_id varchar(36) NOT NULL REFERENCES knowledge.business_flows(flow_id) ON DELETE CASCADE,
    involvement_type varchar(100),
    decision_authority varchar(100),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_decisions (
    decision_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    decision_name varchar(500) NOT NULL,
    decision_question text,
    decision_criteria text,
    decision_maker_role uuid REFERENCES knowledge.business_roles(business_role_id) ON DELETE SET NULL,
    decision_options jsonb,
    decision_context text,
    workflow_id varchar(36) REFERENCES knowledge.business_flows(flow_id) ON DELETE SET NULL,
    workflow_step_id varchar(36) REFERENCES knowledge.business_flow_steps(step_id) ON DELETE SET NULL,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_terms (
    term_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    term_name varchar(255) NOT NULL,
    business_definition text,
    technical_definition text,
    synonyms jsonb,
    category varchar(100),
    business_owner varchar(255),
    used_in_workflows jsonb,
    used_in_rules jsonb,
    mapped_to_data_model uuid REFERENCES knowledge.knowledge_data_models(data_model_id) ON DELETE SET NULL,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_policies (
    policy_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    policy_name varchar(500) NOT NULL,
    policy_type varchar(100),
    description text,
    business_rationale text,
    regulatory_requirement varchar(255),
    enforcement_level varchar(50),
    implemented_by_rules jsonb,
    implemented_by_workflows jsonb,
    business_owner varchar(255),
    effective_date date,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge.business_metrics (
    metric_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id text NOT NULL REFERENCES knowledge.projects(project_id) ON DELETE CASCADE,
    metric_name varchar(500) NOT NULL,
    metric_type varchar(100),
    description text,
    calculation_method text,
    target_value varchar(100),
    measured_by_workflow varchar(36) REFERENCES knowledge.business_flows(flow_id) ON DELETE SET NULL,
    measured_by_component varchar(36) REFERENCES knowledge.solution_components(component_id) ON DELETE SET NULL,
    business_capability_id uuid REFERENCES knowledge.business_capabilities(capability_id) ON DELETE SET NULL,
    source_artifact_id varchar(36) REFERENCES knowledge.artifacts(artifact_id) ON DELETE SET NULL,
    embedding vector(768),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_ingestion_documents_source_hash
    ON knowledge.ingestion_documents(project_id, source_bucket, source_object, content_hash);
CREATE UNIQUE INDEX ux_artifacts_source_hash
    ON knowledge.artifacts(project_id, source_bucket, source_object, content_hash);
CREATE UNIQUE INDEX ux_semantic_chunks_source_hash_index
    ON knowledge.semantic_chunks(project_id, source_bucket, source_object, document_content_hash, chunk_index);

CREATE INDEX ix_artifacts_project_source ON knowledge.artifacts(project_id, source_object);
CREATE INDEX ix_artifacts_current ON knowledge.artifacts(project_id, is_current);
CREATE INDEX ix_semantic_chunks_project_source ON knowledge.semantic_chunks(project_id, source_object, chunk_index);
CREATE INDEX ix_semantic_chunks_document ON knowledge.semantic_chunks(document_id);
CREATE INDEX ix_semantic_chunks_artifact ON knowledge.semantic_chunks(artifact_id);
CREATE INDEX ix_solution_components_project_artifact ON knowledge.solution_components(project_id, source_artifact_id);
CREATE INDEX ix_solution_components_project_name ON knowledge.solution_components(project_id, component_name);
CREATE INDEX ix_business_rules_project_artifact ON knowledge.business_rules(project_id, source_artifact_id);
CREATE INDEX ix_business_flows_project_artifact ON knowledge.business_flows(project_id, source_artifact_id);
CREATE INDEX ix_business_flow_steps_flow ON knowledge.business_flow_steps(flow_id);
CREATE INDEX ix_deployment_resources_project_artifact ON knowledge.deployment_resources(project_id, source_artifact_id);
CREATE INDEX ix_deployment_resources_project_env ON knowledge.deployment_resources(project_id, environment);
CREATE INDEX ix_deployment_resource_configs_resource ON knowledge.deployment_resource_configs(resource_id);
CREATE INDEX ix_knowledge_relationships_project_artifact ON knowledge.knowledge_relationships(project_id, source_artifact_id);
CREATE INDEX ix_knowledge_relationships_source ON knowledge.knowledge_relationships(project_id, source_name, source_type);
CREATE INDEX ix_knowledge_relationships_target ON knowledge.knowledge_relationships(project_id, target_name, target_type);
CREATE INDEX ix_knowledge_notes_project_artifact ON knowledge.knowledge_notes(project_id, source_artifact_id);
CREATE INDEX ix_usage_profiles_project_env ON knowledge.usage_profiles(project_id, environment);
CREATE INDEX ix_resource_cost_estimates_project_env ON knowledge.resource_cost_estimates(project_id, environment);
CREATE INDEX ix_resource_cost_estimates_resource ON knowledge.resource_cost_estimates(resource_id);
CREATE INDEX ix_knowledge_data_models_project ON knowledge.knowledge_data_models(project_id);
CREATE INDEX ix_knowledge_data_fields_model ON knowledge.knowledge_data_fields(data_model_id);
CREATE INDEX ix_document_knowledge_artifact ON knowledge.document_knowledge(artifact_id);
CREATE INDEX ix_knowledge_components_project ON knowledge.knowledge_components(project_id);
CREATE INDEX ix_knowledge_components_name ON knowledge.knowledge_components(project_id, component_name);
CREATE INDEX ix_knowledge_apis_project ON knowledge.knowledge_apis(project_id);
CREATE INDEX ix_knowledge_business_rules_project ON knowledge.knowledge_business_rules(project_id);
CREATE INDEX ix_knowledge_workflows_project ON knowledge.knowledge_workflows(project_id);
CREATE INDEX ix_knowledge_resources_project ON knowledge.knowledge_resources(project_id);
CREATE INDEX ix_business_roles_project ON knowledge.business_roles(project_id);
CREATE INDEX ix_business_users_project ON knowledge.business_users(project_id);
CREATE INDEX ix_business_capabilities_project ON knowledge.business_capabilities(project_id);
CREATE INDEX ix_business_decisions_project ON knowledge.business_decisions(project_id);
CREATE INDEX ix_business_terms_project ON knowledge.business_terms(project_id);
CREATE INDEX ix_business_policies_project ON knowledge.business_policies(project_id);
CREATE INDEX ix_business_metrics_project ON knowledge.business_metrics(project_id);

CREATE INDEX ix_semantic_chunks_embedding_hnsw ON knowledge.semantic_chunks USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_solution_components_embedding_hnsw ON knowledge.solution_components USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_rules_embedding_hnsw ON knowledge.business_rules USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_flows_embedding_hnsw ON knowledge.business_flows USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_flow_steps_embedding_hnsw ON knowledge.business_flow_steps USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_deployment_resources_embedding_hnsw ON knowledge.deployment_resources USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_relationships_embedding_hnsw ON knowledge.knowledge_relationships USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_notes_embedding_hnsw ON knowledge.knowledge_notes USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_data_models_embedding_hnsw ON knowledge.knowledge_data_models USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_data_fields_embedding_hnsw ON knowledge.knowledge_data_fields USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_document_knowledge_embedding_hnsw ON knowledge.document_knowledge USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_components_embedding_hnsw ON knowledge.knowledge_components USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_apis_embedding_hnsw ON knowledge.knowledge_apis USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_business_rules_embedding_hnsw ON knowledge.knowledge_business_rules USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_workflows_embedding_hnsw ON knowledge.knowledge_workflows USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_workflow_steps_embedding_hnsw ON knowledge.knowledge_workflow_steps USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_integrations_embedding_hnsw ON knowledge.knowledge_integrations USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_knowledge_resources_embedding_hnsw ON knowledge.knowledge_resources USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_roles_embedding_hnsw ON knowledge.business_roles USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_users_embedding_hnsw ON knowledge.business_users USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_capabilities_embedding_hnsw ON knowledge.business_capabilities USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_decisions_embedding_hnsw ON knowledge.business_decisions USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_terms_embedding_hnsw ON knowledge.business_terms USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_policies_embedding_hnsw ON knowledge.business_policies USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX ix_business_metrics_embedding_hnsw ON knowledge.business_metrics USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
