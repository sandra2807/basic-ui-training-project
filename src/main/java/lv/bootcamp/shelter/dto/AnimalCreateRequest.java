package lv.bootcamp.shelter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lv.bootcamp.shelter.model.AnimalType;

/**
 * JSON request body for creating a new animal via the REST API.
 * Status is not included; all new animals start as AVAILABLE.
 */
public record AnimalCreateRequest(

        @Schema(description = "Animal's name. Must not be blank.", example = "Milo")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Animal type: CAT, DOG, or OTHER.", example = "CAT")
        @NotNull(message = "Type is required")
        AnimalType type,

        @Schema(description = "Animal's breed, if known. Optional.", example = "Mixed breed")
        String breed,

        @Schema(description = "Age in whole years. Optional; zero or greater when provided.", example = "2")
        @Min(value = 0, message = "Age cannot be negative")
        Integer age,

        @Schema(description = "Optional information about the animal.", example = "Friendly and playful.")
        String description,

        @Schema(description = "Optional image filename, path, or URL. Omit or leave blank to use a default photo.", example = "Luna.jpeg")
        String imageUrl
) {}
