-- liquibase formatted sql

-- changeset kengine:project-schema-align-v2
ALTER TABLE knowledge.knowledge_domains
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_subdomains
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_modules
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_components
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_business_rules
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_workflows
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_apis
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_data_models
    ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES knowledge.knowledge_modules(module_id) ON DELETE SET NULL;

ALTER TABLE knowledge.knowledge_relationships
    ADD COLUMN IF NOT EXISTS source_chunk_id UUID REFERENCES knowledge.knowledge_source_chunks(source_chunk_id) ON DELETE SET NULL;
