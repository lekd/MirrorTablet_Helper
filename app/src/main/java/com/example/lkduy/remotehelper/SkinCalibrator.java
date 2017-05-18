package com.example.lkduy.remotehelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkduy on 5/17/2017.
 */

public class SkinCalibrator {
    public final static String paramSkinMeanY = "SkinMeanY";
    public final static String paramSkinMeanCr = "SkinMeanCr";
    public final static String paramSkinMeanCb = "SkinMeanCb";
    public final static String paramSkinStdevY = "SkinStdevY";
    public final static String paramSkinStdevCr = "SkinStdevCr";
    public final static String paramSkinStdevCb = "SkinStdevCb";
    Context context;
    private ISkinCalibratedEventHandler skinCalibratedHandler = null;
    public void setHandDetectionHandler(ISkinCalibratedEventHandler handler){
        skinCalibratedHandler = handler;
    }
    private boolean isCalibrating = false;
    public boolean IsCalibrating(){
        return isCalibrating;
    }
    public void startCalibrating(){
        isCalibrating = true;
        Handler handler = new Handler();
        handler.postDelayed(runnableSchedulingStopCalibrating,4000);
    }
    public void stopCalibrating(){
        isCalibrating = false;
    }
    public SkinCalibrator(Context ctx){
        context = ctx;
    }
    private Mat prevCalibMask = null;
    public Mat calibrateSkinColor(Mat img){
        Imgproc.cvtColor(img,img,Imgproc.COLOR_RGB2YCrCb);
        Mat mask = detectSkinUsingRawThreshold(img);
        Mat maskedImg = new Mat(img.rows(),img.cols(),img.type());
        img.copyTo(maskedImg, mask);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdev = new MatOfDouble();
        Core.meanStdDev(img,mean,stdev,mask);
        if(prevCalibMask == null) {
            saveMeanStdevVal(mean,stdev);
            prevCalibMask = mask;
        }
        else {
            Scalar prevSumMask = Core.sumElems(prevCalibMask);
            Scalar curSumMask = Core.sumElems(mask);
            if(curSumMask.val[0] > prevSumMask.val[0]){
                saveMeanStdevVal(mean,stdev);
                prevCalibMask = mask;
            }
        }
        return mask;
    }
    Runnable runnableSchedulingStopCalibrating = new Runnable() {
        @Override
        public void run() {
            stopCalibrating();
            if(skinCalibratedHandler != null){
                skinCalibratedHandler.skinCalibrated();
            }
        }
    };
    protected void saveMeanStdevVal(MatOfDouble mean, MatOfDouble stdev){
        double[] meanArray = mean.toArray();
        double[] stdevArray = stdev.toArray();
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        prefEditor.putFloat(paramSkinMeanY, (float) meanArray[0]);
        prefEditor.putFloat(paramSkinMeanCr, (float) meanArray[1]);
        prefEditor.putFloat(paramSkinMeanCb, (float) meanArray[2]);
        prefEditor.putFloat(paramSkinStdevY, (float) stdevArray[0]);
        prefEditor.putFloat(paramSkinStdevCr, (float) stdevArray[1]);
        prefEditor.putFloat(paramSkinStdevCb, (float) stdevArray[2]);
        prefEditor.commit();
    }
    public Mat detectSkinUsingRawThreshold(Mat img){
        Mat yCrCb = new Mat(img.rows(),img.cols(),img.type());
        Mat thresholded = new Mat(img.height(),img.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(img,yCrCb, Imgproc.COLOR_RGB2YCrCb);
        //Core.inRange(yCrCb, new Scalar(20,85,135), new Scalar(255,135,180),thresholded);
        Scalar[] skinVals = getDefaultSkinVals();
        Core.inRange(yCrCb, skinVals[0], skinVals[1],thresholded);

        //Mat cloneThresholded = thresholded.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholded,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
        hierarchy.release();
        double minArea = img.width()*img.height()*0.05;
        Mat mask = new Mat(img.height(),img.width(), CvType.CV_8UC1);
        for(int contourIdx = 0; contourIdx < contours.size(); contourIdx++){
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());

            double epsilon = 0.0002*Imgproc.arcLength(contour2f,true);
            MatOfPoint2f approxCurve2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f,approxCurve2f, epsilon, true);
            MatOfPoint approxCurve = new MatOfPoint(approxCurve2f.toArray());
            if(Imgproc.contourArea(approxCurve)>= minArea){
                //Imgproc.drawContours(thresholded,contours, contourIdx,new Scalar(120,120,120),10);
                Imgproc.drawContours(mask,contours,contourIdx, new Scalar(255,255,255), -1);
            }
        }
        return mask;
    }
    Scalar[] skinValBoundaries =null;
    public void loadSkinValBoundaries(){
        skinValBoundaries = getSkinBoundaryVals();
    }
    Mat thresholded = null;
    Mat yCrCb = null;
    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    public Mat detectSkin(Mat img){
        if(yCrCb == null) {
            yCrCb = new Mat(img.rows(), img.cols(), img.type());
        }
        yCrCb.setTo(new Scalar(0,0,0,255));
        Imgproc.cvtColor(img,yCrCb, Imgproc.COLOR_RGB2YCrCb);
        //Core.inRange(yCrCb, new Scalar(20,85,135), new Scalar(255,135,180),thresholded);
        if(skinValBoundaries == null) {
            skinValBoundaries = getSkinBoundaryVals();
        }
        if(thresholded == null) {
            thresholded = new Mat(img.height(), img.width(), CvType.CV_8UC1);
        }
        thresholded.setTo(new Scalar(0,0,0,255));
        Core.inRange(yCrCb, skinValBoundaries[0], skinValBoundaries[1],thresholded);
        Imgproc.erode(thresholded, thresholded,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9)));
        Imgproc.dilate(thresholded, thresholded,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9)));
        return thresholded;
    }
    Scalar[] getDefaultSkinVals(){
        Scalar[] skinBoundaries = new Scalar[2];
        Scalar lowerSkinVal = new Scalar(20,95,65,255);
        Scalar upperSkinVal = new Scalar(255,195,155,255);
        skinBoundaries[0] = lowerSkinVal;
        skinBoundaries[1] = upperSkinVal;
        return skinBoundaries;
    }
    Scalar[] getSkinBoundaryVals(){
        Scalar[] skinBoundaries = getDefaultSkinVals();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if(prefs.contains(paramSkinMeanY)){
            float meanY = prefs.getFloat(paramSkinMeanY, 0.0f);
            float meanCr = prefs.getFloat(paramSkinMeanCr, 0.0f);
            float meanCb = prefs.getFloat(paramSkinMeanCb, 0.0f);
            float stdevY = prefs.getFloat(paramSkinStdevY, 0.0f);
            float stdevCr = prefs.getFloat(paramSkinStdevY, 0.0f);
            float stdevCb = prefs.getFloat(paramSkinStdevCb, 0.0f);
            Scalar lowerSkinVal = new Scalar(0,0,0,255);
            Scalar upperSkinVal = new Scalar(0,0,0,255);
            float ampVal = 2f;
            lowerSkinVal.set(new double[]{meanY - ampVal*stdevY, meanCr - ampVal*stdevCr, meanCb - ampVal*stdevCb, 255});
            upperSkinVal.set(new double[]{meanY + ampVal*stdevY, meanCr + ampVal*stdevCr, meanCb + ampVal*stdevCb, 255});
            skinBoundaries[0] = lowerSkinVal;
            skinBoundaries[1] = upperSkinVal;
        }
        return skinBoundaries;
    }
    interface ISkinCalibratedEventHandler {
        void skinCalibrated();
    }
}
