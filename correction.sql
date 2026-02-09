-- ============================================================================
-- SCRIPT DE CORRECTION SQL - Route Signalement
-- À exécuter si des problèmes de base de données apparaissent
-- ============================================================================

-- ============================================================================
-- 1. Colonnes manquantes dans local_users
-- ============================================================================

-- Ajouter firebase_sync_date
ALTER TABLE local_users 
ADD COLUMN IF NOT EXISTS firebase_sync_date TIMESTAMP;

-- Ajouter password_plain_temp
ALTER TABLE local_users 
ADD COLUMN IF NOT EXISTS password_plain_temp VARCHAR(255);

-- Ajouter synced_to_firebase
ALTER TABLE local_users 
ADD COLUMN IF NOT EXISTS synced_to_firebase BOOLEAN DEFAULT FALSE;

-- ============================================================================
-- 2. Colonnes manquantes dans signalement_firebase
-- ============================================================================

-- Ajouter avancement_pourcentage
ALTER TABLE signalement_firebase 
ADD COLUMN IF NOT EXISTS avancement_pourcentage INTEGER DEFAULT 0;

-- Ajouter date_debut_travaux
ALTER TABLE signalement_firebase 
ADD COLUMN IF NOT EXISTS date_debut_travaux TIMESTAMP;

-- Ajouter date_fin_travaux
ALTER TABLE signalement_firebase 
ADD COLUMN IF NOT EXISTS date_fin_travaux TIMESTAMP;

-- ============================================================================
-- 3. Corriger la contrainte FK user_sessions (IMPORTANT)
-- Permet la mise à jour automatique du firebase_uid lors de la synchronisation
-- ============================================================================

-- Supprimer les sessions des utilisateurs locaux non synchronisés
DELETE FROM user_sessions WHERE firebase_uid LIKE 'local-%';

-- Modifier la contrainte pour ajouter ON UPDATE CASCADE
ALTER TABLE user_sessions DROP CONSTRAINT IF EXISTS fk_session_user;
ALTER TABLE user_sessions 
ADD CONSTRAINT fk_session_user 
    FOREIGN KEY (firebase_uid) 
    REFERENCES local_users(firebase_uid) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE;

-- ============================================================================
-- 4. Vérification (décommenter pour exécuter)
-- ============================================================================

-- SELECT column_name, data_type FROM information_schema.columns 
-- WHERE table_name = 'local_users' ORDER BY ordinal_position;

-- SELECT column_name, data_type FROM information_schema.columns 
-- WHERE table_name = 'signalement_firebase' ORDER BY ordinal_position;

-- SELECT conname, confupdtype, confdeltype FROM pg_constraint 
-- WHERE conname = 'fk_session_user';

-- ============================================================================
-- FIN DU SCRIPT
-- ============================================================================