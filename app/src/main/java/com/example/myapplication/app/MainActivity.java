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


import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.android.ConvertBitmap;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.FastQueue;
import georegression.struct.point.Point2D_F64;

public class MainActivity extends ActionBarActivity {

    Button addImageButton;
    ImageView image;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addImageButton = (Button) findViewById(R.id.addImageButton);

        image = (ImageView) findViewById(R.id.image);

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select contact image"), 1);
            }
        });

    }

    public void onActivityResult(int reqCode, int resCode, Intent imageReturnedIntent){
        super.onActivityResult(reqCode, resCode, imageReturnedIntent);

        if (resCode == RESULT_OK){
            if (reqCode == 1) {
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

                DetectDescribePoint<ImageFloat32,SurfFeature> surf = FactoryDetectDescribe. surfStable(new ConfigFastHessian(0, 2, 200, 2, 9, 4, 4), null, null, ImageFloat32.class);
                surf.detect(img);

                System.out.println("Found Features: "+surf.getNumberOfFeatures());
                System.out.println("First descriptor's first value: "+surf.getDescription(0).value[0]);

                image.setImageURI(imageReturnedIntent.getData());

            }
        }

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
