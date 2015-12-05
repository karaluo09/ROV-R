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
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.io.*;
import java.net.*;

import javax.microedition.khronos.egl.EGLConfig;

import lex.rov_r.R;

/**
 * A Cardboard sample application.
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private static final String TAG = "MainActivity";

    private Vibrator vibrator;
    private CardboardVideoView overlayView;

    private double x;
    private double y;
    private double z;
    private double rz;
    private float yaw;
    private float roll;

    private float[] headRotate;

    private ServerSocket server;
    Thread serverThread = null;

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRestoreGLStateEnabled(false);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        overlayView = (CardboardVideoView) findViewById(R.id.overlay);

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

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
        GLES20.glClearColor(1f, 1f, 1f, 0.5f); // Dark background so text shows up well.
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getEulerAngles(headRotate, 0);
        if(headRotate[2]*180/Math.PI>90){
            headRotate[2]=90;
        }
        else if (headRotate[2]*180/Math.PI<-90){
            headRotate[0]=-90;
        }
//        headTransform.getHeadView(headView, 0);
//        if (headView[0] == 1.0f)
//        {
//            (float)yaw = Math.atan2((float)headView[2], (float)headView[11]);
//            roll = 0;
//
//        }else if (headView[0] == -1.0f)
//        {
//            yaw = Math.atan2(headView[2], _34);
//            roll = 0;
//        }else
//        {
//
//            yaw = Math.atan2(-_31,_11);
//            roll = Math.atan2(-_23,_22);
//        }
//        float roll = (float) Math.atan2();
//        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

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

        //overlayView.show3DToast("Shiver me timbers!");

        // Always give user feedback.
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

    private void processJoystickInput(MotionEvent event, int historyPos) throws IOException {
        InputDevice mInputDevice = event.getDevice();
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
            Socket socket = null;
            try {
                server = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = server.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        class CommunicationThread implements Runnable {

            private Socket clientSocket;

            PrintWriter out;

            public CommunicationThread(Socket clientSocket) {

                this.clientSocket = clientSocket;

                try {
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void run() {

                while (!Thread.currentThread().isInterrupted()) {
                    String toClient = String.valueOf(x)+","+String.valueOf(y)+","+String.valueOf(z)+","+String.valueOf(rz);//+","+String.valueOf(headRotate[0]*180/(2*Math.PI)+90)+","+String.valueOf(headRotate[2]+90);
                    out.println(toClient);
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
