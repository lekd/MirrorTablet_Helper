package com.example.lkduy.remotehelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MirrorImageCalibrator extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, DeviceScreenFinder.IScreenFoundEventHandler, SkinCalibrator.ISkinCalibratedEventHandler{
    static {
        System.loadLibrary("opencv_java3");
    }
    Context mainContext;
    private final String _TAG = "CalibActivity:";

    private enum CalibViewState{
        Normal,
        CalibSkin,
        CalibScreen
    }

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView imvOriginFrame;
    private ImageView imvProcessed;
    private Button btnFindScreen;
    private Button btnCalibSkin;
    private Button btnBack;
    private TextView tvCalibStatus;
    ImageView calibSquare ;

    DeviceScreenFinder screenFinder ;
    SkinCalibrator skinCalibrator;
    CalibViewState viewState = CalibViewState.Normal;

    boolean isShowingWindowResult = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror_image_calibrator);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainContext = this;
        screenFinder = new DeviceScreenFinder(this);
        screenFinder.setScreenFoundEventHandler(this);
        skinCalibrator = new SkinCalibrator(this);
        skinCalibrator.setHandDetectionHandler(this);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.calibAct_openCVCamView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        imvOriginFrame = (ImageView)findViewById(R.id.calibAct_imvOriginFrame);
        imvProcessed = (ImageView)findViewById(R.id.calibAct_imv_processed);
        calibSquare = (ImageView)findViewById(R.id.calibAct_imvCalibSquare);

        btnFindScreen = (Button)findViewById(R.id.calibAct_btnCalibScreen);
        btnFindScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        calibSquare.setBackgroundColor(Color.rgb(0xcc,0,0));
                        setCalibStatusText("Calibrating Screen", 0xffff0000);
                        hideShowButton(false);
                    }
                });
                try {
                    Thread.sleep(500);
                    screenFinder.startCalibrating();
                    viewState = CalibViewState.CalibScreen;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        btnCalibSkin = (Button)findViewById(R.id.calibAct_btnCalibSkin);
        btnCalibSkin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewState = CalibViewState.CalibSkin;
                setCalibStatusText("Please place your open hand on the tablet",0xff0000ff);
                hideShowButton(false);
            }
        });
        btnBack = (Button)findViewById(R.id.calibAct_btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewState = CalibViewState.Normal;
                Intent returnIntent = new Intent();
                returnIntent.putExtra("Result",1);
                setResult(Activity.RESULT_OK, returnIntent);
                ((Activity)mainContext).finish();
            }
        });
        handleTouchOnCalibSquare();
        tvCalibStatus = (TextView)findViewById(R.id.calibAct_tvCalibStatus);
        tvCalibStatus.setTextColor(Color.rgb(255,255,0));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.calib_option_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.calib_menu_showResultWindow:
                isShowingWindowResult = !isShowingWindowResult;
                if(isShowingWindowResult){
                    item.setTitle("Turn off result window");
                    imvProcessed.setVisibility(View.VISIBLE);
                }
                else {
                    item.setTitle("Turn on result window");
                    imvProcessed.setVisibility(View.INVISIBLE);
                }
                break;
        }
        return true;
    }
    void hideShowButton(final boolean isShown){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isShown){
                    btnCalibSkin.setVisibility(View.VISIBLE);
                    btnFindScreen.setVisibility(View.VISIBLE);
                    btnBack.setVisibility(View.VISIBLE);
                }
                else{
                    btnCalibSkin.setVisibility(View.INVISIBLE);
                    btnFindScreen.setVisibility(View.INVISIBLE);
                    btnBack.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    void handleTouchOnCalibSquare(){
        calibSquare.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(viewState != CalibViewState.CalibSkin){
                    return false;
                }
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if(!skinCalibrator.IsCalibrating()) {
                            skinCalibrator.startCalibrating();

                        }
                        break;
                }
                return true;
            }
        });
    }
    void setCalibStatusText(final String text, final int textColor){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCalibStatus.setTextColor(textColor);
                tvCalibStatus.setText(text);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        String TAG = new StringBuilder(_TAG).append("onResume").toString();
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initiation");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback);
        } else {
            Log.i(TAG, "OpenCV library found inside package. Using it");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    protected void onPause() {
        String TAG = new StringBuilder(_TAG).append("onPause").toString();
        Log.i(TAG, "Disabling a camera view");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        String TAG = new StringBuilder(_TAG).append("onDestroy").toString();
        Log.i(TAG, "Disabling a camera view");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        super.onDestroy();
    }
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            String TAG = new StringBuilder(_TAG).append("onManagerConnected").toString();

            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    Mat rotatedOrigin = null;
    Mat bbox = null;
    Mat originRotMatrix = null;
    Mat screenImg = null;
    Mat skinDetected = null;
    Rect roi = new Rect();
    org.opencv.core.Size resultSize = null;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat origin = inputFrame.rgba();
        int maxDimension = origin.width()>origin.height()?origin.width():origin.height();
        if(bbox == null){
            bbox = new Mat(maxDimension,maxDimension,origin.type());
        }
        if (rotatedOrigin == null) {
            //croppedOrigin = new Mat(origin.cols(),origin.rows(),origin.type());

            resultSize = new Size(origin.height(),origin.width());
        }
        if(originRotMatrix == null){
            originRotMatrix = Imgproc.getRotationMatrix2D(new Point(bbox.width()/2,bbox.height()/2),90,1.0);

        }

        //Imgproc.warpAffine(origin,bbox,originRotMatrix,resultSize,Imgproc.INTER_LINEAR);
        //Core.flip(bbox,bbox,1);
        //croppedOrigin = bbox.clone();
        /*roi.x = roi.y = 0;
        roi.width = bbox.width();
        roi.height = bbox.height()/2;*/
        roi.x = origin.width()/2;
        roi.y = 0;
        roi.width = origin.width()/2;
        roi.height = origin.height();
        rotatedOrigin = new Mat(origin,roi);

        if(screenFinder.IsSearchingScreen()) {
            screenImg = screenFinder.findDeviceScreenInImage(rotatedOrigin);
        }
        else {
            screenImg = screenFinder.findDeviceScreenInImage(rotatedOrigin);
            screenImg = screenFinder.extractScreenAreaFromImage(rotatedOrigin,calibSquare.getHeight()/2,calibSquare.getWidth()/2);
            if(skinCalibrator.IsCalibrating()){
                skinDetected = skinCalibrator.calibrateSkinColor(screenImg);
            }
            else{
                skinDetected = skinCalibrator.detectSkin(screenImg);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Bitmap bmpOrigin = Utilities.getBitmapOfMat(croppedOrigin);
                //imvOriginFrame.setImageBitmap(bmpOrigin);
                if(skinDetected != null) {
                    Bitmap bmpProcessed = Utilities.getBitmapOfMat(screenImg, false);
                    imvProcessed.setImageBitmap(bmpProcessed);
                }
            }
        });


        return null;
    }

    @Override
    public void skinCalibrated() {
        viewState = CalibViewState.Normal;
        setCalibStatusText("",0x00000000);
        hideShowButton(true);
    }

    @Override
    public void screenFound() {
        screenFinder.setScreenUnwrapMat(screenFinder.getScreenUnwrappingMatrix(calibSquare.getHeight()/2,calibSquare.getWidth()/2));
        viewState = CalibViewState.Normal;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calibSquare.setBackgroundColor(Color.rgb(0,0,0x55));
            }
        });
        setCalibStatusText("",0x00000000);
        hideShowButton(true);
    }
}
