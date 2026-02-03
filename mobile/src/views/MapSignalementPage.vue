<template>
  <ion-page>
    <!-- Header -->
    <ion-header>
      <ion-toolbar color="primary">
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>
          <div class="header-title">
            <span>🗺️ Signalement Tana</span>
          </div>
        </ion-title>
        <ion-buttons slot="end">
          <ion-button @click="showFilterModal = true" class="header-btn">
            <ion-icon :icon="filterOutline"></ion-icon>
            <span class="filter-badge" v-if="activeFiltersCount > 0">{{ activeFiltersCount }}</span>
          </ion-button>
          <ion-button @click="showStatsModal = true" class="header-btn">
            <ion-icon :icon="statsChartOutline"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <!-- Menu latéral -->
    <ion-menu content-id="main-content" side="start">
      <ion-header>
        <ion-toolbar color="primary">
          <ion-title>Menu</ion-title>
        </ion-toolbar>
      </ion-header>
      <ion-content>
        <div class="menu-user-card">
          <div class="user-avatar">{{ userInitial }}</div>
          <div class="user-info">
            <div class="user-email">{{ userEmail }}</div>
            <div class="user-status">🟢 Connecté</div>
          </div>
        </div>

        <ion-list lines="none" class="menu-list">
          <ion-list-header>
            <ion-label>📊 Affichage</ion-label>
          </ion-list-header>
          
          <ion-item button @click="setFilterMode('all')" :class="{ active: filterMode === 'all' }">
            <ion-icon :icon="globeOutline" slot="start" color="primary"></ion-icon>
            <ion-label>Tous les signalements</ion-label>
            <ion-badge slot="end" color="primary">{{ allStats.totalPoints }}</ion-badge>
          </ion-item>
          
          <ion-item button @click="setFilterMode('mine')" :class="{ active: filterMode === 'mine' }">
            <ion-icon :icon="personOutline" slot="start" color="secondary"></ion-icon>
            <ion-label>Mes signalements</ion-label>
            <ion-badge slot="end" color="secondary">{{ myStats.totalPoints }}</ion-badge>
          </ion-item>

          <ion-list-header>
            <ion-label>⚙️ Actions</ion-label>
          </ion-list-header>
          
          <ion-item button @click="openStatsFromMenu">
            <ion-icon :icon="statsChartOutline" slot="start" color="tertiary"></ion-icon>
            <ion-label>Voir récapitulatif</ion-label>
          </ion-item>
          
          <ion-item button @click="openFilterFromMenu">
            <ion-icon :icon="filterOutline" slot="start" color="warning"></ion-icon>
            <ion-label>Filtrer</ion-label>
          </ion-item>
          
          <ion-item button @click="centerOnTana">
            <ion-icon :icon="locateOutline" slot="start" color="success"></ion-icon>
            <ion-label>Recentrer carte</ion-label>
          </ion-item>
          
          <ion-item button @click="logout" class="logout-item">
            <ion-icon :icon="logOutOutline" slot="start" color="danger"></ion-icon>
            <ion-label color="danger">Déconnexion</ion-label>
          </ion-item>
        </ion-list>
      </ion-content>
    </ion-menu>

    <ion-content id="main-content" :scroll-y="false">
      <!-- Conteneur principal flex -->
      <div class="map-page-wrapper">
        <!-- Barre de filtres actifs -->
        <div class="active-filters-bar" v-if="hasActiveFilters">
          <div class="filters-scroll">
            <ion-chip v-if="filterMode === 'mine'" color="secondary" @click="setFilterMode('all')">
              <ion-icon :icon="personOutline"></ion-icon>
              <ion-label>Mes signalements</ion-label>
              <ion-icon :icon="closeCircle"></ion-icon>
            </ion-chip>
            <ion-chip v-if="filterProbleme" color="warning" @click="clearFilterProbleme">
            <span class="chip-emoji">{{ getProblemeIcon(filterProbleme) }}</span>
            <ion-label>{{ getProblemeNom(filterProbleme) }}</ion-label>
            <ion-icon :icon="closeCircle"></ion-icon>
          </ion-chip>
          <ion-chip v-if="filterStatus" :color="getStatusColor(filterStatus)" @click="clearFilterStatus">
            <ion-label>{{ getStatusLabel(filterStatus) }}</ion-label>
            <ion-icon :icon="closeCircle"></ion-icon>
          </ion-chip>
            <ion-chip color="medium" @click="resetFilters">
              <ion-icon :icon="refreshOutline"></ion-icon>
              <ion-label>Effacer</ion-label>
            </ion-chip>
          </div>
        </div>

        <!-- Carte Leaflet -->
        <div id="map" ref="mapContainer" class="map-container"></div>
      </div>

      <!-- Info-bulle signalements -->
      <div class="map-info-bubble" v-if="!loading">
        <span class="bubble-count">{{ filteredSignalements.length }}</span>
        <span class="bubble-label">signalement{{ filteredSignalements.length !== 1 ? 's' : '' }}</span>
      </div>

      <!-- ===== BARRE D'ACTIONS FLOTTANTE ===== -->
      <div class="floating-action-bar">
        <button class="action-btn filter-btn" @click="showFilterModal = true">
          <ion-icon :icon="filterOutline"></ion-icon>
          <span>Filtres</span>
          <span class="action-badge" v-if="activeFiltersCount > 0">{{ activeFiltersCount }}</span>
        </button>
        <button class="action-btn stats-btn" @click="showStatsModal = true">
          <ion-icon :icon="statsChartOutline"></ion-icon>
          <span>Récap</span>
        </button>
        <button class="action-btn add-btn" @click="startAddSignalement">
          <ion-icon :icon="addOutline"></ion-icon>
          <span>Signaler</span>
        </button>
      </div>

      <!-- ================== MODAL NOUVEAU SIGNALEMENT ================== -->
      <ion-modal :is-open="showProblemSelector" @did-dismiss="cancelAddSignalement">
        <ion-header>
          <ion-toolbar color="danger">
            <ion-buttons slot="start">
              <ion-button @click="cancelAddSignalement">
                <ion-icon :icon="arrowBackOutline"></ion-icon>
              </ion-button>
            </ion-buttons>
            <ion-title>Nouveau signalement</ion-title>
          </ion-toolbar>
        </ion-header>
        
        <ion-content>
          <div class="new-signalement-container">
            <!-- Position -->
            <div class="location-box">
              <div class="location-icon">📍</div>
              <div class="location-text">
                <span class="location-title">Position sélectionnée</span>
                <span class="location-coords">{{ newSignalement.latitude.toFixed(4) }}°, {{ newSignalement.longitude.toFixed(4) }}°</span>
              </div>
              <button class="use-location-btn" @click="useMyLocation" :disabled="gettingLocation">
                <ion-spinner v-if="gettingLocation" name="crescent"></ion-spinner>
                <ion-icon v-else :icon="navigateOutline"></ion-icon>
                <span>{{ gettingLocation ? 'GPS...' : 'Ma position' }}</span>
              </button>
            </div>

            <!-- Titre section -->
            <div class="form-section-title">
              <span>Sélectionnez le type de problème</span>
            </div>
            
            <!-- Grille des problèmes -->
            <div class="problems-grid">
              <div 
                v-for="probleme in typesProblemes" 
                :key="probleme.id"
                class="problem-item"
                :class="{ 'selected': newSignalement.problemeId === probleme.id }"
                @click="selectProbleme(probleme)"
              >
                <span class="problem-icon">{{ probleme.icone }}</span>
                <span class="problem-name">{{ probleme.nom }}</span>
                <span class="problem-badge" :class="'priority-' + probleme.priorite">
                  {{ getPrioriteLabel(probleme.priorite) }}
                </span>
              </div>
            </div>

            <!-- Description optionnelle -->
            <div class="form-section-title" v-if="newSignalement.problemeId">
              <span>Description (optionnel)</span>
            </div>
            
            <div class="description-field" v-if="newSignalement.problemeId">
              <textarea 
                v-model="newSignalement.description"
                placeholder="Décrivez brièvement le problème..."
                rows="3"
              ></textarea>
            </div>

            <!-- Section Photos -->
            <div class="form-section-title" v-if="newSignalement.problemeId">
              <span>📷 Photos (optionnel - max 5)</span>
            </div>

            <div class="photos-section" v-if="newSignalement.problemeId">
              <!-- Boutons pour ajouter des photos -->
              <div class="photo-buttons">
                <button 
                  class="photo-btn"
                  @click="takePhoto"
                  :disabled="signalementPhotos.length >= 5"
                >
                  <ion-icon :icon="cameraOutline"></ion-icon>
                  <span>Caméra</span>
                </button>
                <button 
                  class="photo-btn"
                  @click="pickPhotos"
                  :disabled="signalementPhotos.length >= 5"
                >
                  <ion-icon :icon="imagesOutline"></ion-icon>
                  <span>Galerie</span>
                </button>
              </div>

              <!-- Aperçu des photos -->
              <div class="photos-preview" v-if="signalementPhotos.length > 0">
                <div 
                  v-for="photo in signalementPhotos" 
                  :key="photo.id"
                  class="photo-item"
                >
                  <img :src="photo.webviewPath" alt="Photo signalement" />
                  <button class="remove-photo-btn" @click="removePhoto(photo.id)">
                    <ion-icon :icon="closeCircleOutline"></ion-icon>
                  </button>
                </div>
              </div>

              <div class="photos-count" v-if="signalementPhotos.length > 0">
                {{ signalementPhotos.length }}/5 photo{{ signalementPhotos.length > 1 ? 's' : '' }}
              </div>
            </div>

            <!-- Bouton envoyer -->
            <button 
              class="send-button"
              :class="{ 'disabled': !newSignalement.problemeId || uploadingPhotos }"
              :disabled="!newSignalement.problemeId || uploadingPhotos"
              @click="submitSignalement"
            >
              <ion-icon :icon="sendOutline" v-if="!uploadingPhotos"></ion-icon>
              <ion-spinner v-if="uploadingPhotos" name="crescent"></ion-spinner>
              <span>{{ uploadingPhotos ? 'Envoi en cours...' : 'Envoyer le signalement' }}</span>
            </button>
          </div>
        </ion-content>
      </ion-modal>

      <!-- ================== MODAL RÉCAPITULATIF ================== -->
      <ion-modal :is-open="showStatsModal" @did-dismiss="showStatsModal = false">
        <ion-header>
          <ion-toolbar color="tertiary">
            <ion-title>📊 Récapitulatif</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="showStatsModal = false">
                <ion-icon :icon="closeOutline"></ion-icon>
              </ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        
        <ion-content>
          <div class="stats-container">
            <!-- Cartes statistiques -->
            <div class="stats-cards">
              <div class="stat-card card-blue">
                <div class="stat-value">{{ currentStats.totalPoints }}</div>
                <div class="stat-label">📍 Total signalements</div>
              </div>
              <div class="stat-card card-green">
                <div class="stat-value">{{ currentStats.avancementPercent }}%</div>
                <div class="stat-label">✅ Taux résolution</div>
              </div>
            </div>

            <!-- Barre de progression par statut -->
            <div class="status-section">
              <h3 class="section-title">État des signalements</h3>
              <div class="status-bar">
                <div class="status-segment status-new" :style="{ width: getStatusPercent('nouveau') + '%' }">
                  {{ currentStats.nouveaux > 0 ? currentStats.nouveaux : '' }}
                </div>
                <div class="status-segment status-progress" :style="{ width: getStatusPercent('en_cours') + '%' }">
                  {{ currentStats.enCours > 0 ? currentStats.enCours : '' }}
                </div>
                <div class="status-segment status-done" :style="{ width: getStatusPercent('termine') + '%' }">
                  {{ currentStats.termines > 0 ? currentStats.termines : '' }}
                </div>
              </div>
              <div class="status-legend">
                <span class="legend-item"><span class="dot dot-new"></span> Nouveau ({{ currentStats.nouveaux }})</span>
                <span class="legend-item"><span class="dot dot-progress"></span> En cours ({{ currentStats.enCours }})</span>
                <span class="legend-item"><span class="dot dot-done"></span> Terminé ({{ currentStats.termines }})</span>
              </div>
            </div>

            <!-- Par type de problème -->
            <div class="type-section">
              <h3 class="section-title">Par type de problème</h3>
              <div class="type-list">
                <div v-for="probleme in typesProblemes" :key="probleme.id" class="type-row">
                  <span class="type-icon">{{ probleme.icone }}</span>
                  <span class="type-name">{{ probleme.nom }}</span>
                  <span class="type-count">{{ currentStats.parProbleme[probleme.id] || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Entreprises -->
            <div class="enterprise-section">
              <h3 class="section-title">🏢 Entreprises partenaires</h3>
              <div class="enterprise-list">
                <div v-for="entreprise in entreprises" :key="entreprise.id" class="enterprise-row">
                  <div class="enterprise-info">
                    <span class="enterprise-name">{{ entreprise.nom }}</span>
                    <span class="enterprise-spec">{{ entreprise.specialite }}</span>
                  </div>
                  <span class="enterprise-count">{{ getEntrepriseCount(entreprise.id) }}</span>
                </div>
              </div>
            </div>
          </div>
        </ion-content>
      </ion-modal>

      <!-- ================== MODAL FILTRES ================== -->
      <ion-modal :is-open="showFilterModal" @did-dismiss="showFilterModal = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>🔍 Filtres</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="showFilterModal = false">
                <ion-icon :icon="closeOutline"></ion-icon>
              </ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        
        <ion-content>
          <div class="filter-container">
            <!-- Filtre par type -->
            <div class="filter-group">
              <h3 class="filter-group-title">Type de problème</h3>
              <div class="filter-chips">
                <button 
                  class="filter-btn"
                  :class="{ 'active': filterProbleme === '' }"
                  @click="filterProbleme = ''"
                >
                  Tous
                </button>
                <button 
                  v-for="p in typesProblemes"
                  :key="p.id"
                  class="filter-btn"
                  :class="{ 'active': filterProbleme === p.id }"
                  @click="filterProbleme = p.id"
                >
                  {{ p.icone }} {{ p.nom }}
                </button>
              </div>
            </div>

            <!-- Filtre par statut -->
            <div class="filter-group">
              <h3 class="filter-group-title">Statut</h3>
              <div class="filter-chips">
                <button 
                  class="filter-btn"
                  :class="{ 'active': filterStatus === '' }"
                  @click="filterStatus = ''"
                >
                  Tous
                </button>
                <button 
                  class="filter-btn btn-red"
                  :class="{ 'active': filterStatus === 'nouveau' }"
                  @click="filterStatus = 'nouveau'"
                >
                  🔴 Nouveau
                </button>
                <button 
                  class="filter-btn btn-orange"
                  :class="{ 'active': filterStatus === 'en_cours' }"
                  @click="filterStatus = 'en_cours'"
                >
                  🟡 En cours
                </button>
                <button 
                  class="filter-btn btn-green"
                  :class="{ 'active': filterStatus === 'termine' }"
                  @click="filterStatus = 'termine'"
                >
                  🟢 Terminé
                </button>
              </div>
            </div>

            <!-- Boutons d'action -->
            <div class="filter-actions">
              <button class="action-btn btn-reset" @click="resetFilters">
                <ion-icon :icon="refreshOutline"></ion-icon>
                Réinitialiser
              </button>
              <button class="action-btn btn-apply" @click="applyFilters">
                Appliquer ({{ filteredSignalements.length }})
              </button>
            </div>
          </div>
        </ion-content>
      </ion-modal>

      <!-- ================== MODAL DÉTAIL SIGNALEMENT ================== -->
      <ion-modal :is-open="showDetailModal" @did-dismiss="showDetailModal = false">
        <ion-header v-if="selectedSignalement">
          <ion-toolbar :color="getStatusColor(selectedSignalement.status)">
            <ion-title>{{ selectedSignalement.problemeNom }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="showDetailModal = false">
                <ion-icon :icon="closeOutline"></ion-icon>
              </ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        
        <ion-content v-if="selectedSignalement">
          <div class="detail-container">
            <!-- Statut -->
            <div class="detail-status" :class="'status-' + selectedSignalement.status">
              {{ getStatusLabel(selectedSignalement.status) }}
            </div>

            <!-- Infos -->
            <div class="detail-info">
              <div class="info-row">
                <span class="info-icon">📅</span>
                <div class="info-content">
                  <span class="info-label">Date</span>
                  <span class="info-value">{{ formatDate(selectedSignalement.dateCreation) }}</span>
                </div>
              </div>

              <div class="info-row">
                <span class="info-icon">📍</span>
                <div class="info-content">
                  <span class="info-label">Position</span>
                  <span class="info-value">{{ selectedSignalement.latitude.toFixed(5) }}, {{ selectedSignalement.longitude.toFixed(5) }}</span>
                </div>
              </div>

              <div class="info-row" v-if="selectedSignalement.description">
                <span class="info-icon">📝</span>
                <div class="info-content">
                  <span class="info-label">Description</span>
                  <span class="info-value">{{ selectedSignalement.description }}</span>
                </div>
              </div>

              <div class="info-row">
                <span class="info-icon">👤</span>
                <div class="info-content">
                  <span class="info-label">Signalé par</span>
                  <span class="info-value">{{ selectedSignalement.userEmail }}</span>
                </div>
              </div>

              <!-- Photos du signalement -->
              <div class="info-row photos-row" v-if="selectedSignalement.photos && selectedSignalement.photos.length > 0">
                <span class="info-icon">📷</span>
                <div class="info-content">
                  <span class="info-label">Photos ({{ selectedSignalement.photos.length }})</span>
                  <div class="detail-photos-grid">
                    <div 
                      v-for="(photoUrl, index) in selectedSignalement.photos" 
                      :key="index"
                      class="detail-photo-item"
                      @click="openPhotoViewer(photoUrl)"
                    >
                      <img :src="photoUrl" alt="Photo signalement" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Bouton supprimer -->
            <button 
              v-if="selectedSignalement.userId === currentUserId"
              class="delete-btn"
              @click="deleteCurrentSignalement"
            >
              <ion-icon :icon="trashOutline"></ion-icon>
              Supprimer ce signalement
            </button>
          </div>
        </ion-content>
      </ion-modal>

      <!-- Loading -->
      <ion-loading :is-open="loading" message="Chargement..."></ion-loading>

      <!-- Toast -->
      <ion-toast
        :is-open="showToast"
        :message="toastMessage"
        :color="toastColor"
        :duration="3000"
        @did-dismiss="showToast = false"
        position="bottom"
      ></ion-toast>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { menuController, alertController } from '@ionic/vue';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonButtons, IonButton,
  IonIcon, IonFab, IonFabButton, IonModal, IonItem, IonLabel,
  IonList, IonBadge, IonLoading, IonToast, IonMenu, IonMenuButton,
  IonChip, IonListHeader, IonSpinner, isPlatform
} from '@ionic/vue';
import {
  addOutline, closeOutline, statsChartOutline, logOutOutline, trashOutline,
  filterOutline, globeOutline, personOutline, locateOutline,
  closeCircle, sendOutline, locationOutline, refreshOutline, arrowBackOutline,
  cameraOutline, imagesOutline, closeCircleOutline, navigateOutline
} from 'ionicons/icons';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Geolocation } from '@capacitor/geolocation';
import { auth } from '@/firebase';
import { signOut } from 'firebase/auth';
import { signalementService } from '@/services/signalementService';
import { photoService, type SignalementPhoto } from '@/services/photoService';
import { 
  STATUS_CONFIG, 
  PRIORITE_CONFIG, 
  TYPES_PROBLEMES_INITIAUX,
  ENTREPRISES_INITIALES,
  type Signalement, 
  type TypeProbleme, 
  type Entreprise 
} from '@/types/signalement';

const router = useRouter();

// État utilisateur
const currentUserId = ref<string | null>(null);
const userEmail = ref('');
const userInitial = computed(() => userEmail.value ? userEmail.value[0].toUpperCase() : '?');

// Carte
const mapContainer = ref<HTMLElement | null>(null);
let map: L.Map | null = null;
let markersLayer: L.LayerGroup | null = null;
let tempMarker: L.Marker | null = null;

// Filtres
const filterMode = ref<'all' | 'mine'>('all');
const filterProbleme = ref('');
const filterStatus = ref<'' | 'nouveau' | 'en_cours' | 'termine'>('');
const showFilterModal = ref(false);

const hasActiveFilters = computed(() => 
  filterMode.value !== 'all' || filterProbleme.value !== '' || filterStatus.value !== ''
);

const activeFiltersCount = computed(() => {
  let count = 0;
  if (filterMode.value !== 'all') count++;
  if (filterProbleme.value) count++;
  if (filterStatus.value) count++;
  return count;
});

// Données
const allSignalements = ref<Signalement[]>([]);
const mySignalements = ref<Signalement[]>([]);
const typesProblemes = ref<TypeProbleme[]>(TYPES_PROBLEMES_INITIAUX);
const entreprises = ref<Entreprise[]>(ENTREPRISES_INITIALES);
const loading = ref(true);

// Unsubscribes
let unsubscribeAll: (() => void) | null = null;
let unsubscribeMine: (() => void) | null = null;
let unsubscribeProblemes: (() => void) | null = null;
let unsubscribeEntreprises: (() => void) | null = null;

// Stats calculées
const allStats = computed(() => signalementService.calculateStats(allSignalements.value));
const myStats = computed(() => signalementService.calculateStats(mySignalements.value));
const currentStats = computed(() => filterMode.value === 'all' ? allStats.value : myStats.value);

// Signalements filtrés
const filteredSignalements = computed(() => {
  let signalements = filterMode.value === 'all' ? allSignalements.value : mySignalements.value;
  
  if (filterProbleme.value) {
    signalements = signalements.filter(s => s.problemeId === filterProbleme.value);
  }
  
  if (filterStatus.value) {
    signalements = signalements.filter(s => s.status === filterStatus.value);
  }
  
  return signalements;
});

// Modals
const showProblemSelector = ref(false);
const showStatsModal = ref(false);
const showDetailModal = ref(false);
const selectedSignalement = ref<Signalement | null>(null);

// Nouveau signalement
const newSignalement = ref({
  latitude: -18.8792,
  longitude: 47.5079,
  problemeId: '',
  problemeNom: '',
  description: ''
});

// Photos pour le signalement
const signalementPhotos = ref<SignalementPhoto[]>([]);
const uploadingPhotos = ref(false);
const gettingLocation = ref(false);

// Toast
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Coordonnées de Tananarive
const TANA_CENTER: [number, number] = [-18.8792, 47.5079];
const TANA_ZOOM = 13;

// Initialisation de la carte
const initMap = () => {
  if (!mapContainer.value || map) return;

  map = L.map(mapContainer.value).setView(TANA_CENTER, TANA_ZOOM);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap',
    maxZoom: 19
  }).addTo(map);

  markersLayer = L.layerGroup().addTo(map);

  map.on('click', (e: L.LeafletMouseEvent) => {
    const { lat, lng } = e.latlng;
    newSignalement.value.latitude = lat;
    newSignalement.value.longitude = lng;

    if (tempMarker) {
      tempMarker.setLatLng([lat, lng]);
    } else {
      tempMarker = L.marker([lat, lng], {
        icon: createIcon('#9900ff')
      }).addTo(map!);
    }

    showProblemSelector.value = true;
  });
};

