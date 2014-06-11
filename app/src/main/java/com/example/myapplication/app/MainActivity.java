package com.example.myapplication.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;



public class MainActivity<Desc extends TupleDesc> extends ActionBarActivity{

    //Global Constant
    private static int LOAD_IMAGE_RESULTS = 1;


    // GUI components
    private Button button,button2;  // The button
    private ImageView image,image2;// ImageView


    //Global variable
    int count = 0;
    String path1 = "";
    String path2 = "";
    AssociatePoints app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Find references to the GUI objects
        button = (Button)findViewById(R.id.button);
        image = (ImageView)findViewById(R.id.image);
        button2 = (Button)findViewById(R.id.button2);
        image2 = (ImageView)findViewById(R.id.image2);

        // Set button's onClick listener object.
        button.setOnClickListener(new Button_Clicker1());
        button2.setOnClickListener(new Button_Clicker2());

        //Initialize associatePoints class
        Class imageType = ImageFloat32.class;
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null, null, imageType);
        ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
        AssociateDescription associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
        app = new AssociatePoints(detDesc,associate,imageType);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOAD_IMAGE_RESULTS && resultCode == RESULT_OK && data != null) {
            associateTwoPhotos(data);
        }

    }

    private void associateTwoPhotos(Intent data) {
        Uri pickedImage = data.getData();
        String[] filePath = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
        cursor.moveToFirst();
        String imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
        System.out.println("image path "+imagePath);

        if(count == 0){
            count++;
            path1 = imagePath;
            image.setImageBitmap(BitmapFactory.decodeFile(path1));
            return;
        }
        else{
            path2 = imagePath;
            image2.setImageBitmap(BitmapFactory.decodeFile(path2));
            count=0;
        }

        Bitmap yourSelectedImage = BitmapFactory.decodeFile(path1);
        Bitmap yourSelectedImage2 = BitmapFactory.decodeFile(path2);
        app.associate(yourSelectedImage,yourSelectedImage2);

        // At the end remember to close the cursor or you will end with the RuntimeException!
        cursor.close();
    }

    class Button_Clicker1 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {
            // Create the Intent for Image Gallery.
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // Start new activity with the LOAD_IMAGE_RESULTS to handle back the results when image is picked from the Image Gallery.
            startActivityForResult(i, LOAD_IMAGE_RESULTS);
        }
    }
    class Button_Clicker2 implements Button.OnClickListener
    {
        @Override
        public void onClick(View v) {
            // Create the Intent for Image Gallery.
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // Start new activity with the LOAD_IMAGE_RESULTS to handle back the results when image is picked from the Image Gallery.
            startActivityForResult(i, LOAD_IMAGE_RESULTS);
        }
    }
}
