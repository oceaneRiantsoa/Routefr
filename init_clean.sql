-- ============================================================================
-- INIT_CLEAN.SQL - Script d'initialisation de la base de données Route Signalement
-- Version: 2.0 - Février 2026
-- Inclut: Tables d'avancement, authentification, et gestion des signalements
-- ============================================================================

-- Active l'extension PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================================
-- SECTION 1: SUPPRESSION DES TABLES EXISTANTES (ordre inversé des dépendances)
-- ============================================================================

DROP VIEW IF EXISTS vue_recapitulation;
DROP TABLE IF EXISTS user_sessions CASCADE;
DROP TABLE IF EXISTS local_users CASCADE;
DROP TABLE IF EXISTS security_settings CASCADE;
DROP TABLE IF EXISTS signalement_status CASCADE;
DROP TABLE IF EXISTS signalement_details CASCADE;
DROP TABLE IF EXISTS signalement CASCADE;
DROP TABLE IF EXISTS signalement_firebase CASCADE;
DROP TABLE IF EXISTS entreprise CASCADE;
DROP TABLE IF EXISTS probleme CASCADE;
DROP TABLE IF EXISTS profils CASCADE;

-- ============================================================================
-- SECTION 2: TABLES DE RÉFÉRENCE
-- ============================================================================

-- Table des profils utilisateurs
CREATE TABLE profils (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    id_role_value INT NOT NULL
);

-- Table des entreprises de travaux
CREATE TABLE entreprise (
    id BIGSERIAL PRIMARY KEY,
    nom_entreprise VARCHAR(150) NOT NULL,
    localisation VARCHAR(200),
    contact VARCHAR(100)
);

-- Table des types de problèmes routiers
CREATE TABLE probleme (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    detail TEXT,
    cout_par_m2 NUMERIC(12,2) NOT NULL
);

-- ============================================================================
-- SECTION 3: TABLES DE SIGNALEMENT LOCAL
-- ============================================================================

-- Table principale des signalements (version locale)
CREATE TABLE signalement (
    id BIGSERIAL PRIMARY KEY,
    idprofils INTEGER NOT NULL,
    datetime_signalement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_signalement_profils 
        FOREIGN KEY (idprofils) REFERENCES profils(id)
);

-- Table des détails des signalements
CREATE TABLE signalement_details (
    id BIGSERIAL PRIMARY KEY,
    id_signalement INTEGER,
    id_probleme INTEGER,
    id_entreprise INTEGER,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    surface NUMERIC(10,2),
    commentaires TEXT,
    notes_manager TEXT,
    budget_estime NUMERIC(15,2),
    statut_manager VARCHAR(50),
    date_modification TIMESTAMP,
    geom GEOGRAPHY(Point, 4326),
    
    CONSTRAINT fk_details_signalement 
        FOREIGN KEY (id_signalement) REFERENCES signalement(id),
    CONSTRAINT fk_details_probleme 
        FOREIGN KEY (id_probleme) REFERENCES probleme(id),
    CONSTRAINT fk_details_entreprise 
        FOREIGN KEY (id_entreprise) REFERENCES entreprise(id)
);

-- Index spatial pour les signalements
CREATE INDEX idx_signalement_details_geom ON signalement_details USING GIST (geom);

-- Table des statuts des signalements
CREATE TABLE signalement_status (
    id BIGSERIAL PRIMARY KEY,
    id_signalement INTEGER NOT NULL,
    idstatut INTEGER DEFAULT 10,  -- 10=En attente, 20=En cours, 30=Traité, 40=Rejeté
    
    CONSTRAINT fk_status_signalement 
        FOREIGN KEY (id_signalement) REFERENCES signalement(id)
);

-- ============================================================================
-- SECTION 4: TABLE SIGNALEMENT FIREBASE (avec colonnes d'avancement)
-- ============================================================================

CREATE TABLE signalement_firebase (
    id BIGSERIAL PRIMARY KEY,
    
    -- Identifiants Firebase
    firebase_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255),
    user_email VARCHAR(255),
    
    -- Localisation
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geom GEOGRAPHY(Point, 4326),
    
    -- Informations du problème
    probleme_id VARCHAR(100),
    probleme_nom VARCHAR(200),
    description TEXT,
    photo_url TEXT,
    
    -- Données financières
    surface NUMERIC(10,2),
    budget NUMERIC(15,2),
    budget_estime NUMERIC(15,2),
    
    -- Entreprise assignée
    entreprise_id VARCHAR(100),
    entreprise_nom VARCHAR(200),
    
    -- Statuts
    status VARCHAR(50),                                    -- Statut Firebase original
    statut_local VARCHAR(50) DEFAULT 'non_traite',         -- Statut local
    
    -- ========== COLONNES D'AVANCEMENT (NOUVELLES) ==========
    avancement_pourcentage INTEGER DEFAULT 0,              -- 0%, 50%, 100%
    date_debut_travaux TIMESTAMP,                          -- Date passage à "en_cours" (50%)
    date_fin_travaux TIMESTAMP,                            -- Date passage à "terminé" (100%)
    -- =======================================================
    
    -- ========== COLONNES CALCUL BUDGET ==========
    niveau_reparation INTEGER DEFAULT 1 CHECK (niveau_reparation BETWEEN 1 AND 10),  -- Niveau 1 à 10
    budget_calcule NUMERIC(15,2),                          -- Budget calculé automatiquement en Ariary
    -- ============================================
    
    -- Notes du manager
    notes_manager TEXT,
    
    -- Dates
    date_creation_firebase TIMESTAMP,                      -- Date création sur mobile
    date_synchronisation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification_local TIMESTAMP
);

