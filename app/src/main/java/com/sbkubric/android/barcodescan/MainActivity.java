package com.sbkubric.android.barcodescan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.hardware.Camera.PictureCallback;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements PictureCallback {
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private final String TAG = "BarcodeScannerApp";
    private String mPicturePath;
    private TextView mRawOutputTv;
    private MainActivity mMainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        safeCameraOpen(Camera.CameraInfo.CAMERA_FACING_BACK);
        setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
        mRawOutputTv = (TextView) findViewById(R.id.tv_raw_output);
        Button captureButton = (Button) findViewById(R.id.button_capture);
        mMainActivity = this;
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mMainActivity);
                processPicture();
            }
        });
    }

    private void processPicture() {
        Bitmap bitmap = BitmapFactory.decodeFile(mPicturePath);
        if (bitmap == null) {
            Toast.makeText(getApplicationContext(), R.string.failed_to_process_the_picture_toast, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        BarcodeDetector detector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .build();
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(frame);
        if (barcodes.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.failed_to_recognize_the_barcode_toast, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Barcode thiscode = barcodes.valueAt(0);
        mRawOutputTv.setText(thiscode.rawValue);
        if(!detector.isOperational()){
            Toast.makeText(getApplicationContext(), R.string.could_not_set_up_detector_toast, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), getString(R.string.failed_to_open_camera_toast));
            Toast.makeText(getApplicationContext(), R.string.failed_to_open_camera_toast, Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }

        return qOpened;
    }

    private void releaseCameraAndPreview() {
        mCameraPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

//    PictureCallback mMainActivity = new PictureCallback() {
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            File pictureFile = getOutputMediaFile();
//            if (pictureFile == null) {
//                return;
//            }
//            try {
//                mPicturePath = pictureFile.getAbsolutePath();
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//                camera.startPreview();
//            } catch (FileNotFoundException e) {
//
//            } catch (IOException e) {
//            }
//
//            try {
//                ExifInterface exifi = new ExifInterface(pictureFile.getAbsolutePath());
//                exifi.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
//                exifi.saveAttributes();
//            } catch (IOException e) {
//                Log.e(TAG, "Exif error");
//            }
//        }
//
//    };

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            return;
        }
        try {
            mPicturePath = pictureFile.getAbsolutePath();
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            camera.startPreview();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }

        try {
            ExifInterface exifi = new ExifInterface(pictureFile.getAbsolutePath());
            exifi.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            exifi.saveAttributes();
        } catch (IOException e) {
            Log.e(TAG, "Exif error");
        }
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "BarcodeScan");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("BarcodeScan", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCameraAndPreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera == null) {
            setContentView(R.layout.activity_main);
            safeCameraOpen(Camera.CameraInfo.CAMERA_FACING_BACK);
            setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
            mCameraPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mCameraPreview);

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseCameraAndPreview();
    }

}
