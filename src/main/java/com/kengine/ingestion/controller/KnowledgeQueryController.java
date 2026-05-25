package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.service.KnowledgeQueryService;
import com.kengine.ingestion.service.VectorSearchService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/knowledge")
@RequiredArgsConstructor
public class KnowledgeQueryController {

  private final KnowledgeQueryService knowledgeQueryService;
  private final VectorSearchService vectorSearchService;

  @GetMapping("/sources")
  public List<KnowledgeSource> sources(@PathVariable UUID subjectId) {
    return knowledgeQueryService.sources(subjectId);
  }

  @GetMapping("/chunks")
  public List<SourceChunk> chunks(
      @PathVariable UUID subjectId,
      @RequestParam(required = false) String sourceObject,
      @RequestParam(required = false, name = "q") String query,
      @RequestParam(required = false) Integer limit) {
    return knowledgeQueryService.chunks(subjectId, sourceObject, query, limit);
  }

  @PostMapping("/chunks/search")
  public VectorSearchResponse<SemanticChunkSearchResult> searchChunks(
      @PathVariable UUID subjectId, @Valid @RequestBody VectorSearchRequest request) {
    return vectorSearchService.searchSemanticChunks(subjectId, request);
  }

  @PostMapping("/components/search")
  public VectorSearchResponse<ComponentSearchResult> searchComponents(
      @PathVariable UUID subjectId, @Valid @RequestBody VectorSearchRequest request) {
    return vectorSearchService.searchComponents(subjectId, request);
  }

  @PostMapping("/business-rules/search")
  public VectorSearchResponse<BusinessRuleSearchResult> searchBusinessRules(
      @PathVariable UUID subjectId, @Valid @RequestBody VectorSearchRequest request) {
    return vectorSearchService.searchBusinessRules(subjectId, request);
  }

  @PostMapping("/workflows/search")
  public VectorSearchResponse<WorkflowSearchResult> searchWorkflows(
      @PathVariable UUID subjectId, @Valid @RequestBody VectorSearchRequest request) {
    return vectorSearchService.searchWorkflows(subjectId, request);
  }
}
