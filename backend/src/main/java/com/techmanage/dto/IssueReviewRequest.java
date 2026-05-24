package com.techmanage.dto;

public record IssueReviewRequest(
    boolean approved,
    String comment
) {}
