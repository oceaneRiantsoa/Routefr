// Service pour gérer les signalements avec Firebase Realtime Database
import { ref as dbRef, push, set, get, onValue, update, remove } from 'firebase/database';
import { realtimeDb, auth } from '@/firebase';
import type { Signalement, SignalementStats, TypeProbleme, Entreprise } from '@/types/signalement';
import { TYPES_PROBLEMES_INITIAUX, ENTREPRISES_INITIALES } from '@/types/signalement';
import { ref, type Ref } from 'vue';
import { photoService, type SignalementPhoto } from './photoService';

class SignalementService {
  private signalementPath = 'signalements';
  private problemesPath = 'types_problemes';
  private entreprisesPath = 'entreprises';

  // ==================== SIGNALEMENTS ====================

  // Créer un nouveau signalement (mobile - simplifié)
  async createSignalement(data: {
    latitude: number;
    longitude: number;
    problemeId: string;
    problemeNom: string;
    description?: string;
    photos?: SignalementPhoto[];
  }): Promise<string> {
    const user = auth.currentUser;
    if (!user) throw new Error('Utilisateur non connecté');

    const newSignalementRef = push(dbRef(realtimeDb, this.signalementPath));
    const signalementId = newSignalementRef.key!;

    // Upload des photos si présentes
    let photoUrls: string[] = [];
    if (data.photos && data.photos.length > 0) {
      try {
        photoUrls = await photoService.uploadPhotos(data.photos, signalementId);
      } catch (error) {
        console.error('Erreur upload photos:', error);
        // Continue sans les photos
      }
    }
    
    // Construire l'objet signalement SANS les champs undefined
    // Firebase Realtime Database n'accepte pas les valeurs undefined
    const newSignalement: Record<string, any> = {
      id: signalementId,
      userId: user.uid,
      userEmail: user.email || 'unknown',
      latitude: data.latitude,
      longitude: data.longitude,
      problemeId: data.problemeId,
      problemeNom: data.problemeNom,
      description: data.description || '',
      status: 'nouveau',
      dateCreation: Date.now(),
    };
    
    // Ajouter les photos seulement si elles existent
    if (photoUrls.length > 0) {
      newSignalement.photos = photoUrls;
    }

    await set(newSignalementRef, newSignalement);
    return signalementId;
  }

  // Obtenir tous les signalements (temps réel)
  subscribeToAllSignalements(callback: (signalements: Signalement[]) => void): () => void {
    const signalementRef = dbRef(realtimeDb, this.signalementPath);
    
    const unsubscribe = onValue(signalementRef, (snapshot) => {
      const signalements: Signalement[] = [];
      snapshot.forEach((child) => {
        signalements.push({
          id: child.key,
          ...child.val()
        } as Signalement);
      });
      // Trier par date décroissante
      signalements.sort((a, b) => b.dateCreation - a.dateCreation);
      callback(signalements);
    }, (error) => {
      console.error('Erreur Firebase signalements:', error);
      callback([]); // Retourner tableau vide en cas d'erreur
    });

    return unsubscribe;
  }

  // Obtenir les signalements d'un utilisateur spécifique (temps réel)
  subscribeToUserSignalements(userId: string, callback: (signalements: Signalement[]) => void): () => void {
    const signalementRef = dbRef(realtimeDb, this.signalementPath);
    
    const unsubscribe = onValue(signalementRef, (snapshot) => {
      const signalements: Signalement[] = [];
      snapshot.forEach((child) => {
        const data = child.val();
        if (data.userId === userId) {
          signalements.push({
            id: child.key,
            ...data
          } as Signalement);
        }
      });
      signalements.sort((a, b) => b.dateCreation - a.dateCreation);
      callback(signalements);
    });

    return unsubscribe;
  }

  // Supprimer un signalement
  async deleteSignalement(id: string): Promise<void> {
    const signalementRef = dbRef(realtimeDb, `${this.signalementPath}/${id}`);
    await remove(signalementRef);
  }

  // Calculer les statistiques (avec données simulées pour budget/surface)
  calculateStats(signalements: Signalement[]): SignalementStats {
    const nouveaux = signalements.filter(s => s.status === 'nouveau').length;
    const enCours = signalements.filter(s => s.status === 'en_cours').length;
    const termines = signalements.filter(s => s.status === 'termine').length;
    
    const totalPoints = signalements.length;
    
    // Données simulées pour surface et budget (remplis côté web normalement)
    const totalSurface = signalements.reduce((sum, s) => sum + (s.surface || this.getSimulatedSurface(s)), 0);
    const totalBudget = signalements.reduce((sum, s) => sum + (s.budget || this.getSimulatedBudget(s)), 0);
    
    // Avancement = % de signalements terminés
    const avancementPercent = totalPoints > 0 ? Math.round((termines / totalPoints) * 100) : 0;

    // Stats par type de problème
    const parProbleme: { [key: string]: number } = {};
    signalements.forEach(s => {
      parProbleme[s.problemeId] = (parProbleme[s.problemeId] || 0) + 1;
    });

    return {
      totalPoints,
      totalSurface,
      totalBudget,
      avancementPercent,
      nouveaux,
      enCours,
      termines,
      parProbleme
    };
  }

