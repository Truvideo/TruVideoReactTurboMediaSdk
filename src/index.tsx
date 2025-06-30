import { NativeModules, NativeEventEmitter } from 'react-native';
import TruvideoReactTurboMediaSdk from './NativeTruvideoReactTurboMediaSdk';
const TruvideoReactTurboMedia = NativeModules.TruvideoReactTurboMediaSdk;
const mediaEventEmitter = new NativeEventEmitter(TruvideoReactTurboMedia);
export interface MediaData {
  id: string;
  filePath: string;
  fileType: string;
  durationMilliseconds: number;
  remoteId: string;
  remoteURL: string;
  transcriptionURL: string;
  transcriptionLength: number;
  status: string;
  progress: number;
  // Add other properties as per your mapOf keys
}

export interface UploadProgressEvent {
  id: string;
  progress: string;
}

export interface UploadCompleteEventData {
  id: string;
  createdDate?: string;
  remoteId?: string;
  uploadedFileURL?: string;
  metaData?: any; // Change to 'any' or a specific type after JSON.parse
  tags?: any;
  transcriptionURL?: string;
  transcriptionLength?: number;
  fileType?: string;
}

export interface UploadErrorEvent {
  id: string;
  error: any;
}

// Define the signature for the callbacks MediaBuilder will expect
export interface UploadCallbacks {
  onProgress?: (event: UploadProgressEvent) => void;
  onComplete?: (event: UploadCompleteEventData) => void;
  onError?: (event: UploadErrorEvent) => void;
}

export function getFileUploadRequestById(id: string): Promise<string> {
  return TruvideoReactTurboMediaSdk.getFileUploadRequestById(id);
}

export function getAllFileUploadRequests(status: string): Promise<string> {
  return TruvideoReactTurboMediaSdk.getAllFileUploadRequests(status);
}

export function search(
  tag: string,
  type: string,
  page: number,
  pageSize: number
): Promise<string> {
  return TruvideoReactTurboMediaSdk.search(
    tag,
    type,
    page.toString(),
    pageSize.toString()
  );
}

export class MediaBuilder {
  private _filePath: string;
  private _metaData: Map<string, string> = new Map(); // Default to empty JSON string or similar
  private _tag: Map<string, string> = new Map();
  private mediaDetail: MediaData | undefined;
  private listeners: any[] = []; // To store event listener subscriptions
  private currentUploadId: string | undefined;
  constructor(filePath: string) {
    if (!filePath) {
      throw new Error('filePath is required for MediaBuilder.');
    }
    this._filePath = filePath;
  }

  setTag(key: string, value: string): MediaBuilder {
    this._tag.set(key, value);
    return this;
  }

  getTag(): Map<string, string> {
    return this._tag;
  }

  getMetaData(): Map<string, string> {
    return this._metaData;
  }

  setMetaData(key: string, value: string): MediaBuilder {
    this._metaData.set(key, value);
    return this;
  }

  clearTags(): MediaBuilder {
    this._tag.clear;
    return this;
  }

  deleteTag(key: string): MediaBuilder {
    this._tag.delete(key);
    return this;
  }

  deleteMetaData(key: string): MediaBuilder {
    this._metaData.delete(key);
    return this;
  }

  clearMetaDatas(): MediaBuilder {
    this._metaData.clear;
    return this;
  }

  mapToJsonObject(map: Map<string, string>): { [key: string]: string } {
    const obj: { [key: string]: string } = {};
    map.forEach((value, key) => {
      obj[key] = value;
    });
    return obj;
  }

  async build(): Promise<MediaBuilder> {
    const jsonObjectTag = this.mapToJsonObject(this._tag);
    const tag = JSON.stringify(jsonObjectTag);
    const jsonObjectMetadata = this.mapToJsonObject(this._tag);
    const metaData = JSON.stringify(jsonObjectMetadata);
    const response = await TruvideoReactTurboMediaSdk.mediaBuilder(
      this._filePath,
      tag,
      metaData
    );
    this.mediaDetail = JSON.parse(response);
    return this;
  }

  cancel(): Promise<string> {
    if (this.mediaDetail === undefined) {
      return Promise.reject(
        new Error('Cannot cancel: mediaDetail is undefined.')
      );
    }
    return TruvideoReactTurboMediaSdk.cancelMedia(this.mediaDetail.id);
  }
  delete(): Promise<string> {
    if (this.mediaDetail === undefined) {
      return Promise.reject(
        new Error('Cannot delete: mediaDetail is undefined.')
      );
    }
    return TruvideoReactTurboMediaSdk.deleteMedia(this.mediaDetail.id);
  }
  pause(): Promise<string> {
    if (this.mediaDetail === undefined) {
      return Promise.reject(
        new Error('Cannot pause: mediaDetail is undefined.')
      );
    }
    return TruvideoReactTurboMediaSdk.pauseMedia(this.mediaDetail.id);
  }
  resume(): Promise<string> {
    if (this.mediaDetail === undefined) {
      return Promise.reject(
        new Error('Cannot resume: mediaDetail is undefined.')
      );
    }
    return TruvideoReactTurboMediaSdk.resumeMedia(this.mediaDetail.id);
  }
  upload(callbacks: UploadCallbacks): Promise<string> {
    if (this.mediaDetail === undefined) {
      return Promise.reject(
        new Error('Cannot upload: mediaDetail is undefined.')
      );
    }

    this.removeEventListeners;
    // Store the ID of the current upload this instance is handling
    this.currentUploadId = this.mediaDetail.id;

    // Add new listeners
    this.listeners.push(
      mediaEventEmitter.addListener('onProgress', (eventJson: string) => {
        const event: UploadProgressEvent = JSON.parse(eventJson);
        if (event.id === this.currentUploadId && callbacks?.onProgress) {
          callbacks.onProgress(event);
        }
      })
    );

    this.listeners.push(
      mediaEventEmitter.addListener('onComplete', (eventJson: string) => {
        const event: UploadCompleteEventData = JSON.parse(eventJson);
        if (event.id === this.currentUploadId && callbacks?.onComplete) {
          // Parse nested JSON strings if they exist
          if (event.metaData && typeof event.metaData === 'string') {
            event.metaData = JSON.parse(event.metaData);
          }
          if (event.tags && typeof event.tags === 'string') {
            event.tags = JSON.parse(event.tags);
          }
          callbacks.onComplete(event);
        }
        // Always remove listeners after a complete or error event for this upload
        this.removeEventListeners();
      })
    );

    this.listeners.push(
      mediaEventEmitter.addListener('onError', (eventJson: string) => {
        const event: UploadErrorEvent = JSON.parse(eventJson);
        if (event.id === this.currentUploadId && callbacks?.onError) {
          callbacks.onError(event);
        }
        // Always remove listeners after a complete or error event for this upload
        this.removeEventListeners();
      })
    );
    return TruvideoReactTurboMediaSdk.uploadMedia(this.mediaDetail.id);
  }

  removeEventListeners(): void {
    this.listeners.forEach((listener) => listener.remove());
    this.listeners = []; // Clear the array
    this.currentUploadId = undefined; // Clear the current upload ID
  }
}
