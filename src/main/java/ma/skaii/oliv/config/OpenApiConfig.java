package ma.skaii.oliv.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI olivOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("O'liv v3.0 — API Logistique à la demande")
                        .version("3.0.0")
                        .description("Documentation REST de la plateforme O'liv (Expéditeurs B2B/B2C, Livreurs, Dispatching, Finance et Suivi Public).")
                        .contact(new Contact()
                                .name("Skaii Service — Équipe Technique")
                                .email("tech@oliv.ma")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Collez uniquement le token JWT obtenu via POST /api/auth/login (sans le préfixe 'Bearer ').")));
    }
}