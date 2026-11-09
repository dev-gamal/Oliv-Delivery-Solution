-- =============================================================================
-- Migration Flyway : V1__init_schema_oliv.sql
-- Projet           : O'liv v3.0 - Plateforme de logistique à la demande (B2B/B2C)
-- SGBD cible       : MySQL 8.0+ (InnoDB / utf8mb4_unicode_ci)
-- Description      : Création complète du schéma relationnel (13 entités métier),
--                    contraintes d'intégrité, index de performance et jeu de
--                    données initial des règles tarifaires v3.0.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. TABLE : organisations_expediteurs (Comptes Pro B2B & Abonnements)
-- -----------------------------------------------------------------------------
CREATE TABLE organisations_expediteurs (
                                           id CHAR(36) NOT NULL,
                                           raison_sociale VARCHAR(200) NOT NULL,
                                           ice VARCHAR(50) NULL COMMENT 'Identifiant Commun de l Entreprise (Maroc)',
                                           nom_contact_principal VARCHAR(150) NULL,
                                           telephone_contact VARCHAR(20) NULL,
                                           email_facturation VARCHAR(150) NULL,
                                           adresse_facturation TEXT NULL,
                                           adresses_collecte_favorites_json TEXT NULL,
                                           plan_abonnement_actif VARCHAR(30) NULL COMMENT 'START_20, BUSINESS_50, PRO_100, MIX_100',
                                           prix_mensuel_abonnement DECIMAL(10, 2) NULL,
                                           quota_courses_incluses_mensuel INT NULL DEFAULT 0,
                                           courses_consommees_mois_courant INT NULL DEFAULT 0,
                                           frais_service_sous_contrat DECIMAL(8, 2) NULL COMMENT '0.00 à 2.00 DH selon contrat B2B',
                                           date_renouvellement_abonnement DATE NULL,
                                           PRIMARY KEY (id),
                                           INDEX idx_org_ice (ice),
                                           INDEX idx_org_raison_sociale (raison_sociale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. TABLE : utilisateurs (Acteurs de la plateforme)
-- -----------------------------------------------------------------------------
CREATE TABLE utilisateurs (
                              id CHAR(36) NOT NULL,
                              nom_complet VARCHAR(150) NOT NULL,
                              telephone VARCHAR(20) NOT NULL,
                              email VARCHAR(150) NULL,
                              mot_de_passe_hash VARCHAR(255) NOT NULL,
                              role VARCHAR(30) NOT NULL COMMENT 'EXPEDITEUR_PARTICULIER, EXPEDITEUR_PRO, LIVREUR, OPERATEUR_DISPATCH, FINANCE, ADMIN',
                              statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_VERIFICATION' COMMENT 'EN_ATTENTE_VERIFICATION, ACTIF, SUSPENDU, DESACTIVE',
                              telephone_verifie TINYINT(1) NOT NULL DEFAULT 0,
                              email_verifie TINYINT(1) NOT NULL DEFAULT 0,
                              organisation_id CHAR(36) NULL,
                              date_creation DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              PRIMARY KEY (id),
                              UNIQUE KEY uk_user_telephone (telephone),
                              UNIQUE KEY uk_user_email (email),
                              INDEX idx_user_role_statut (role, statut),
                              CONSTRAINT fk_user_organisation FOREIGN KEY (organisation_id)
                                  REFERENCES organisations_expediteurs (id) ON DELETE SET NULL ON UPDATE CASCADE,
                              CONSTRAINT chk_user_role CHECK (role IN (
                                                                       'EXPEDITEUR_PARTICULIER', 'EXPEDITEUR_PRO', 'LIVREUR',
                                                                       'OPERATEUR_DISPATCH', 'FINANCE', 'ADMIN'
                                  )),
                              CONSTRAINT chk_user_statut CHECK (statut IN (
                                                                           'EN_ATTENTE_VERIFICATION', 'ACTIF', 'SUSPENDU', 'DESACTIVE'
                                  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. TABLE : profils_livreurs (Flotte indépendante, KYC & Portefeuille)
-- -----------------------------------------------------------------------------
CREATE TABLE profils_livreurs (
                                  id CHAR(36) NOT NULL,
                                  utilisateur_id CHAR(36) NOT NULL,
                                  zone_affectation VARCHAR(100) NOT NULL,
                                  vehicule_declare VARCHAR(20) NOT NULL COMMENT 'ECO, MOTO, AUTO, PRO',
                                  en_ligne TINYINT(1) NOT NULL DEFAULT 0,
                                  statut_verification VARCHAR(30) NOT NULL DEFAULT 'CANDIDAT' COMMENT 'CANDIDAT, PIECES_SOUMISES, VERIFIE, REJETE, SUSPENDU',
                                  piece_identite_url VARCHAR(500) NULL,
                                  permis_conduire_url VARCHAR(500) NULL,
                                  justificatif_statut_auto_entrepreneur_url VARCHAR(500) NULL,
                                  formation_courte_validee TINYINT(1) NOT NULL DEFAULT 0,
                                  derniere_latitude DOUBLE NULL,
                                  derniere_longitude DOUBLE NULL,
                                  date_derniere_position DATETIME(6) NULL,
                                  note_moyenne DECIMAL(3, 2) NULL DEFAULT 5.00,
                                  taux_acceptation DECIMAL(5, 2) NULL DEFAULT 100.00,
                                  solde_a_percevoir_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Fonds de tiers dus au livreur',
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_livreur_utilisateur (utilisateur_id),
                                  INDEX idx_livreur_dispo_zone (en_ligne, statut_verification, zone_affectation, vehicule_declare),
                                  CONSTRAINT fk_livreur_utilisateur FOREIGN KEY (utilisateur_id)
                                      REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT chk_livreur_vehicule CHECK (vehicule_declare IN ('ECO', 'MOTO', 'AUTO', 'PRO')),
                                  CONSTRAINT chk_livreur_kyc CHECK (statut_verification IN (
                                                                                            'CANDIDAT', 'PIECES_SOUMISES', 'VERIFIE', 'REJETE', 'SUSPENDU'
                                      ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. TABLE : acceptations_conditions (Conformité juridique CGU & CNDP)
-- -----------------------------------------------------------------------------
CREATE TABLE acceptations_conditions (
                                         id CHAR(36) NOT NULL,
                                         utilisateur_id CHAR(36) NOT NULL,
                                         type_document VARCHAR(50) NOT NULL COMMENT 'CGU_EXPEDITEUR, CGU_LIVREUR, POLITIQUE_CONFIDENTIALITE, CONSENTEMENT_GPS',
                                         version_document VARCHAR(30) NOT NULL,
                                         date_acceptation DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                         adresse_ip VARCHAR(45) NULL,
                                         PRIMARY KEY (id),
                                         INDEX idx_cgu_utilisateur (utilisateur_id, type_document),
                                         CONSTRAINT fk_cgu_utilisateur FOREIGN KEY (utilisateur_id)
                                             REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. TABLE : regles_tarifaires (Moteur de prix et commissions versionné)
-- -----------------------------------------------------------------------------
CREATE TABLE regles_tarifaires (
                                   id CHAR(36) NOT NULL,
                                   code_version VARCHAR(30) NOT NULL COMMENT 'Ex: 2026-10-v3.0 (Règle d or n°4 et n°10)',
                                   zone VARCHAR(100) NOT NULL,
                                   vehicule VARCHAR(20) NOT NULL COMMENT 'ECO, MOTO, AUTO, PRO',
                                   prix_tranche_0_a_2_km DECIMAL(8, 2) NOT NULL,
                                   prix_tranche_2_a_4_km DECIMAL(8, 2) NOT NULL,
                                   prix_tranche_4_a_6_km DECIMAL(8, 2) NOT NULL,
                                   prix_tranche_6_a_8_km DECIMAL(8, 2) NOT NULL,
                                   distance_incluse_km DECIMAL(8, 2) NOT NULL DEFAULT 2.00,
                                   coeff_km_supplementaire DECIMAL(8, 2) NOT NULL COMMENT 'Coefficient X au-delà de 2 km ou 8 km',
                                   utiliser_grille_jusqua_8_km TINYINT(1) NOT NULL DEFAULT 1,
                                   frais_service_standard DECIMAL(8, 2) NOT NULL DEFAULT 3.00,
                                   frais_service_express DECIMAL(8, 2) NOT NULL DEFAULT 5.00,
                                   supplement_option_express DECIMAL(8, 2) NOT NULL DEFAULT 5.00,
                                   supplement_creneau_garanti DECIMAL(8, 2) NOT NULL DEFAULT 5.00,
                                   tarif_attente_par_10_min DECIMAL(8, 2) NOT NULL DEFAULT 5.00,
                                   supplement_arret_simple DECIMAL(8, 2) NOT NULL DEFAULT 5.00,
                                   pourcentage_gain_livreur DECIMAL(5, 2) NOT NULL DEFAULT 70.00,
                                   pourcentage_commission_oliv DECIMAL(5, 2) NOT NULL DEFAULT 30.00,
                                   reserve_support_paiement_par_course DECIMAL(8, 2) NOT NULL DEFAULT 1.00,
                                   valide_depuis DATETIME(6) NOT NULL,
                                   valide_jusqua DATETIME(6) NULL,
                                   actif TINYINT(1) NOT NULL DEFAULT 1,
                                   PRIMARY KEY (id),
                                   UNIQUE KEY uk_tarif_version_zone_vehicule (code_version, zone, vehicule),
                                   INDEX idx_tarif_actif (zone, vehicule, actif),
                                   CONSTRAINT chk_tarif_vehicule CHECK (vehicule IN ('ECO', 'MOTO', 'AUTO', 'PRO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. TABLE : commandes (Objet métier unique : Demande de livraison)
-- -----------------------------------------------------------------------------
CREATE TABLE commandes (
                           id CHAR(36) NOT NULL,
                           reference_unique VARCHAR(30) NOT NULL COMMENT 'Identifiant unique lisible (Règle n°1)',
                           type_demande VARCHAR(30) NOT NULL COMMENT 'COLIS, DOCUMENT, COMMANDE_COMMERCANT, RETOUR_ALLER_RETOUR',
                           vehicule_requis VARCHAR(20) NOT NULL COMMENT 'ECO, MOTO, AUTO, PRO',
                           statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
                           zone VARCHAR(100) NOT NULL,
                           expediteur_id CHAR(36) NOT NULL,
                           organisation_id CHAR(36) NULL,
                           livreur_affecte_id CHAR(36) NULL,
                           adresse_collecte TEXT NOT NULL,
                           latitude_collecte DOUBLE NOT NULL,
                           longitude_collecte DOUBLE NOT NULL,
                           contact_collecte_nom VARCHAR(150) NULL,
                           contact_collecte_tel VARCHAR(20) NULL,
                           instructions_collecte TEXT NULL,
                           reference_interne_commercant VARCHAR(100) NULL COMMENT 'Référence ticket restaurant / boutique',
                           commande_signalee_prete TINYINT(1) NOT NULL DEFAULT 0,
                           heure_signalement_prete DATETIME(6) NULL,
                           adresse_livraison TEXT NOT NULL,
                           latitude_livraison DOUBLE NOT NULL,
                           longitude_livraison DOUBLE NOT NULL,
                           destinataire_nom VARCHAR(150) NOT NULL,
                           destinataire_telephone VARCHAR(20) NOT NULL,
                           instructions_livraison TEXT NULL,
                           token_suivi_destinataire VARCHAR(64) NOT NULL COMMENT 'Jeton d accès suivi sans compte (Section 3)',
                           code_pin_livraison_hash VARCHAR(255) NULL,
                           nature_contenu VARCHAR(255) NOT NULL,
                           declaration_marchandise_autorisee TINYINT(1) NOT NULL DEFAULT 1,
                           poids_kg DOUBLE NULL,
                           dimensions_cm VARCHAR(50) NULL,
                           valeur_declaree_dh DECIMAL(10, 2) NULL,
                           distance_itineraire_km DECIMAL(8, 2) NOT NULL,
                           distance_facturee_km DECIMAL(8, 2) NOT NULL COMMENT 'Arrondie au 0.5 km supérieur (Section 19.13)',
                           option_express TINYINT(1) NOT NULL DEFAULT 0,
                           option_creneau_garanti TINYINT(1) NOT NULL DEFAULT 0,
                           creneau_debut DATETIME(6) NULL,
                           creneau_fin DATETIME(6) NULL,
                           nombre_arrets_supplementaires INT NOT NULL DEFAULT 0,
                           minutes_attente_collecte INT NOT NULL DEFAULT 0,
                           retour_point_collecte_requis TINYINT(1) NOT NULL DEFAULT 0,
                           date_creation DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           date_confirmation DATETIME(6) NULL,
                           date_affectation_livreur DATETIME(6) NULL,
                           date_collecte_effective DATETIME(6) NULL,
                           date_livraison_effective DATETIME(6) NULL,
                           date_cloture DATETIME(6) NULL COMMENT 'Règle n°6 : Une commande clôturée est immuable',
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_cmd_reference (reference_unique),
                           UNIQUE KEY uk_cmd_token_suivi (token_suivi_destinataire),
                           INDEX idx_cmd_statut_zone (statut, zone),
                           INDEX idx_cmd_expediteur (expediteur_id, date_creation),
                           INDEX idx_cmd_organisation (organisation_id, date_creation),
                           INDEX idx_cmd_livreur (livreur_affecte_id, statut),
                           CONSTRAINT fk_cmd_expediteur FOREIGN KEY (expediteur_id)
                               REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                           CONSTRAINT fk_cmd_organisation FOREIGN KEY (organisation_id)
                               REFERENCES organisations_expediteurs (id) ON DELETE SET NULL ON UPDATE CASCADE,
                           CONSTRAINT fk_cmd_livreur FOREIGN KEY (livreur_affecte_id)
                               REFERENCES profils_livreurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                           CONSTRAINT chk_cmd_type_demande CHECK (type_demande IN (
                                                                                   'COLIS', 'DOCUMENT', 'COMMANDE_COMMERCANT', 'RETOUR_ALLER_RETOUR'
                               )),
                           CONSTRAINT chk_cmd_vehicule CHECK (vehicule_requis IN ('ECO', 'MOTO', 'AUTO', 'PRO')),
                           CONSTRAINT chk_cmd_statut CHECK (statut IN (
                                                                       'BROUILLON', 'CONFIRMEE', 'EN_RECHERCHE_LIVREUR', 'OFFRE_ENVOYEE',
                                                                       'ACCEPTEE', 'EN_ROUTE_COLLECTE', 'COLLECTEE', 'EN_LIVRAISON',
                                                                       'LIVREE', 'ECHEC_INCIDENT', 'CLOTUREE', 'ANNULEE'
                               ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. TABLE : details_financiers_commandes (Séparation comptable & Marge)
-- -----------------------------------------------------------------------------
CREATE TABLE details_financiers_commandes (
                                              id CHAR(36) NOT NULL,
                                              commande_id CHAR(36) NOT NULL,
                                              regle_tarifaire_id CHAR(36) NOT NULL,
                                              version_regle_texte VARCHAR(30) NOT NULL COMMENT 'Version figée à la confirmation (Règle n°4)',
                                              prix_livraison_base_dh DECIMAL(10, 2) NOT NULL COMMENT 'Prix P',
                                              frais_service_dh DECIMAL(10, 2) NOT NULL COMMENT 'Ligne distincte obligatoire (Règle n°3)',
                                              supplement_express_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                              supplement_attente_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                              supplement_arrets_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                              frais_echec_ou_retour_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Coûts d échec jamais masqués (Règle n°9)',
                                              remise_appliquee_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                              code_promo_ou_motif_remise VARCHAR(100) NULL,
                                              total_paye_expediteur_dh DECIMAL(10, 2) NOT NULL COMMENT 'P + service + options - remise',
                                              gain_net_livreur_dh DECIMAL(10, 2) NOT NULL COMMENT 'Fonds de tiers (70% P + part options)',
                                              commission_oliv_sur_livraison_dh DECIMAL(10, 2) NOT NULL COMMENT '30% P',
                                              revenu_propre_oliv_dh DECIMAL(10, 2) NOT NULL COMMENT 'Commission + Frais de service (CA O liv)',
                                              couts_variables_reserve_dh DECIMAL(10, 2) NOT NULL DEFAULT 1.00,
                                              marge_contributive_dh DECIMAL(10, 2) NOT NULL COMMENT 'Revenu propre - Coûts variables',
                                              mode_paiement VARCHAR(30) NOT NULL COMMENT 'PREPAIEMENT_EN_LIGNE, PAIEMENT_LIVRAISON_COD, FACTURATION_MENSUELLE_PRO',
                                              statut_paiement VARCHAR(30) NOT NULL DEFAULT 'A_FACTURER',
                                              montant_cod_a_encaisser_marchandise_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'Fonds de tiers encaissés pour l expéditeur',
                                              montant_cod_reverse_expediteur TINYINT(1) NOT NULL DEFAULT 0,
                                              reference_rapprochement_paiement VARCHAR(100) NULL,
                                              PRIMARY KEY (id),
                                              UNIQUE KEY uk_fin_commande (commande_id),
                                              INDEX idx_fin_statut_paiement (statut_paiement, mode_paiement),
                                              CONSTRAINT fk_fin_commande FOREIGN KEY (commande_id)
                                                  REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                              CONSTRAINT fk_fin_regle_tarifaire FOREIGN KEY (regle_tarifaire_id)
                                                  REFERENCES regles_tarifaires (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                              CONSTRAINT chk_fin_mode_paiement CHECK (mode_paiement IN (
                                                                                                        'PREPAIEMENT_EN_LIGNE', 'PAIEMENT_LIVRAISON_COD', 'FACTURATION_MENSUELLE_PRO'
                                                  )),
                                              CONSTRAINT chk_fin_statut_paiement CHECK (statut_paiement IN (
                                                                                                            'A_FACTURER', 'FACTURE', 'PARTIELLEMENT_PAYE', 'PAYE', 'EN_LITIGE', 'REMBOURSE'
                                                  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. TABLE : historiques_statuts (Journal immuable des transitions d'état)
-- -----------------------------------------------------------------------------
CREATE TABLE historiques_statuts (
                                     id CHAR(36) NOT NULL,
                                     commande_id CHAR(36) NOT NULL,
                                     ancien_statut VARCHAR(30) NULL,
                                     nouveau_statut VARCHAR(30) NOT NULL,
                                     horodatage DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     auteur_id CHAR(36) NULL,
                                     motif_ou_commentaire VARCHAR(255) NULL,
                                     PRIMARY KEY (id),
                                     INDEX idx_hist_commande_date (commande_id, horodatage),
                                     CONSTRAINT fk_hist_commande FOREIGN KEY (commande_id)
                                         REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                     CONSTRAINT fk_hist_auteur FOREIGN KEY (auteur_id)
                                         REFERENCES utilisateurs (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. TABLE : offres_courses (Journalisation du Matching Manuel & Auto)
-- -----------------------------------------------------------------------------
CREATE TABLE offres_courses (
                                id CHAR(36) NOT NULL,
                                commande_id CHAR(36) NOT NULL,
                                livreur_id CHAR(36) NOT NULL,
                                mode_dispatch VARCHAR(20) NOT NULL DEFAULT 'MANUEL_ASSISTE' COMMENT 'MANUEL_ASSISTE, AUTOMATIQUE',
                                operateur_id CHAR(36) NULL,
                                distance_approche_collecte_km DECIMAL(8, 2) NOT NULL,
                                gain_net_estime_affiche_dh DECIMAL(10, 2) NOT NULL COMMENT 'Visible par le livreur avant acceptation',
                                heure_envoi DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                delai_reponse_secondes INT NOT NULL DEFAULT 60,
                                heure_expiration DATETIME(6) NOT NULL,
                                statut_reponse VARCHAR(20) NOT NULL DEFAULT 'ENVOYEE' COMMENT 'ENVOYEE, ACCEPTEE, REFUSEE, EXPIREE',
                                heure_reponse DATETIME(6) NULL,
                                motif_refus VARCHAR(255) NULL,
                                PRIMARY KEY (id),
                                INDEX idx_offre_cmd_livreur (commande_id, livreur_id, statut_reponse),
                                INDEX idx_offre_livreur_statut (livreur_id, statut_reponse),
                                CONSTRAINT fk_offre_commande FOREIGN KEY (commande_id)
                                    REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                CONSTRAINT fk_offre_livreur FOREIGN KEY (livreur_id)
                                    REFERENCES profils_livreurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                CONSTRAINT fk_offre_operateur FOREIGN KEY (operateur_id)
                                    REFERENCES utilisateurs (id) ON DELETE SET NULL ON UPDATE CASCADE,
                                CONSTRAINT chk_offre_mode CHECK (mode_dispatch IN ('MANUEL_ASSISTE', 'AUTOMATIQUE')),
                                CONSTRAINT chk_offre_statut CHECK (statut_reponse IN ('ENVOYEE', 'ACCEPTEE', 'REFUSEE', 'EXPIREE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 10. TABLE : preuves_livraison (Preuves obligatoires Collecte & Livraison)
-- -----------------------------------------------------------------------------
CREATE TABLE preuves_livraison (
                                   id CHAR(36) NOT NULL,
                                   commande_id CHAR(36) NOT NULL,
                                   etape VARCHAR(20) NOT NULL COMMENT 'COLLECTE, LIVRAISON',
                                   type_preuve VARCHAR(30) NOT NULL COMMENT 'CODE_PIN_OTP, SIGNATURE, PHOTO, DEROGATION_TRACEE',
                                   valeur_ou_fichier_url VARCHAR(500) NOT NULL,
                                   auteur_id CHAR(36) NOT NULL,
                                   horodatage DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   latitude_capture DOUBLE NULL,
                                   longitude_capture DOUBLE NULL,
                                   PRIMARY KEY (id),
                                   INDEX idx_preuve_commande_etape (commande_id, etape),
                                   CONSTRAINT fk_preuve_commande FOREIGN KEY (commande_id)
                                       REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_preuve_auteur FOREIGN KEY (auteur_id)
                                       REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT chk_preuve_etape CHECK (etape IN ('COLLECTE', 'LIVRAISON')),
                                   CONSTRAINT chk_preuve_type CHECK (type_preuve IN (
                                                                                     'CODE_PIN_OTP', 'SIGNATURE', 'PHOTO', 'DEROGATION_TRACEE'
                                       ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 11. TABLE : versements_livreurs (Règlement périodique des gains livreurs)
-- -----------------------------------------------------------------------------
CREATE TABLE versements_livreurs (
                                     id CHAR(36) NOT NULL,
                                     livreur_id CHAR(36) NOT NULL,
                                     reference_paiement VARCHAR(50) NOT NULL,
                                     periode_debut DATE NOT NULL,
                                     periode_fin DATE NOT NULL,
                                     nombre_courses_incluses INT NOT NULL DEFAULT 0,
                                     montant_gains_courses_dh DECIMAL(10, 2) NOT NULL,
                                     montant_primes_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                     montant_total_verse_dh DECIMAL(10, 2) NOT NULL,
                                     statut_versement VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE' COMMENT 'EN_ATTENTE, VIRE, ECHEC',
                                     mode_versement VARCHAR(50) NULL COMMENT 'VIREMENT_BANCAIRE, ESPECES_AGENCE',
                                     date_execution DATETIME(6) NULL,
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_versement_reference (reference_paiement),
                                     INDEX idx_versement_livreur_periode (livreur_id, periode_debut, periode_fin),
                                     CONSTRAINT fk_versement_livreur FOREIGN KEY (livreur_id)
                                         REFERENCES profils_livreurs (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 12. TABLE : factures_clients (Facturation B2B mensuelle & B2C)
-- -----------------------------------------------------------------------------
CREATE TABLE factures_clients (
                                  id CHAR(36) NOT NULL,
                                  numero_facture VARCHAR(50) NOT NULL,
                                  organisation_id CHAR(36) NULL,
                                  expediteur_particulier_id CHAR(36) NULL,
                                  date_emission DATE NOT NULL,
                                  date_echeance DATE NULL,
                                  montant_ht_dh DECIMAL(10, 2) NOT NULL,
                                  montant_tva_dh DECIMAL(10, 2) NOT NULL,
                                  montant_ttc_dh DECIMAL(10, 2) NOT NULL,
                                  statut VARCHAR(30) NOT NULL DEFAULT 'FACTURE',
                                  mode_reglement VARCHAR(50) NULL,
                                  reference_rapprochement_bancaire VARCHAR(100) NULL,
                                  fichier_pdf_url VARCHAR(500) NULL,
                                  date_reglement_effectif DATETIME(6) NULL,
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_facture_numero (numero_facture),
                                  INDEX idx_facture_org (organisation_id, date_emission),
                                  INDEX idx_facture_statut (statut),
                                  CONSTRAINT fk_facture_organisation FOREIGN KEY (organisation_id)
                                      REFERENCES organisations_expediteurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT fk_facture_expediteur FOREIGN KEY (expediteur_particulier_id)
                                      REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 13. TABLE : notes_avis (Notation mutuelle Expéditeur <-> Livreur)
-- -----------------------------------------------------------------------------
CREATE TABLE notes_avis (
                            id CHAR(36) NOT NULL,
                            commande_id CHAR(36) NOT NULL,
                            auteur_id CHAR(36) NOT NULL,
                            cible_id CHAR(36) NOT NULL,
                            note_sur_cinq INT NOT NULL,
                            commentaire TEXT NULL,
                            date_creation DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            PRIMARY KEY (id),
                            INDEX idx_note_commande (commande_id),
                            INDEX idx_note_cible (cible_id),
                            CONSTRAINT fk_note_commande FOREIGN KEY (commande_id)
                                REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                            CONSTRAINT fk_note_auteur FOREIGN KEY (auteur_id)
                                REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                            CONSTRAINT fk_note_cible FOREIGN KEY (cible_id)
                                REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                            CONSTRAINT chk_note_valeur CHECK (note_sur_cinq BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 14. TABLE : incidents_litiges (Parcours d'échec & Décisions Opérateur)
-- -----------------------------------------------------------------------------
CREATE TABLE incidents_litiges (
                                   id CHAR(36) NOT NULL,
                                   commande_id CHAR(36) NOT NULL,
                                   declarant_id CHAR(36) NOT NULL,
                                   motif VARCHAR(40) NOT NULL COMMENT '10 motifs normalisés obligatoires (Section 15)',
                                   description TEXT NULL,
                                   piece_jointe_photo_url VARCHAR(500) NULL,
                                   decision_operateur VARCHAR(40) NULL COMMENT 'NOUVELLE_TENTATIVE, RETOUR_EXPEDITEUR, ANNULATION, CLOTURE',
                                   operateur_id CHAR(36) NULL,
                                   montant_remboursement_client_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                   indemnite_temps_engage_livreur_dh DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                                   date_declaration DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   date_resolution DATETIME(6) NULL,
                                   PRIMARY KEY (id),
                                   INDEX idx_incident_commande (commande_id),
                                   INDEX idx_incident_motif (motif),
                                   CONSTRAINT fk_incident_commande FOREIGN KEY (commande_id)
                                       REFERENCES commandes (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_incident_declarant FOREIGN KEY (declarant_id)
                                       REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_incident_operateur FOREIGN KEY (operateur_id)
                                       REFERENCES utilisateurs (id) ON DELETE SET NULL ON UPDATE CASCADE,
                                   CONSTRAINT chk_incident_motif CHECK (motif IN (
                                                                                  'DESTINATAIRE_ABSENT', 'ADRESSE_INCORRECTE', 'POINT_COLLECTE_FERME',
                                                                                  'COMMANDE_NON_PRETE', 'COLIS_REFUSE', 'COLIS_ENDOMMAGE',
                                                                                  'PANNE_VEHICULE', 'ACCIDENT', 'RETARD', 'AUTRE'
                                       ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 15. TABLE : journal_audit (Traçabilité de sécurité & Règle d'or n°7)
-- -----------------------------------------------------------------------------
CREATE TABLE journal_audit (
                               id CHAR(36) NOT NULL,
                               utilisateur_id CHAR(36) NOT NULL,
                               action VARCHAR(60) NOT NULL COMMENT 'MODIFICATION_PRIX_APRES_VALIDATION, SUSPENSION_LIVREUR, AJUSTEMENT_CLOTURE...',
                               type_objet VARCHAR(40) NOT NULL,
                               objet_id VARCHAR(64) NOT NULL,
                               ancienne_valeur_json TEXT NULL,
                               nouvelle_valeur_json TEXT NULL,
                               motif_obligatoire VARCHAR(255) NOT NULL,
                               horodatage DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               adresse_ip VARCHAR(45) NULL,
                               PRIMARY KEY (id),
                               INDEX idx_audit_objet (type_objet, objet_id),
                               INDEX idx_audit_horodatage (horodatage),
                               CONSTRAINT fk_audit_utilisateur FOREIGN KEY (utilisateur_id)
                                   REFERENCES utilisateurs (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- JEU DE DONNÉES INITIAL (SEED) : Grille Tarifaire O'liv v3.0 (Sections 9 & 19)
-- =============================================================================
INSERT INTO regles_tarifaires (
    id, code_version, zone, vehicule,
    prix_tranche_0_a_2_km, prix_tranche_2_a_4_km, prix_tranche_4_a_6_km, prix_tranche_6_a_8_km,
    distance_incluse_km, coeff_km_supplementaire, utiliser_grille_jusqua_8_km,
    frais_service_standard, frais_service_express, supplement_option_express,
    supplement_creneau_garanti, tarif_attente_par_10_min, supplement_arret_simple,
    pourcentage_gain_livreur, pourcentage_commission_oliv, reserve_support_paiement_par_course,
    valide_depuis, actif
) VALUES
-- 1. Véhicule ECO (Vélo) : 10 DH (0-2km), 14 DH (2-4km), 18 DH (4-6km), 23 DH (6-8km), Coeff X = 2.00 DH/km
(
    '11111111-1111-1111-1111-111111111101', '2026-10-v3.0', 'ZONE_PILOTE_MAROC', 'ECO',
    10.00, 14.00, 18.00, 23.00,
    2.00, 2.00, 1,
    3.00, 5.00, 5.00,
    5.00, 5.00, 5.00,
    70.00, 30.00, 1.00,
    '2026-10-28 00:00:00.000000', 1
),
-- 2. Véhicule MOTO : 15 DH (0-2km), 19 DH (2-4km), 24 DH (4-6km), 30 DH (6-8km), Coeff X = 2.50 DH/km
(
    '11111111-1111-1111-1111-111111111102', '2026-10-v3.0', 'ZONE_PILOTE_MAROC', 'MOTO',
    15.00, 19.00, 24.00, 30.00,
    2.00, 2.50, 1,
    3.00, 5.00, 5.00,
    5.00, 5.00, 5.00,
    70.00, 30.00, 1.00,
    '2026-10-28 00:00:00.000000', 1
),
-- 3. Véhicule AUTO : 25 DH (0-2km), 30 DH (2-4km), 40 DH (4-6km), 50 DH (6-8km), Coeff X = 5.00 DH/km (> 4km/8km)
(
    '11111111-1111-1111-1111-111111111103', '2026-10-v3.0', 'ZONE_PILOTE_MAROC', 'AUTO',
    25.00, 30.00, 40.00, 50.00,
    2.00, 5.00, 1,
    3.00, 5.00, 5.00,
    5.00, 5.00, 5.00,
    70.00, 30.00, 1.00,
    '2026-10-28 00:00:00.000000', 1
);

SET FOREIGN_KEY_CHECKS = 1;