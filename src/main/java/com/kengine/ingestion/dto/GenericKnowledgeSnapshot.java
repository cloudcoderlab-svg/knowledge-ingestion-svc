package com.kengine.ingestion.dto;

import java.util.List;
import java.util.Map;

public record GenericKnowledgeSnapshot(
    String projectId,
    String sourceObject,
    List<Map<String, Object>> businessRules,
    List<Map<String, Object>> businessFlows,
    List<Map<String, Object>> businessFlowSteps,
    List<Map<String, Object>> solutionComponents,
    List<Map<String, Object>> deploymentResources,
    List<Map<String, Object>> deploymentResourceConfigs,
    List<Map<String, Object>> knowledgeRelationships,
    List<Map<String, Object>> knowledgeNotes,
    List<Map<String, Object>> knowledgeDataModels,
    List<Map<String, Object>> knowledgeDataFields,
    List<Map<String, Object>> usageProfiles,
    List<Map<String, Object>> resourceCostEstimates) {}
