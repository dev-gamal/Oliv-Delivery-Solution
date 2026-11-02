package ma.skaii.oliv.domain.enums;

public enum StatutVerificationLivreur {
    CANDIDAT,
    PIECES_SOUMISES,
    VERIFIE, // Obligatoire pour recevoir une course (Section 3)
    REJETE,
    SUSPENDU
}
