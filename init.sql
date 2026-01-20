-- ============================
-- INIT.SQL COMPLET POUR DOCKER + POSTGIS
-- ============================

-- Active PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================
-- SUPPRESSION DES OBJETS SI EXISTENT
-- ============================
DROP VIEW IF EXISTS vue_recapitulation;
DROP TABLE IF EXISTS signalement_status;
DROP TABLE IF EXISTS signalement_details;
DROP TABLE IF EXISTS signalement;
DROP TABLE IF EXISTS entreprise;
DROP TABLE IF EXISTS profils;
DROP TABLE IF EXISTS probleme;

-- ============================
-- TABLE : profils
-- ============================
CREATE TABLE profils (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    id_role_value INT NOT NULL
);

-- ============================
-- TABLE : entreprise
-- ============================
CREATE TABLE entreprise (
    id SERIAL PRIMARY KEY,
    nom_entreprise VARCHAR(150) NOT NULL,
    localisation VARCHAR(200),
    contact VARCHAR(100)
);

-- ============================
-- TABLE : probleme
-- ============================
CREATE TABLE probleme (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    detail TEXT,
    cout_par_m2 NUMERIC(12,2) NOT NULL
);

-- ============================
-- TABLE : signalement
-- ============================
CREATE TABLE signalement (
    id SERIAL PRIMARY KEY,
    idProfils INT NOT NULL,
    datetime_signalement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_signalement_profils
        FOREIGN KEY (idProfils) REFERENCES profils(id)
);

-- ============================
-- TABLE : signalement_details (POSTGIS)
-- ============================
CREATE TABLE signalement_details (
    id SERIAL PRIMARY KEY,
    id_signalement INT NOT NULL,
    id_probleme INT NOT NULL,
    surface NUMERIC(10,2) NOT NULL,
    id_entreprise INT,
    commentaires TEXT,
    geom GEOGRAPHY(Point, 4326) NOT NULL,

    CONSTRAINT fk_details_signalement
        FOREIGN KEY (id_signalement) REFERENCES signalement(id),
    CONSTRAINT fk_details_probleme
        FOREIGN KEY (id_probleme) REFERENCES probleme(id),
    CONSTRAINT fk_details_entreprise
        FOREIGN KEY (id_entreprise) REFERENCES entreprise(id)
);

CREATE INDEX idx_signalement_geom
ON signalement_details
USING GIST (geom);

-- ============================
-- TABLE : signalement_status
-- ============================
CREATE TABLE signalement_status (
    id SERIAL PRIMARY KEY,
    id_signalement INT NOT NULL,
    idStatut INT NOT NULL,
    CONSTRAINT fk_status_signalement
        FOREIGN KEY (id_signalement) REFERENCES signalement(id)
);

-- ============================
-- DONNÉES D'EXEMPLE
-- ============================
INSERT INTO profils (nom, id_role_value) VALUES
('Ocy', 10),
('Rado', 20);

INSERT INTO entreprise (nom_entreprise, localisation, contact) VALUES
('Colas Madagascar', 'Ankorondrano', '034 12 345 67');

INSERT INTO probleme (nom, detail, cout_par_m2) VALUES
('Nid de poule', 'Trou sur la chaussée', 50000),
('Route fissurée', 'Fissures importantes', 30000);

INSERT INTO signalement (idProfils, datetime_signalement) VALUES
(1, '2026-01-10 08:30'),
(2, '2026-01-12 14:15');

INSERT INTO signalement_details
(id_signalement, id_probleme, surface, id_entreprise, commentaires, geom)
VALUES
(
  1, 1, 12.5, 1,
  'Près arrêt bus Analakely',
  ST_SetSRID(ST_MakePoint(47.5146, -18.8792), 4326)::GEOGRAPHY
),
(
  2, 2, 20.0, 1,
  'Route fissurée Ankorondrano',
  ST_SetSRID(ST_MakePoint(47.5310, -18.8725), 4326)::GEOGRAPHY
);

INSERT INTO signalement_status (id_signalement, idStatut) VALUES
(1, 10),
(2, 20);

-- ============================
-- VUE : RÉCAPITULATION
-- ============================
CREATE VIEW vue_recapitulation AS
SELECT
    COUNT(DISTINCT s.id) AS nb_point,
    SUM(sd.surface) AS total_surface,
    CASE
        WHEN MIN(st.idStatut) = 20 THEN 20
        WHEN MAX(st.idStatut) >= 10 THEN 10
        ELSE 1
    END AS avancement,
    SUM(sd.surface * p.cout_par_m2) AS total_budget
FROM signalement s
JOIN signalement_details sd ON sd.id_signalement = s.id
JOIN probleme p ON p.id = sd.id_probleme
LEFT JOIN signalement_status st ON st.id_signalement = s.id;
