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
  cancel(id: string): Promise<string>;
  delete(id: string): Promise<string>;
  pause(id: string): Promise<string>;
  resume(id: string): Promise<string>;
  upload(id: string): Promise<string>;
  search(
    tag: string,
    type: string,
    page: string,
    pageSize: string
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'TruvideoReactTurboMediaSdk'
);
