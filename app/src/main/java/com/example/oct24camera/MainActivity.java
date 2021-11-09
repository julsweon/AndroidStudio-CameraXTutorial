package com.example.oct24camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Button capture, analysiss, retake;
    String path = "";
    String date;
    //File file = new File(path);
    File root;
    PreviewView previewView;
    //public static final int LENS_FACING_FRONT = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);


        capture = findViewById(R.id.capture);
        capture.setOnClickListener(this);

        analysiss = findViewById(R.id.imageAnalyisis);
        analysiss.setOnClickListener(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        initCameraX(cameraProviderFuture);
    }

    private void initCameraX(ListenableFuture<ProcessCameraProvider> cameraProviderFuture) {
        capture.setText("CAPTURE");

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                startCameraX(cameraProvider);


            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // Choose the camera by requiring a lens facing, selector use case
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview = new Preview.Builder().build();

        // Connect the preview use case to the previewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Set up the capture use case to allow users to take photos
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageCapture, imageAnalysis);

    }

    private void capturePhoto() {
        File photoDir = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+"/CameraXPhotos");
        if (!photoDir.exists())
            photoDir.mkdir();

        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());
        String photoFilePath = photoDir.getAbsolutePath()+"/"+timestamp+".jpg";

        File photoFile = new File(photoFilePath);
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull @NotNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "photo saved successfully", Toast.LENGTH_SHORT).show();
                        Log.d("whatswrong", photoDir.getAbsolutePath()+"에 저장됨");

                        Drawable d = Drawable.createFromPath(String.valueOf(photoFile));
                        try {
                            cameraProviderFuture.get().unbindAll();
                            previewView.setBackground(d);

                            capture.setText("RETAKE");

                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(@NonNull @NotNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Photo save failed", Toast.LENGTH_SHORT).show();
                        Log.d("whatswrong", exception.toString()+"\n"+exception.getMessage());
                    }
                }
        );

    }

    private void imageAnalyze() {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull @NotNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                Log.d("whatswrong", "analyze: got the frame at: "+image.getImageInfo().getTimestamp());
                image.close();
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.capture:
                if (((Button)v).getText() == "CAPTURE")
                    capturePhoto();
                else if (((Button)v).getText() == "RETAKE")
                    initCameraX(cameraProviderFuture);
                break;

            case R.id.imageAnalyisis:
                imageAnalyze();
        }
    }

}