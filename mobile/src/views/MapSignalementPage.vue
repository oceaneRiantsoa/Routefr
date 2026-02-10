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
            <ion-icon :icon="mapOutline" class="header-icon"></ion-icon>
            <span>Signalement Tana</span>
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
            <div class="user-status"><ion-icon :icon="ellipseOutline" class="status-dot"></ion-icon> Connecté</div>
          </div>
        </div>

        <ion-list lines="none" class="menu-list">
          <ion-list-header>
            <ion-label><ion-icon :icon="barChartOutline" class="section-icon"></ion-icon> Affichage</ion-label>
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
            <ion-label><ion-icon :icon="settingsOutline" class="section-icon"></ion-icon> Actions</ion-label>
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
              <div class="location-icon"><ion-icon :icon="locationOutline"></ion-icon></div>
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
                <ion-icon :icon="getIonIcon(probleme.icone)" class="problem-icon"></ion-icon>
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
              <ion-icon :icon="imageOutline" class="section-icon"></ion-icon>
              <span>Photos (optionnel - max 5)</span>
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
            <ion-title><ion-icon :icon="barChartOutline" style="margin-right:8px;vertical-align:middle"></ion-icon>Récapitulatif</ion-title>
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
                <div class="stat-label"><ion-icon :icon="pinOutline" class="stat-icon"></ion-icon> Total signalements</div>
              </div>
              <div class="stat-card card-green">
                <div class="stat-value">{{ currentStats.avancementPercent }}%</div>
                <div class="stat-label"><ion-icon :icon="checkmarkCircleOutline" class="stat-icon"></ion-icon> Taux résolution</div>
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
                  <ion-icon :icon="getIonIcon(probleme.icone)" class="type-icon"></ion-icon>
                  <span class="type-name">{{ probleme.nom }}</span>
                  <span class="type-count">{{ currentStats.parProbleme[probleme.id] || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Entreprises -->
            <div class="enterprise-section">
              <h3 class="section-title"><ion-icon :icon="businessOutline" class="section-icon"></ion-icon> Entreprises partenaires</h3>
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
            <ion-title><ion-icon :icon="filterOutline" style="margin-right:8px;vertical-align:middle"></ion-icon>Filtres</ion-title>
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
                  <ion-icon :icon="getIonIcon(p.icone)" style="margin-right:4px;vertical-align:middle;"></ion-icon> {{ p.nom }}
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
                  Nouveau
                </button>
                <button 
                  class="filter-btn btn-orange"
                  :class="{ 'active': filterStatus === 'en_cours' }"
                  @click="filterStatus = 'en_cours'"
                >
                  En cours
                </button>
                <button 
                  class="filter-btn btn-green"
                  :class="{ 'active': filterStatus === 'termine' }"
                  @click="filterStatus = 'termine'"
                >
                  Terminé
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
                <span class="info-icon"><ion-icon :icon="calendarOutline"></ion-icon></span>
                <div class="info-content">
                  <span class="info-label">Date</span>
                  <span class="info-value">{{ formatDate(selectedSignalement.dateCreation) }}</span>
                </div>
              </div>

              <div class="info-row">
                <span class="info-icon"><ion-icon :icon="pinOutline"></ion-icon></span>
                <div class="info-content">
                  <span class="info-label">Position</span>
                  <span class="info-value">{{ selectedSignalement.latitude.toFixed(5) }}, {{ selectedSignalement.longitude.toFixed(5) }}</span>
                </div>
              </div>

              <div class="info-row" v-if="selectedSignalement.description">
                <span class="info-icon"><ion-icon :icon="documentTextOutline"></ion-icon></span>
                <div class="info-content">
                  <span class="info-label">Description</span>
                  <span class="info-value">{{ selectedSignalement.description }}</span>
                </div>
              </div>

              <div class="info-row">
                <span class="info-icon"><ion-icon :icon="personCircleOutline"></ion-icon></span>
                <div class="info-content">
                  <span class="info-label">Signalé par</span>
                  <span class="info-value">{{ selectedSignalement.userEmail }}</span>
                </div>
              </div>

              <!-- Photos du signalement -->
              <div class="info-row photos-row" v-if="selectedSignalement.photos && selectedSignalement.photos.length > 0">
                <span class="info-icon"><ion-icon :icon="imageOutline"></ion-icon></span>
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
  cameraOutline, imagesOutline, closeCircleOutline, navigateOutline,
  mapOutline, barChartOutline, settingsOutline, checkmarkCircleOutline,
  ellipseOutline, timeOutline, calendarOutline, pinOutline, createOutline,
  personCircleOutline, businessOutline, alertCircleOutline,
  warningOutline, constructOutline, waterOutline, flashOutline,
  walkOutline, trashBinOutline, megaphoneOutline, flagOutline,
  layersOutline, shieldCheckmarkOutline, documentTextOutline, imageOutline
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
    toastMessage.value = 'Signalement envoyé !';
    toastColor.value = 'success';

    showProblemSelector.value = false;
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    resetNewSignalement();

  } catch (error: any) {
    showToast.value = true;
    toastMessage.value = `Erreur: ${error.message}`;
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
      toastMessage.value = 'Photo ajoutée';
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
        toastMessage.value = `${Math.min(photos.length, remaining)} photo(s) ajoutée(s)`;
        toastColor.value = 'success';
      }
      if (signalementPhotos.value.length >= 5) {
        showToast.value = true;
        toastMessage.value = 'Maximum 5 photos atteint';
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
    toastMessage.value = 'Position mise à jour';
    toastColor.value = 'success';
    
  } catch (error: any) {
    console.error('Erreur géolocalisation:', error);
    showToast.value = true;
    toastMessage.value = `${error.message || 'Impossible d\'obtenir la position'}`;
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
            toastMessage.value = 'Supprimé';
            toastColor.value = 'success';
          } catch (error: any) {
            showToast.value = true;
            toastMessage.value = 'Erreur lors de la suppression';
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
    case 'nouveau': return 'Nouveau';
    case 'en_cours': return 'En cours';
    case 'termine': return 'Terminé';
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
  return typesProblemes.value.find(p => p.id === id)?.icone || 'pin-outline';
};

// Mapping des noms d'icônes vers les objets Ionicon importés
const ionIconMap: Record<string, string> = {
  'construct-outline': constructOutline,
  'water-outline': waterOutline,
  'warning-outline': warningOutline,
  'flash-outline': flashOutline,
  'walk-outline': walkOutline,
  'trash-bin-outline': trashBinOutline,
  'flag-outline': flagOutline,
  'pin-outline': pinOutline,
  'alert-circle-outline': alertCircleOutline,
  'time-outline': timeOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
};

const getIonIcon = (name: string): string => {
  return ionIconMap[name] || pinOutline;
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
/* ============================================================
   DESIGN PROFESSIONNEL — Couleurs unies, Ionicons partout
   ============================================================ */

/* ===== HEADER ===== */
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 17px;
}

.header-icon {
  font-size: 20px;
}

.header-btn {
  position: relative;
}

.filter-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #dc2626;
  color: #fff;
  font-size: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

/* ===== MENU LATÉRAL ===== */
.menu-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px;
  background: #1a56db;
  color: #ffffff;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}

.user-email {
  font-weight: 500;
  font-size: 14px;
}

.user-status {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  font-size: 10px;
  color: #4ade80;
}

.section-icon {
  font-size: 16px;
  vertical-align: middle;
  margin-right: 4px;
}

.menu-list ion-item {
  --padding-start: 16px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
}

.menu-list ion-item.active {
  --background: rgba(26, 86, 219, 0.08);
}

/* ===== FILTRES ACTIFS ===== */
.active-filters-bar {
  background: #f1f5f9;
  padding: 8px 12px;
  border-bottom: 1px solid #e2e8f0;
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
  top: 68px;
  left: 12px;
  background: #ffffff;
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 6px;
}

.bubble-count {
  font-weight: 700;
  font-size: 18px;
  color: #1a56db;
}

.bubble-label {
  font-size: 12px;
  color: #64748b;
}

/* ===== BARRE D'ACTIONS FLOTTANTE ===== */
.floating-action-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.16);
  z-index: 1001;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 10px 18px;
  border: none;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  transition: all 0.15s;
}

