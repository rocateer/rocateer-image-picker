#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(RocateerImagePicker, NSObject)

RCT_EXTERN_METHOD(openImagePicker:(NSString *)title
                  resolve:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)


// requiresMainQueueSetup를 노출시키는 올바른 방법
+ (BOOL)requiresMainQueueSetup
{
  return YES; // 또는 Swift 파일의 설정에 맞게 true/false
}

@end
