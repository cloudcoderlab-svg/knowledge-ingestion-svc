-- Add version columns for optimistic locking

-- Add version column to ingestion_documents table
ALTER TABLE ingestion_documents
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add version column to knowledge.knowledge_ingestion_process table
ALTER TABLE knowledge.knowledge_ingestion_process
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
