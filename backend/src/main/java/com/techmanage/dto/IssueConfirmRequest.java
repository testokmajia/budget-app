package com.techmanage.dto;

public record IssueConfirmRequest(
    boolean satisfied,
    String remark
) {}
