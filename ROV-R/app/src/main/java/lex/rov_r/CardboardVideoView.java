/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lex.rov_r;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * Contains two sub-views to provide a simple stereo HUD.
 */
public class CardboardVideoView extends LinearLayout {
    // stores an overlayeyeview for each side
    private final CardboardOverlayEyeView leftView;
    private final CardboardOverlayEyeView rightView;
    private final CardboardOverlayEyeView dummyView;

    public CardboardVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        // build layout using a linear layout, setting each side's overlayeyeview to fill one-half the screen
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(0, 0, 0, 0);

        LayoutParams dummyParams = new LayoutParams(
                0, LayoutParams.MATCH_PARENT, 0.01f);
        dummyParams.setMargins(0, 0, 0, 0);

        dummyView = new CardboardOverlayEyeView(context, attrs);
        dummyView.setLayoutParams(dummyParams);
        addView(dummyView);

        leftView = new CardboardOverlayEyeView(context, attrs);
        leftView.setLayoutParams(params);
        addView(leftView);

        rightView = new CardboardOverlayEyeView(context, attrs);
        rightView.setLayoutParams(params);
        addView(rightView);

        // Set some reasonable defaults.
        setDepthOffset(0.01f);
        setVisibility(View.VISIBLE);
    }

    private abstract class EndAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private void setDepthOffset(float offset) {
        leftView.setOffset(offset);
        rightView.setOffset(-offset);
    }


    private class CardboardOverlayEyeView extends ViewGroup {
        private final WebView webView;
        private float offset;

        public CardboardOverlayEyeView(Context context, AttributeSet attrs) {
            super(context, attrs);

            // initialize webview
            webView = new WebView(context, attrs);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient());
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setSupportZoom(false);

            // load rovr.html into webview to display live video feed
            webView.loadUrl("file:///android_asset/rovr.html");
            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
            });

            webView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    if(event.getAction() == KeyEvent.KEYCODE_DPAD_LEFT||event.getAction() == KeyEvent.KEYCODE_DPAD_RIGHT
                            ||event.getAction() == KeyEvent.KEYCODE_DPAD_DOWN||event.getAction() == KeyEvent.KEYCODE_DPAD_UP){
                        return true;
                    }
                    else{
                        return false;
                    }
                }

            });


            // add this webview to the overlayeyeview
            addView(webView);
        }


        public void setOffset(float offset) {
            this.offset = offset;
        }


        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Width and height of this ViewGroup.
            final int width = right - left;
            final int height = bottom - top;

            // The size of the image, given as a fraction of the dimension as a ViewGroup.
            // We multiply both width and heading with this number to compute the image's bounding
            // box. Inside the box, the image is the horizontally and vertically centered.
            final float imageSize = 1.0f;

            // The fraction of this ViewGroup's height by which we shift the image off the
            // ViewGroup's center. Positive values shift downwards, negative values shift upwards.
            final float verticalImageOffset = -0.07f;

            // Vertical position of the text, specified in fractions of this ViewGroup's height.
            final float verticalTextPos = 0.52f;

            // Layout ImageView
            float adjustedOffset = offset;
            // If the half screen width is bigger than 1000 pixels, that means it's a big screen
            // phone and we need to use a different offset value.
            if (width > 1000) {
                adjustedOffset = 3.8f * offset;
            }
            float imageMargin = (1.0f - imageSize) / 2.0f;
            float leftMargin = (int) (width * (imageMargin + adjustedOffset));
            float topMargin = (int) (height * (imageMargin + verticalImageOffset));
            webView.layout(
                    (int) leftMargin, (int) topMargin,
                    (int) (leftMargin + width * imageSize), (int) (topMargin + height * imageSize));
        }
    }
}
