package com.example.lkduy.remotehelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.ByteArrayOutputStream;

/**
 * Created by lkduy on 5/17/2017.
 */

public class Utilities {
    public  static Bitmap getBitmapOfMat(Mat img, boolean preserveTransparency){
        Bitmap bmp = null;
        try {
            if(preserveTransparency) {
                bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            }
            else {
                bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.RGB_565);
            }
            Utils.matToBitmap(img, bmp);
        } catch (CvException e) {
            Log.d("SAVING IMAGE", e.getMessage());
        }
        return bmp;
    }
    public  static byte[] getBytesFromBitmap(Bitmap bitmap, boolean preserveTransparency){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(preserveTransparency){
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
        }else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        }
        return stream.toByteArray();
    }
    public static double calDistanceBetweenTwoPoints(Point p1, Point p2){
        return Math.sqrt((p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y));
    }
    public static Bitmap getScreenshot(View view){
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),view.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public static Point rotatePointAroundPointAnAngle(Point p, double angle, Point center){
        Point rotatedP = new Point();
        double shiftX = p.x - center.x;
        double shiftY = p.y - center.y;
        double rotatedX = shiftX*Math.cos(angle) - shiftY*Math.sin(angle);
        double rotatedY = shiftX*Math.sin(angle) + shiftY*Math.cos(angle);
        rotatedP.x = rotatedX + center.x;
        rotatedP.y = rotatedY + center.y;
        return rotatedP;
    }
}
