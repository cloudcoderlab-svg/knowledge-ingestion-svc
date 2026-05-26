package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.model.ProcessStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing process tracking entities.
 *
 * <p>Tracks the lifecycle and status of document ingestion processes. Each process represents a
 * batch of documents being parsed, analyzed, and stored.
 *
 * <p>Process statuses: INIT, IN_PROGRESS, SUCCESS, FAILED
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on process records
 *   <li>Query by subject, status, or combination
 *   <li>Results ordered by creation time (newest first)
 * </ul>
 */
@Repository
public interface ProcessTrackingRepository extends JpaRepository<ProcessTrackingEntity, UUID> {

  /**
   * Finds all processes for a subject, ordered by creation time (newest first).
   *
   * @param subjectId Subject UUID
   * @return List of processes ordered by creation time descending
   */
  List<ProcessTrackingEntity> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);

  /**
   * Finds all processes with a specific status, ordered by creation time (newest first).
   *
   * <p>Useful for monitoring active processes or identifying failed ones.
   *
   * @param status Process status (INIT, IN_PROGRESS, SUCCESS, FAILED)
   * @return List of processes with the specified status
   */
  List<ProcessTrackingEntity> findByStatusOrderByCreatedAtDesc(ProcessStatus status);

  /**
   * Finds processes for a specific subject and status, ordered by creation time (newest first).
   *
   * <p>Combines subject and status filtering for targeted queries.
   *
   * @param subjectId Subject UUID
   * @param status Process status (INIT, IN_PROGRESS, SUCCESS, FAILED)
   * @return List of matching processes
   */
  List<ProcessTrackingEntity> findBySubjectIdAndStatusOrderByCreatedAtDesc(
      UUID subjectId, ProcessStatus status);
}
