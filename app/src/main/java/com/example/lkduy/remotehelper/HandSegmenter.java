package com.example.lkduy.remotehelper;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkduy on 5/18/2017.
 */

public class HandSegmenter {
    Mat fgModel, bgModel;
    Rect grabCutRect = new Rect();
    Mat bufOriginImg;
    Size smallSize = null;
    public Mat segmentHand(Mat originImg,Mat backgroundSubtractionMask){
        if(smallSize == null){
            smallSize = new Size((int)(originImg.width()/4), (int)(originImg.height()/4));
        }
        Mat scaledOrigin = new Mat();
        Imgproc.resize(originImg,scaledOrigin,smallSize);
        Mat scaledMask = new Mat();
        Imgproc.resize(backgroundSubtractionMask, scaledMask, smallSize);
       //find all the contours in the background subtraction mask
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(scaledMask,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
        for(int contourIdx = 0; contourIdx < contours.size(); contourIdx++){
            MatOfPoint curContour = contours.get(contourIdx);
            double contourArea = Imgproc.contourArea(curContour);

        }
        hierarchy.release();
        contours.clear();
        if(fgModel == null){
            fgModel = new Mat();
        }
        if(bgModel == null){
            bgModel = new Mat();
        }
        fgModel.setTo(new Scalar(255,255,255));
        bgModel.setTo(new Scalar(255,255,255));
        if(bufOriginImg == null){
            bufOriginImg = new Mat(scaledOrigin.rows(),scaledOrigin.cols(), CvType.CV_8UC3);
        }
        Imgproc.cvtColor(scaledOrigin,bufOriginImg,Imgproc.COLOR_BGRA2RGB);
        grabCutRect.x = grabCutRect.y = 30;
        grabCutRect.width = (int)(smallSize.width/2);
        grabCutRect.height = (int)(smallSize.height/2);
        Mat grabCutMask = new Mat(scaledMask.rows(),scaledMask.cols(),scaledMask.type());
        grabCutMask.setTo(new Scalar(125));
        Imgproc.grabCut(bufOriginImg,grabCutMask,grabCutRect,bgModel,fgModel,5,Imgproc.GC_INIT_WITH_RECT);
        return grabCutMask;
    }
}
