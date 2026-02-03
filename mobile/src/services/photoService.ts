// Service pour gérer l'upload des photos vers Firebase Storage
import { storage, storageRef, uploadBytes, getDownloadURL, deleteObject, auth } from '@/firebase';
import { Camera, CameraResultType, CameraSource, Photo } from '@capacitor/camera';
import { Filesystem } from '@capacitor/filesystem';
import { isPlatform } from '@ionic/vue';
import { Capacitor } from '@capacitor/core';

export interface SignalementPhoto {
  id: string;
  localPath?: string;
  webviewPath?: string;
  firebaseUrl?: string;
  uploading?: boolean;
  error?: string;
}

class PhotoService {
  private storagePath = 'signalements';

  /**
   * Vérifier si la caméra est disponible
   */
  isAvailable(): boolean {
    return true; // Capacitor Camera fonctionne sur web et mobile
  }

  /**
   * Prendre une photo avec la caméra
   */
  async takePhoto(): Promise<SignalementPhoto | null> {
    try {
      const photo = await Camera.getPhoto({
        resultType: CameraResultType.DataUrl, // Utiliser DataUrl pour compatibilité web
        source: CameraSource.Camera,
        quality: 80,
        width: 1024,
        height: 1024,
        allowEditing: false,
      });

      return this.processPhotoDataUrl(photo);
    } catch (error: any) {
      // Annulé par l'utilisateur - pas une erreur
      if (this.isUserCancellation(error)) {
        return null;
      }
      console.error('Erreur prise de photo:', error);
      return null; // Retourner null au lieu de throw pour éviter les crashs
    }
  }

  /**
   * Choisir une photo depuis la galerie
   */
  async pickFromGallery(): Promise<SignalementPhoto | null> {
    try {
      const photo = await Camera.getPhoto({
        resultType: CameraResultType.DataUrl,
        source: CameraSource.Photos,
        quality: 80,
        width: 1024,
        height: 1024,
      });

      return this.processPhotoDataUrl(photo);
    } catch (error: any) {
      if (this.isUserCancellation(error)) {
        return null;
      }
      console.error('Erreur sélection photo:', error);
      return null;
    }
  }

  /**
   * Choisir plusieurs photos depuis la galerie
   * Fallback vers pickFromGallery si pickImages n'est pas supporté
   */
  async pickMultipleFromGallery(): Promise<SignalementPhoto[]> {
    try {
      // Sur web, pickImages peut ne pas être supporté, utiliser pickFromGallery
      if (!isPlatform('hybrid')) {
        const photo = await this.pickFromGallery();
        return photo ? [photo] : [];
      }

      const result = await Camera.pickImages({
        quality: 80,
        width: 1024,
        height: 1024,
        limit: 5,
      });

      const photos: SignalementPhoto[] = [];
      for (const photo of result.photos) {
        try {
          const processed = await this.processPickedPhoto(photo);
          if (processed) {
            photos.push(processed);
          }
        } catch (e) {
          console.warn('Erreur traitement photo:', e);
        }
      }
      return photos;
    } catch (error: any) {
      if (this.isUserCancellation(error)) {
        return [];
      }
      console.error('Erreur sélection multiple:', error);
      // Fallback: essayer avec une seule photo
      try {
        const photo = await this.pickFromGallery();
        return photo ? [photo] : [];
      } catch {
        return [];
      }
    }
  }

  /**
   * Vérifier si l'erreur est une annulation utilisateur
   */
  private isUserCancellation(error: any): boolean {
    const message = error?.message?.toLowerCase() || '';
    return (
      message.includes('cancel') ||
      message.includes('user') ||
      message.includes('dismiss') ||
      message.includes('denied') ||
      error?.code === 'CANCELLED'
    );
  }

