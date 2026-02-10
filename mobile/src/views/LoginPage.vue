<template>
  <ion-page>
    <ion-content class="login-content" :fullscreen="true">
      <div class="login-wrapper">
        <!-- Logo / Branding -->
        <div class="login-brand">
          <div class="brand-icon">
            <ion-icon :icon="mapOutline"></ion-icon>
          </div>
          <h1 class="brand-title">RouteFR</h1>
          <p class="brand-subtitle">Signalement routier — Antananarivo</p>
        </div>

        <!-- Formulaire -->
        <div class="login-card">
          <div class="card-header">
            <ion-icon :icon="logInOutline" class="card-header-icon"></ion-icon>
            <span>Connexion</span>
          </div>

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
              @keyup.enter="login"
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
              placeholder="Votre mot de passe"
              class="form-input"
              @keyup.enter="login"
            />
          </div>

          <!-- Message d'erreur -->
          <div v-if="message" class="alert" :class="messageClass === 'success' ? 'alert-success' : 'alert-danger'">
            <ion-icon :icon="messageClass === 'success' ? checkmarkCircleOutline : alertCircleOutline"></ion-icon>
            <span>{{ message }}</span>
          </div>

          <button 
            @click="login" 
            class="btn-primary"
            :disabled="!email || !password || isLoading"
          >
            <ion-spinner v-if="isLoading" name="crescent"></ion-spinner>
            <ion-icon v-else :icon="logInOutline"></ion-icon>
            <span>{{ isLoading ? 'Connexion...' : 'Se connecter' }}</span>
          </button>
        </div>


      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { signInWithEmailAndPassword, signOut, getIdToken } from 'firebase/auth';
import { useRouter } from 'vue-router';
import { 
  IonPage, IonContent, IonIcon, IonSpinner
} from '@ionic/vue';
import { 
  mapOutline, logInOutline, mailOutline, lockClosedOutline, 
  checkmarkCircleOutline, alertCircleOutline 
} from 'ionicons/icons';
import { auth } from '@/firebase';

const router = useRouter();

const email = ref('');
const password = ref('');
const message = ref('');
const messageClass = ref('');
const isLoading = ref(false);
const user = ref<any>(null);
const userToken = ref('');

const login = async () => {
  if (!email.value || !password.value) return;
  try {
    isLoading.value = true;
    message.value = '';
    const userCredential = await signInWithEmailAndPassword(auth, email.value, password.value);
    
    const token = await getIdToken(userCredential.user);
    
    user.value = {
      email: userCredential.user.email,
      uid: userCredential.user.uid,
    };
    userToken.value = token;
    
    message.value = 'Connexion réussie';
    messageClass.value = 'success';
    
    email.value = '';
    password.value = '';

    setTimeout(() => {
      router.push('/map');
    }, 600);
  } catch (error: any) {
    message.value = 'Email ou mot de passe incorrect';
    messageClass.value = 'danger';
    user.value = null;
  } finally {
    isLoading.value = false;
  }
};

</script>

<style scoped>
.login-content {
  --background: #f1f5f9;
}

.login-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 24px 20px;
}

/* Branding */
.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: #1a56db;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 4px 16px rgba(26, 86, 219, 0.3);
}

.brand-icon ion-icon {
  font-size: 36px;
  color: #ffffff;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 6px 0 0;
}

/* Card */
.login-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.card-header-icon {
  font-size: 22px;
  color: #1a56db;
}

/* Form */
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

/* Alert */
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

/* Button */
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

/* Footer */
.login-footer {
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