-- Index pour signalement_firebase
CREATE UNIQUE INDEX idx_signalement_firebase_id ON signalement_firebase(firebase_id);
CREATE INDEX idx_signalement_firebase_user_id ON signalement_firebase(user_id);
CREATE INDEX idx_signalement_firebase_status ON signalement_firebase(status);
CREATE INDEX idx_signalement_firebase_statut_local ON signalement_firebase(statut_local);
CREATE INDEX idx_signalement_firebase_avancement ON signalement_firebase(avancement_pourcentage);
CREATE INDEX idx_signalement_firebase_geom ON signalement_firebase USING GIST (geom);

-- ============================================================================
-- SECTION 5: TABLES D'AUTHENTIFICATION
-- ============================================================================

-- Table des utilisateurs locaux (liés à Firebase Auth)
CREATE TABLE local_users (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    password_hash VARCHAR(255),                            -- Pour authentification hors-ligne
    role VARCHAR(50) DEFAULT 'USER',                       -- USER, MANAGER, ADMIN
    failed_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    firebase_sync_date TIMESTAMP,                          -- Date dernière synchro Firebase
    password_plain_temp VARCHAR(255),                      -- Mot de passe temporaire pour sync
    synced_to_firebase BOOLEAN DEFAULT FALSE               -- Indicateur de synchronisation
);

-- Index pour local_users
CREATE INDEX idx_local_users_firebase_uid ON local_users(firebase_uid);
CREATE INDEX idx_local_users_email ON local_users(email);

-- Table des sessions utilisateurs
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    ip_address VARCHAR(50),
    user_agent TEXT,
    
    CONSTRAINT fk_session_user 
        FOREIGN KEY (firebase_uid) REFERENCES local_users(firebase_uid) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Index pour user_sessions
CREATE INDEX idx_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_sessions_firebase_uid ON user_sessions(firebase_uid);
CREATE INDEX idx_sessions_active ON user_sessions(active);

