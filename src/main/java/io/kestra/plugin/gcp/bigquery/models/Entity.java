package io.kestra.plugin.gcp.bigquery.models;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;

@Getter
@Builder
@Jacksonized
public class Entity {
    @NotNull
    @Schema(
        title = "The type of the entity (USER, GROUP, DOMAIN or IAM_MEMBER)."
    )
    private final Property<Type> type;

    @NotNull
    @Schema(
        title = "The value for the entity.",
        description = "For example, user email if the type is USER."
    )
    private final Property<String> value;

    @SuppressWarnings("unused")
    public enum Type {
        DOMAIN,
        GROUP,
        USER,
        IAM_MEMBER
    }
}
