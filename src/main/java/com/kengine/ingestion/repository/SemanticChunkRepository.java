package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SemanticChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing semantic chunk entities.
 *
 * <p>Provides data access for document text chunks that have been semantically segmented and
 * embedded. Supports vector similarity search using PostgreSQL pgvector extension.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on semantic chunks
 *   <li>Vector similarity search using cosine distance
 *   <li>Filtering by subject, source object, and domain
 *   <li>Bulk deletion operations
 * </ul>
 */
@Repository
public interface SemanticChunkRepository
    extends JpaRepository<SemanticChunkEntity, String>,
        JpaSpecificationExecutor<SemanticChunkEntity> {

  /**
   * Finds all chunks for a specific subject and source object, ordered by chunk index.
   *
   * @param subjectId Subject UUID
   * @param sourceObject Source file name or identifier
   * @return List of chunks in sequential order
   */
  List<SemanticChunkEntity> findBySubjectIdAndSourceObjectOrderByChunkIndex(
      UUID subjectId, String sourceObject);

  /**
   * Deletes all chunks belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of chunks deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all chunks associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of chunks deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total chunks for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of chunks
   */
  long countBySubjectId(UUID subjectId);

  /**
   * Finds semantically similar chunks using vector similarity search.
   *
   * <p>Uses PostgreSQL pgvector's cosine distance operator (&lt;=&gt;) to find chunks with
   * embeddings most similar to the query embedding. Results are ordered by similarity (highest
   * first) and limited to the specified count.
   *
   * <p>Optional filters:
   *
   * <ul>
   *   <li>sourceObject - restrict to specific source file
   *   <li>domain - restrict to specific domain
   * </ul>
   *
   * @param subjectId Subject UUID (required)
   * @param embedding Query embedding in pgvector string format: "[0.1,0.2,0.3,...]"
   * @param sourceObject Optional source file filter (null = all sources)
   * @param domain Optional domain filter (null = all domains)
   * @param limit Maximum number of results to return
   * @return List of Object arrays containing: [chunk_id, artifact_id, subject_id, source_object,
   *     domain, chunk_index, content, chunk_content_hash, similarity_score]
   */
  @Query(
      value =
          "SELECT chunk_id, artifact_id, subject_id, source_object, domain, "
              + "chunk_index, content, chunk_content_hash, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge.semantic_chunks "
              + "WHERE subject_id = :subjectId "
              + "AND (:sourceObject IS NULL OR source_object = :sourceObject) "
              + "AND (:domain IS NULL OR domain = :domain) "
              + "AND embedding IS NOT NULL "
              + "ORDER BY embedding <=> CAST(:embedding AS vector) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findSimilarByEmbedding(
      @Param("subjectId") UUID subjectId,
      @Param("embedding") String embedding,
      @Param("sourceObject") String sourceObject,
      @Param("domain") String domain,
      @Param("limit") int limit);
}
