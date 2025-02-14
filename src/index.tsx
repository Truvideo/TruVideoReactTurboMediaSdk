import TruvideoReactTurboMediaSdk from './NativeTruvideoReactTurboMediaSdk';

export function multiply(a: number, b: number): number {
  return TruvideoReactTurboMediaSdk.multiply(a, b);
}

export function uploadMedia(
  filePath: string,
  tag: string,
  metaData: string
): Promise<string> {
  return TruvideoReactTurboMediaSdk.uploadMedia(filePath, tag, metaData);
}
