package com.example.lkduy.remotehelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, IDataReceivedEvent, ConnectionEstablisedNotifier, IPointingEventTriggered{
    static {
        System.loadLibrary("opencv_java3");
    }
    private final String _TAG = "MainActivity:";
    Context mainContext;
    private CameraBridgeViewBase mOpenCvCameraView;
    RelativeLayout mainContainer;
    ImageView imvRemoteVideoFrame;
    private ImageView imvProcessed;
    AnnotationView annoView;
    NetworkCommunicator networkCom;
    AsyncDataReceiver networkDataReceiver;

    SkinCalibrator skinCalibrator;
    DeviceScreenFinder screenFinder;

    boolean isSendingHand = false;
    boolean isShowingResultWindow = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainContext = this;
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.main_openCVCamView);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mainContainer = (RelativeLayout)findViewById(R.id.mainContainer);
        imvRemoteVideoFrame = (ImageView)findViewById(R.id.imvRemoteVideoFrame);
        imvProcessed = (ImageView)findViewById(R.id.main_imvProcessed);

        annoView = new AnnotationView(this);
        RelativeLayout.LayoutParams annoViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        annoView.setLayoutParams(annoViewParams);
        mainContainer.addView(annoView);
        initNetworkHandlers();

        skinCalibrator = new SkinCalibrator(this);
        screenFinder = new DeviceScreenFinder(this);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_option_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()){
            case R.id.menuItem_omniCalib:
                Intent intent = new Intent(mainContext,MirrorImageCalibrator.class);
                startActivity(intent);
                break;
            case R.id.menuItem_sendHand:
                isSendingHand = !isSendingHand;
                if(isSendingHand == false){
                    item.setTitle("Turn on sending hand");
                }
                else
                {
                    item.setTitle("Turn off sending hand");
                }
                break;
            case R.id.main_menuItem_showResultWindow:
                isShowingResultWindow = !isShowingResultWindow;
                if(isShowingResultWindow){
                    imvProcessed.setVisibility(View.VISIBLE);
                    item.setTitle("Turn off show result");
                }
                else {
                    imvProcessed.setVisibility(View.INVISIBLE);
                    item.setTitle("Turn on show result");
                }
                break;
        }
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(annoView != null) {
            annoView.setPointingEventHandler(this);
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        skinCalibrator.loadSkinValBoundaries();
        screenFinder.reset();
    }
    void initNetworkHandlers(){
        networkCom = new NetworkCommunicator();
        networkCom.setConnectionEstablishedNotifier(this);
        networkCom.Connect();
    }
    @Override
    public void connectionEstablished(boolean result) {
        if(result == true) {
            networkDataReceiver = new AsyncDataReceiver(networkCom.getSocket());
            networkDataReceiver.setDataReceivedEventHandler(this);
            new Thread(networkDataReceiver).start();
        }
    }
    @Override
    protected void onDestroy() {
        //dataReceiver.stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String TAG = new StringBuilder(_TAG).append("onDestroy").toString();
        Log.i(TAG, "Disabling a camera view");

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        super.onDestroy();
        super.onDestroy();
    }

    Bitmap remoteVideoFrame;
    private BitmapFactory.Options bitmap_options = new BitmapFactory.Options();
    @Override
    public void dataReceived(int messageCode, Object[] messageParams) {
        if(messageCode == 9001){
            //handle image message
            byte[] bitmapData_64Based = (byte[]) messageParams[0];
            byte[] rawData = Base64.decode(bitmapData_64Based, Base64.DEFAULT);
            remoteVideoFrame = BitmapFactory.decodeByteArray(rawData, 0, rawData.length, bitmap_options);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        imvRemoteVideoFrame.setImageBitmap(remoteVideoFrame);

                    }catch (Exception ex){
                        Log.i("ParsingFrame", "Error parsing");
                    }
                }
            });
        }
    }


    @Override
    public void HandlePointingEvent(int pathID, float x, float y, int eventType) {
        Object[] messageParams = new Object[5];
        messageParams[0] = 9002;
        messageParams[1] = pathID;
        messageParams[2] = x;
        messageParams[3] = y;
        messageParams[4] = eventType;
        AsyncDataSendingTask sendingTask = new AsyncDataSendingTask(networkCom.getSocket());
        sendingTask.execute(messageParams);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    Mat origin;
    Mat cloneOrigin;
    Mat croppedOrigin = null;
    Mat cloneScreenArea;
    Rect roi = new Rect();
    Mat screenArea;
    Mat cacheScreenImg;
    Mat maskedScreenArea;
    Mat bufMaskedScreenArea;
    Mat skinMask;
    Mat backgroundRemoval;
    Mat outcome;
    Mat opaqueMask;
    BitmapOrientationAdjuster adjuster = new BitmapOrientationAdjuster();
    Mat diffImg;
    List<Mat> channels = new ArrayList<Mat>();
    HandSegmenter handSegmenter = new HandSegmenter();
    Mat blankHand = null;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        origin = inputFrame.rgba();
        if(isSendingHand == false){
            //send a blank picture
            if(blankHand == null) {
                blankHand = new Mat(mainContainer.getHeight() / 8, mainContainer.getWidth() / 8, CvType.CV_8UC4);
                blankHand.setTo(new Scalar(0,0,0,0));
            }
            Bitmap bmpProcessed = Utilities.getBitmapOfMat(blankHand, true);
            Object[] messageParams = new Object[2];
            messageParams[0] = 9003;
            messageParams[1] = Base64.encodeToString(Utilities.getBytesFromBitmap(bmpProcessed,true),Base64.DEFAULT);
            AsyncDataSendingTask sendingTask = new AsyncDataSendingTask(networkCom.getSocket());
            sendingTask.execute(messageParams);
            return  origin;
        }
        cloneOrigin = origin.clone();
        roi.x = origin.width()/2;
        roi.y = 0;
        roi.width = origin.width()/2;
        roi.height = origin.height();
        croppedOrigin = new Mat(origin, roi);
        screenArea = screenFinder.extractScreenAreaFromImage(croppedOrigin,mainContainer.getHeight()/8,mainContainer.getWidth()/8);
        cloneScreenArea = screenArea.clone();
        final Bitmap cacheScreenBmp = Utilities.getScreenshot(mainContainer);
        final Bitmap adjustedCacheScreenBmp = adjuster.RotateAndFlip(cacheScreenBmp);
        if(cacheScreenImg == null){
            cacheScreenImg = new Mat(adjustedCacheScreenBmp.getHeight(),adjustedCacheScreenBmp.getWidth(), CvType.CV_8UC4);
        }
        Utils.bitmapToMat(adjustedCacheScreenBmp,cacheScreenImg);
        Mat cloneCacheScreen = new Mat(cacheScreenImg.rows(), cacheScreenImg.cols(),CvType.CV_8UC4);
        cloneCacheScreen.setTo(new Scalar(0,0,0,255));
        cacheScreenImg.copyTo(cloneCacheScreen);
        Imgproc.cvtColor(cacheScreenImg,cacheScreenImg,Imgproc.COLOR_RGB2YCrCb);
        Imgproc.cvtColor(screenArea,screenArea, Imgproc.COLOR_RGB2YCrCb);
        if(diffImg == null) {
            diffImg = new Mat(cacheScreenImg.rows(), cacheScreenImg.height(), cacheScreenImg.type());
        }
        Core.subtract(cacheScreenImg,screenArea,diffImg);
        Core.split(diffImg,channels);
        if(backgroundRemoval == null){
            backgroundRemoval = new Mat(screenArea.rows(),screenArea.cols(),CvType.CV_8UC1);
        }
        if(maskedScreenArea == null){
            maskedScreenArea = new Mat(screenArea.rows(), screenArea.cols(), screenArea.type());
        }
        backgroundRemoval.setTo(new Scalar(0,0,0,255));
        channels.get(2).copyTo(backgroundRemoval);
        Imgproc.threshold(backgroundRemoval, backgroundRemoval,1, 255,Imgproc.THRESH_BINARY);
        Imgproc.GaussianBlur(backgroundRemoval,backgroundRemoval, new Size(5,5),2);
        Imgproc.erode(backgroundRemoval,backgroundRemoval,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9)));
        Imgproc.dilate(backgroundRemoval, backgroundRemoval,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(51, 11)));
        Imgproc.GaussianBlur(backgroundRemoval,backgroundRemoval, new Size(15,5),2);
        Imgproc.threshold(backgroundRemoval, backgroundRemoval,1, 255,Imgproc.THRESH_BINARY);
        maskedScreenArea.setTo(new Scalar(0,0,0));
        cloneScreenArea.copyTo(maskedScreenArea,backgroundRemoval);
        skinMask = skinCalibrator.detectSkin(maskedScreenArea);
        if(outcome == null){
            outcome = new Mat(screenArea.rows(),screenArea.cols(),CvType.CV_8UC4);
        }

        //Core.addWeighted(backgroundRemoval,0.5,skinMask,0.5,0,skinMask);
        outcome.setTo(new Scalar(0,0,0,0));
        cloneScreenArea.copyTo(outcome,skinMask);
        Imgproc.GaussianBlur(outcome,outcome,new Size(5,5),2);
        skinMask.release();
        cloneScreenArea.release();
        final Bitmap bmpProcessed = Utilities.getBitmapOfMat(outcome, true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imvProcessed.setImageBitmap(bmpProcessed);
            }
        });
        //send over hand image
        Object[] messageParams = new Object[2];
        messageParams[0] = 9003;
        messageParams[1] = Base64.encodeToString(Utilities.getBytesFromBitmap(bmpProcessed,true),Base64.DEFAULT);
        AsyncDataSendingTask sendingTask = new AsyncDataSendingTask(networkCom.getSocket());
        sendingTask.execute(messageParams);
        return origin;
    }
}
