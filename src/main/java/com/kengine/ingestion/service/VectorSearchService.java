package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.entity.DomainEntity;
import com.kengine.ingestion.repository.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

  private final EmbeddingService embeddingService;
  private final SemanticChunkRepository semanticChunkRepository;
  private final KnowledgeComponentRepository knowledgeComponentRepository;
  private final KnowledgeBusinessRuleRepository knowledgeBusinessRuleRepository;
  private final KnowledgeWorkflowRepository knowledgeWorkflowRepository;
  private final DomainRepository domainRepository;

  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 100;

  public VectorSearchResponse<SemanticChunkSearchResult> searchSemanticChunks(
      UUID subjectId, VectorSearchRequest request) {
    validateRequest(request);

    String embeddingString = getEmbeddingString(request);
    int limit = validateLimit(request.getLimit());

    List<Object[]> results =
        semanticChunkRepository.findSimilarByEmbedding(
            subjectId, embeddingString, request.getSourceObject(), request.getDomain(), limit);

    List<SearchResult<SemanticChunkSearchResult>> searchResults =
        results.stream()
            .map(this::mapToSemanticChunkSearchResult)
            .filter(result -> filterByThreshold(result, request.getThreshold()))
            .collect(Collectors.toList());

    return buildResponse(
        searchResults, request.hasQueryText() ? "text" : "embedding", "semantic_chunk");
  }

  public VectorSearchResponse<ComponentSearchResult> searchComponents(
      UUID subjectId, VectorSearchRequest request) {
    validateRequest(request);

    String embeddingString = getEmbeddingString(request);
    int limit = validateLimit(request.getLimit());
    UUID domainId = resolveDomainId(subjectId, request.getDomain());

    List<Object[]> results =
        knowledgeComponentRepository.findSimilarByEmbedding(
            subjectId, embeddingString, domainId, limit);

    List<SearchResult<ComponentSearchResult>> searchResults =
        results.stream()
            .map(this::mapToComponentSearchResult)
            .filter(result -> filterByThreshold(result, request.getThreshold()))
            .collect(Collectors.toList());

    return buildResponse(searchResults, request.hasQueryText() ? "text" : "embedding", "component");
  }

  public VectorSearchResponse<BusinessRuleSearchResult> searchBusinessRules(
      UUID subjectId, VectorSearchRequest request) {
    validateRequest(request);

    String embeddingString = getEmbeddingString(request);
    int limit = validateLimit(request.getLimit());
    UUID domainId = resolveDomainId(subjectId, request.getDomain());

    List<Object[]> results =
        knowledgeBusinessRuleRepository.findSimilarByEmbedding(
            subjectId, embeddingString, domainId, limit);

    List<SearchResult<BusinessRuleSearchResult>> searchResults =
        results.stream()
            .map(this::mapToBusinessRuleSearchResult)
            .filter(result -> filterByThreshold(result, request.getThreshold()))
            .collect(Collectors.toList());

    return buildResponse(
        searchResults, request.hasQueryText() ? "text" : "embedding", "business_rule");
  }

  public VectorSearchResponse<WorkflowSearchResult> searchWorkflows(
      UUID subjectId, VectorSearchRequest request) {
    validateRequest(request);

    String embeddingString = getEmbeddingString(request);
    int limit = validateLimit(request.getLimit());
    UUID domainId = resolveDomainId(subjectId, request.getDomain());

    List<Object[]> results =
        knowledgeWorkflowRepository.findSimilarByEmbedding(
            subjectId, embeddingString, domainId, limit);

    List<SearchResult<WorkflowSearchResult>> searchResults =
        results.stream()
            .map(this::mapToWorkflowSearchResult)
            .filter(result -> filterByThreshold(result, request.getThreshold()))
            .collect(Collectors.toList());

    return buildResponse(searchResults, request.hasQueryText() ? "text" : "embedding", "workflow");
  }

  private void validateRequest(VectorSearchRequest request) {
    if (!request.isValid()) {
      throw new IllegalArgumentException("Exactly one of queryText or embedding must be provided");
    }
  }

  private UUID resolveDomainId(UUID subjectId, String domainName) {
    if (domainName == null || domainName.isBlank()) {
      return null;
    }
    return domainRepository
        .findBySubjectIdAndDomain(subjectId, domainName)
        .map(DomainEntity::getDomainId)
        .orElse(null);
  }

  private String getEmbeddingString(VectorSearchRequest request) {
    List<Double> embedding;

    if (request.hasQueryText()) {
      log.debug("Generating embedding for query text: {}", request.getQueryText());
      embedding = embeddingService.embedding(request.getQueryText());
    } else {
      embedding = request.getEmbedding();
    }

    return embeddingToString(embedding);
  }

  private String embeddingToString(List<Double> embedding) {
    if (embedding == null || embedding.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < embedding.size(); i++) {
      if (i > 0) sb.append(",");
      sb.append(embedding.get(i));
    }
    sb.append("]");
    return sb.toString();
  }

  private int validateLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  private boolean filterByThreshold(SearchResult<?> result, Double threshold) {
    if (threshold == null) {
      return true;
    }
    return result.getSimilarityScore() >= threshold;
  }

  private SearchResult<SemanticChunkSearchResult> mapToSemanticChunkSearchResult(Object[] row) {
    int i = 0;
    SemanticChunkSearchResult result =
        SemanticChunkSearchResult.builder()
            .chunkId((UUID) row[i++])
            .artifactId((UUID) row[i++])
            .subjectId((UUID) row[i++])
            .sourceObject((String) row[i++])
            .domain((String) row[i++])
            .chunkIndex(row[i] != null ? ((Number) row[i]).intValue() : null)
            .content((String) row[++i])
            .contentHash((String) row[++i])
            .similarityScore(((Number) row[++i]).doubleValue())
            .build();

    return SearchResult.<SemanticChunkSearchResult>builder()
        .entity(result)
        .similarityScore(result.getSimilarityScore())
        .entityType("semantic_chunk")
        .build();
  }

  private SearchResult<ComponentSearchResult> mapToComponentSearchResult(Object[] row) {
    int i = 0;
    ComponentSearchResult result =
        ComponentSearchResult.builder()
            .componentId((UUID) row[i++])
            .artifactId((UUID) row[i++])
            .subjectId((UUID) row[i++])
            .domainId((UUID) row[i++])
            .componentName((String) row[i++])
            .componentType((String) row[i++])
            .category((String) row[i++])
            .description((String) row[i++])
            .responsibility((String) row[i++])
            .technology((String) row[i++])
            .capability((String) row[i++])
            .owner((String) row[i++])
            .lifecycle((String) row[i++])
            .confidence(row[i] != null ? ((Number) row[i]).doubleValue() : null)
            .similarityScore(((Number) row[++i]).doubleValue())
            .build();

    return SearchResult.<ComponentSearchResult>builder()
        .entity(result)
        .similarityScore(result.getSimilarityScore())
        .entityType("component")
        .build();
  }

  private SearchResult<BusinessRuleSearchResult> mapToBusinessRuleSearchResult(Object[] row) {
    int i = 0;
    BusinessRuleSearchResult result =
        BusinessRuleSearchResult.builder()
            .ruleId((UUID) row[i++])
            .artifactId((UUID) row[i++])
            .componentId((UUID) row[i++])
            .subjectId((UUID) row[i++])
            .domainId((UUID) row[i++])
            .ruleName((String) row[i++])
            .ruleType((String) row[i++])
            .conditionText((String) row[i++])
            .outcomeText((String) row[i++])
            .priority((String) row[i++])
            .confidence(row[i] != null ? ((Number) row[i]).doubleValue() : null)
            .similarityScore(((Number) row[++i]).doubleValue())
            .build();

    return SearchResult.<BusinessRuleSearchResult>builder()
        .entity(result)
        .similarityScore(result.getSimilarityScore())
        .entityType("business_rule")
        .build();
  }

  private SearchResult<WorkflowSearchResult> mapToWorkflowSearchResult(Object[] row) {
    int i = 0;
    WorkflowSearchResult result =
        WorkflowSearchResult.builder()
            .workflowId((UUID) row[i++])
            .artifactId((UUID) row[i++])
            .subjectId((UUID) row[i++])
            .domainId((UUID) row[i++])
            .workflowName((String) row[i++])
            .triggerText((String) row[i++])
            .outcomeText((String) row[i++])
            .owner((String) row[i++])
            .confidence(row[i] != null ? ((Number) row[i]).doubleValue() : null)
            .similarityScore(((Number) row[++i]).doubleValue())
            .build();

    return SearchResult.<WorkflowSearchResult>builder()
        .entity(result)
        .similarityScore(result.getSimilarityScore())
        .entityType("workflow")
        .build();
  }

  private <T> VectorSearchResponse<T> buildResponse(
      List<SearchResult<T>> results, String searchType, String entityType) {
    return VectorSearchResponse.<T>builder()
        .results(results)
        .totalResults(results.size())
        .searchType(searchType)
        .entityType(entityType)
        .build();
  }
}
