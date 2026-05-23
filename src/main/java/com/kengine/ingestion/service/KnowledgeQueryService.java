package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.KnowledgeSource;
import com.kengine.ingestion.dto.SourceChunk;
import com.kengine.ingestion.entity.ArtifactEntity;
import com.kengine.ingestion.entity.SemanticChunkEntity;
import com.kengine.ingestion.repository.ArtifactRepository;
import com.kengine.ingestion.repository.SemanticChunkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {

  private static final int DEFAULT_CHUNK_LIMIT = 100;
  private static final int MAX_CHUNK_LIMIT = 1000;

  private final ArtifactRepository artifactRepository;
  private final SemanticChunkRepository semanticChunkRepository;

  public List<KnowledgeSource> sources(String projectId) {
    List<ArtifactEntity> artifacts =
        artifactRepository.findAll(
            (root, query, cb) -> cb.equal(root.get("projectId"), projectId),
            Sort.by(Sort.Direction.DESC, "sourceObject", "createdAt"));

    return artifacts.stream()
        .map(
            artifact ->
                new KnowledgeSource(
                    artifact.getArtifactId(),
                    artifact.getProjectId(),
                    artifact.getDomain(),
                    artifact.getSubdomain(),
                    artifact.getSourceBucket(),
                    artifact.getSourceObject(),
                    artifact.getSourceGeneration(),
                    artifact.getSourceChecksum(),
                    artifact.getContentHash(),
                    artifact.getArtifactType(),
                    artifact.getFileType(),
                    artifact.getTitle(),
                    artifact.getVersion(),
                    artifact.getIsCurrent()))
        .toList();
  }

  public List<SourceChunk> chunks(
      String projectId, String sourceObject, String query, Integer requestedLimit) {
    int limit = chunkLimit(requestedLimit);

    Specification<SemanticChunkEntity> spec =
        (root, criteriaQuery, cb) -> cb.equal(root.get("projectId"), projectId);

    if (!isBlank(sourceObject)) {
      spec =
          spec.and((root, criteriaQuery, cb) -> cb.equal(root.get("sourceObject"), sourceObject));
    }

    if (!isBlank(query)) {
      String lowerQuery = "%" + query.toLowerCase() + "%";
      spec =
          spec.and((root, criteriaQuery, cb) -> cb.like(cb.lower(root.get("content")), lowerQuery));
    }

    List<SemanticChunkEntity> chunks =
        semanticChunkRepository
            .findAll(
                spec, PageRequest.of(0, limit, Sort.by("sourceObject").and(Sort.by("chunkIndex"))))
            .getContent();

    return chunks.stream()
        .map(
            chunk ->
                new SourceChunk(
                    chunk.getChunkId(),
                    chunk.getDocumentId(),
                    chunk.getArtifactId(),
                    chunk.getProjectId(),
                    chunk.getSourceBucket(),
                    chunk.getSourceObject(),
                    chunk.getSourceGeneration(),
                    chunk.getSourceChecksum(),
                    chunk.getDocumentContentHash(),
                    chunk.getChunkIndex(),
                    chunk.getTotalChunks(),
                    chunk.getCharStart(),
                    chunk.getCharEnd(),
                    chunk.getChunkContentHash(),
                    chunk.getDomain(),
                    chunk.getSubdomain(),
                    chunk.getContent()))
        .toList();
  }

  private int chunkLimit(Integer requestedLimit) {
    if (requestedLimit == null || requestedLimit <= 0) {
      return DEFAULT_CHUNK_LIMIT;
    }
    return Math.min(requestedLimit, MAX_CHUNK_LIMIT);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
