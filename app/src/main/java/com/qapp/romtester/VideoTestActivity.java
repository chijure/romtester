package com.qapp.romtester;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_VIDEO_CAPTURE = 1;

    private TextView statusText;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        statusText = findViewById(R.id.text_video_status);
        videoView = findViewById(R.id.video_view);
        videoView.setMediaController(new MediaController(this));

        Button recordButton = findViewById(R.id.button_record_video);
        recordButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_record_video) {
            recordVideo();
        }
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
        } else {
            statusText.setText(getString(R.string.video_no_camera_app));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VIDEO_CAPTURE) {
            return;
        }
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri videoUri = data.getData();
            statusText.setText(getString(R.string.video_status_playing_back));
            videoView.setVideoURI(videoUri);
            videoView.start();
        } else {
            statusText.setText(getString(R.string.video_status_cancelled));
        }
    }
}
