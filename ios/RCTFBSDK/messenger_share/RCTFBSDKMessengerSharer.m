// Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#import "RCTFBSDKMessengerSharer.h"

#import <RCTUtils.h>
#import <CoreMedia/CMTime.h>
#import <AVFoundation/AVAsset.h>
#import <AVFoundation/AVAssetImageGenerator.h>

@interface RCTFBSDKMessengerSharer ()
@end

@implementation RCTFBSDKMessengerSharer
{
  // RCTPromiseResolveBlock _showResolve;
  // RCTPromiseRejectBlock _showReject;
}

RCT_EXPORT_MODULE(FBMessengerSharer);

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

#pragma mark - Object Lifecycle

- (instancetype)init
{
  if ((self = [super init])) {
    //_dialog = [[FBSDKMessageDialog alloc] init];
    //_dialog.delegate = self;
  }
  return self;
}

#pragma mark - Helper method
- (UIImage*) drawText:(NSString*) text
             inImage:(UIImage*)  image
             atPoint:(CGPoint)   point
             withFont:(UIFont*)   font
{
    UIGraphicsBeginImageContext(image.size);
    [image drawInRect:CGRectMake(0,0,image.size.width,image.size.height)];
    CGRect rect = CGRectMake(point.x, point.y,
                             image.size.width, image.size.height);
    [[UIColor blackColor] set];
    CGContextFillRect(UIGraphicsGetCurrentContext(), 
                      CGRectMake(0, (image.size.height-[text sizeWithFont:font].height), 
                                 image.size.width, image.size.height));
    [[UIColor whiteColor] set];
    [text drawInRect:CGRectIntegral(rect) withFont:font]; 
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return newImage;
}

- (int) maxFontSize:(NSString*) text
             inImage:(UIImage*) image
{
    int low = 1;
    int high = 1000;
    while(low < high) {
      int k = (low + high + 1)/2;
      int width = [text sizeWithFont:[UIFont boldSystemFontOfSize:k]].width;
      if (width >= image.size.width) {
        high = k-1;
      } else {
        low = k;
      }
    }

    return low;
}

- (UIImage *)thumbnailFromVideoAtURL:(NSURL *)url
                              atTime:(CMTime )time
{
  AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:url options:nil];
  AVAssetImageGenerator *gen = [[AVAssetImageGenerator alloc] initWithAsset:asset];
  gen.appliesPreferredTrackTransform = YES;
  NSError *error = nil;
  CMTime actualTime;

  CGImageRef image = [gen copyCGImageAtTime:time actualTime:&actualTime error:&error];
  UIImage *thumb = [[UIImage alloc] initWithCGImage:image];
  CGImageRelease(image);

  return thumb;
}

- (void) sendUIImageWithCaption:(UIImage *) image
              metadata:(NSString *) metadata
              caption:(NSString *) caption
{
  int maxFontSize = [self maxFontSize:caption inImage:image];
  UIFont *font = [UIFont boldSystemFontOfSize:maxFontSize];
  image = [self drawText:caption
                inImage:image
                atPoint:CGPointMake(0, image.size.height - [caption sizeWithFont:font].height)
                withFont:font];

  FBSDKMessengerShareOptions *options = [[FBSDKMessengerShareOptions alloc] init];
  options.metadata = metadata;
  options.contextOverride = [[FBSDKMessengerBroadcastContext alloc] init];

  [FBSDKMessengerSharer shareImage:image withOptions:options];
}

#pragma mark - React Native Methods

// Load data from disk and return the String.
RCT_EXPORT_METHOD(send:(NSString *) mediaType
                  pathForResource:(NSString *) pathForResource
                  metadata:(NSString *) metadata
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                 )
{
  
  NSData *data = [NSData dataWithContentsOfURL : [NSURL URLWithString:pathForResource]];
  FBSDKMessengerShareOptions *options = [[FBSDKMessengerShareOptions alloc] init];
  options.metadata = metadata;
  options.contextOverride = [[FBSDKMessengerBroadcastContext alloc] init];
  
  if([mediaType isEqualToString: @"image"]){
    [FBSDKMessengerSharer shareAnimatedGIF:data withOptions:options];
  } else if ([mediaType isEqualToString:@"gif"]){
    [FBSDKMessengerSharer shareAnimatedGIF:data withOptions:options];
  } else if ([mediaType isEqualToString:@"video"]){
    [FBSDKMessengerSharer shareVideo:data withOptions:options];
  } else if ([mediaType isEqualToString:@"audio"]){
    [FBSDKMessengerSharer shareAudio:data withOptions:options];
  }
  
  resolve(@YES);
}

// Send an image with a caption
RCT_EXPORT_METHOD(sendImageWithCaption:(NSString *) pathForResource
                  metadata:(NSString *) metadata
                  caption:(NSString *) caption
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                 )
{
  NSData *data = [NSData dataWithContentsOfURL:[NSURL URLWithString:pathForResource]];
  UIImage *image = [UIImage imageWithData:data];
  [self sendUIImageWithCaption:image metadata:metadata caption:caption];

  resolve(@YES);
}

// Send the first frame of a video as an image (with a caption)
RCT_EXPORT_METHOD(sendVideoPreviewWithCaption:(NSString *) pathForResource
                  metadata:(NSString *) metadata
                  caption:(NSString *) caption
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                 )
{
  UIImage * image = [self thumbnailFromVideoAtURL:[NSURL URLWithString:pathForResource]
                          atTime:CMTimeMakeWithSeconds(0.0, 600)];

  [self sendUIImageWithCaption:image metadata:metadata caption:caption];
  resolve(@YES);
}
@end
