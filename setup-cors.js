/**
 * Script pour configurer CORS sur Firebase Storage
 * Usage: node setup-cors.js
 * 
 * Permet à localhost:5173, 5174, et toute origine localhost
 * d'accéder à Firebase Storage (upload/download de photos)
 */
const https = require('https');
const crypto = require('crypto');
const fs = require('fs');

// Charger le service account
const serviceAccount = JSON.parse(
  fs.readFileSync('./backend/src/main/resources/serviceAccountKey.json', 'utf8')
);

const BUCKET = 'test-8f6f5.appspot.com';

// Créer un JWT pour obtenir un access token
function createJWT() {
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: 'RS256', typ: 'JWT' };
  const payload = {
    iss: serviceAccount.client_email,
    scope: 'https://www.googleapis.com/auth/devstorage.full_control',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  };

  const encHeader = Buffer.from(JSON.stringify(header)).toString('base64url');
  const encPayload = Buffer.from(JSON.stringify(payload)).toString('base64url');
  const signInput = `${encHeader}.${encPayload}`;

  const sign = crypto.createSign('RSA-SHA256');
  sign.update(signInput);
  const signature = sign.sign(serviceAccount.private_key, 'base64url');

  return `${signInput}.${signature}`;
}

// Obtenir un access token
function getAccessToken() {
  return new Promise((resolve, reject) => {
    const jwt = createJWT();
    const postData = `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`;

    const req = https.request({
      hostname: 'oauth2.googleapis.com',
      path: '/token',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData),
      },
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        const json = JSON.parse(data);
        if (json.access_token) resolve(json.access_token);
        else reject(new Error('Pas de token: ' + data));
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

// Configurer CORS sur le bucket
function setCORS(accessToken) {
  return new Promise((resolve, reject) => {
    const corsConfig = {
      cors: [
        {
          origin: [
            'http://localhost:5173',
            'http://localhost:5174',
            'http://localhost:3000',
            'http://localhost:8080',
            'http://localhost:8086',
            'http://localhost',
            'capacitor://localhost',
            'ionic://localhost',
            'http://localhost:*'
          ],
          method: ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'OPTIONS'],
          maxAgeSeconds: 3600,
          responseHeader: [
            'Content-Type',
            'Authorization',
            'Content-Length',
            'X-Requested-With',
            'x-goog-resumable',
            'Access-Control-Allow-Origin'
          ]
        }
      ]
    };

    const body = JSON.stringify(corsConfig);

    const req = https.request({
      hostname: 'storage.googleapis.com',
      path: `/storage/v1/b/${BUCKET}?fields=cors`,
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
      },
    }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) {
          console.log('✅ CORS configuré avec succès sur Firebase Storage!');
          console.log('Origins autorisées: localhost:5173, 5174, 3000, 8080, 8086');
          resolve(JSON.parse(data));
        } else {
          reject(new Error(`Erreur ${res.statusCode}: ${data}`));
        }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  try {
    console.log('🔑 Obtention du token d\'accès...');
    const token = await getAccessToken();
    console.log('✅ Token obtenu');

    console.log('🔧 Configuration CORS sur le bucket:', BUCKET);
    await setCORS(token);
  } catch (error) {
    console.error('❌ Erreur:', error.message);
  }
}

main();
