package lv.bootcamp.shelter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.service.AnimalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for shelter animal endpoints.
 * Returns JSON — does not render HTML pages.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/animals")
@Tag(name = "Animals", description = "View, create, and adopt shelter animals.")
public class AnimalApiController {

    private final AnimalService animalService;

    @GetMapping
    @Operation(summary = "List all animals", description = "Returns all animals sorted by ID.")
    @ApiResponse(responseCode = "200", description = "List of animals (may be empty)",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AnimalResponse.class))))
    public List<AnimalResponse> findAll() {
        return animalService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find an animal by ID")
    @ApiResponse(responseCode = "200", description = "Animal found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalResponse.class)))
    @ApiResponse(responseCode = "404", description = "No animal has this ID", content = @Content)
    @ApiResponse(responseCode = "400", description = "The ID is not a whole number",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    public ResponseEntity<AnimalResponse> findById(
            @Parameter(description = "ID of the animal to find", example = "1") @PathVariable Long id) {
        return animalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists adopted animals.
     */
    @GetMapping("/adopted")
    @Operation(summary = "List adopted animals", description = "Returns only adopted animals, sorted by ID.")
    @ApiResponse(responseCode = "200", description = "List of adopted animals (may be empty)",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AnimalResponse.class))))
    public List<AnimalResponse> findAdopted() {
        return animalService.findAdopted();
    }

    /**
     * Creates a new animal.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an animal",
            description = "Creates an animal with status AVAILABLE. Name and type are required. "
                    + "Age is optional but cannot be negative. A missing or blank image uses a default photo.")
    @ApiResponse(responseCode = "201", description = "Animal created with its assigned ID and resolved image path",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid JSON, missing required fields, invalid type, or negative age",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    public AnimalResponse create(@RequestBody @Valid AnimalCreateRequest request) {
        return animalService.create(request);
    }

    /**
     * Adopts an animal
     */
    @PostMapping("/{id}/adopt")
    @Operation(summary = "Adopt an animal", description = "Marks an available animal as ADOPTED and records today's date. No request body is needed.")
    @ApiResponse(responseCode = "200", description = "Animal adopted",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnimalResponse.class)))
    @ApiResponse(responseCode = "404", description = "No animal has this ID", content = @Content)
    @ApiResponse(responseCode = "409", description = "Animal is already adopted",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Animal 3 is already adopted")))
    @ApiResponse(responseCode = "400", description = "The ID is not a whole number",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    public ResponseEntity<AnimalResponse> adopt(
            @Parameter(description = "ID of the animal to adopt", example = "1") @PathVariable Long id) {
        return animalService.adopt(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleAlreadyAdopted(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