const createIcon = (color: string) => {
  return L.divIcon({
    className: 'custom-marker',
    html: `<div style="
      background-color: ${color};
      width: 30px;
      height: 30px;
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg);
      border: 3px solid white;
      box-shadow: 0 2px 8px rgba(0,0,0,0.4);
    "></div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 30]
  });
};

const updateMarkers = () => {
  if (!markersLayer || !map) return;
  markersLayer.clearLayers();

  filteredSignalements.value.forEach(s => {
    const color = STATUS_CONFIG[s.status]?.color || '#888888';
    const marker = L.marker([s.latitude, s.longitude], {
      icon: createIcon(color)
    });

    marker.on('click', () => {
      selectedSignalement.value = s;
      showDetailModal.value = true;
    });

    marker.bindTooltip(`${s.problemeNom}`, {
      permanent: false,
      direction: 'top'
    });

    markersLayer!.addLayer(marker);
  });
};

// Chargement des données
const loadData = () => {
  loading.value = true;

  // Timeout de sécurité (3 secondes max)
  const loadingTimeout = setTimeout(() => {
    if (loading.value) {
      console.log('Timeout loading - utilisation données locales');
      loading.value = false;
      updateMarkers();
    }
  }, 3000);

  unsubscribeProblemes = signalementService.subscribeToTypesProblemes((data) => {
    if (data.length > 0) {
      typesProblemes.value = data;
    }
  });

  unsubscribeEntreprises = signalementService.subscribeToEntreprises((data) => {
    if (data.length > 0) {
      entreprises.value = data;
    }
  });

  unsubscribeAll = signalementService.subscribeToAllSignalements((data) => {
    clearTimeout(loadingTimeout);
    allSignalements.value = data;
    loading.value = false;
    updateMarkers();
  });

  if (currentUserId.value) {
    unsubscribeMine = signalementService.subscribeToUserSignalements(currentUserId.value, (data) => {
      mySignalements.value = data;
      if (filterMode.value === 'mine') {
        updateMarkers();
      }
    });
  }
};

// Watchers
watch([filterMode, filterProbleme, filterStatus], () => {
  updateMarkers();
});

// Redimensionner la carte quand les filtres changent
watch(hasActiveFilters, () => {
  setTimeout(() => {
    if (map) {
      map.invalidateSize();
    }
  }, 100);
});

// Actions filtres
const setFilterMode = (mode: 'all' | 'mine') => {
  filterMode.value = mode;
  menuController.close();
};

const clearFilterProbleme = () => {
  filterProbleme.value = '';
};

const clearFilterStatus = () => {
  filterStatus.value = '';
};

const resetFilters = () => {
  filterMode.value = 'all';
  filterProbleme.value = '';
  filterStatus.value = '';
};

const applyFilters = () => {
  showFilterModal.value = false;
};

const openStatsFromMenu = () => {
  menuController.close();
  setTimeout(() => {
    showStatsModal.value = true;
  }, 300);
};

const openFilterFromMenu = () => {
  menuController.close();
  setTimeout(() => {
    showFilterModal.value = true;
  }, 300);
};

const centerOnTana = () => {
  if (map) {
    map.setView(TANA_CENTER, TANA_ZOOM);
  }
  menuController.close();
};

const startAddSignalement = () => {
  if (map) {
    const center = map.getCenter();
    newSignalement.value.latitude = center.lat;
    newSignalement.value.longitude = center.lng;
    
    if (tempMarker) {
      tempMarker.setLatLng([center.lat, center.lng]);
    } else {
      tempMarker = L.marker([center.lat, center.lng], {
        icon: createIcon('#9900ff')
      }).addTo(map);
    }
  }
  showProblemSelector.value = true;
};

const selectProbleme = (probleme: TypeProbleme) => {
  newSignalement.value.problemeId = probleme.id;
  newSignalement.value.problemeNom = probleme.nom;
};

const cancelAddSignalement = () => {
  showProblemSelector.value = false;
  if (tempMarker && map) {
    map.removeLayer(tempMarker);
    tempMarker = null;
  }
  resetNewSignalement();
};

const submitSignalement = async () => {
  if (!newSignalement.value.problemeId) return;
  
  try {
    uploadingPhotos.value = true;
    
    await signalementService.createSignalement({
      latitude: newSignalement.value.latitude,
      longitude: newSignalement.value.longitude,
      problemeId: newSignalement.value.problemeId,
      problemeNom: newSignalement.value.problemeNom,
      description: newSignalement.value.description,
      photos: signalementPhotos.value.length > 0 ? signalementPhotos.value : undefined
    });

    showToast.value = true;
    toastMessage.value = '✅ Signalement envoyé!';
    toastColor.value = 'success';

    showProblemSelector.value = false;
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    resetNewSignalement();

  } catch (error: any) {
    showToast.value = true;
    toastMessage.value = `❌ Erreur: ${error.message}`;
    toastColor.value = 'danger';
  } finally {
    uploadingPhotos.value = false;
  }
};

// Fonctions pour gérer les photos
const takePhoto = async () => {
  try {
    const photo = await photoService.takePhoto();
    if (photo) {
      signalementPhotos.value.push(photo);
      showToast.value = true;
      toastMessage.value = '📸 Photo ajoutée';
      toastColor.value = 'success';
    }
    // Si photo est null, l'utilisateur a annulé - pas de message d'erreur
  } catch (error: any) {
    console.error('Erreur caméra:', error);
    // Ne pas afficher d'erreur si c'est une annulation
  }
};

const pickPhotos = async () => {
  try {
    const photos = await photoService.pickMultipleFromGallery();
    if (photos.length > 0) {
      // Limiter à 5 photos au total
      const remaining = 5 - signalementPhotos.value.length;
      if (remaining > 0) {
        signalementPhotos.value.push(...photos.slice(0, remaining));
        showToast.value = true;
        toastMessage.value = `📸 ${Math.min(photos.length, remaining)} photo(s) ajoutée(s)`;
        toastColor.value = 'success';
      }
      if (signalementPhotos.value.length >= 5) {
        showToast.value = true;
        toastMessage.value = '📸 Maximum 5 photos atteint';
        toastColor.value = 'warning';
      }
    }
    // Si photos est vide, l'utilisateur a annulé - pas de message d'erreur
  } catch (error: any) {
    console.error('Erreur galerie:', error);
    // Ne pas afficher d'erreur si c'est une annulation
  }
};

const removePhoto = (photoId: string) => {
  signalementPhotos.value = signalementPhotos.value.filter(p => p.id !== photoId);
};

// Ouvrir une photo en plein écran
const openPhotoViewer = (photoUrl: string) => {
  window.open(photoUrl, '_blank');
};

// Utiliser ma position GPS (compatible web et mobile)
const useMyLocation = async () => {
  try {
    gettingLocation.value = true;
    
    let latitude: number;
    let longitude: number;
    
    // Sur mobile (Android/iOS), utiliser Capacitor Geolocation
    if (isPlatform('hybrid')) {
      // Demander les permissions
      const permStatus = await Geolocation.checkPermissions();
      if (permStatus.location !== 'granted') {
        const requestStatus = await Geolocation.requestPermissions();
        if (requestStatus.location !== 'granted') {
          throw new Error('Permission de localisation refusée');
        }
      }
      
      // Récupérer la position
      const position = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 15000
      });
      
      latitude = position.coords.latitude;
      longitude = position.coords.longitude;
    } else {
      // Sur web, utiliser l'API native du navigateur
      if (!navigator.geolocation) {
        throw new Error('La géolocalisation n\'est pas supportée par votre navigateur');
      }
      
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(
          resolve,
          (error) => {
            switch (error.code) {
              case error.PERMISSION_DENIED:
                reject(new Error('Permission de localisation refusée. Autorisez ce site à accéder à votre position.'));
                break;
              case error.POSITION_UNAVAILABLE:
                reject(new Error('Position indisponible'));
                break;
              case error.TIMEOUT:
                reject(new Error('Délai d\'attente dépassé'));
                break;
              default:
                reject(new Error('Erreur de géolocalisation'));
            }
          },
          {
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 0
          }
        );
      });
      
      latitude = position.coords.latitude;
      longitude = position.coords.longitude;
    }
    
    // Mettre à jour le signalement
    newSignalement.value.latitude = latitude;
    newSignalement.value.longitude = longitude;
    
    // Mettre à jour le marqueur temporaire
    if (tempMarker && map) {
      tempMarker.setLatLng([latitude, longitude]);
    } else if (map) {
      tempMarker = L.marker([latitude, longitude], {
        icon: createIcon('#9900ff')
      }).addTo(map);
    }
    
    // Centrer la carte sur la position
    if (map) {
      map.setView([latitude, longitude], 17);
    }
    
    showToast.value = true;
    toastMessage.value = '📍 Position mise à jour!';
    toastColor.value = 'success';
    
  } catch (error: any) {
    console.error('Erreur géolocalisation:', error);
    showToast.value = true;
    toastMessage.value = `❌ ${error.message || 'Impossible d\'obtenir la position'}`;
    toastColor.value = 'danger';
  } finally {
    gettingLocation.value = false;
  }
};

const deleteCurrentSignalement = async () => {
  if (!selectedSignalement.value) return;

  const alert = await alertController.create({
    header: 'Confirmer',
    message: 'Supprimer ce signalement ?',
    buttons: [
      { text: 'Non', role: 'cancel' },
      {
        text: 'Oui',
        role: 'destructive',
        handler: async () => {
          try {
            await signalementService.deleteSignalement(selectedSignalement.value!.id!);
            showDetailModal.value = false;
            showToast.value = true;
            toastMessage.value = '✅ Supprimé';
            toastColor.value = 'success';
          } catch (error: any) {
            showToast.value = true;
            toastMessage.value = `❌ Erreur`;
            toastColor.value = 'danger';
          }
        }
      }
    ]
  });

  await alert.present();
};

const resetNewSignalement = () => {
  newSignalement.value = {
    latitude: TANA_CENTER[0],
    longitude: TANA_CENTER[1],
    problemeId: '',
    problemeNom: '',
    description: ''
  };
  signalementPhotos.value = [];
};

// Utilitaires
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const getStatusColor = (status?: string) => {
  switch (status) {
    case 'nouveau': return 'danger';
    case 'en_cours': return 'warning';
    case 'termine': return 'success';
    default: return 'medium';
  }
};

const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'nouveau': return '🔴 Nouveau';
    case 'en_cours': return '🟡 En cours';
    case 'termine': return '🟢 Terminé';
    default: return status;
  }
};

const getStatusPercent = (status: string) => {
  const total = currentStats.value.totalPoints;
  if (total === 0) return 0;
  switch (status) {
    case 'nouveau': return (currentStats.value.nouveaux / total) * 100;
    case 'en_cours': return (currentStats.value.enCours / total) * 100;
    case 'termine': return (currentStats.value.termines / total) * 100;
    default: return 0;
  }
};

const getPrioriteLabel = (priorite: number) => {
  return PRIORITE_CONFIG[priorite as keyof typeof PRIORITE_CONFIG]?.label || '';
};

const getProblemeNom = (id: string) => {
  return typesProblemes.value.find(p => p.id === id)?.nom || id;
};

const getProblemeIcon = (id: string) => {
  return typesProblemes.value.find(p => p.id === id)?.icone || '📌';
};

const getEntrepriseCount = (entrepriseId: string): number => {
  const total = currentStats.value.totalPoints;
  const idx = entreprises.value.findIndex(e => e.id === entrepriseId);
  return Math.floor(total / (entreprises.value.length || 1)) + (idx < total % (entreprises.value.length || 1) ? 1 : 0);
};

// Déconnexion
const logout = async () => {
  try {
    await signOut(auth);
    router.push('/login');
  } catch (error) {
    console.error('Erreur:', error);
  }
};

// Lifecycle
onMounted(() => {
  auth.onAuthStateChanged((user) => {
    if (user) {
      currentUserId.value = user.uid;
      userEmail.value = user.email || '';
      loadData();
    } else {
      router.push('/login');
    }
  });

  setTimeout(() => {
    initMap();
  }, 100);
});

onUnmounted(() => {
  if (unsubscribeAll) unsubscribeAll();
  if (unsubscribeMine) unsubscribeMine();
  if (unsubscribeProblemes) unsubscribeProblemes();
  if (unsubscribeEntreprises) unsubscribeEntreprises();
  if (map) {
    map.remove();
    map = null;
  }
});
</script>

<style scoped>
/* ===== HEADER ===== */
.header-title {
  font-weight: 600;
  font-size: 18px;
}

.header-btn {
  position: relative;
}

.filter-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #ef4444;
  color: white;
  font-size: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* ===== MENU ===== */
.menu-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  background: linear-gradient(135deg, #3880ff, #5260ff);
  color: white;
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: bold;
}

.user-email {
  font-weight: 500;
  font-size: 14px;
}

.user-status {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 2px;
}

.menu-list ion-item {
  --padding-start: 16px;
  margin: 4px 8px;
  border-radius: 8px;
}

.menu-list ion-item.active {
  --background: rgba(56, 128, 255, 0.1);
}

/* ===== FILTRES ACTIFS ===== */
.active-filters-bar {
  background: #f8f9fa;
  padding: 10px 12px;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.filters-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.filters-scroll::-webkit-scrollbar {
  display: none;
}

.chip-emoji {
  margin-right: 4px;
}

/* ===== CONTENEUR PAGE ===== */
.map-page-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

/* ===== CARTE ===== */
.map-container {
  flex: 1;
  width: 100%;
  min-height: 0;
  z-index: 1;
}

.map-info-bubble {
  position: fixed;
  top: 70px;
  left: 12px;
  background: white;
  padding: 10px 16px;
  border-radius: 25px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bubble-count {
  font-weight: 700;
  font-size: 20px;
  color: #3880ff;
}

.bubble-label {
  font-size: 13px;
  color: #666;
}

/* ===== BARRE D'ACTIONS FLOTTANTE ===== */
.floating-action-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 10px 16px;
  background: white;
  border-radius: 50px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.25);
  z-index: 1001;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 16px;
  border: none;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.action-btn ion-icon {
  font-size: 22px;
}

.action-btn span {
  font-size: 11px;
}

.filter-btn {
  background: #fff3e0;
  color: #f57c00;
}

.filter-btn:active {
  background: #ffe0b2;
}

.stats-btn {
  background: #e8f5e9;
  color: #388e3c;
}

.stats-btn:active {
  background: #c8e6c9;
}

.add-btn {
  background: #ef4444;
  color: white;
}

.add-btn:active {
  background: #dc2626;
}

.action-badge {
  position: absolute;
  top: 2px;
  right: 8px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* ===== MODAL NOUVEAU SIGNALEMENT ===== */
.new-signalement-container {
  padding: 20px;
  background: #f5f6f8;
  min-height: 100%;
}

.location-box {
  display: flex;
  align-items: center;
  gap: 14px;
  background: white;
  padding: 18px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.location-icon {
  font-size: 32px;
}

.location-text {
  flex: 1;
}

.use-location-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #3880ff, #5260ff);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 80px;
}

.use-location-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(56, 128, 255, 0.4);
}

.use-location-btn:disabled {
  opacity: 0.7;
  cursor: wait;
}

.use-location-btn ion-icon {
  font-size: 20px;
}

.use-location-btn ion-spinner {
  width: 20px;
  height: 20px;
}

.use-location-btn span {
  font-size: 11px;
  font-weight: 600;
}

.location-title {
  display: block;
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.location-coords {
  display: block;
  font-size: 13px;
  color: #888;
  margin-top: 3px;
}

.form-section-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
  margin-bottom: 14px;
  padding-left: 4px;
}

.problems-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.problem-item {
  background: white;
  border: 3px solid transparent;
  border-radius: 16px;
  padding: 18px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.problem-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.problem-item.selected {
  border-color: #ef4444;
  background: #fef2f2;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2);
}

.problem-icon {
  display: block;
  font-size: 36px;
  margin-bottom: 10px;
}

.problem-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.problem-badge {
  display: inline-block;
  font-size: 10px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.priority-1 {
  background: #fee2e2;
  color: #dc2626;
}

.priority-2 {
  background: #fef3c7;
  color: #d97706;
}

.priority-3 {
  background: #e5e7eb;
  color: #6b7280;
}

.description-field {
  margin-bottom: 24px;
}

.description-field textarea {
  width: 100%;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 14px;
  font-size: 15px;
  resize: none;
  font-family: inherit;
  background: white;
  transition: border-color 0.2s;
}

.description-field textarea:focus {
  outline: none;
  border-color: #3880ff;
}

.send-button {
  width: 100%;
  padding: 18px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4);
}

.send-button:active {
  transform: scale(0.98);
}

.send-button.disabled {
  background: #d1d5db;
  box-shadow: none;
  cursor: not-allowed;
}

/* ===== SECTION PHOTOS ===== */
.photos-section {
  margin-bottom: 24px;
}

.photo-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.photo-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: white;
  border: 2px dashed #d1d5db;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.photo-btn:hover:not(:disabled) {
  border-color: #3880ff;
  background: #f0f7ff;
}

.photo-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.photo-btn ion-icon {
  font-size: 28px;
  color: #3880ff;
}

.photo-btn span {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.photos-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-photo-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-photo-btn:hover {
  background: #ef4444;
}

.remove-photo-btn ion-icon {
  font-size: 18px;
}

.photos-count {
  text-align: center;
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

/* ===== MODAL RÉCAPITULATIF ===== */
.stats-container {
  padding: 20px;
  background: #f5f6f8;
  min-height: 100%;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.card-blue {
  border-top: 4px solid #3880ff;
}

.card-green {
  border-top: 4px solid #22c55e;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 6px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.status-section, .type-section, .enterprise-section {
  background: white;
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.status-bar {
  display: flex;
  height: 32px;
  border-radius: 16px;
  overflow: hidden;
  background: #e5e7eb;
  margin-bottom: 14px;
}

.status-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 13px;
  font-weight: 600;
  transition: width 0.3s;
}

.status-new { background: #ef4444; }
.status-progress { background: #f59e0b; }
.status-done { background: #22c55e; }

.status-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot-new { background: #ef4444; }
.dot-progress { background: #f59e0b; }
.dot-done { background: #22c55e; }

.type-list, .enterprise-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.type-row, .enterprise-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.type-row:last-child, .enterprise-row:last-child {
  border-bottom: none;
}

.type-icon {
  font-size: 24px;
  margin-right: 12px;
}

.type-name, .enterprise-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.type-count, .enterprise-count {
  background: #e5e7eb;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.enterprise-info {
  flex: 1;
}

.enterprise-spec {
  display: block;
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.enterprise-count {
  background: #3880ff;
  color: white;
}

/* ===== MODAL FILTRES ===== */
.filter-container {
  padding: 20px;
  background: #f5f6f8;
  min-height: 100%;
}

.filter-group {
  background: white;
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.filter-group-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-btn {
  padding: 10px 18px;
  background: #e5e7eb;
  border: 2px solid transparent;
  border-radius: 25px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn.active {
  background: #3880ff;
  color: white;
}

.filter-btn.btn-red.active { background: #ef4444; }
.filter-btn.btn-orange.active { background: #f59e0b; }
.filter-btn.btn-green.active { background: #22c55e; }

.filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.action-btn {
  flex: 1;
  padding: 16px;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-reset {
  background: #e5e7eb;
  color: #374151;
}

.btn-apply {
  background: #3880ff;
  color: white;
}

/* ===== MODAL DÉTAIL ===== */
.detail-container {
  padding: 0;
  background: #f5f6f8;
  min-height: 100%;
}

.detail-status {
  padding: 16px;
  text-align: center;
  font-weight: 600;
  font-size: 15px;
}

.detail-status.status-nouveau { background: #fef2f2; color: #dc2626; }
.detail-status.status-en_cours { background: #fffbeb; color: #d97706; }
.detail-status.status-termine { background: #f0fdf4; color: #16a34a; }

.detail-info {
  background: white;
  margin: 16px;
  border-radius: 16px;
  overflow: hidden;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-icon {
  font-size: 22px;
}

.info-label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 2px;
}

.info-value {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: calc(100% - 32px);
  margin: 16px;
  padding: 16px;
  background: #fef2f2;
  color: #dc2626;
  border: 2px solid #fecaca;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.delete-btn:active {
  background: #fee2e2;
}

/* ===== PHOTOS DANS DETAIL ===== */
.photos-row .info-content {
  flex: 1;
  width: 100%;
}

.detail-photos-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.detail-photo-item {
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  transition: transform 0.2s;
}

.detail-photo-item:hover {
  transform: scale(1.05);
}

.detail-photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ===== MARKERS ===== */
:deep(.custom-marker) {
  background: transparent !important;
  border: none !important;
}

/* ===== RESPONSIVE ===== */
@media (min-width: 500px) {
  .problems-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 700px) {
  .problems-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
