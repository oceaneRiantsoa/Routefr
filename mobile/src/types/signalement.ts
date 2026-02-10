// Types pour les signalements de problèmes routiers

export interface Signalement {
  id?: string;
  userId: string;
  userEmail: string;
  latitude: number;
  longitude: number;
  problemeId: string; // Référence vers TypeProbleme
  problemeNom: string; // Nom du problème (dénormalisé pour affichage)
  description?: string; // Description additionnelle optionnelle
  status: 'nouveau' | 'en_cours' | 'termine';
  // Champs remplis côté WEB (admin)
  surface?: number; // en m²
  budget?: number; // en Ariary
  entrepriseId?: string;
  entrepriseNom?: string;
  dateCreation: number; // timestamp
  dateModification?: number;
  dateTraitement?: number;
  photos?: string[]; // URLs des photos (optionnel)
}

export interface TypeProbleme {
  id: string;
  nom: string;
  icone: string;
  description: string;
  priorite: number; // 1 = urgent, 2 = normal, 3 = faible
  actif: boolean;
}

export interface Entreprise {
  id: string;
  nom: string;
  contact?: string;
  telephone?: string;
  email?: string;
  specialite?: string;
  actif: boolean;
}

export interface SignalementStats {
  totalPoints: number;
  totalSurface: number;
  totalBudget: number;
  avancementPercent: number;
  nouveaux: number;
  enCours: number;
  termines: number;
  parProbleme: { [key: string]: number };
}

// Données initiales à insérer dans Firebase
export const TYPES_PROBLEMES_INITIAUX: TypeProbleme[] = [
  { id: 'route', nom: 'Route défectueuse', icone: 'construct-outline', description: 'Nid de poule, fissure, affaissement', priorite: 1, actif: true },
  { id: 'eau', nom: 'Problème d\'eau', icone: 'water-outline', description: 'Fuite, inondation, canalisation', priorite: 1, actif: true },
  { id: 'chantier', nom: 'Chantier dangereux', icone: 'warning-outline', description: 'Chantier mal sécurisé', priorite: 1, actif: true },
  { id: 'eclairage', nom: 'Éclairage public', icone: 'flash-outline', description: 'Lampadaire en panne', priorite: 2, actif: true },
  { id: 'trottoir', nom: 'Trottoir abîmé', icone: 'walk-outline', description: 'Trottoir cassé ou dangereux', priorite: 2, actif: true },
  { id: 'dechet', nom: 'Déchets/Débris', icone: 'trash-bin-outline', description: 'Déchets ou débris sur la voie', priorite: 2, actif: true },
  { id: 'signalisation', nom: 'Signalisation', icone: 'flag-outline', description: 'Panneau ou feu défaillant', priorite: 2, actif: true },
  { id: 'autre', nom: 'Autre', icone: 'pin-outline', description: 'Autre problème', priorite: 3, actif: true },
];

export const ENTREPRISES_INITIALES: Entreprise[] = [
  { id: 'colas', nom: 'COLAS Madagascar', contact: 'Direction Tana', telephone: '+261 20 22 XXX', specialite: 'Routes et voiries', actif: true },
  { id: 'sogea', nom: 'SOGEA SATOM', contact: 'Bureau Antananarivo', telephone: '+261 20 22 XXX', specialite: 'Travaux publics', actif: true },
  { id: 'eiffage', nom: 'EIFFAGE', contact: 'Siège Madagascar', telephone: '+261 20 22 XXX', specialite: 'Construction', actif: true },
  { id: 'ravinala', nom: 'RAVINALA Roads', contact: 'Direction technique', telephone: '+261 20 22 XXX', specialite: 'Entretien routier', actif: true },
  { id: 'agetipa', nom: 'AGETIPA', contact: 'Agence nationale', telephone: '+261 20 22 XXX', specialite: 'Infrastructure publique', actif: true },
  { id: 'jirama', nom: 'JIRAMA', contact: 'Service technique', telephone: '+261 20 22 XXX', specialite: 'Éclairage et réseaux', actif: true },
];

// Statuts avec couleurs pour la carte
export const STATUS_CONFIG = {
  nouveau: {
    label: 'Nouveau',
    color: '#dc2626',
    icon: 'alert-circle-outline'
  },
  en_cours: {
    label: 'En cours',
    color: '#d97706',
    icon: 'time-outline'
  },
  termine: {
    label: 'Terminé',
    color: '#059669',
    icon: 'checkmark-circle-outline'
  }
};

// Priorités
export const PRIORITE_CONFIG = {
  1: { label: 'Urgent', color: 'danger' },
  2: { label: 'Normal', color: 'warning' },
  3: { label: 'Faible', color: 'medium' }
};
