package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.entity.DomainEntity;
import com.kengine.ingestion.helper.EmbeddingUtils;
import com.kengine.ingestion.repository.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for performing vector similarity searches across different knowledge entities.
 *
 * <p>Supports semantic search on chunks, components, business rules, and workflows using embeddings
 * and cosine similarity.
 */
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

  /**
   * Searches semantic chunks by vector similarity.
   *
   * @param subjectId Subject UUID
   * @param request Search request with query or embedding
   * @return Search response with ranked results
   */
  public VectorSearchResponse<SemanticChunkSearchResult> searchSemanticChunks(
      UUID subjectId, VectorSearchRequest request) {
    return executeSearch(
        request,
        (embeddingString, limit) ->
            semanticChunkRepository.findSimilarByEmbedding(
                subjectId, embeddingString, request.getSourceObject(), request.getDomain(), limit),
        this::mapToSemanticChunkSearchResult,
        "semantic_chunk");
  }

  /**
   * Searches knowledge components by vector similarity.
   *
   * @param subjectId Subject UUID
   * @param request Search request with query or embedding
   * @return Search response with ranked results
   */
  public VectorSearchResponse<ComponentSearchResult> searchComponents(
      UUID subjectId, VectorSearchRequest request) {
    UUID domainId = resolveDomainId(subjectId, request.getDomain());
    return executeSearch(
        request,
        (embeddingString, limit) ->
            knowledgeComponentRepository.findSimilarByEmbedding(
                subjectId, embeddingString, domainId, limit),
        this::mapToComponentSearchResult,
        "component");
  }

  /**
   * Searches business rules by vector similarity.
   *
   * @param subjectId Subject UUID
   * @param request Search request with query or embedding
   * @return Search response with ranked results
   */
  public VectorSearchResponse<BusinessRuleSearchResult> searchBusinessRules(
      UUID subjectId, VectorSearchRequest request) {
    UUID domainId = resolveDomainId(subjectId, request.getDomain());
    return executeSearch(
        request,
        (embeddingString, limit) ->
            knowledgeBusinessRuleRepository.findSimilarByEmbedding(
                subjectId, embeddingString, domainId, limit),
        this::mapToBusinessRuleSearchResult,
        "business_rule");
  }

  /**
   * Searches workflows by vector similarity.
   *
   * @param subjectId Subject UUID
   * @param request Search request with query or embedding
   * @return Search response with ranked results
   */
  public VectorSearchResponse<WorkflowSearchResult> searchWorkflows(
      UUID subjectId, VectorSearchRequest request) {
    UUID domainId = resolveDomainId(subjectId, request.getDomain());
    return executeSearch(
        request,
        (embeddingString, limit) ->
            knowledgeWorkflowRepository.findSimilarByEmbedding(
                subjectId, embeddingString, domainId, limit),
        this::mapToWorkflowSearchResult,
        "workflow");
  }

  /**
   * Generic search execution template that eliminates code duplication.
   *
   * @param request Search request
   * @param repositoryQuery Function to execute repository query
   * @param resultMapper Function to map Object[] to SearchResult
   * @param entityType Type of entity being searched
   * @return Search response with results
   */
  private <T> VectorSearchResponse<T> executeSearch(
      VectorSearchRequest request,
      RepositoryQuery repositoryQuery,
      Function<Object[], SearchResult<T>> resultMapper,
      String entityType) {

    validateRequest(request);

    String embeddingString = getEmbeddingString(request);
    int limit = validateLimit(request.getLimit());

    List<Object[]> results = repositoryQuery.execute(embeddingString, limit);

    List<SearchResult<T>> searchResults =
        results.stream()
            .map(resultMapper)
            .filter(result -> filterByThreshold(result, request.getThreshold()))
            .collect(Collectors.toList());

    return buildResponse(searchResults, request.hasQueryText() ? "text" : "embedding", entityType);
  }

  /** Functional interface for repository query execution. */
  @FunctionalInterface
  private interface RepositoryQuery {
    List<Object[]> execute(String embeddingString, int limit);
  }

  /** Validates that request contains exactly one of queryText or embedding. */
  private void validateRequest(VectorSearchRequest request) {
    if (!request.isValid()) {
      throw new IllegalArgumentException("Exactly one of queryText or embedding must be provided");
    }
  }

  /**
   * Resolves domain name to domain ID.
   *
   * @param subjectId Subject UUID
   * @param domainName Domain name
   * @return Domain UUID or null if not found
   */
  private UUID resolveDomainId(UUID subjectId, String domainName) {
    if (domainName == null || domainName.isBlank()) {
      return null;
    }
    return domainRepository
        .findBySubjectIdAndDomain(subjectId, domainName)
        .map(DomainEntity::getDomainId)
        .orElse(null);
  }

  /**
   * Gets embedding string from request, generating from text if needed.
   *
   * @param request Search request
   * @return Embedding as string
   */
  private String getEmbeddingString(VectorSearchRequest request) {
    List<Double> embedding;

    if (request.hasQueryText()) {
      log.debug("Generating embedding for query text: {}", request.getQueryText());
      embedding = embeddingService.embedding(request.getQueryText());
    } else {
      embedding = request.getEmbedding();
    }

    return EmbeddingUtils.embeddingToString(embedding);
  }

  /**
   * Validates and bounds the result limit.
   *
   * @param limit Requested limit
   * @return Validated limit between 1 and MAX_LIMIT
   */
  private int validateLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  /**
   * Filters results by similarity threshold.
   *
   * @param result Search result
   * @param threshold Minimum similarity score
   * @return True if result passes threshold
   */
  private boolean filterByThreshold(SearchResult<?> result, Double threshold) {
    if (threshold == null) {
      return true;
    }
    return result.getSimilarityScore() >= threshold;
  }

  /** Maps database row to semantic chunk search result. */
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

  /** Maps database row to component search result. */
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

  /** Maps database row to business rule search result. */
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

  /** Maps database row to workflow search result. */
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

  /**
   * Builds vector search response.
   *
   * @param results List of search results
   * @param searchType Type of search ("text" or "embedding")
   * @param entityType Type of entity searched
   * @return Complete search response
   */
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
