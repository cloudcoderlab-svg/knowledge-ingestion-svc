-- liquibase formatted sql

-- changeset kengine:003-add-document-knowledge-fields
-- comment: Add fields to document_knowledge table for enhanced entity tracking and cross-document analysis

ALTER TABLE document_knowledge
    ADD COLUMN IF NOT EXISTS overall_architecture TEXT,
    ADD COLUMN IF NOT EXISTS identified_components TEXT[],
    ADD COLUMN IF NOT EXISTS identified_apis TEXT[],
    ADD COLUMN IF NOT EXISTS identified_workflows TEXT[],
    ADD COLUMN IF NOT EXISTS identified_capabilities TEXT[],
    ADD COLUMN IF NOT EXISTS identified_roles TEXT[],
    ADD COLUMN IF NOT EXISTS identified_terms TEXT[],
    ADD COLUMN IF NOT EXISTS identified_policies TEXT[],
    ADD COLUMN IF NOT EXISTS identified_decisions TEXT[];

COMMENT ON COLUMN document_knowledge.overall_architecture IS 'High-level architecture description from document-level analysis';
COMMENT ON COLUMN document_knowledge.identified_components IS 'Array of component/service names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_apis IS 'Array of API names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_workflows IS 'Array of workflow names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_capabilities IS 'Array of business capability names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_roles IS 'Array of business role names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_terms IS 'Array of business term names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_policies IS 'Array of policy names identified in the document';
COMMENT ON COLUMN document_knowledge.identified_decisions IS 'Array of decision point names identified in the document';
