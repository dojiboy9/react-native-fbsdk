/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @flow
 */

'use strict';

const MessengerSharer = require('react-native').NativeModules.FBMessengerSharer;

const ShareType = {
    image: 'image',
    gif: 'gif',
    video: 'video',
    audio: 'audio',
};

module.exports = {
  send(type: ShareType, filePath: String, metadata: String|Object ): void {
    if(type in ShareType) {
        if(typeof metadata !== 'string'){
            metadata = JSON.stringify(metadata);
        }
        MessengerSharer.send(type, filePath, metadata);
    } else {
        throw new Error(`
        "${type}" is not a valid type for this method.
            Valid values are: ShareType.image, ShareType.gif, ShareType.video, ShareType.audio`);
    }
  },

  /**
   * A hack to enable text on images
   */
  sendImageWithCaption(filePath: String, metadata: String|Object, caption: String): void {
    if(typeof metadata !== 'string'){
        metadata = JSON.stringify(metadata);
    }
    MessengerSharer.sendImageWithCaption(filePath, metadata, caption);
  },

  /**
   * A hack to enable sending a preview of a video (with text on it)
   */
  sendVideoPreviewWithCaption(filePath: String, metadata: String|Object, caption: String): void {
    if(typeof metadata !== 'string'){
        metadata = JSON.stringify(metadata);
    }
    MessengerSharer.sendVideoPreviewWithCaption(filePath, metadata, caption);
  },

  ShareType
};
