package com.mag.gateway.config;

import java.util.List;
import java.util.stream.Collectors;

/** Thrown when a candidate configuration is invalid. The current snapshot is left untouched. */
public class ConfigValidationException extends RuntimeException {

    private final transient List<FieldError> errors;

    public ConfigValidationException(List<FieldError> errors) {
        super(errors.stream().map(e -> e.field() + ": " + e.reason()).collect(Collectors.joining("; ")));
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> errors() {
        return errors;
    }
}