  /**
   * Traiter une photo avec DataUrl (compatible web et mobile)
   */
  private processPhotoDataUrl(photo: Photo): SignalementPhoto | null {
    if (!photo.dataUrl) {
      console.error('Photo sans dataUrl');
      return null;
    }

    const id = `photo_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    return {
      id,
      localPath: photo.path,
      webviewPath: photo.dataUrl, // DataUrl fonctionne directement comme src d'image
    };
  }

  /**
   * Traiter une photo sélectionnée (pickImages - mobile uniquement)
   */
  private async processPickedPhoto(photo: { webPath: string; path?: string }): Promise<SignalementPhoto | null> {
    const id = `photo_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    let webviewPath = photo.webPath;

    // Sur mobile, convertir le chemin pour affichage
    if (isPlatform('hybrid') && photo.path) {
      webviewPath = Capacitor.convertFileSrc(photo.path);
    }

    return {
      id,
      localPath: photo.path || photo.webPath,
      webviewPath,
    };
  }

  /**
   * Uploader une photo vers Firebase Storage
   */
  async uploadPhoto(photo: SignalementPhoto, signalementId: string): Promise<string> {
    const user = auth.currentUser;
    if (!user) throw new Error('Utilisateur non connecté');

    const fileName = `${photo.id}.jpg`;
    const path = `${this.storagePath}/${user.uid}/${signalementId}/${fileName}`;
    const ref = storageRef(storage, path);

    // Convertir en blob
    const blob = await this.photoToBlob(photo);

    // Upload
    await uploadBytes(ref, blob, {
      contentType: 'image/jpeg',
    });

    // Récupérer l'URL de téléchargement
    const downloadUrl = await getDownloadURL(ref);
    return downloadUrl;
  }

  /**
   * Uploader plusieurs photos (retourne les URLs uploadées avec succès)
   */
  async uploadPhotos(photos: SignalementPhoto[], signalementId: string): Promise<string[]> {
    if (!photos || photos.length === 0) {
      return []; // Pas de photos = pas d'erreur
    }

    const urls: string[] = [];

    for (const photo of photos) {
      try {
        const url = await this.uploadPhoto(photo, signalementId);
        urls.push(url);
      } catch (error) {
        console.error(`Erreur upload photo ${photo.id}:`, error);
        // Continue avec les autres photos - ne pas faire échouer tout l'upload
      }
    }

    return urls;
  }

  /**
   * Convertir une photo en Blob pour l'upload
   */
  private async photoToBlob(photo: SignalementPhoto): Promise<Blob> {
    // Si c'est un DataUrl (commence par data:)
    if (photo.webviewPath?.startsWith('data:')) {
      const response = await fetch(photo.webviewPath);
      return await response.blob();
    }

    // Sur mobile avec chemin local
    if (isPlatform('hybrid') && photo.localPath) {
      try {
        const readFile = await Filesystem.readFile({
          path: photo.localPath,
        });

        const base64Data = readFile.data as string;
        // Vérifier si c'est déjà un data URL complet
        if (base64Data.startsWith('data:')) {
          const response = await fetch(base64Data);
          return await response.blob();
        }
        // Sinon, construire le data URL
        const response = await fetch(`data:image/jpeg;base64,${base64Data}`);
        return await response.blob();
      } catch (error) {
        console.error('Erreur lecture fichier mobile:', error);
      }
    }

    // Fallback: fetch direct
    if (photo.webviewPath) {
      const response = await fetch(photo.webviewPath);
      return await response.blob();
    }

    throw new Error('Impossible de convertir la photo en blob');
  }

  /**
   * Supprimer une photo de Firebase Storage
   */
  async deletePhoto(photoUrl: string): Promise<void> {
    try {
      const ref = storageRef(storage, photoUrl);
      await deleteObject(ref);
    } catch (error) {
      console.error('Erreur suppression photo:', error);
    }
  }

  /**
   * Supprimer toutes les photos d'un signalement
   */
  async deleteSignalementPhotos(photoUrls: string[]): Promise<void> {
    for (const url of photoUrls) {
      await this.deletePhoto(url);
    }
  }
}

export const photoService = new PhotoService();