  // Simulation de surface selon le type de problème
  private getSimulatedSurface(s: Signalement): number {
    const surfaceParType: { [key: string]: number } = {
      'route': 25,
      'eau': 15,
      'chantier': 100,
      'eclairage': 0,
      'trottoir': 20,
      'dechet': 5,
      'signalisation': 0,
      'autre': 10
    };
    return surfaceParType[s.problemeId] || 10;
  }

  // Simulation de budget selon le type de problème (en Ariary MGA)
  private getSimulatedBudget(s: Signalement): number {
    const budgetParType: { [key: string]: number } = {
      'route': 1500000,
      'eau': 2000000,
      'chantier': 500000,
      'eclairage': 800000,
      'trottoir': 1000000,
      'dechet': 100000,
      'signalisation': 300000,
      'autre': 500000
    };
    return budgetParType[s.problemeId] || 500000;
  }

  // ==================== TYPES DE PROBLEMES ====================

  // Initialiser les types de problèmes dans Firebase
  async initializeTypesProblemes(): Promise<void> {
    const ref = dbRef(realtimeDb, this.problemesPath);
    const snapshot = await get(ref);
    
    if (!snapshot.exists()) {
      const data: { [key: string]: TypeProbleme } = {};
      TYPES_PROBLEMES_INITIAUX.forEach(p => {
        data[p.id] = p;
      });
      await set(ref, data);
      console.log('Types de problèmes initialisés');
    }
  }

  // S'abonner aux types de problèmes (avec fallback local)
  subscribeToTypesProblemes(callback: (types: TypeProbleme[]) => void): () => void {
    const ref = dbRef(realtimeDb, this.problemesPath);
    
    const unsubscribe = onValue(ref, (snapshot) => {
      const types: TypeProbleme[] = [];
      snapshot.forEach((child) => {
        const data = child.val();
        if (data.actif) {
          types.push({ id: child.key, ...data } as TypeProbleme);
        }
      });
      
      // Si Firebase est vide, utiliser les données locales
      if (types.length === 0) {
        console.log('Firebase vide, utilisation des types locaux');
        callback(TYPES_PROBLEMES_INITIAUX.filter(t => t.actif));
        return;
      }
      
      types.sort((a, b) => a.priorite - b.priorite);
      callback(types);
    }, (error) => {
      console.error('Erreur Firebase, utilisation des types locaux:', error);
      callback(TYPES_PROBLEMES_INITIAUX.filter(t => t.actif));
    });

    return unsubscribe;
  }

  // ==================== ENTREPRISES ====================

  // Initialiser les entreprises dans Firebase
  async initializeEntreprises(): Promise<void> {
    const ref = dbRef(realtimeDb, this.entreprisesPath);
    const snapshot = await get(ref);
    
    if (!snapshot.exists()) {
      const data: { [key: string]: Entreprise } = {};
      ENTREPRISES_INITIALES.forEach(e => {
        data[e.id] = e;
      });
      await set(ref, data);
      console.log('Entreprises initialisées');
    }
  }

  // S'abonner aux entreprises (avec fallback local)
  subscribeToEntreprises(callback: (entreprises: Entreprise[]) => void): () => void {
    const ref = dbRef(realtimeDb, this.entreprisesPath);
    
    const unsubscribe = onValue(ref, (snapshot) => {
      const entreprises: Entreprise[] = [];
      snapshot.forEach((child) => {
        const data = child.val();
        if (data.actif) {
          entreprises.push({ id: child.key, ...data } as Entreprise);
        }
      });
      
      // Si Firebase est vide, utiliser les données locales
      if (entreprises.length === 0) {
        console.log('Firebase vide, utilisation des entreprises locales');
        callback(ENTREPRISES_INITIALES.filter(e => e.actif));
        return;
      }
      
      entreprises.sort((a, b) => a.nom.localeCompare(b.nom));
      callback(entreprises);
    }, (error) => {
      console.error('Erreur Firebase, utilisation des entreprises locales:', error);
      callback(ENTREPRISES_INITIALES.filter(e => e.actif));
    });

    return unsubscribe;
  }

  // Initialiser toutes les données de référence
  async initializeAllData(): Promise<void> {
    await this.initializeTypesProblemes();
    await this.initializeEntreprises();
  }
}

export const signalementService = new SignalementService();

// Composable pour utiliser dans les composants Vue
export function useSignalements() {
  const signalements: Ref<Signalement[]> = ref([]);
  const stats: Ref<SignalementStats> = ref({
    totalPoints: 0,
    totalSurface: 0,
    totalBudget: 0,
    avancementPercent: 0,
    nouveaux: 0,
    enCours: 0,
    termines: 0,
    parProbleme: {}
  });
  const loading = ref(true);
  const error = ref<string | null>(null);
  let unsubscribe: (() => void) | null = null;

  const loadAllSignalements = () => {
    loading.value = true;
    unsubscribe = signalementService.subscribeToAllSignalements((data) => {
      signalements.value = data;
      stats.value = signalementService.calculateStats(data);
      loading.value = false;
    });
  };

  const loadUserSignalements = (userId: string) => {
    loading.value = true;
    unsubscribe = signalementService.subscribeToUserSignalements(userId, (data) => {
      signalements.value = data;
      stats.value = signalementService.calculateStats(data);
      loading.value = false;
    });
  };

  const cleanup = () => {
    if (unsubscribe) {
      unsubscribe();
      unsubscribe = null;
    }
  };

  return {
    signalements,
    stats,
    loading,
    error,
    loadAllSignalements,
    loadUserSignalements,
    cleanup
  };
}
