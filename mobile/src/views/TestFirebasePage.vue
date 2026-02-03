<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Test Firebase</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <h2>Tester Firebase</h2>
      
      <ion-button expand="block" @click="testAuth" color="primary">
        Test Authentication
      </ion-button>
      
      <ion-button expand="block" @click="testFirestore" color="success">
        Test Firestore
      </ion-button>

      <div class="ion-margin-top">
        <p v-if="message" :class="messageClass" style="white-space: pre-wrap; word-break: break-all;">{{ message }}</p>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { createUserWithEmailAndPassword, getIdToken } from 'firebase/auth';
import { collection, addDoc } from 'firebase/firestore';
import { IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonButton } from '@ionic/vue';
import { auth, db } from '@/firebase';

const message = ref('');
const messageClass = ref('');

const testAuth = async () => {
  try {
    const userCredential = await createUserWithEmailAndPassword(
      auth,
      `test${Date.now()}@example.com`,
      'password123'
    );
    
    // Obtenir le token
    const token = await getIdToken(userCredential.user);
    
    const uid = userCredential.user.uid;
    const email = userCredential.user.email;
    
    message.value = `✅ Auth OK!\n\n📧 Email: ${email}\n🆔 UID: ${uid}\n\n🔑 Token:\n${token}`;
    messageClass.value = 'success';
  } catch (error: any) {
    message.value = `❌ Erreur Auth: ${error.message}`;
    messageClass.value = 'error';
  }
};

const testFirestore = async () => {
  try {
    const docRef = await addDoc(collection(db, 'test'), {
      message: 'Test Firestore',
      timestamp: new Date(),
    });
    message.value = `✅ Firestore OK! Doc ID: ${docRef.id}`;
    messageClass.value = 'success';
  } catch (error: any) {
    message.value = `❌ Erreur Firestore: ${error.message}`;
    messageClass.value = 'error';
  }
};
</script>

<style scoped>
.success {
  color: green;
  font-weight: bold;
}

.error {
  color: red;
  font-weight: bold;
}
</style>