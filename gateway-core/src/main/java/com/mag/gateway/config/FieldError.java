package com.mag.gateway.config;

/** One validation problem: which field and why. Returned verbatim by the Admin API. */
public record FieldError(String field, String reason) { }