-- Table des paramètres de sécurité
CREATE TABLE security_settings (
    id BIGSERIAL PRIMARY KEY,
    session_duration INTEGER DEFAULT 30,                   -- Durée session en minutes
    max_login_attempts INTEGER DEFAULT 5,                  -- Tentatives avant blocage
    lockout_duration INTEGER DEFAULT 15,                   -- Durée blocage en minutes
    lockout_message TEXT DEFAULT 'Votre compte a été temporairement bloqué.',
    auto_lock_enabled BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE signalement_firebase ADD COLUMN IF NOT EXISTS photos TEXT;
ALTER TABLE signalement_firebase ADD COLUMN IF NOT EXISTS needs_firebase_sync BOOLEAN DEFAULT FALSE;

-- Index pour retrouver rapidement les signalements modifiés à synchroniser
CREATE INDEX IF NOT EXISTS idx_signalement_firebase_needs_sync ON signalement_firebase(needs_firebase_sync) WHERE needs_firebase_sync = TRUE;

-- ============================================================================
-- SECTION 6: DONNÉES DE TEST / RÉFÉRENCE
-- ============================================================================

-- Profils utilisateurs
INSERT INTO profils (nom, id_role_value) VALUES 
    ('Utilisateur', 1),
    ('Manager', 2),
    ('Administrateur', 3);

-- Entreprises de travaux
INSERT INTO entreprise (nom_entreprise, localisation, contact) VALUES 
    ('Colas Madagascar', 'Antananarivo', '+261 20 22 123 45'),
    ('SOGEA-SATOM', 'Antananarivo', '+261 20 22 234 56'),
    ('RAZEL-BEC', 'Toamasina', '+261 20 53 123 45'),
    ('EIFFAGE', 'Antananarivo', '+261 20 22 345 67'),
    ('China Road', 'Antananarivo', '+261 20 22 456 78');

-- Types de problèmes routiers
INSERT INTO probleme (nom, detail, cout_par_m2) VALUES 
    ('Nid de poule', 'Trou dans la chaussée nécessitant rebouchage', 50000.00),
    ('Route fissuree', 'Fissures longitudinales ou transversales', 30000.00),
    ('Affaissement', 'Affaissement de la chaussée', 75000.00),
    ('Inondation', 'Zone inondable nécessitant drainage', 100000.00),
    ('Fissure', 'Fissures mineures à traiter', 25000.00),
    ('Erosion', 'Erosion des bords de route', 60000.00),
    ('Deformation', 'Déformation de la surface', 45000.00);

-- Paramètres de sécurité par défaut
INSERT INTO security_settings (session_duration, max_login_attempts, lockout_duration, auto_lock_enabled) 
VALUES (30, 5, 15, TRUE);

-- Utilisateur test pour les démonstrations
-- IMPORTANT: password_hash = BCrypt de "password123" et password_plain_temp pour la sync Firebase
INSERT INTO local_users (firebase_uid, email, display_name, role, synced_to_firebase, password_hash, password_plain_temp) 
VALUES 
    ('local-5fa20835-e5ab-44d0-a758-6c3d9af954ab', 'manager@routefr.com', 'Manager Routefr', 'MANAGER', FALSE, 
     '$2a$10$hJSneLLD//Q4M8zExxj5h.f3KJrbV2AIJaDM6VbtRzFLt0bkVqdiO', '123456'),
    ('user-001', 'user@routefr.com', 'User Test', 'USER', FALSE,
     '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkHx6g0B7Q7QxKq/7FhqxHPOm', 'password123');




    --  DELETE FROM local_users WHERE email = 'manager@routefr.com';

-- ============================================================================
-- SECTION 7: SIGNALEMENTS DE TEST
-- ============================================================================

-- Signalement de test #1
-- INSERT INTO signalement (idprofils, datetime_signalement) 
-- VALUES (1, '2026-01-10 08:30:00');

-- INSERT INTO signalement_details (id_signalement, id_probleme, id_entreprise, latitude, longitude, surface, commentaires) 
-- VALUES (1, 1, 1, -18.8792, 47.5146, 12.50, 'Pres arret bus');

-- INSERT INTO signalement_status (id_signalement, idstatut) 
-- VALUES (1, 10);

-- -- Signalement de test #2
-- INSERT INTO signalement (idprofils, datetime_signalement) 
-- VALUES (1, '2026-01-12 14:15:00');

-- INSERT INTO signalement_details (id_signalement, id_probleme, id_entreprise, latitude, longitude, surface, commentaires, notes_manager) 
-- VALUES (2, 2, 1, -18.8725, 47.5310, 20.00, 'Route fissuree', 'deja traite');

-- INSERT INTO signalement_status (id_signalement, idstatut) 
-- VALUES (2, 30);

-- ============================================================================
-- SECTION 8: VUE RÉCAPITULATIVE
-- ============================================================================

CREATE OR REPLACE VIEW vue_recapitulation AS
SELECT 
    sd.id,
    s.datetime_signalement,
    p.nom AS probleme,
    p.cout_par_m2,
    sd.surface,
    (sd.surface * p.cout_par_m2) AS cout_total,
    e.nom_entreprise,
    ss.idstatut,
    CASE ss.idstatut
        WHEN 10 THEN 'En attente'
        WHEN 20 THEN 'En cours'
        WHEN 30 THEN 'Traité'
        WHEN 40 THEN 'Rejeté'
        ELSE 'Inconnu'
    END AS statut_libelle,
    sd.latitude,
    sd.longitude
FROM signalement_details sd
JOIN signalement s ON sd.id_signalement = s.id
JOIN probleme p ON sd.id_probleme = p.id
LEFT JOIN entreprise e ON sd.id_entreprise = e.id
LEFT JOIN signalement_status ss ON ss.id_signalement = s.id;

-- ============================================================================
-- SECTION 9: COMMENTAIRES SUR LES COLONNES D'AVANCEMENT
-- ============================================================================

COMMENT ON COLUMN signalement_firebase.avancement_pourcentage IS 
    'Pourcentage d''avancement: 0% = nouveau, 50% = en_cours, 100% = terminé';

COMMENT ON COLUMN signalement_firebase.date_debut_travaux IS 
    'Date de passage au statut "en_cours" (50%)';

COMMENT ON COLUMN signalement_firebase.date_fin_travaux IS 
    'Date de passage au statut "terminé" (100%)';

COMMENT ON COLUMN signalement_firebase.niveau_reparation IS 
    'Niveau de réparation de 1 à 10 (complexité/gravité du problème)';

COMMENT ON COLUMN signalement_firebase.budget_calcule IS 
    'Budget calculé automatiquement en Ariary: prix_par_m2 × niveau × surface';

-- ============================================================================
-- FIN DU SCRIPT
-- ============================================================================
