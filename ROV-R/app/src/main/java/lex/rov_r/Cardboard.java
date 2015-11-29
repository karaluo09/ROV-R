package lex.rov_r;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.net.Uri;
import android.widget.VideoView;

public class Cardboard extends Activity {
    private final static String videoPath = "http://www.html5tutorial.info/media/html5iscool.mp4";
    private static ProgressDialog progressDialog;
    VideoView videoView_left;
    VideoView videoView_right;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        videoView_left = (VideoView) findViewById(R.id.leftEye);
        videoView_right = (VideoView) findViewById(R.id.rightEye);

        progressDialog = ProgressDialog.show(Cardboard.this, "", "Buffering video...", true);
        progressDialog.setCancelable(true);

        PlayVideo();
    }

    private void PlayVideo() {
        try {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);

            Uri video = Uri.parse(videoPath);
            videoView_left.setVideoURI(video);
            videoView_right.setVideoURI(video);
            videoView_left.requestFocus();
            videoView_right.requestFocus();
            videoView_left.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                public void onPrepared(MediaPlayer mp) {
                    progressDialog.dismiss();
                    videoView_left.start();
                }
            });

            videoView_right.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                public void onPrepared(MediaPlayer mp) {
                    progressDialog.dismiss();
                    videoView_right.start();
                }
            });


        } catch (Exception e) {
            progressDialog.dismiss();
            System.out.println("Video Play Error :" + e.toString());
            finish();
        }

    }
}
