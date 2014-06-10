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


import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.android.ConvertBitmap;
import boofcv.android.ImplConvertBitmap;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.feature.FancyInterestPointRender;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;


public class MainActivity extends ActionBarActivity {

    Button addImageIButton, addImageIIButton;
    ImageView imageI, imageII;
    List<ImageInfor> imageList = new ArrayList<ImageInfor>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addImageIButton = (Button) findViewById(R.id.addImageIButton);
        addImageIIButton = (Button) findViewById(R.id.addImageIIButton);

        imageI = (ImageView) findViewById(R.id.imageI);
        imageII = (ImageView) findViewById(R.id.imageII);

        addImageIButton.setOnClickListener(buttonOneListener);

        addImageIIButton.setOnClickListener(buttonTwoListener);
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

            imageList.add(new ImageInfor(descriptions, resultPoints));

            System.out.println("Found Features: "+resultPoints.size());
            System.out.println("First descriptor's first value: "+descriptions.get(0));
            System.out.println("First descriptor's first location: "+resultPoints.get(0).x+"," +resultPoints.get(0).y);

            Bitmap resultImage = yourSelectedImage.copy(yourSelectedImage.getConfig(), true);
                // Bitmap enable change pixel.

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
