package com.example.lkduy.remotehelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
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

public class DeviceScreenFinder {
    public static final String paramScrTopLeftX = "ScrTopLeftX";
    public static final String paramScrTopLeftY = "ScrTopLeftY";
    public static final String paramScrTopRightX = "ScrTopRightX";
    public static final String paramScrTopRightY = "ScrTopRightY";
    public static final String paramScrBottomRightX = "ScrBottomRightX";
    public static final String paramScrBottomRightY = "ScrBottomRightY";
    public static final String paramScrBottomLeftX = "ScrBottomLeftX";
    public static final String paramScrBottomLeftY = "ScrBottomLeftY";
    List<MatOfPoint> screenContoursInCalib = new ArrayList<>();
    private boolean isSearchingScreen = false;
    private Context context;
    public boolean IsSearchingScreen(){
        return isSearchingScreen;
    }
    private Mat screenUnwrapMat;
    public void setScreenUnwrapMat(Mat mat){
        screenUnwrapMat = mat;
    }
    private IScreenFoundEventHandler screenFoundEventHandler;
    public void setScreenFoundEventHandler(IScreenFoundEventHandler handler){
        screenFoundEventHandler = handler;
    }
    public DeviceScreenFinder(Context ctx){
        context = ctx;
    }
    public void startCalibrating(){
        isSearchingScreen = true;
        screenContoursInCalib.clear();
    }
    Mat ycrcbImg = null;
    public Mat findDeviceScreenInImage(Mat rgbImg){
        if(ycrcbImg == null){
            ycrcbImg = new Mat(rgbImg.rows(),rgbImg.cols(),rgbImg.type());
        }
        Imgproc.cvtColor(rgbImg, ycrcbImg,Imgproc.COLOR_RGB2YCrCb);
        Mat thresholded = new Mat(rgbImg.rows(),rgbImg.cols(), CvType.CV_8UC4);
        //Core.inRange(rgbImg,new Scalar(150, 0, 0,255),new Scalar(255,125,125,255),thresholded);
        Core.inRange(ycrcbImg,new Scalar(0,150,0,255), new Scalar(255,255,150,255), thresholded);
        if(!isSearchingScreen){
            Point[] screenLocations = getScreenLocation();
            Imgproc.circle(rgbImg,screenLocations[0],10,new Scalar(255,0,0),10);
            Imgproc.circle(rgbImg,screenLocations[1],10,new Scalar(0,255,0),10);
            Imgproc.circle(rgbImg,screenLocations[2],10,new Scalar(0,0,255),10);
            Imgproc.circle(rgbImg,screenLocations[3],10,new Scalar(255,255,255),10);
            return rgbImg;
        }
        //find contours in thresholded image
        Mat cloneThresholded = thresholded.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cloneThresholded,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
        hierarchy.release();
        List<MatOfPoint> listRectContours = new ArrayList<MatOfPoint>();
        double minArea = thresholded.width() * thresholded.height()*0.05;
        for(int contourIdx = 0; contourIdx < contours.size(); contourIdx++){
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contourIdx).toArray());

