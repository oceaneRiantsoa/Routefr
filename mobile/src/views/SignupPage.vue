<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-back-button default-href="/"></ion-back-button>
        <ion-title>Créer un compte</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="signup-container">
        <h1>S'inscrire</h1>
        
        <!-- Formulaire d'inscription -->
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
            placeholder="Min 6 caractères"
          ></ion-input>
        </ion-item>

        <ion-item>
          <ion-label position="floating">Confirmer mot de passe</ion-label>
          <ion-input 
            v-model="confirmPassword" 
            type="password" 
            placeholder="Confirmer"
          ></ion-input>
        </ion-item>

        <!-- Bouton d'inscription -->
        <ion-button 
          expand="block" 
          @click="signup" 
          color="primary"
          class="ion-margin-top"
          :disabled="!email || !password || !confirmPassword"
        >
          S'inscrire
        </ion-button>

        <!-- Message d'erreur ou succès -->
        <div class="ion-margin-top">
          <ion-card v-if="message" :color="messageClass">
            <ion-card-content>
              <p style="white-space: pre-wrap; word-break: break-all;">{{ message }}</p>
            </ion-card-content>
          </ion-card>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { createUserWithEmailAndPassword, getIdToken } from 'firebase/auth';
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
  IonCardContent,
  IonBackButton
} from '@ionic/vue';
import { auth } from '@/firebase';

const router = useRouter();

const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const message = ref('');
const messageClass = ref('');

const signup = async () => {
  // Vérifier que les mots de passe correspondent
  if (password.value !== confirmPassword.value) {
    message.value = '❌ Les mots de passe ne correspondent pas!';
    messageClass.value = 'danger';
    return;
  }

  // Vérifier la longueur du mot de passe
  if (password.value.length < 6) {
    message.value = '❌ Le mot de passe doit contenir au moins 6 caractères!';
    messageClass.value = 'danger';
    return;
  }

  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email.value, password.value);
    
    // Obtenir le token
    const token = await getIdToken(userCredential.user);
    
    message.value = `✅ Compte créé avec succès!\n\n📧 Email: ${userCredential.user.email}\n🆔 UID: ${userCredential.user.uid}`;
    messageClass.value = 'success';
    
    // Rediriger vers la connexion après 2 secondes
    setTimeout(() => {
      router.push('/login');
    }, 2000);
    
  } catch (error: any) {
    message.value = `❌ Erreur d'inscription:\n${error.message}`;
    messageClass.value = 'danger';
  }
};
</script>

<style scoped>
.signup-container {
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
</style>
