package com.techmanage.dto;

public record SmartDraftResponse(
    String suggestedDoneWork,
    String lastWeekPlanWork,
    boolean hasLastWeek
) {}