.action-btn ion-icon {
  font-size: 20px;
}

.action-btn span {
  font-size: 10px;
  letter-spacing: 0.2px;
}

.filter-btn {
  background: #f1f5f9;
  color: #475569;
}

.filter-btn:active {
  background: #e2e8f0;
}

.stats-btn {
  background: #f1f5f9;
  color: #475569;
}

.stats-btn:active {
  background: #e2e8f0;
}

.add-btn {
  background: #1a56db;
  color: #ffffff;
}

.add-btn:active {
  background: #164bc1;
}

.action-badge {
  position: absolute;
  top: 2px;
  right: 10px;
  background: #dc2626;
  color: #fff;
  font-size: 9px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

/* ===== MODAL NOUVEAU SIGNALEMENT ===== */
.new-signalement-container {
  padding: 20px;
  background: #f1f5f9;
  min-height: 100%;
}

.location-box {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #ffffff;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.location-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.location-icon ion-icon {
  font-size: 22px;
  color: #1a56db;
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
  background: #1a56db;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  min-width: 76px;
}

.use-location-btn:hover:not(:disabled) {
  background: #164bc1;
}

.use-location-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.use-location-btn ion-icon {
  font-size: 18px;
}

.use-location-btn ion-spinner {
  width: 18px;
  height: 18px;
}

.use-location-btn span {
  font-size: 10px;
  font-weight: 600;
}

.location-title {
  display: block;
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}

.location-coords {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  font-family: 'Courier New', monospace;
}

.form-section-title {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
  margin-bottom: 12px;
  padding-left: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.problems-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.problem-item {
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
}

.problem-item:active {
  transform: scale(0.97);
}

.problem-item.selected {
  border-color: #1a56db;
  background: #eff6ff;
}

.problem-icon {
  display: block;
  font-size: 28px;
  margin-bottom: 8px;
  color: #1a56db;
}

.problem-name {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
}

.problem-badge {
  display: inline-block;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.priority-1 {
  background: #fef2f2;
  color: #dc2626;
}

.priority-2 {
  background: #fffbeb;
  color: #d97706;
}

.priority-3 {
  background: #f1f5f9;
  color: #64748b;
}

.description-field {
  margin-bottom: 20px;
}

.description-field textarea {
  width: 100%;
  padding: 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
  background: #ffffff;
  transition: border-color 0.2s;
  box-sizing: border-box;
  outline: none;
}

.description-field textarea:focus {
  border-color: #1a56db;
  box-shadow: 0 0 0 3px rgba(26, 86, 219, 0.08);
}

.send-button {
  width: 100%;
  padding: 16px;
  background: #1a56db;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.send-button:active:not(.disabled) {
  transform: scale(0.98);
  background: #164bc1;
}

.send-button.disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

/* ===== SECTION PHOTOS ===== */
.photos-section {
  margin-bottom: 20px;
}

.photo-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.photo-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px;
  background: #ffffff;
  border: 1.5px dashed #cbd5e1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.photo-btn:hover:not(:disabled) {
  border-color: #1a56db;
  background: #eff6ff;
}

.photo-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.photo-btn ion-icon {
  font-size: 24px;
  color: #1a56db;
}

.photo-btn span {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.photos-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
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
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.remove-photo-btn:active {
  background: #dc2626;
}

.remove-photo-btn ion-icon {
  font-size: 16px;
}

.photos-count {
  text-align: center;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

/* ===== MODAL RÉCAPITULATIF ===== */
.stats-container {
  padding: 20px;
  background: #f1f5f9;
  min-height: 100%;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  border-left: 4px solid transparent;
}

.card-blue {
  border-left-color: #1a56db;
}

.card-green {
  border-left-color: #059669;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.stat-icon {
  font-size: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-section, .type-section, .enterprise-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.status-bar {
  display: flex;
  height: 28px;
  border-radius: 8px;
  overflow: hidden;
  background: #e2e8f0;
  margin-bottom: 12px;
}

.status-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  transition: width 0.3s;
}

.status-new { background: #dc2626; }
.status-progress { background: #d97706; }
.status-done { background: #059669; }

.status-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot-new { background: #dc2626; }
.dot-progress { background: #d97706; }
.dot-done { background: #059669; }

.type-list, .enterprise-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.type-row, .enterprise-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.type-row:last-child, .enterprise-row:last-child {
  border-bottom: none;
}

.type-icon {
  font-size: 20px;
  margin-right: 10px;
  width: 28px;
  text-align: center;
  color: #1a56db;
}

.type-name, .enterprise-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #0f172a;
}

.type-count {
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.enterprise-info {
  flex: 1;
}

.enterprise-spec {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.enterprise-count {
  background: #1a56db;
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

/* ===== MODAL FILTRES ===== */
.filter-container {
  padding: 20px;
  background: #f1f5f9;
  min-height: 100%;
}

.filter-group {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.filter-group-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chips .filter-btn {
  padding: 8px 16px;
  background: #f1f5f9;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  color: #475569;
  font-weight: 500;
  flex-direction: row;
}

.filter-chips .filter-btn.active {
  background: #1a56db;
  color: #ffffff;
  border-color: #1a56db;
}

.filter-chips .filter-btn.btn-red.active { background: #dc2626; border-color: #dc2626; }
.filter-chips .filter-btn.btn-orange.active { background: #d97706; border-color: #d97706; }
.filter-chips .filter-btn.btn-green.active { background: #059669; border-color: #059669; }

.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.filter-actions .action-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-direction: row;
}

.btn-reset {
  background: #f1f5f9;
  color: #475569;
}

.btn-apply {
  background: #1a56db;
  color: #ffffff;
}

/* ===== MODAL DÉTAIL ===== */
.detail-container {
  padding: 0;
  background: #f1f5f9;
  min-height: 100%;
}

.detail-status {
  padding: 14px;
  text-align: center;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-status.status-nouveau { background: #fef2f2; color: #dc2626; }
.detail-status.status-en_cours { background: #fffbeb; color: #d97706; }
.detail-status.status-termine { background: #ecfdf5; color: #059669; }

.detail-info {
  background: #ffffff;
  margin: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.info-row:last-child {
  border-bottom: none;
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-icon ion-icon {
  font-size: 18px;
  color: #475569;
}

.info-label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 600;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
}

.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: calc(100% - 32px);
  margin: 16px;
  padding: 14px;
  background: #ffffff;
  color: #dc2626;
  border: 1.5px solid #fecaca;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.delete-btn:active {
  background: #fef2f2;
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
  margin-top: 8px;
}

.detail-photo-item {
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s;
}

.detail-photo-item:active {
  transform: scale(0.95);
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
