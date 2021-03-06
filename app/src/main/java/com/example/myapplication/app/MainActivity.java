package com.example.myapplication.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.android.ConvertBitmap;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import android.os.AsyncTask;


import boofcv.abst.calib.CalibrateMonoPlanar;
import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.io.*;
import java.util.*;
import java.text.*;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import java.util.ArrayList;
import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;


public class MainActivity<Desc extends TupleDesc> extends ActionBarActivity{

    //Global Constant
    static int TAKE_PICTURE = 1;
    private static final int IMAGE_CAPTURE = 0;


    // GUI components
    private Button button,button2,button3;  // The button
    private ImageView image;// ImageView


    //Global variable
    AssociatePoints app;
    List<Bitmap> photos;
    ExampleStructureFromMotion sfm;
    IntrinsicParameters intrinsic;
    private Uri imageUri;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Find references to the GUI objects
        button = (Button)findViewById(R.id.button);
        image = (ImageView)findViewById(R.id.image);
        button2 = (Button)findViewById(R.id.button2);
        button3 = (Button)findViewById(R.id.button3);

        // Set button's onClick listener object.
        button.setOnClickListener(new Button_Clicker1());
        button2.setOnClickListener(new Button_Clicker2());
        button3.setOnClickListener(new Button_Clicker3());

        //Initialize associatePoints class
        Class imageType = ImageFloat32.class;
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, imageType);
        ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
        app = new AssociatePoints(detDesc,associate,imageType);

        // SFM:
        sfm = new ExampleStructureFromMotion();

        //Initialize other golbal variables
        photos = new ArrayList<Bitmap>();

        // Does your device have a camera?
        if(hasCamera()){
            System.out.println("has camera");
        }

        // Do you have Camera Apps?
        if(hasDefaultCameraApp(MediaStore.ACTION_IMAGE_CAPTURE)){
            System.out.println("has camera app");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IMAGE_CAPTURE && resultCode == RESULT_OK){

            image.setImageURI(imageUri);

            System.out.println("imageUri");
            System.out.println("");
            System.out.println("");
            Bitmap bm;
            try
            {
                bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);


                int width = bm.getWidth();
                int height = bm.getHeight();
                float scaleWidth = ((float) 340) / width;
                float scaleHeight = ((float) 200) / height;
                // CREATE A MATRIX FOR THE MANIPULATION
                Matrix matrix = new Matrix();
                // RESIZE THE BIT MAP
                matrix.postScale(scaleWidth, scaleHeight);
                Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

                photos.add(resizedBitmap);

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }



/*            Bundle extras = intent.getExtras();


            Bitmap bitMap = (Bitmap) extras.get("data");
            System.out.println("test1");
            photos.add(bitMap);
            System.out.println("test2");
            image.setImageBitmap(bitMap);
            System.out.println("test3");*/


        }
    }

    private class AsyncTaskRunner extends AsyncTask<Object, Void, Boolean>{

        protected Boolean doInBackground(Object... param) {

            Bitmap yourSelectedImage = (Bitmap)param[0];
            Bitmap yourSelectedImage2 = (Bitmap)param[1];
            app.associate(yourSelectedImage,yourSelectedImage2);

            return true;
        }

        protected void onProgressUpdate()
        {
        }

        protected void onPostExecute(Intent data) {

        }

        protected void onPreExecute() {}
    }

    private class AsyncTaskRunnerSFM extends AsyncTask<Object, Void, Boolean>{

        protected Boolean doInBackground(Object... param) {



            sfm.process(intrinsic,photos);

            return true;
        }

        protected void onProgressUpdate()
        {
        }

        protected void onPostExecute(Intent data) {

        }

        protected void onPreExecute() {}
    }

    class Button_Clicker1 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentDateandTime = sdf.format(new Date());


            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, currentDateandTime);
            values.put(MediaStore.Images.Media.DESCRIPTION,
                    "Image capture by camera");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            imageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(intent, IMAGE_CAPTURE);
        }
    }

    class Button_Clicker2 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {
            System.out.println("The size of photos is"+photos.size());
            /*for(int i=0;i<photos.size()-1;i++) {
                Bitmap file1 = photos.get(i);
                Bitmap file2= photos.get(i+1);
                AsyncTaskRunner runner = new AsyncTaskRunner();
                runner.execute(file1, file2);
            }*/
            System.out.println("photos.size is : "+photos.size());
            AsyncTaskRunnerSFM runner = new AsyncTaskRunnerSFM();
            runner.execute();
        }
    }

    class Button_Clicker3 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {
            System.out.println("The size of photos issssss"+photos.size());


            // Detects the target and calibration point inside the target
            PlanarCalibrationDetector detector;

            // Description of the target's physical dimension
            PlanarCalibrationTarget target;
            // Use the wrapper below for chessboard targets.
            detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(8,8));

            // physical description
            target = FactoryPlanarCalibrationTarget.gridChess(8, 8, 30);

            CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector, false);

            // tell it type type of target and which parameters to estimate
            calibrationAlg.configure(target, true, 2);


            for(int i=0;i<photos.size();i++) {
                ImageFloat32 image = new ImageFloat32(photos.get(i).getWidth(),photos.get(i).getHeight());
                image = ConvertBitmap.bitmapToGray(photos.get(i), image, null);
                if( !calibrationAlg.addImage(image) )
                    System.err.println("Failed to detect target in " + i);
            }

            // process and compute intrinsic parameters
            intrinsic = calibrationAlg.process();
            photos.clear();

        }
    }

    // method to check if you have a Camera
    private boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // method to check you have Camera Apps
    private boolean hasDefaultCameraApp(String action){
        final PackageManager packageManager = getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

}
