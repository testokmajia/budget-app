package com.techmanage.dto;

public record ChangeReviewRequest(
    boolean approved,
    String comment
) {}
