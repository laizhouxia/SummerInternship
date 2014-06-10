package com.example.myapplication.app;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;

/**
 * Created by nuonuomisu on 10/6/14.
 */
public class ImageInfor {


    private Bitmap img;
    private List<SurfFeature> descriptions;
    private List<ScalePoint> resultPoints;

    public ImageInfor(Bitmap img, List<SurfFeature> descriptions, List<ScalePoint> resultPoints) {
        this.img = img;
        this.descriptions = descriptions;
        this.resultPoints = resultPoints;
    }

    public Bitmap getImg() {
        return img;
    }

    public List<SurfFeature> getDescriptions() {
        return descriptions;
    }

    public List<ScalePoint> getResultPoints() {
        return resultPoints;
    }
}
