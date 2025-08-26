package com.example.classifierapi.dto;

import java.util.List;

public record ClassifyResponse(String label, List<DecisionStep> path) {}
