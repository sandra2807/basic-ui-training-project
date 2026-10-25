package lv.bootcamp.shelter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;

/**
 * Response body for a single animal returned by the API.
 * {@code adoptionNote} contains the adoption date for adopted animals and is otherwise null.
 */
public record AnimalResponse(
        @Schema(description = "Unique ID assigned by the application.", example = "7")
        Long id,
        @Schema(description = "Animal's name.", example = "Milo")
        String name,
        @Schema(description = "Animal type: CAT, DOG, or OTHER.", example = "CAT")
        AnimalType type,
        @Schema(description = "Animal's breed, if known.", example = "Mixed breed", nullable = true)
        String breed,
        @Schema(description = "Age in whole years, if known.", example = "2", nullable = true)
        Integer age,
        @Schema(description = "Information about the animal, if provided.", example = "Friendly and playful.", nullable = true)
        String description,
        @Schema(description = "Adoption status. New animals start as AVAILABLE.", example = "AVAILABLE")
        AnimalStatus status,
        @Schema(description = "Resolved image path or URL, including a default photo when no image was provided.",
                example = "/images/fallback/fallback-cat.jpg")
        String imageUrl,
        @Schema(description = "Adoption date note for adopted animals; null for animals not yet adopted.",
                example = "adopted on 2026-06-01", nullable = true)
        String adoptionNote
) {}
