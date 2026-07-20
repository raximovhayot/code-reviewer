package uz.uzinfoweb.code_reviewer.core;

public record ReviewComment(
    String filePath,
    int lineNumber,
    String comment,
    String ruleViolated
) {}
