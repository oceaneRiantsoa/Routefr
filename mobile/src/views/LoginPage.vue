<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Connexion Firebase</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="login-container">
        <h1>Se connecter</h1>
        
        <!-- Formulaire de connexion -->
        <ion-item>
          <ion-label position="floating">Email</ion-label>
          <ion-input 
            v-model="email" 
            type="email" 
            placeholder="votre@email.com"
          ></ion-input>
        </ion-item>

        <ion-item>
          <ion-label position="floating">Mot de passe</ion-label>
          <ion-input 
            v-model="password" 
            type="password" 
            placeholder="Votre mot de passe"
          ></ion-input>
        </ion-item>

        <!-- Bouton de connexion -->
        <ion-button 
          expand="block" 
          @click="login" 
          color="primary"
          class="ion-margin-top"
          :disabled="!email || !password"
        >
          Se connecter
        </ion-button>

        <!-- Message d'erreur ou succès -->
        <div class="ion-margin-top">
          <ion-card v-if="message" :color="messageClass">
            <ion-card-content>
              <p style="white-space: pre-wrap; word-break: break-all;">{{ message }}</p>
            </ion-card-content>
          </ion-card>
        </div>

        <!-- Afficher les infos si connecté -->
        <ion-card v-if="user" color="success" class="ion-margin-top">
          <ion-card-header>
            <ion-card-title>✅ Connecté !</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <p><strong>📧 Email:</strong> {{ user.email }}</p>
            <p><strong>🆔 UID:</strong> {{ user.uid }}</p>
            <p><strong>🔑 Token:</strong></p>
            <p style="word-break: break-all; font-size: 12px;">{{ userToken }}</p>
            <ion-button @click="logout" color="danger" expand="block" class="ion-margin-top">
              Se déconnecter
            </ion-button>
          </ion-card-content>
        </ion-card>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { signInWithEmailAndPassword, signOut, getIdToken } from 'firebase/auth';
import { useRouter } from 'vue-router';
import { 
  IonPage, 
  IonHeader, 
  IonToolbar, 
  IonTitle, 
  IonContent, 
  IonItem,
  IonLabel,
  IonInput,
  IonButton,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent
} from '@ionic/vue';
import { auth } from '@/firebase';

const router = useRouter();

const email = ref('');
const password = ref('');
const message = ref('');
const messageClass = ref('');
const user = ref<any>(null);
const userToken = ref('');

const login = async () => {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email.value, password.value);
    
    // Obtenir le token
    const token = await getIdToken(userCredential.user);
    
    user.value = {
      email: userCredential.user.email,
      uid: userCredential.user.uid,
    };
    userToken.value = token;
    
    message.value = '✅ Connexion réussie!';
    messageClass.value = 'success';
    
    // Effacer les champs
    email.value = '';
    password.value = '';

    // Rediriger vers la carte après connexion
    setTimeout(() => {
      router.push('/map');
    }, 1000);
  } catch (error: any) {
    message.value = `❌ Erreur de connexion:\n${error.message}`;
    messageClass.value = 'danger';
    user.value = null;
  }
};

const logout = async () => {
  try {
    await signOut(auth);
    user.value = null;
    userToken.value = '';
    message.value = '✅ Déconnexion réussie';
    messageClass.value = 'success';
  } catch (error: any) {
    message.value = `❌ Erreur: ${error.message}`;
    messageClass.value = 'danger';
  }
};

const goToSignup = () => {
  router.push('/signup');
};
</script>

<style scoped>
.login-container {
  max-width: 500px;
  margin: 0 auto;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
}

ion-item {
  margin-bottom: 20px;
}

.ion-text-center {
  text-align: center;
}
</style>
