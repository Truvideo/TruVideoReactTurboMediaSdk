import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  mediaBuilder(
    filePath: string,
    tag: string,
    metaData: string
  ): Promise<string>;
  getFileUploadRequestById(id: string): Promise<string>;
  getAllFileUploadRequests(status: string): Promise<string>;
  cancelMedia(id: string): Promise<string>;
  deleteMedia(id: string): Promise<string>;
  pauseMedia(id: string): Promise<string>;
  resumeMedia(id: string): Promise<string>;
  uploadMedia(id: string): Promise<string>;
  createStreamUploadRequest(filePath: string): Promise<string>;
  getAllStreamUploadRequests(): Promise<string>;
  getStreamUploadRequestById(id: string): Promise<string>;
  uploadStreamUploadRequest(
    id: string,
    title: string,
    tags: string,
    metadata: string,
    includeInReport: boolean,
    isLibrary: boolean
  ): Promise<string>;
  pauseStreamUploadRequest(id: string): Promise<string>;
  resumeStreamUploadRequest(id: string): Promise<string>;
  retryStreamUploadRequest(id: string): Promise<string>;
  deleteStreamUploadRequest(id: string): Promise<string>;
  search(
    tag: string,
    type: string,
    page: string,
    pageSize: string
  ): Promise<string>;
  searchById(id: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'TruvideoReactTurboMediaSdk'
);
