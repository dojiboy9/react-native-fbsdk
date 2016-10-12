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
 */

package com.facebook.reactnative.androidsdk;

import android.content.Intent;
import android.util.Log;
import java.util.HashMap;
import android.net.*;
import android.graphics.*;
import android.os.Environment;
import android.os.Build;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.webkit.MimeTypeMap;

import java.net.*;
import java.io.*;

import com.facebook.CallbackManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.ShareToMessengerParams;

import com.facebook.FacebookSdk;
import android.media.MediaMetadataRetriever;

/**
 * Provides functionality to send content via the Facebook Message Dialog
 */
public class FBMessengerSharerModule extends FBSDKDialogBaseJavaModule {
    private static final String EXTRA_PROTOCOL_VERSION = "com.facebook.orca.extra.PROTOCOL_VERSION";
    private static final String EXTRA_APP_ID = "com.facebook.orca.extra.APPLICATION_ID";
    private static final int PROTOCOL_VERSION = 20150314;
    private static final int SHARE_TO_MESSENGER_REQUEST_CODE = 1;
    private static final String EXTRA_METADATA = "com.facebook.orca.extra.METADATA";

    private boolean mShouldFailOnDataError;

    public FBMessengerSharerModule(ReactApplicationContext reactContext, CallbackManager callbackManager) {
        super(reactContext, callbackManager);
    }

    @Override
    public String getName() {
        return "FBMessengerSharer";
    }

    /**
     * Send with Messenger
     * @param type
     * @param contentUri
     * @param metadata
     */
    @ReactMethod
    public void send(String type, String filePath, String metadata, Promise promise) {
        try {
            String mimeType = "";

            switch (type) {
                case "image": mimeType = "image/*";
                              break;
                case "gif": mimeType = "image/gif";
                              break;
                case "video": mimeType = "video/*";
                              break;
                case "audio": mimeType = "audio/*";   // TODO: THIS IS UNTESTED
                              break;
            }

            // contentUri points to the content being shared to Messenger
            Uri contentUri = Uri.parse(filePath);

            this.send(contentUri, mimeType, metadata);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject(e.getMessage());
        }
    }

    @ReactMethod
    public void sendImageWithCaption(String pathOrUrl, String metadata, String caption, Promise promise) {
        try {
            Bitmap bitmap = this.getBitmapFromPath(pathOrUrl);
            Uri uri = this.drawCaptionOnBitmap(bitmap, caption, (int)(bitmap.getHeight() * 0.75));
            this.send(uri, "image/*", metadata);

            promise.resolve(true);
        } catch (Exception e) {
            promise.reject(e.getMessage());
        }
    }

    @ReactMethod
    public void sendVideoPreviewWithCaption(String pathOrUrl, String metadata, String caption, Promise promise) {
        try {
            Bitmap bitmap = this.getVideoFrameFromVideo(pathOrUrl);
            Uri uri = this.drawCaptionOnBitmap(bitmap, caption, (int)(bitmap.getHeight() * 0.75));
            this.send(uri, "image/*", metadata);

            promise.resolve(true);
        } catch (Exception e) {
            promise.reject(e.getMessage());
        }
    }

    /**
     * Private helper methods
     */
    private void send(Uri contentUri, String mimeType, String metadata) throws ActivityNotFoundException {
        String APP_ID = FacebookSdk.getApplicationId();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setPackage("com.facebook.orca");
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.putExtra(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION);
        intent.putExtra(EXTRA_APP_ID, APP_ID);
        intent.putExtra(EXTRA_METADATA, metadata);

        Activity activity = getCurrentActivity();
        activity.startActivityForResult(intent, SHARE_TO_MESSENGER_REQUEST_CODE);
    }

    // Return a Bitmap from path
    private Bitmap getBitmapFromPath(String path) throws IOException, Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        String scheme = Uri.parse(path).getScheme();

        if (scheme.equals("http") || scheme.equals("https")) {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            return BitmapFactory.decodeStream(connection.getInputStream(), null, options);
        } else {
            // HACK: For some reason the path comes in as "file:/something" rather than "file://something"
            path = path.replace("file:///","/").replace("file://","/");
            return BitmapFactory.decodeFile(path, options);
        }
    }

    public static Bitmap getVideoFrameFromVideo(String videoPath) throws Exception
    {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try
        {
            String scheme = Uri.parse(videoPath).getScheme();

            mediaMetadataRetriever = new MediaMetadataRetriever();

            if (scheme.equals("http") || scheme.equals("https")) {
                if (Build.VERSION.SDK_INT >= 14)
                    mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
                else
                    mediaMetadataRetriever.setDataSource(videoPath);
            } else {
                videoPath = videoPath.replace("file:///","/").replace("file://","/");
                FileInputStream fstream = new FileInputStream(videoPath);
                mediaMetadataRetriever.setDataSource(fstream.getFD());
            }

            bitmap = mediaMetadataRetriever.getFrameAtTime();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new Exception(
                    "Exception in getVideoFrameFromVideo(String videoPath)"
                            + e.getMessage());

        }
        finally
        {
            if (mediaMetadataRetriever != null)
            {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }

    private Uri drawCaptionOnBitmap(Bitmap bitmap, String caption, int y) throws IOException, FileNotFoundException {
        File file = new File("/sdcard/challo-temp.jpg");
        if (file.exists ()) file.delete ();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int horizontalPadding = 30;
        int verticalPadding = 10;

        FileOutputStream out = new FileOutputStream(file);

        Canvas canvas = new Canvas(bitmap);

        // Setup the text paint (styling)
        Paint paint = new Paint();
        paint.setTextSize(this.textSizeForWidth(paint, width - 2 * horizontalPadding, caption));
        float textHeight = this.textHeight(paint, caption);

        // Draw original photo
        canvas.drawBitmap(bitmap, 0, 0, paint);

        // Draw rectangle (caption background)
        Rect r = new Rect(0, y, width, (int)(y + 2 * verticalPadding + textHeight));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(153,0,0,0));  // 60% opacity
        canvas.drawRect(r, paint);

        // Setup the caption
        paint.setColor(Color.WHITE); // Text Color
        canvas.drawText(caption, horizontalPadding, y + textHeight, paint);

        // Write out
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();

        return Uri.fromFile(file);
    }

    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     *
     * @param paint
     *            the Paint to set the text size for
     * @param desiredWidth
     *            the desired width
     * @param text
     *            the text that should be that width
     */
    private float textSizeForWidth(Paint paint, float desiredWidth, String text) {
        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();
        return desiredTextSize;
    }

    private float textHeight(Paint paint, String text) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.height();
    }
}
