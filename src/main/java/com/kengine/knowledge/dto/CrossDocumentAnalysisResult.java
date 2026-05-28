package com.kengine.knowledge.dto;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossDocumentAnalysisResult {
  private List<CrossDocumentRelationship> crossDocumentRelationships;
  private Object crossDocumentInsights;
}
