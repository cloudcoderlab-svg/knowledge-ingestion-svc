-- Add missing version column to knowledge_ingestion_document_process table
ALTER TABLE knowledge.knowledge_ingestion_document_process
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
