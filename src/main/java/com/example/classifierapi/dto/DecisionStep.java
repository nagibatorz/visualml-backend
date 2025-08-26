package com.example.classifierapi.dto;

public record DecisionStep(String feature, double threshold, double value, String direction) {}
