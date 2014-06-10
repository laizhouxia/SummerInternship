package com.example.myapplication.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.ddogleg.struct.FastQueue;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.android.ConvertBitmap;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;

import boofcv.gui.feature.AssociationPanel;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;

import java.util.ArrayList;
import java.util.List;





import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;


public class MainActivity extends ActionBarActivity {

    Button addImageIButton, addImageIIButton, matchButton;
    ImageView imageI, imageII;
    List<ImageInfor> imageList = new ArrayList<ImageInfor>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addImageIButton = (Button) findViewById(R.id.addImageIButton);
        addImageIIButton = (Button) findViewById(R.id.addImageIIButton);
        matchButton = (Button) findViewById(R.id.matchButton);

        imageI = (ImageView) findViewById(R.id.imageI);
        imageII = (ImageView) findViewById(R.id.imageII);

        addImageIButton.setOnClickListener(buttonOneListener);

        addImageIIButton.setOnClickListener(buttonTwoListener);

        matchButton.setOnClickListener(matchButtonListener);

    }

    public View.OnClickListener buttonOneListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select contact image"), 1);
        }
    };

    public View.OnClickListener buttonTwoListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select contact image"), 2);
        }
    };

    public View.OnClickListener matchButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            match(imageList.size()-2 ,imageList.size()-1);
        }
    };

    private <T extends ImageSingleBand, TD extends TupleDesc> void match(int one, int two){
        Bitmap imgOne = imageList.get(one).getImg();
        Bitmap imgTwo = imageList.get(two).getImg();

        Class imageType = ImageFloat32.class;
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null,null, imageType);

        ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

        T inputA = (T)ConvertBitmap.bitmapToGray(imgOne,  null, imageType, null);
                //ConvertBufferedImage.convertFromSingle(imageA, null, imageType);
        T inputB = (T)ConvertBitmap.bitmapToGray(imgTwo,  null, imageType, null);

        List<Point2D_F64> pointsA = new ArrayList<Point2D_F64>();
        List<Point2D_F64> pointsB = new ArrayList<Point2D_F64>();

        FastQueue<TD> descA = UtilFeature.createQueue(detDesc, 100);
        FastQueue<TD> descB = UtilFeature.createQueue(detDesc,100);

        detDesc.detect(inputA);
        for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
            pointsA.add( detDesc.getLocation(i).copy() );
            descA.grow().setTo(detDesc.getDescription(i));
        }

        detDesc.detect(inputB);
        for( int i = 0; i < detDesc.getNumberOfFeatures(); i++ ) {
            pointsB.add( detDesc.getLocation(i).copy() );
            descB.grow().setTo(detDesc.getDescription(i));
        }


        associate.setSource(descA);
        associate.setDestination(descB);
        associate.associate();

        FastQueue<AssociatedIndex> matches = associate.getMatches();

        Bitmap reImgOne = imgOne.copy(imgOne.getConfig(), true);
        Bitmap reImgTwo = imgTwo.copy(imgTwo.getConfig(), true);


        for( int i = 0; i < matches.size; i++ ) {
            AssociatedIndex a = matches.data[i];

            ScalePoint point1 = imageList.get(one).getResultPoints().get(a.src);
            reImgOne.setPixel((int)point1.x, (int)point1.y, Color.RED);


            ScalePoint point2 = imageList.get(two).getResultPoints().get(a.src);
            reImgTwo.setPixel((int)point2.x, (int)point2.y, Color.RED);

        }

        System.out.println("Matches:" + associate.getMatches().size());

        imageI.setImageBitmap(reImgOne);
        imageII.setImageBitmap(reImgTwo);
      //  AssociationPanel panel = new AssociationPanel(20);
       // panel.setAssociation(pointsA,pointsB,associate.getMatches());



    }



    public void onActivityResult(int reqCode, int resCode, Intent imageReturnedIntent){

        super.onActivityResult(reqCode, resCode, imageReturnedIntent);

        if (resCode == RESULT_OK){

            Uri selectedImage = imageReturnedIntent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

            ImageFloat32 img = new ImageFloat32(yourSelectedImage.getWidth(), yourSelectedImage.getHeight());
            byte[] workBuffer = ConvertBitmap.declareStorage(yourSelectedImage, null);
            img = ConvertBitmap.bitmapToGray(yourSelectedImage, img, workBuffer);
            // get the ImageFloat32 image

            List<SurfFeature> descriptions = new ArrayList<SurfFeature>();
            List<ScalePoint> resultPoints = harder(img, descriptions);
            // get the feature points


            System.out.println("Found Features: "+resultPoints.size());

            for (int i = 0; i< descriptions.get(0).getValue().length; i++) {
                System.out.println("First descriptor's "+i + " value: " + descriptions.get(0).value[i]);
            }
            System.out.println("First descriptor's first location: "+resultPoints.get(0).x+"," +resultPoints.get(0).y);

            Bitmap resultImage = yourSelectedImage.copy(yourSelectedImage.getConfig(), true);
                // Bitmap enable change pixel.

            imageList.add(new ImageInfor(yourSelectedImage, descriptions, resultPoints));

            for( ScalePoint p : resultPoints ) {

                resultImage.setPixel((int)p.x, (int)p.y, Color.RED);
                resultImage.setPixel((int)p.x-1, (int)p.y, Color.RED);
                resultImage.setPixel((int)p.x+1, (int)p.y, Color.RED);
                resultImage.setPixel((int)p.x, (int)p.y-1, Color.RED);
                resultImage.setPixel((int)p.x, (int)p.y+1, Color.RED);
                resultImage.setPixel((int)p.x-1, (int)p.y-1, Color.RED);
                resultImage.setPixel((int)p.x+1, (int)p.y-1, Color.RED);
                resultImage.setPixel((int)p.x-1, (int)p.y+1, Color.RED);
                resultImage.setPixel((int)p.x+1, (int)p.y+1, Color.RED);

            }

            switch (reqCode) {
                case 1:
                    imageI.setImageBitmap(resultImage);
                    break;
                case 2:
                    imageII.setImageBitmap(resultImage);
                    break;
            }

        }

    }



    public static <II extends ImageSingleBand> List<ScalePoint> harder( ImageFloat32 image_ , List<SurfFeature> descriptions) {
        // SURF works off of integral images
        ImageFloat32 image = image_;
        Class<II> integralType = GIntegralImageOps.getIntegralType(ImageFloat32.class);

        // define the feature detection algorithm
        NonMaxSuppression extractor =
                FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 0, 5, true));
        FastHessianFeatureDetector<II> detector =
                new FastHessianFeatureDetector<II>(extractor,200,2, 9,4,4);

        // estimate orientation
        OrientationIntegral<II> orientation =
                FactoryOrientationAlgs.sliding_ii(null, integralType);

        DescribePointSurf<II> descriptor = FactoryDescribePointAlgs.<II>surfStability(null, integralType);

        // compute the integral image of 'image'
        II integral = GeneralizedImageOps.createSingleBand(integralType, image.width, image.height);
        GIntegralImageOps.transform(image, integral);

        // detect fast hessian features
        detector.detect(integral);
        // tell algorithms which image to process
        orientation.setImage(integral);
        descriptor.setImage(integral);

        List<ScalePoint> points = detector.getFoundPoints();


        for( ScalePoint p : points ) {
            // estimate orientation
            orientation.setScale(p.scale);
            double angle = orientation.compute(p.x,p.y);

            // extract the SURF description for this region
            SurfFeature desc = descriptor.createDescription();
            descriptor.describe(p.x,p.y,angle,p.scale,desc);

            // save everything for processing later on
            descriptions.add(desc);
        }

        return points;
    }



    // this line is for testing commit in Github
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
