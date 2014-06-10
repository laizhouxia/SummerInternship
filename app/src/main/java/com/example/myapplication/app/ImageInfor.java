package com.example.myapplication.app;

import java.util.ArrayList;
import java.util.List;

import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;

/**
 * Created by nuonuomisu on 10/6/14.
 */
public class ImageInfor {

    List<SurfFeature> descriptions;
    List<ScalePoint> resultPoints;

    public ImageInfor(List<SurfFeature> descriptions, List<ScalePoint> resultPoints) {
        this.descriptions = descriptions;
        this.resultPoints = resultPoints;
    }


}
