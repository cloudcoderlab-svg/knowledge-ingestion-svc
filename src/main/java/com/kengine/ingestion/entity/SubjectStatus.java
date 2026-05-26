package com.kengine.ingestion.entity;

/**
 * Status of a knowledge subject representing its lifecycle state.
 *
 * <p>Subjects progress through these states:
 *
 * <ul>
 *   <li>DRAFT: Subject created but no ingestion started
 *   <li>INGESTING: Documents are currently being processed
 *   <li>ACTIVE: All documents successfully ingested, subject ready for use
 *   <li>FAILED: Ingestion failed, subject not ready for use
 * </ul>
 */
public enum SubjectStatus {
  /** Subject is created but no documents have been ingested yet. */
  DRAFT,

  /** Documents are currently being ingested and processed. */
  INGESTING,

  /** All documents have been successfully ingested. Subject is ready for knowledge queries. */
  ACTIVE,

  /** Document ingestion failed. Subject is not ready for use. */
  FAILED
}
