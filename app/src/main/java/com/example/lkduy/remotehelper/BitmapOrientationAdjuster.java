package com.example.lkduy.remotehelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * Created by lkduy on 5/17/2017.
 */

public class BitmapOrientationAdjuster {
    Matrix matrix = null;
    public Bitmap RotateAndFlip(Bitmap bmp){
        Bitmap adjusted = Bitmap.createBitmap(bmp.getHeight()/8, bmp.getWidth()/8, Bitmap.Config.RGB_565);
        if(matrix == null){
            matrix = new Matrix();
            matrix.postRotate(90);
            matrix.postScale(0.125f,-0.125f);
            matrix.postTranslate(bmp.getHeight()/8, bmp.getWidth()/8);
        }
        Canvas adjustedCanvas = new Canvas(adjusted);
        adjustedCanvas.drawBitmap(bmp,matrix,null);
        return adjusted;
    }
    public Bitmap adjustCacheScreenshot(Bitmap screenshot, int dstW, int dstH){
        if(matrix == null){
            matrix = new Matrix();
            matrix.postRotate(90);
            matrix.postScale(1, -1);
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(screenshot,0,0,screenshot.getWidth(), screenshot.getHeight(),matrix,true);
        return Bitmap.createScaledBitmap(rotatedBitmap,dstW, dstH, true);
    }
}
