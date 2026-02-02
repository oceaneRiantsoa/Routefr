-- ============================
-- INIT.SQL COMPLET POUR DOCKER + POSTGIS
-- ============================

CREATE EXTENSION IF NOT EXISTS postgis;

DROP VIEW IF EXISTS vue_recapitulation;
DROP TABLE IF EXISTS signalement_status;
DROP TABLE IF EXISTS signalement_details;
DROP TABLE IF EXISTS signalement;
DROP TABLE IF EXISTS entreprise;
DROP TABLE IF EXISTS profils;
DROP TABLE IF EXISTS probleme;
DROP TABLE IF EXISTS user_sessions;
DROP TABLE IF EXISTS local_users;
DROP TABLE IF EXISTS security_settings;
DROP TABLE IF EXISTS signalement_firebase;

CREATE TABLE profils (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    id_role_value INT NOT NULL
);

CREATE TABLE entreprise (
    id BIGSERIAL PRIMARY KEY,
    nom_entreprise VARCHAR(150) NOT NULL,
    localisation VARCHAR(200),
    contact VARCHAR(100)
);

CREATE TABLE probleme (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    detail TEXT,
    cout_par_m2 NUMERIC(12,2) NOT NULL
);

CREATE TABLE signalement (
    id BIGSERIAL PRIMARY KEY,
    idProfils INT NOT NULL,
    datetime_signalement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signalement_profils FOREIGN KEY (idProfils) REFERENCES profils(id)
);

CREATE TABLE signalement_details (
    id BIGSERIAL PRIMARY KEY,
    id_signalement INT NOT NULL,
    id_probleme INT NOT NULL,
    surface NUMERIC(10,2) NOT NULL,
    id_entreprise INT,
    commentaires TEXT,
    geom GEOGRAPHY(Point, 4326) NOT NULL,
    budget_estime NUMERIC(15,2),
    entreprise_assignee VARCHAR(200),
    notes_manager TEXT,
    statut_manager VARCHAR(50),
    date_modification TIMESTAMP,
    CONSTRAINT fk_details_signalement FOREIGN KEY (id_signalement) REFERENCES signalement(id),
    CONSTRAINT fk_details_probleme FOREIGN KEY (id_probleme) REFERENCES probleme(id),
    CONSTRAINT fk_details_entreprise FOREIGN KEY (id_entreprise) REFERENCES entreprise(id)
);

CREATE INDEX idx_signalement_geom ON signalement_details USING GIST (geom);

CREATE TABLE signalement_status (
    id BIGSERIAL PRIMARY KEY,
    id_signalement INT NOT NULL,
    idStatut INT NOT NULL,
    CONSTRAINT fk_status_signalement FOREIGN KEY (id_signalement) REFERENCES signalement(id)
);

CREATE TABLE local_users (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'USER',
    failed_attempts INT DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_local_users_email ON local_users(email);
CREATE INDEX idx_local_users_firebase_uid ON local_users(firebase_uid);

CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    ip_address VARCHAR(50),
    user_agent TEXT,
    CONSTRAINT fk_session_user FOREIGN KEY (firebase_uid) REFERENCES local_users(firebase_uid) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_sessions_firebase_uid ON user_sessions(firebase_uid);
CREATE INDEX idx_sessions_active ON user_sessions(active);

CREATE TABLE security_settings (
    id BIGSERIAL PRIMARY KEY,
    session_duration INT DEFAULT 30,
    max_login_attempts INT DEFAULT 5,
    lockout_duration INT DEFAULT 15,
    lockout_message TEXT DEFAULT 'Votre compte a ete temporairement bloque.',
    auto_lock_enabled BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE signalement_firebase (
    id BIGSERIAL PRIMARY KEY,
    firebase_id VARCHAR(255) UNIQUE NOT NULL,
    user_id VARCHAR(255),
    user_email VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    probleme_id VARCHAR(100),
    probleme_nom VARCHAR(200),
    description TEXT,
    status VARCHAR(50),
    surface NUMERIC(10,2),
    budget NUMERIC(15,2),
    date_creation_firebase TIMESTAMP,
    photo_url TEXT,
    entreprise_id VARCHAR(100),
    entreprise_nom VARCHAR(200),
    notes_manager TEXT,
    statut_local VARCHAR(50) DEFAULT 'non_traite',
    budget_estime NUMERIC(15,2),
    date_synchronisation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification_local TIMESTAMP,
    geom GEOGRAPHY(Point, 4326)
);

CREATE INDEX idx_signalement_firebase_id ON signalement_firebase(firebase_id);
CREATE INDEX idx_signalement_firebase_geom ON signalement_firebase USING GIST (geom);

CREATE VIEW vue_recapitulation AS
SELECT
    COUNT(DISTINCT s.id) AS nb_point,
    SUM(sd.surface) AS total_surface,
    CASE WHEN MIN(st.idStatut) = 20 THEN 20 WHEN MAX(st.idStatut) >= 10 THEN 10 ELSE 1 END AS avancement,
    SUM(sd.surface * p.cout_par_m2) AS total_budget
FROM signalement s
JOIN signalement_details sd ON sd.id_signalement = s.id
JOIN probleme p ON p.id = sd.id_probleme
LEFT JOIN signalement_status st ON st.id_signalement = s.id;

INSERT INTO profils (nom, id_role_value) VALUES ('Ocy', 10), ('Rado', 20);
INSERT INTO entreprise (nom_entreprise, localisation, contact) VALUES ('Colas Madagascar', 'Ankorondrano', '034 12 345 67');
INSERT INTO probleme (nom, detail, cout_par_m2) VALUES ('Nid de poule', 'Trou sur la chaussee', 50000), ('Route fissuree', 'Fissures importantes', 30000);
INSERT INTO signalement (idProfils, datetime_signalement) VALUES (1, '2026-01-10 08:30'), (2, '2026-01-12 14:15');
INSERT INTO signalement_details (id_signalement, id_probleme, surface, id_entreprise, commentaires, geom) VALUES (1, 1, 12.5, 1, 'Pres arret bus', ST_SetSRID(ST_MakePoint(47.5146, -18.8792), 4326)::GEOGRAPHY), (2, 2, 20.0, 1, 'Route fissuree', ST_SetSRID(ST_MakePoint(47.5310, -18.8725), 4326)::GEOGRAPHY);
INSERT INTO signalement_status (id_signalement, idStatut) VALUES (1, 10), (2, 20);
INSERT INTO local_users (firebase_uid, email, display_name, role, failed_attempts, account_locked) VALUES ('uid_admin_001', 'admin@routefr.com', 'Admin', 'ADMIN', 0, FALSE), ('uid_blocked_001', 'blocked@test.com', 'Bloque 1', 'USER', 5, TRUE), ('uid_blocked_002', 'blocked2@test.com', 'Bloque 2', 'USER', 7, TRUE);
INSERT INTO security_settings (session_duration, max_login_attempts, lockout_duration, auto_lock_enabled) VALUES (30, 5, 15, TRUE);
INSERT INTO signalement_firebase (firebase_id, user_id, user_email, latitude, longitude, probleme_nom, description, status, surface, statut_local, geom) VALUES ('-NxFirebase001', 'uid_admin_001', 'admin@routefr.com', -18.8792, 47.5146, 'Nid de poule', 'Test', 'en_attente', 15.5, 'non_traite', ST_SetSRID(ST_MakePoint(47.5146, -18.8792), 4326)::GEOGRAPHY);
