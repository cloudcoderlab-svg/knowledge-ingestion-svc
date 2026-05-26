package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.BusinessRuleDto;
import com.kengine.ingestion.repository.KnowledgeBusinessRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying business rules.
 *
 * <p>Provides endpoints for retrieving business rules extracted from documents with optional
 * filtering by type and domain.
 */
@RestController
@RequestMapping("/api/v1/business-rules")
@RequiredArgsConstructor
@Tag(
    name = "Business Rules",
    description =
        "APIs for querying business rules extracted from documents. "
            + "Business rules represent policies, constraints, and decision logic.")
public class BusinessRuleController {

  private final KnowledgeBusinessRuleRepository businessRuleRepository;

  /**
   * Retrieves business rules with optional filtering.
   *
   * @param limit Maximum number of results to return (default: 10)
   * @param ruleType Optional rule type filter
   * @param domain Optional domain filter
   * @return List of business rules
   */
  @Operation(
      summary = "Get business rules",
      description =
          "Retrieves business rules extracted from documents with optional filtering by rule type and domain. "
              + "Results are paginated with a configurable limit.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Business rules retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BusinessRuleDto.class)))
      })
  @GetMapping
  public List<BusinessRuleDto> getBusinessRules(
      @Parameter(
              description = "Maximum number of results to return",
              schema = @Schema(defaultValue = "10"))
          @RequestParam(required = false, defaultValue = "10")
          Integer limit,
      @Parameter(
              description = "Filter by rule type (e.g., validation, calculation, decision)",
              required = false)
          @RequestParam(required = false)
          String ruleType,
      @Parameter(description = "Filter by domain", required = false) @RequestParam(required = false)
          String domain) {

    List<Object[]> results;
    if (ruleType != null) {
      results =
          businessRuleRepository.findByRuleTypeWithoutEmbedding(ruleType, PageRequest.of(0, limit));
    } else {
      results = businessRuleRepository.findAllWithoutEmbedding(PageRequest.of(0, limit));
    }

    return results.stream().map(this::toDto).toList();
  }

  private BusinessRuleDto toDto(Object[] row) {
    return BusinessRuleDto.builder()
        .ruleId((UUID) row[0])
        .artifactId(row[1] != null ? row[1].toString() : null)
        .ruleName((String) row[5])
        .ruleType((String) row[6])
        .conditionText((String) row[7])
        .outcomeText((String) row[8])
        .priority((String) row[9])
        .confidence((Double) row[10])
        .build();
  }
}
