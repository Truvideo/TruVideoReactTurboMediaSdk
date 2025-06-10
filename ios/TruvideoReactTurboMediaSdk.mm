#import "TruvideoReactTurboMediaSdk.h"
#import "truvideo_react_turbo_media_sdk-Swift.h"

@implementation TruvideoReactTurboMediaSdk
RCT_EXPORT_MODULE()

- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

- (void)cancel:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo cancelWithId:id resolve:resolve reject:reject];
}


- (void)delete:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo deleteWithId:id resolve:resolve reject:reject];
}


- (void)getAllFileUploadRequests:(nonnull NSString *)status resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  
}


- (void)getFileUploadRequestById:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo getFileUploadRequestByIdWithId:id resolve:resolve reject:reject];
}


- (void)mediaBuilder:(nonnull NSString *)filePath tag:(nonnull NSString *)tag metaData:(nonnull NSString *)metaData resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo mediaBuilderWithFilePath:filePath tag:tag metaData:metaData resolve:resolve reject:reject];
}


- (void)pause:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo pauseWithId:id resolve:resolve reject:reject];
}


- (void)resume:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo resumeWithId:id resolve:resolve reject:reject];
}


- (void)search:(nonnull NSString *)tag type:(nonnull NSString *)type page:(nonnull NSString *)page pageSize:(nonnull NSString *)pageSize resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo searchWithTag:tag type:type page:page pageSize:pageSize resolve:resolve reject:reject];
}


- (void)upload:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo uploadWithId:id resolve:resolve reject:reject];
}


- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeTruvideoReactTurboMediaSdkSpecJSI>(params);
}

@end
