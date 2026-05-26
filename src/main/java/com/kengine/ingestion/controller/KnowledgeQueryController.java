package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.service.KnowledgeQueryService;
import com.kengine.ingestion.service.SubjectManagementService;
import com.kengine.ingestion.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for querying extracted knowledge entities.
 *
 * <p>Provides endpoints for retrieving knowledge sources, semantic chunks, and performing
 * vector-based similarity searches across components, business rules, and workflows.
 */
@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/knowledge")
@RequiredArgsConstructor
@Tag(
    name = "Knowledge Query",
    description =
        "APIs for querying extracted knowledge entities including semantic chunks, components, "
            + "business rules, and workflows. Supports both keyword-based queries and vector similarity search.")
public class KnowledgeQueryController {

  private final KnowledgeQueryService knowledgeQueryService;
  private final VectorSearchService vectorSearchService;
  private final SubjectManagementService subjectManagementService;

  /**
   * Lists knowledge sources for a subject.
   *
   * @param subjectId Subject UUID
   * @return List of knowledge sources (documents)
   */
  @Operation(
      summary = "List knowledge sources",
      description =
          "Retrieves all knowledge sources (documents/artifacts) that have been ingested for a subject.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sources retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @GetMapping("/sources")
  public List<KnowledgeSource> sources(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return knowledgeQueryService.sources(subjectId);
  }

  /**
   * Queries semantic chunks with optional filters.
   *
   * @param subjectId Subject UUID
   * @param sourceObject Optional source object (file) filter
   * @param query Optional text query filter
   * @param limit Optional result limit
   * @return List of semantic chunks
   */
  @Operation(
      summary = "Query semantic chunks",
      description =
          "Retrieves semantic chunks (text segments) extracted from documents. "
              + "Supports filtering by source file, text query, and result limit.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Chunks retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @GetMapping("/chunks")
  public List<SourceChunk> chunks(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Filter by source file path", required = false)
          @RequestParam(required = false)
          String sourceObject,
      @Parameter(description = "Text query for filtering chunks", required = false)
          @RequestParam(required = false, name = "q")
          String query,
      @Parameter(description = "Maximum number of results to return", required = false)
          @RequestParam(required = false)
          Integer limit) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return knowledgeQueryService.chunks(subjectId, sourceObject, query, limit);
  }

  /**
   * Performs vector similarity search on semantic chunks.
   *
   * @param subjectId Subject UUID
   * @param request Vector search request with query and filters
   * @return Vector search response with ranked results
   */
  @Operation(
      summary = "Vector search on semantic chunks",
      description =
          "Performs semantic similarity search on text chunks using vector embeddings. "
              + "Returns chunks ranked by relevance to the search query.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VectorSearchResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search request",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @PostMapping("/chunks/search")
  public VectorSearchResponse<SemanticChunkSearchResult> searchChunks(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Search query and filters", required = true) @Valid @RequestBody
          VectorSearchRequest request) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return vectorSearchService.searchSemanticChunks(subjectId, request);
  }

  /**
   * Performs vector similarity search on knowledge components.
   *
   * @param subjectId Subject UUID
   * @param request Vector search request with query and filters
   * @return Vector search response with ranked component results
   */
  @Operation(
      summary = "Vector search on knowledge components",
      description =
          "Performs semantic similarity search on extracted knowledge components. "
              + "Components represent software entities, modules, classes, functions, etc.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search request",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @PostMapping("/components/search")
  public VectorSearchResponse<ComponentSearchResult> searchComponents(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Search query and filters", required = true) @Valid @RequestBody
          VectorSearchRequest request) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return vectorSearchService.searchComponents(subjectId, request);
  }

  /**
   * Performs vector similarity search on business rules.
   *
   * @param subjectId Subject UUID
   * @param request Vector search request with query and filters
   * @return Vector search response with ranked business rule results
   */
  @Operation(
      summary = "Vector search on business rules",
      description =
          "Performs semantic similarity search on extracted business rules, policies, and constraints.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search request",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @PostMapping("/business-rules/search")
  public VectorSearchResponse<BusinessRuleSearchResult> searchBusinessRules(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Search query and filters", required = true) @Valid @RequestBody
          VectorSearchRequest request) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return vectorSearchService.searchBusinessRules(subjectId, request);
  }

  /**
   * Performs vector similarity search on workflows.
   *
   * @param subjectId Subject UUID
   * @param request Vector search request with query and filters
   * @return Vector search response with ranked workflow results
   */
  @Operation(
      summary = "Vector search on workflows",
      description =
          "Performs semantic similarity search on extracted workflows and business processes.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search request",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @PostMapping("/workflows/search")
  public VectorSearchResponse<WorkflowSearchResult> searchWorkflows(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Search query and filters", required = true) @Valid @RequestBody
          VectorSearchRequest request) {
    // Validate subject is ACTIVE before querying knowledge
    subjectManagementService.validateSubjectIsActive(subjectId);
    return vectorSearchService.searchWorkflows(subjectId, request);
  }
}
