package ma.skaii.oliv.dto.preuve;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.skaii.oliv.domain.enums.TypePreuve;

public record PreuveSubmitRequest(
        @NotNull TypePreuve typePreuve,
        @NotBlank(message = "Le code PIN ou l'URL du fichier preuve est obligatoire")
        String valeurOuFichierUrl,
        Double latitudeCapture,
        Double longitudeCapture
) {}