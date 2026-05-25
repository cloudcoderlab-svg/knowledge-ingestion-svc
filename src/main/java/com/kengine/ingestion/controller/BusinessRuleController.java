package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.BusinessRuleDto;
import com.kengine.ingestion.repository.KnowledgeBusinessRuleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-rules")
@RequiredArgsConstructor
public class BusinessRuleController {

  private final KnowledgeBusinessRuleRepository businessRuleRepository;

  @GetMapping
  public List<BusinessRuleDto> getBusinessRules(
      @RequestParam(required = false, defaultValue = "10") Integer limit,
      @RequestParam(required = false) String ruleType,
      @RequestParam(required = false) String domain) {

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
