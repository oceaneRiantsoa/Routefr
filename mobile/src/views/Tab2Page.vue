<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Photo Gallery</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content :fullscreen="true">
      <ion-header collapse="condense">
        <ion-toolbar>
          <ion-title size="large">Photo Gallery</ion-title>
        </ion-toolbar>
      </ion-header>

      <ion-grid>
        <ion-row>
          <ion-col size="6" :key="photo.filepath" v-for="photo in photos">
            <ion-img :src="photo.webviewPath"></ion-img>
          </ion-col>
        </ion-row>
      </ion-grid>

      <!-- CHANGE: Ajouter les boutons de navigation -->
      <div class="ion-padding">
        <ion-button expand="block" @click="goToHome">
          Aller à Home
        </ion-button>
        <ion-button expand="block" @click="goToDetail">
          Aller à Detail
        </ion-button>
      </div>

      <ion-fab vertical="bottom" horizontal="center" slot="fixed">
        <ion-fab-button @click="addNewToGallery()">
          <ion-icon :icon="camera"></ion-icon>
        </ion-fab-button>
      </ion-fab>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { camera } from 'ionicons/icons';
import {
  IonPage,
  IonHeader,
  IonFab,
  IonFabButton,
  IonIcon,
  IonToolbar,
  IonTitle,
  IonContent,
  IonGrid,
  IonRow,
  IonCol,
  IonImg,
  IonButton,
  useIonRouter,
} from '@ionic/vue';
import { usePhotoGallery } from '@/composables/usePhotoGallery';

const { photos, addNewToGallery } = usePhotoGallery();
const ionRouter = useIonRouter();

// const goToHome = () => {
//   ionRouter.push('/home', 'forward', 'push');
// };

// const goToDetail = () => {
//   ionRouter.push('/detail', 'forward', 'push');
// };
const goToHome = () => {
  ionRouter.push('/home');
};

const goToDetail = () => {
  ionRouter.push('/detail');
};
</script>