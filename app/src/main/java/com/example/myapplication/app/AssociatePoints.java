package com.example.myapplication.app;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.feature.UtilFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import boofcv.android.ConvertBitmap;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;


public class AssociatePoints<T extends ImageSingleBand, TD extends TupleDesc> {

    // algorithm used to detect and describe interest points
    DetectDescribePoint<T, TD> detDesc;
    // Associated descriptions together by minimizing an error metric
    AssociateDescription<TD> associate;

    // location of interest points
    public List<Point2D_F64> pointsA;
    public List<Point2D_F64> pointsB;

    Class<T> imageType;

    public AssociatePoints(DetectDescribePoint<T, TD> detDesc,
                                  AssociateDescription<TD> associate,
                                  Class<T> imageType) {
        this.detDesc = detDesc;
        this.associate = associate;
        this.imageType = imageType;
    }

    /**
     * Detect and associate point features in the two images.  Display the results.
     */
    public void associate( Bitmap imageA , Bitmap imageB )
    {
        T inputA = ConvertBitmap.bitmapToGray(imageA, null, imageType, null);
        T inputB = ConvertBitmap.bitmapToGray(imageB, null, imageType, null);

        // stores the location of detected interest points
        pointsA = new ArrayList<Point2D_F64>();
        pointsB = new ArrayList<Point2D_F64>();

        // stores the description of detected interest points
        FastQueue<TD> descA = UtilFeature.createQueue(detDesc,100);
        FastQueue<TD> descB = UtilFeature.createQueue(detDesc,100);

        // describe each image using interest points
        describeImage(inputA,pointsA,descA);
        describeImage(inputB,pointsB,descB);

        // Associate features between the two images
        associate.setSource(descA);
        associate.setDestination(descB);
        associate.associate();

        System.out.println(pointsA);
        System.out.println(pointsB);
        System.out.println(associate.getMatches());

        System.out.println("this line is size"+associate.getMatches().size());
        int res = 0;
        for(int i=0;i<associate.getMatches().size();i++) {
            System.out.println(associate.getMatches().get(i).fitScore);
            if(associate.getMatches().get(i).fitScore<0.1)
                res++;
        }
        System.out.print("this is good match: "+res);
    }

    /**
     * Detects features inside the two images and computes descriptions at those points.
     */
    private void describeImage(T input, List<Point2D_F64> points, FastQueue<TD> descs )
    {
        detDesc.detect(input);

        for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
            points.add( detDesc.getLocation(i).copy() );
            descs.grow().setTo(detDesc.getDescription(i));
        }
    }

}