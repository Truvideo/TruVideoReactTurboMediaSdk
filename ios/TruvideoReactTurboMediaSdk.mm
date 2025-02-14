#import "TruvideoReactTurboMediaSdk.h"

@implementation TruvideoReactTurboMediaSdk
RCT_EXPORT_MODULE()

- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

- (void)uploadMedia:(nonnull NSString *)filePath tag:(nonnull NSString *)tag metaData:(nonnull NSString *)metaData resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
  TruvideoReactTurboMediaSdk *truvideo = [[TruvideoReactTurboMediaSdk alloc] init];
  [truvideo uploadMedia:filePath tag:tag metaData:metaData resolve:resolve reject:reject];
}


- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeTruvideoReactTurboMediaSdkSpecJSI>(params);
}

@end
