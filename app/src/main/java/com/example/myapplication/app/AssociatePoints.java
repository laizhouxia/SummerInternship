package com.example.myapplication.app;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.feature.UtilFeature;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import boofcv.android.ConvertBitmap;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;
import org.ejml.data.DenseMatrix64F;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.ModelManagerEpipolarMatrix;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import org.ejml.alg.dense.decomposition.svd.SafeSvd;
import org.ejml.data.FixedMatrix3x3_64F;

public class AssociatePoints<T extends ImageSingleBand, TD extends TupleDesc> {

    // algorithm used to detect and describe interest points
    DetectDescribePoint<T, TD> detDesc;
    // Associated descriptions together by minimizing an error metric
    AssociateDescription<TD> associate;

    // location of interest points
    public List<Point2D_F64> pointsA;
    public List<Point2D_F64> pointsB;

    private DenseMatrix64F F;


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
            if(associate.getMatches().get(i).fitScore<0.1)
                res++;
        }
        System.out.println("this is good match: "+res);


        List<AssociatedPair> matches = new ArrayList<AssociatedPair>();
        FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();
        for( int i = 0; i < matchIndexes.size; i++ )
            if(associate.getMatches().get(i).fitScore<0.1){
                AssociatedIndex a = matchIndexes.get(i);
                AssociatedPair p = new AssociatedPair(pointsA.get(a.src) , pointsB.get(a.dst));
                matches.add(p);
            }

        // Where the fundamental matrix is stored

        // List of matches that matched the model
        List<AssociatedPair> inliers = new ArrayList<AssociatedPair>();

        // estimate and print the results using a robust and simple estimator
        // The results should be difference since there are many false associations in the simple model
        // Also note that the fundamental matrix is only defined up to a scale factor.
        F = robustFundamental(matches, inliers);
        System.out.println("Robust");
        F.print();

        F = simpleFundamental(matches);
        System.out.println("Simple");
        F.print();

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


    public static DenseMatrix64F robustFundamental( List<AssociatedPair> matches ,
                                                    List<AssociatedPair> inliers ) {

        // used to create and copy new instances of the fit model
        ModelManager<DenseMatrix64F> managerF = new ModelManagerEpipolarMatrix();
        // Select which linear algorithm is to be used.  Try playing with the number of remove ambiguity points
        Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_7_LINEAR, 2);
        // Wrapper so that this estimator can be used by the robust estimator
        GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

        // How the error is measured
        DistanceFromModelResidual<DenseMatrix64F,AssociatedPair> errorMetric =
                new DistanceFromModelResidual<DenseMatrix64F,AssociatedPair>(new FundamentalResidualSampson());

        // Use RANSAC to estimate the Fundamental matrix
        ModelMatcher<DenseMatrix64F,AssociatedPair> robustF =
                new Ransac<DenseMatrix64F, AssociatedPair>(123123,managerF,generateF,errorMetric,6000,0.1);

        // Estimate the fundamental matrix while removing outliers
        if( !robustF.process(matches) )
            throw new IllegalArgumentException("Failed");

        // save the set of features that were used to compute the fundamental matrix
        inliers.addAll(robustF.getMatchSet());

        // Improve the estimate of the fundamental matrix using non-linear optimization
        DenseMatrix64F F = new DenseMatrix64F(3,3);
        ModelFitter<DenseMatrix64F,AssociatedPair> refine =
                FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
        if( !refine.fitModel(inliers, robustF.getModelParameters(), F) )
            throw new IllegalArgumentException("Failed");

        // Return the solution
        return F;
    }


    public static DenseMatrix64F simpleFundamental( List<AssociatedPair> matches ) {
        // Use the 8-point algorithm since it will work with an arbitrary number of points
        Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_8_LINEAR, 0);

        DenseMatrix64F F = new DenseMatrix64F(3,3);
        if( !estimateF.process(matches,F) )
            throw new IllegalArgumentException("Failed");

        // while not done here, this initial linear estimate can be refined using non-linear optimization
        // as was done above.
        return F;
    }
}