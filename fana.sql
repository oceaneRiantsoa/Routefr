-- Ajout de la colonne photos
ALTER TABLE signalement_firebase ADD COLUMN IF NOT EXISTS photos TEXT;

-- Table historique d'avancement pour tracer les changements de statut
CREATE TABLE IF NOT EXISTS historique_avancement (
    id BIGSERIAL PRIMARY KEY,
    signalement_id BIGINT,                      -- ID signalement local
    firebase_signalement_id BIGINT,             -- ID signalement Firebase
    ancien_statut VARCHAR(50),
    nouveau_statut VARCHAR(50),
    ancien_avancement INTEGER,
    nouveau_avancement INTEGER,
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur_id VARCHAR(255),
    commentaire TEXT,
    CONSTRAINT chk_signalement_ref CHECK (
        signalement_id IS NOT NULL OR firebase_signalement_id IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_historique_signalement_id ON historique_avancement(signalement_id);
CREATE INDEX IF NOT EXISTS idx_historique_firebase_id ON historique_avancement(firebase_signalement_id);
CREATE INDEX IF NOT EXISTS idx_historique_date ON historique_avancement(date_changement);