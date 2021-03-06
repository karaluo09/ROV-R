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

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import java.io.*;
import java.net.*;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Main class for our Android application. Handles joystick input, displaying video to the Google Cardboard,
 * and head tracking.
 *
 * Cardboard related code adapted from Google's Demo: https://developers.google.com/cardboard/android/get-started
 * Gamepad related code adapted from Google's API docs: http://developer.android.com/training/game-controllers/controller-input.html
 * Networking code adapted from: http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private static final String TAG = "MainActivity";

    // store reference to vibration motor
    private Vibrator vibrator;

    // store reference to CardboardVideoView
    private CardboardVideoView overlayView;
    private CardboardOverlayView textView;

    // global variables for head-tracking/joystick data
    private double x;
    private double y;
    private double z;
    private double rz;
    private float[] headRotate;

    // store reference to server socket
    private ServerSocket server;

    // store reference to server thread
    Thread serverThread = null;

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load layout from XML
        setContentView(R.layout.common_ui);

        // initialize cardboardview as per Google Cardboard SDK instructions
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRestoreGLStateEnabled(false);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        // initialize reference to device vibration motors
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // initialize reference to cardboardvideoview
        overlayView = (CardboardVideoView) findViewById(R.id.overlay);
        textView = (CardboardOverlayView) findViewById(R.id.text);

        // start HTTP socket server thread
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        // initialize array to store head rotation data
        headRotate = new float[3];
        headRotate[0] = 0;
        headRotate[1] = 0;
        headRotate[2] = 0;
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world.
     * <p/>
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(1f, 1f, 1f, 0.5f);
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // get euler angles from latest head rotation data
        headTransform.getEulerAngles(headRotate, 0);

        // cap angles between -90 and 90 degrees (max range of the servos)
        if(headRotate[1]>Math.PI/2){
            headRotate[1]=(float)Math.PI/2;
        }
        else if (headRotate[1]<-Math.PI/2){
            headRotate[1]=(float)-Math.PI/2;
        }
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        //takeScreenshot();
        // vibrate to signify that trigger has been activated (current no use, just for debug purposes)
        vibrator.vibrate(50);
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis) : event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            textView.show3DToast("Screenshot taken!");
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }


    private void processJoystickInput(MotionEvent event, int historyPos) throws IOException {
        // get device that created motionevent
        InputDevice mInputDevice = event.getDevice();

        // store data for each joystick axis
        x = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos);
        y = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos);
        z = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Z, historyPos);
        rz = getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RZ, historyPos);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // Check that the event came from a game controller
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                try {
                    processJoystickInput(event, i);
                } catch (IOException ex) {
                    System.out.println("Error loading history");
                }
            }
            // Process the current movement sample in the batch (position -1)
            try {
                processJoystickInput(event, -1);
            } catch (IOException ex) {
                System.out.println("Error loading current");
            }

            // log joystick values for debugging
            Log.i("x", String.valueOf(x));
            Log.i("y", String.valueOf(y));
            Log.i("z", String.valueOf(z));
            Log.i("rz", String.valueOf(rz));
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    class ServerThread implements Runnable {

        public void run() {
            // stores reference to socket connection
            Socket socket = null;

            // initialize socket server on port 8080
            try {
                server = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // loop until thread killed, waiting for connection from client
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // accept connection from client if one is requested
                    socket = server.accept();

                    // start a new communication thread to send data to the client
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        class CommunicationThread implements Runnable {
            // stores reference to socket connection
            private Socket clientSocket;

            // stores reference to printwriter for outputting to the client
            PrintWriter out;

            public CommunicationThread(Socket clientSocket) {
                // asigns reference to the socket connection
                this.clientSocket = clientSocket;

                try {
                    // initialize printwriter to output to the client
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void run() {

                while (!Thread.currentThread().isInterrupted()) {
                    // build comma-delimited string with joystick and head tracking data
                    String toClient = String.valueOf(x)+","+
                            String.valueOf(y)+","+
                            String.valueOf(z)+","+
                            String.valueOf(rz)+","+
                            String.valueOf(headRotate[0]*180/(Math.PI)+90)+","+
                            String.valueOf(headRotate[1]*180/(Math.PI)+90);

                    // write string to client
                    out.println(toClient);

                    // sleep thread for 100ms before sending new data
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

        }
    }
}
