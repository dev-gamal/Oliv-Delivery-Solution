package ma.skaii.oliv.service;

import lombok.RequiredArgsConstructor;
import ma.skaii.oliv.domain.entity.JournalAudit;
import ma.skaii.oliv.domain.entity.Utilisateur;
import ma.skaii.oliv.repository.JournalAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final JournalAuditRepository journalAuditRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrerActionSensible(
            Utilisateur auteur,
            String action,
            String typeObjet,
            String objetId,
            String ancienneValeurJson,
            String nouvelleValeurJson,
            String motifObligatoire,
            String adresseIp
    ) {
        if (motifObligatoire == null || motifObligatoire.isBlank()) {
            throw new IllegalArgumentException("Un motif explicite est obligatoire pour toute action auditée.");
        }

        JournalAudit trace = JournalAudit.builder()
                .auteur(auteur)
                .action(action)
                .typeObjet(typeObjet)
                .objetId(objetId)
                .ancienneValeurJson(ancienneValeurJson)
                .nouvelleValeurJson(nouvelleValeurJson)
                .motifObligatoire(motifObligatoire)
                .horodatage(OffsetDateTime.now())
                .adresseIp(adresseIp)
                .build();

        journalAuditRepository.save(trace);
    }
}