<template>
  <ion-page>
    <ion-content class="signup-content" :fullscreen="true">
      <div class="signup-wrapper">
        <!-- Logo / Branding -->
        <div class="signup-brand">
          <div class="brand-icon">
            <ion-icon :icon="personAddOutline"></ion-icon>
          </div>
          <h1 class="brand-title">Créer un compte</h1>
          <p class="brand-subtitle">Rejoignez RouteFR — Signalement Tana</p>
        </div>

        <!-- Formulaire -->
        <div class="signup-card">
          <div class="form-group">
            <label class="form-label">
              <ion-icon :icon="mailOutline"></ion-icon>
              Adresse email
            </label>
            <input 
              v-model="email" 
              type="email" 
              placeholder="votre@email.com"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              <ion-icon :icon="lockClosedOutline"></ion-icon>
              Mot de passe
            </label>
            <input 
              v-model="password" 
              type="password" 
              placeholder="Min 6 caractères"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              <ion-icon :icon="shieldCheckmarkOutline"></ion-icon>
              Confirmer le mot de passe
            </label>
            <input 
              v-model="confirmPassword" 
              type="password" 
              placeholder="Confirmer"
              class="form-input"
              @keyup.enter="signup"
            />
          </div>

          <!-- Message -->
          <div v-if="message" class="alert" :class="messageClass === 'success' ? 'alert-success' : 'alert-danger'">
            <ion-icon :icon="messageClass === 'success' ? checkmarkCircleOutline : alertCircleOutline"></ion-icon>
            <span>{{ message }}</span>
          </div>

          <button 
            @click="signup" 
            class="btn-primary"
            :disabled="!email || !password || !confirmPassword || isLoading"
          >
            <ion-spinner v-if="isLoading" name="crescent"></ion-spinner>
            <ion-icon v-else :icon="personAddOutline"></ion-icon>
            <span>{{ isLoading ? 'Création...' : 'Créer le compte' }}</span>
          </button>
        </div>

        <!-- Lien retour -->
        <div class="signup-footer">
          <span>Déjà un compte ?</span>
          <button @click="goToLogin" class="btn-link">Se connecter</button>
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
  IonPage, IonContent, IonIcon, IonSpinner
} from '@ionic/vue';
import { 
  personAddOutline, mailOutline, lockClosedOutline, shieldCheckmarkOutline,
  checkmarkCircleOutline, alertCircleOutline 
} from 'ionicons/icons';
import { auth } from '@/firebase';

const router = useRouter();

const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const message = ref('');
const messageClass = ref('');
const isLoading = ref(false);

const signup = async () => {
  if (password.value !== confirmPassword.value) {
    message.value = 'Les mots de passe ne correspondent pas';
    messageClass.value = 'danger';
    return;
  }

  if (password.value.length < 6) {
    message.value = 'Le mot de passe doit contenir au moins 6 caractères';
    messageClass.value = 'danger';
    return;
  }

  try {
    isLoading.value = true;
    message.value = '';
    await createUserWithEmailAndPassword(auth, email.value, password.value);
    
    message.value = 'Compte créé avec succès';
    messageClass.value = 'success';
    
    setTimeout(() => {
      router.push('/map');
    }, 1000);
    
  } catch (error: any) {
    if (error.code === 'auth/email-already-in-use') {
      message.value = 'Cet email est déjà utilisé';
    } else if (error.code === 'auth/invalid-email') {
      message.value = 'Adresse email invalide';
    } else {
      message.value = 'Erreur lors de la création du compte';
    }
    messageClass.value = 'danger';
  } finally {
    isLoading.value = false;
  }
};

const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.signup-content {
  --background: #f1f5f9;
}

.signup-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 24px 20px;
}

.signup-brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: #1a56db;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 4px 16px rgba(26, 86, 219, 0.3);
}

.brand-icon ion-icon {
  font-size: 32px;
  color: #ffffff;
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.brand-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 6px 0 0;
}

.signup-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.form-label ion-icon {
  font-size: 16px;
  color: #64748b;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  color: #0f172a;
  background: #f8fafc;
  transition: all 0.2s;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #1a56db;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(26, 86, 219, 0.1);
}

.form-input::placeholder {
  color: #94a3b8;
}

.alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 20px;
}

.alert ion-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.alert-success {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.alert-danger {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.btn-primary {
  width: 100%;
  padding: 14px;
  background: #1a56db;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: #164bc1;
}

.btn-primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-primary:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.btn-primary ion-icon {
  font-size: 20px;
}

.btn-primary ion-spinner {
  width: 20px;
  height: 20px;
  color: #ffffff;
}

.signup-footer {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.btn-link {
  background: none;
  border: none;
  color: #1a56db;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover {
  text-decoration: underline;
}
</style>
