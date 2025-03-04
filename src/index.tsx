import TruvideoReactTurboMediaSdk from './NativeTruvideoReactTurboMediaSdk';


export function uploadMedia(
  filePath: string,
  tag: string,
  metaData: string
): Promise<string> {
  return TruvideoReactTurboMediaSdk.uploadMedia(filePath, tag, metaData);
}
