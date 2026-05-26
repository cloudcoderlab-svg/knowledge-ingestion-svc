package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SubjectEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing subject entities.
 *
 * <p>Subjects represent distinct knowledge domains or projects that contain documents and extracted
 * knowledge entities. Each subject has its own GCS folder and isolated knowledge graph.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on subjects
 *   <li>Lookup by unique subject name
 *   <li>Existence checks for name uniqueness validation
 * </ul>
 */
@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

  /**
   * Finds a subject by its unique name.
   *
   * @param subjectName Machine-friendly subject identifier (e.g., "customer-management-system")
   * @return Optional containing the subject if found
   */
  Optional<SubjectEntity> findBySubjectName(String subjectName);

  /**
   * Checks if a subject with the given name already exists.
   *
   * <p>Used for validation during subject creation to ensure name uniqueness.
   *
   * @param subjectName Subject name to check
   * @return true if a subject with this name exists, false otherwise
   */
  boolean existsBySubjectName(String subjectName);

  /**
   * Finds all versions of a subject by name, ordered by updated timestamp descending.
   *
   * @param subjectName Subject name
   * @return List of subject versions ordered by most recent first
   */
  List<SubjectEntity> findBySubjectNameOrderByUpdatedAtDesc(String subjectName);

  /**
   * Finds the latest version of a subject by name based on updated timestamp.
   *
   * @param subjectName Subject name
   * @return Optional containing the latest version if found
   */
  @Query(
      "SELECT s FROM SubjectEntity s WHERE s.subjectName = :subjectName "
          + "ORDER BY s.updatedAt DESC, s.createdAt DESC LIMIT 1")
  Optional<SubjectEntity> findLatestBySubjectName(@Param("subjectName") String subjectName);

  /**
   * Finds the latest version of a subject for a given subject ID.
   *
   * <p>This retrieves the subject by ID, then finds the latest version with the same subject name.
   *
   * @param subjectId Subject UUID
   * @return Optional containing the latest version if found
   */
  @Query(
      "SELECT s2 FROM SubjectEntity s1 "
          + "JOIN SubjectEntity s2 ON s1.subjectName = s2.subjectName "
          + "WHERE s1.subjectId = :subjectId "
          + "ORDER BY s2.updatedAt DESC, s2.createdAt DESC LIMIT 1")
  Optional<SubjectEntity> findLatestVersionBySubjectId(@Param("subjectId") UUID subjectId);
}
