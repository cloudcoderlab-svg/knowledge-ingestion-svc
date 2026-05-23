package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.KnowledgeSource;
import com.kengine.ingestion.dto.SourceChunk;
import com.kengine.ingestion.service.KnowledgeQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/knowledge")
@RequiredArgsConstructor
public class KnowledgeQueryController {

  private final KnowledgeQueryService knowledgeQueryService;

  @GetMapping("/sources")
  public List<KnowledgeSource> sources(@PathVariable String projectId) {
    return knowledgeQueryService.sources(projectId);
  }

  @GetMapping("/chunks")
  public List<SourceChunk> chunks(
      @PathVariable String projectId,
      @RequestParam(required = false) String sourceObject,
      @RequestParam(required = false, name = "q") String query,
      @RequestParam(required = false) Integer limit) {
    return knowledgeQueryService.chunks(projectId, sourceObject, query, limit);
  }
}
