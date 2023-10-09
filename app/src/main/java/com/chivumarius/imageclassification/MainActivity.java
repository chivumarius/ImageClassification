
package com.chivumarius.imageclassification;



import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import android.util.Log;
import android.database.Cursor;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 123;
    public static final int IMAGE_CAPTURE_CODE = 654;
    private static final int PERMISSION_CODE = 321;
    private Uri image_uri;


    // ▼ "DECLARATION" OF "WIDGETS IDS" ▼
    ImageView frame,innerImage;


    // ▼ "DECLARATION" OF "CLASSIFIER" CLASS ▼
    Classifier classifier;

    // ▼ "VARIABLE DECLARATION" ▼
    TextView resultTV;





    // ▬ "ON CREATE()" METHOD ▬
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // ▼ "INITIALIZATION" OF "WIDGETS IDS" ▼
        frame = findViewById(R.id.imageView);
        innerImage = findViewById(R.id.imageView2);

        // ▼ "VARIABLE DECLARATION" ▼
        resultTV = findViewById(R.id.textView);




        // ▼ "SET ON CLICK LISTENER" OF "FRAME" IMAGE VIEW ▼
        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });


        // ▼ "SET ON LONG CLICK LISTENER" OF "FRAME" IMAGE VIEW ▼
        frame.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);

                    } else {
                        openCamera();
                    }

                } else {
                    openCamera();
                }
                return false;
            }
        });



        // ▼ ASK FOR CAMERA PERMISSION ▼
        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission, PERMISSION_CODE);
        }



        // ▼ "INITIALIZATION" OF "CLASSIFIER" CLASS ▼
        try {
            classifier = new Classifier(
                    getAssets(),
                    "mobilenet_v1_1.0_224.tflite",
                    "mobilenet_v1_1.0_224.txt",
                    224
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    // ▬ "OPEN CAMERA()" METHOD ▬
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }





    // ▬ "ON ACTIVITY RESULT()" METHOD ▬
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            image_uri = data.getData();
            //innerImage.setImageURI(image_uri);
            Bitmap bitmap = uriToBitmap(image_uri);
            innerImage.setImageBitmap(bitmap);

            // ▼ CALLING "DO INFERENCE()" METHOD ▼
            doInference(bitmap);
        }

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK){
            //innerImage.setImageURI(image_uri);
            Bitmap bitmap = uriToBitmap(image_uri);
            innerImage.setImageBitmap(bitmap);

            // ▼ CALLING "DO INFERENCE()" METHOD ▼
            doInference(bitmap);
        }

    }




    // ▬ "DO INFERENCE()" METHOD
    //      → IN WHICH WE USE "CLASSIFIER" CLASS ▬
    void doInference(Bitmap input) {

        // ▼ CREATING A "INPUT BITMAP" BITMAP OBJECT ▼
        Bitmap rotated = rotateBitmap(input);

        // ▼ "RECOGNIZE IMAGE" OF "ROTATED BITMAP" → FROM "CLASSIFIER" ▼
        List<Classifier.Recognition> results = classifier.recognizeImage(rotated);

        // ▼ CLEARING THE "TEXTVIEW" ▼
        resultTV.setText("");

        // ▼ LOOPING
        for(int i = 0; i< results.size(); i++) {
            Classifier.Recognition recognition = results.get(i);
            resultTV.append(recognition.title + " " + recognition.confidence + "\n");
        }
    }






    // ▬ "ON REQUEST PERMISSION RESULT()" METHOD
    //      → "ROTATES IMAGE" IF "IMAGE" WAS "CAPTURED" ON "SAMSUNG DEVICES" ▬
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }




    // ▬ "URI TO BITMAP()" METHOD
    //      → IT "TAKES URI" OF THE "IMAGE"
    //      → AND RETURNS "BITMAP" OF IT  ▬
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

}
