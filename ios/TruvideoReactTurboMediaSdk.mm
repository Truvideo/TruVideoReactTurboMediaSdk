#import "TruvideoReactTurboMediaSdk.h"
#import "truvideo_react_turbo_media_sdk-Swift.h"

@implementation TruvideoReactTurboMediaSdk
RCT_EXPORT_MODULE()

- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

- (void)cancelMedia:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo cancelMediaWithId:id resolve:resolve reject:reject];
}


- (void)deleteMedia:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo deleteMediaWithId:id resolve:resolve reject:reject];
}


- (void)getAllFileUploadRequests:(nonnull NSString *)status resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo getAllFileRequestsWithStatus:status resolve:resolve reject:reject];
}


- (void)getFileUploadRequestById:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo getFileUploadRequestByIdWithId:id resolve:resolve reject:reject];
}


- (void)mediaBuilder:(nonnull NSString *)filePath tag:(nonnull NSString *)tag metaData:(nonnull NSString *)metaData resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo mediaBuilderWithFilePath:filePath tag:tag metaData:metaData resolve:resolve reject:reject];
}


- (void)pauseMedia:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo pauseMediaWithId:id resolve:resolve reject:reject];
}


- (void)resumeMedia:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo resumeMediaWithId:id resolve:resolve reject:reject];
}


- (void)search:(nonnull NSString *)tag type:(nonnull NSString *)type page:(nonnull NSString *)page pageSize:(nonnull NSString *)pageSize resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo searchWithTag:tag type:type page:page pageSize:pageSize resolve:resolve reject:reject];
}


- (void)uploadMedia:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo uploadMediaWithId:id resolve:resolve reject:reject];
}

- (void)createStreamUploadRequest:(nonnull NSString *)filePath resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo createStreamUploadRequest:filePath resolve:resolve reject:reject];
}

- (void)getAllStreamUploadRequests:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo getAllStreamUploadRequests:resolve reject:reject];
}

- (void)getStreamUploadRequestById:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo getStreamUploadRequestById:id resolve:resolve reject:reject];
}

- (void)uploadStreamUploadRequest:(nonnull NSString *)id title:(nonnull NSString *)title tags:(nonnull NSString *)tags metadata:(nonnull NSString *)metadata includeInReport:(BOOL)includeInReport isLibrary:(BOOL)isLibrary resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo uploadStreamUploadRequest:id title:title tags:tags metadata:metadata includeInReport:includeInReport isLibrary:isLibrary resolve:resolve reject:reject];
}

- (void)pauseStreamUploadRequest:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo pauseStreamUploadRequest:id resolve:resolve reject:reject];
}

- (void)resumeStreamUploadRequest:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo resumeStreamUploadRequest:id resolve:resolve reject:reject];
}

- (void)retryStreamUploadRequest:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo retryStreamUploadRequest:id resolve:resolve reject:reject];
}

- (void)deleteStreamUploadRequest:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo deleteStreamUploadRequest:id resolve:resolve reject:reject];
}

- (void)searchById:(nonnull NSString *)id resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
  TruVideoReactMediaSdkClass *truvideo = [[TruVideoReactMediaSdkClass alloc] init];
  [truvideo searchById:id resolve:resolve reject:reject];
}


- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeTruvideoReactTurboMediaSdkSpecJSI>(params);
}

@end

