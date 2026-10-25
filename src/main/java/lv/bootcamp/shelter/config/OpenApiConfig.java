package lv.bootcamp.shelter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI animalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Animal Management API")
                        .version("1.0")
                        .description("REST API for managing animals in the application."));
    }
}
