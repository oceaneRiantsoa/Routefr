<template>
  <ion-app>
    <!-- Contenu de la page -->
    <ion-router-outlet />
  </ion-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { logIn, logOut } from 'ionicons/icons';
import { 
  IonApp, 
  IonRouterOutlet, 
  IonHeader, 
  IonToolbar, 
  IonTitle, 
  IonButton, 
  IonButtons,
  IonIcon,
  IonText
} from '@ionic/vue';
import { auth } from './firebase';

const router = useRouter();
const user = ref<any>(null);

onMounted(() => {
  // Vérifier l'état d'authentification
  onAuthStateChanged(auth, (currentUser) => {
    if (currentUser) {
      user.value = {
        email: currentUser.email,
        uid: currentUser.uid,
      };
      // Rediriger vers la carte si connecté
      if (router.currentRoute.value.path === '/login') {
        router.push('/map');
      }
    } else {
      user.value = null;
      // Rediriger vers login si pas connecté
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login');
      }
    }
  });
});

const goToLogin = () => {
  router.push('/login');
};

const logout = async () => {
  try {
    await signOut(auth);
    user.value = null;
    router.push('/login');
  } catch (error) {
    console.error('Erreur logout:', error);
  }
};
</script>

<style scoped>
.user-info {
  margin-right: 15px;
  font-size: 12px;
  color: white;
}
</style>
<!-- <template>
  <ion-button>Button</ion-button>
  <ion-datetime></ion-datetime>
</template>

<script setup lang="ts">
import { IonButton, IonDatetime } from '@ionic/vue';
</script> -->