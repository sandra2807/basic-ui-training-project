package lv.bootcamp.shelter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsAllFiveRestOperationsAndTheirResponses() throws Exception {
        JsonNode docs = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(docs.at("/info/title").asText()).isEqualTo("Animal Management API");
        assertThat(docs.at("/info/version").asText()).isEqualTo("1.0");
        assertThat(docs.at("/info/description").asText())
                .isEqualTo("REST API for managing animals in the application.");
        JsonNode paths = docs.get("paths");
        assertThat(paths.size()).isEqualTo(4);
        String[][] operations = {
                {"/api/animals", "get", "200"},
                {"/api/animals", "post", "201"},
                {"/api/animals/{id}", "get", "200", "400", "404"},
                {"/api/animals/adopted", "get", "200"},
                {"/api/animals/{id}/adopt", "post", "200", "400", "404", "409"}
        };
        for (String[] operation : operations) {
            JsonNode endpoint = paths.get(operation[0]).get(operation[1]);
            assertThat(endpoint.get("summary").asText()).isNotBlank();
            assertThat(endpoint.get("tags").get(0).asText()).isEqualTo("Animals");
            for (int index = 2; index < operation.length; index++) {
                assertThat(endpoint.get("responses").has(operation[index])).isTrue();
            }
        }
        JsonNode findById = paths.get("/api/animals/{id}").get("get");
        assertThat(findById.at("/parameters/0/description").asText()).isNotBlank();
        assertThat(findById.at("/parameters/0/required").asBoolean()).isTrue();
        assertThat(findById.at("/responses/404").has("content")).isFalse();
        assertThat(paths.get("/api/animals/{id}/adopt").get("post")
                .at("/responses/409/content/text~1plain/schema/type").asText()).isEqualTo("string");
    }

    @Test
    void documentsRequestAndResponseSchemas() throws Exception {
        JsonNode docs = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString());
        JsonNode request = docs.at("/components/schemas/AnimalCreateRequest");
        assertThat(request.get("required")).containsExactlyInAnyOrder(
                objectMapper.valueToTree("name"), objectMapper.valueToTree("type"));
        assertThat(request.at("/properties/age/minimum").asInt()).isZero();
        assertThat(request.at("/properties/type/enum")).containsExactly(
                objectMapper.valueToTree("CAT"), objectMapper.valueToTree("DOG"), objectMapper.valueToTree("OTHER"));
        request.get("properties").forEach(field -> assertThat(field.get("description").asText()).isNotBlank());
        docs.at("/components/schemas/AnimalResponse/properties")
                .forEach(field -> assertThat(field.get("description").asText()).isNotBlank());
        JsonNode create = docs.get("paths").get("/api/animals").get("post");
        assertThat(create.at("/requestBody/required").asBoolean()).isTrue();
        assertThat(create.at("/requestBody/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AnimalCreateRequest");
        assertThat(create.at("/responses/201/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AnimalResponse");
        assertThat(create.get("responses").has("200")).isFalse();
    }

    @Test
    void servesSwaggerUiAndItsConfiguration() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui-bundle.js")));
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void createsAnimalAndMakesItVisibleInRestAndThymeleaf() throws Exception {
        JsonNode animal = objectMapper.readTree(mockMvc.perform(post("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Swagger test Milo","type":"CAT","age":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.imageUrl").value("/images/fallback/fallback-cat.jpg"))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(get("/api/animals/{id}", animal.get("id").asLong()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Swagger test Milo"));
        mockMvc.perform(get("/api/animals")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger test Milo")));
        mockMvc.perform(get("/animals")).andExpect(status().isOk()).andExpect(view().name("animals"))
                .andExpect(content().string(containsString("Swagger test Milo")));
    }

    @Test
    void rejectsInvalidCreateRequests() throws Exception {
        String[] invalidBodies = {
                "{}", "{\"name\":\" \",\"type\":\"CAT\"}",
                "{\"name\":\"Milo\",\"type\":\"CAT\",\"age\":-1}",
                "{\"name\":\"Milo\",\"type\":\"BIRD\"}", "not JSON"
        };
        for (String body : invalidBodies) {
            mockMvc.perform(post("/api/animals").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void preservesAdoptionAndNotFoundBehavior() throws Exception {
        JsonNode animal = objectMapper.readTree(mockMvc.perform(post("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Adoption test\",\"type\":\"DOG\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = animal.get("id").asLong();
        mockMvc.perform(post("/api/animals/{id}/adopt", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADOPTED"))
                .andExpect(jsonPath("$.adoptionNote").value(containsString("adopted on ")));
        mockMvc.perform(get("/api/animals/adopted")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Adoption test")));
        mockMvc.perform(post("/api/animals/{id}/adopt", id)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Animal " + id + " is already adopted"));
        mockMvc.perform(get("/api/animals/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(content().string(""));
        mockMvc.perform(post("/api/animals/{id}/adopt", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/animals/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/animals/not-a-number/adopt")).andExpect(status().isBadRequest());
    }

    @Test
    void preservesThymeleafPagesAndFormValidation() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
        mockMvc.perform(get("/animals/new")).andExpect(status().isOk())
                .andExpect(view().name("animals-new"));
        mockMvc.perform(post("/animals").param("name", "Form test Milo").param("type", "CAT"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/animals"));
        mockMvc.perform(get("/animals")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Form test Milo")));
        mockMvc.perform(post("/animals").param("name", "").param("type", "CAT").param("age", "-1"))
                .andExpect(status().isOk()).andExpect(view().name("animals-new"))
                .andExpect(model().attributeHasFieldErrors("form", "name", "age"));
        mockMvc.perform(get("/images/fallback/fallback-cat.jpg")).andExpect(status().isOk());
    }
}
