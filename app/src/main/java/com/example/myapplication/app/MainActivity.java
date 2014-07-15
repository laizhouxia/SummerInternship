package com.example.myapplication.app;

import android.content.Intent;
import android.graphics.Bitmap;
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
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;


public class MainActivity<Desc extends TupleDesc> extends ActionBarActivity{

    //Global Constant
    static int TAKE_PICTURE = 1;

    // GUI components
    private Button button,button2,button3;  // The button
    private ImageView image;// ImageView


    //Global variable
    AssociatePoints app;
    List<Bitmap> photos;
    ExampleStructureFromMotion sfm;

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
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK && intent != null){
            // get bundle
            Bundle extras = intent.getExtras();

            // get bitmap
            Bitmap bitMap = (Bitmap) extras.get("data");
            System.out.println("test1");
            photos.add(bitMap);
            System.out.println("test2");
            image.setImageBitmap(bitMap);
            System.out.println("test3");


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

            IntrinsicParameters in = null;

            sfm.process(in, photos);

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

            // create intent with ACTION_IMAGE_CAPTURE action
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // start camera activity
            System.out.println(intent);
            startActivityForResult(intent, TAKE_PICTURE);
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

            AsyncTaskRunner runner = new AsyncTaskRunner();
            runner.execute();
        }
    }

    class Button_Clicker3 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {
            System.out.println("The size of photos issssss"+photos.size());
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
