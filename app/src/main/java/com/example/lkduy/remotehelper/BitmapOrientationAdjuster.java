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
            matrix.postScale(0.5f,-0.5f);
            matrix.postTranslate(bmp.getHeight()/8, bmp.getWidth()/8);
        }
        Canvas adjustedCanvas = new Canvas(adjusted);
        adjustedCanvas.drawBitmap(bmp,matrix,null);
        return adjusted;
    }
}