            double epsilon = 0.02*Imgproc.arcLength(contour2f,true);
            MatOfPoint2f approxCurve2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f,approxCurve2f, epsilon, true);
            MatOfPoint approxCurve = new MatOfPoint(approxCurve2f.toArray());
            if(approxCurve.total() == 4 && Imgproc.isContourConvex(approxCurve) && Imgproc.contourArea(approxCurve)>= minArea){
                listRectContours.add(approxCurve);
                //Imgproc.drawContours(thresholded,contours,contourIdx,new Scalar(120,120,120),20);
            }
        }
        MatOfPoint screenContour = null;
        for(int i=0; i< listRectContours.size(); i++){
            if(screenContour == null){
                screenContour = listRectContours.get(i);
            }
            else{
                if(Imgproc.contourArea(screenContour) < Imgproc.contourArea(listRectContours.get(i))){
                    screenContour = listRectContours.get(i);
                }
            }
        }
        if(screenContour != null){
            screenContoursInCalib.add(screenContour);
            List<MatOfPoint> tempScreenContourList = new ArrayList<MatOfPoint>();
            tempScreenContourList.add(screenContour);
            Imgproc.drawContours(thresholded,tempScreenContourList,0,new Scalar(120,120,120),20);
        }
        if(screenContoursInCalib.size() >= 30){
            isSearchingScreen = false;
            Point[] meanScreenLocation = computeMeanScreenLocation(screenContoursInCalib);
            //since the image is rotated 90 CCW, we need to rotate the corners 90 CW back
            /*for(int i=0; i<meanScreenLocation.length; i++){
                Point p = meanScreenLocation[i];
                Point rotatedP = new Point();
                rotatedP.x = rgbImg.height() - p.y;
                rotatedP.y = p.x;
                meanScreenLocation[i] = rotatedP;
            }*/
            //save the screen location
            SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
            prefsEditor.putFloat(paramScrTopLeftX, (float)meanScreenLocation[0].x);
            prefsEditor.putFloat(paramScrTopLeftY, (float)meanScreenLocation[0].y);
            prefsEditor.putFloat(paramScrTopRightX, (float)meanScreenLocation[1].x);
            prefsEditor.putFloat(paramScrTopRightY, (float)meanScreenLocation[1].y);
            prefsEditor.putFloat(paramScrBottomRightX, (float)meanScreenLocation[2].x);
            prefsEditor.putFloat(paramScrBottomRightY, (float)meanScreenLocation[2].y);
            prefsEditor.putFloat(paramScrBottomLeftX, (float)meanScreenLocation[3].x);
            prefsEditor.putFloat(paramScrBottomLeftY, (float)meanScreenLocation[3].y);
            prefsEditor.commit();
            if(screenFoundEventHandler != null){
                screenFoundEventHandler.screenFound();
            }
        }
        return thresholded;
    }
    public Mat getScreenUnwrappingMatrix(int screenW, int screenH){
        Point[] screenLocations = getScreenLocation();
        Mat src_Mat = new Mat(4,1, CvType.CV_32FC2);
        src_Mat.put(0,0,
                screenLocations[0].x, screenLocations[0].y,
                screenLocations[1].x, screenLocations[1].y,
                screenLocations[2].x, screenLocations[2].y,
                screenLocations[3].x, screenLocations[3].y);
        Mat dst_Mat = new Mat(4, 1, CvType.CV_32FC2);
        dst_Mat.put(0,0,
                0,0,
                screenW, 0,
                screenW, screenH,
                0, screenH);
        /*dst_Mat.put(0,0,
                0,0,
                screenH, 0,
                screenH, screenW,
                0, screenW);*/
        return Imgproc.getPerspectiveTransform(src_Mat, dst_Mat);
    }
    Mat screenArea;
    public Mat extractScreenAreaFromImage(Mat img,int outputW,int outputH){
        if(screenArea == null) {
            screenArea = new Mat(outputH, outputW, img.type());
        }
        if(screenUnwrapMat == null){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            if(prefs.contains(paramScrTopLeftX)){
                screenUnwrapMat = getScreenUnwrappingMatrix(outputW,outputH);
            }
        }
        if(screenUnwrapMat != null){
            Imgproc.warpPerspective(img, screenArea, screenUnwrapMat,new Size(outputW, outputH));
        }

        return screenArea;
    }
    public void reset(){
        screenUnwrapMat = null;
    }
    Point[] computeMeanScreenLocation(List<MatOfPoint> locationsCollection){
        Point[] meanLocation = new Point[4];
        for(int i=0; i< meanLocation.length; i++){
            meanLocation[i] = new Point(0,0);
        }
        for(int i=0; i<locationsCollection.size(); i++){
            Point[]  loc = locationsCollection.get(i).toArray();
            //loc = sortPoints(loc);
            for(int cornerIdx = 0; cornerIdx <meanLocation.length; cornerIdx++){
                meanLocation[cornerIdx].x += loc[cornerIdx].x;
                meanLocation[cornerIdx].y += loc[cornerIdx].y;
            }
        }
        for(int cornerIdx = 0; cornerIdx <meanLocation.length; cornerIdx++){
            meanLocation[cornerIdx].x /= locationsCollection.size();
            meanLocation[cornerIdx].y /= locationsCollection.size();
        }
        meanLocation = sortPoints(meanLocation);
        return meanLocation;
    }
    public Point[] getScreenLocation(){
        Point[] screenLocations = new Point[4];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        float x = prefs.getFloat(paramScrTopLeftX,0.0f);
        float y = prefs.getFloat(paramScrTopLeftY, 0.0f);
        screenLocations[0] = new Point(x,y);
        x = prefs.getFloat(paramScrTopRightX, 0.0f);
        y = prefs.getFloat(paramScrTopRightY, 0.0f);
        screenLocations[1] = new Point(x,y);
        x = prefs.getFloat(paramScrBottomRightX, 0.0f);
        y = prefs.getFloat(paramScrBottomRightY, 0.0f);
        screenLocations[2] = new Point(x,y);
        x = prefs.getFloat(paramScrBottomLeftX, 0.0f);
        y = prefs.getFloat(paramScrBottomLeftY, 0.0f);
        screenLocations[3] = new Point(x,y);
        return screenLocations;
    }
    Point[] sortPoints(Point[] unsorted){

        //find topleft in unsorted
        Point sysOrigin = new Point(0,0);
        double minToSysOrigin = Utilities.calDistanceBetweenTwoPoints(unsorted[0],sysOrigin);
        int topLeftIndexInUnsorted = 0;
        for(int i=1; i< unsorted.length; i++){
            double len = Utilities.calDistanceBetweenTwoPoints(unsorted[i], sysOrigin);
            if(len < minToSysOrigin){
                topLeftIndexInUnsorted = i;
                minToSysOrigin = len;
            }
        }
        Point topleft = unsorted[topLeftIndexInUnsorted];

        //find topright in unsorted
        double minYDifWithTopLeft = 9999999;
        int toprightIndexInUnsorted = 0;
        for(int i=0; i<unsorted.length; i++){
            if(i == topLeftIndexInUnsorted){
                continue;
            }
            double Ydif = unsorted[i].y - topleft.y;
            if(Ydif < 0){
                toprightIndexInUnsorted = i;
                break;
            }
            else
            {
                if(Ydif >= 0 && Math.abs(Ydif)<minYDifWithTopLeft ){
                    minYDifWithTopLeft = Math.abs(Ydif);
                    toprightIndexInUnsorted = i;
                }
            }
        }
        Point topright = unsorted[toprightIndexInUnsorted];
        //find bottom left
        double minXDifWithTopLeft = 999999;
        int bottomleftIndexInUnsorted = 0;
        for(int i=0; i<unsorted.length; i++){
            if(i == topLeftIndexInUnsorted || i == toprightIndexInUnsorted){
                continue;
            }
            double Xdif = unsorted[i].x - topleft.x;
            if(Xdif < 0){
                toprightIndexInUnsorted = i;
                break;
            }
            else
            {
                if(Xdif >= 0 && Math.abs(Xdif)<minXDifWithTopLeft ){
                    minXDifWithTopLeft = Math.abs(Xdif);
                    bottomleftIndexInUnsorted = i;
                }
            }
        }
        Point bottomleft = unsorted[bottomleftIndexInUnsorted];
        //find bottom right
        int bottomrightIndexInUnsorted = 0;
        for(int i=0; i< unsorted.length; i++){
            if(i != topLeftIndexInUnsorted
                    && i != toprightIndexInUnsorted
                    && i != bottomleftIndexInUnsorted){
                bottomrightIndexInUnsorted = i;
                break;
            }
        }
        Point bottomright = unsorted[bottomrightIndexInUnsorted];
        Point[] sorted = new Point[]{topleft, topright, bottomright, bottomleft};
        return sorted;
    }
    public interface IScreenFoundEventHandler{
        void screenFound();
    }
}
