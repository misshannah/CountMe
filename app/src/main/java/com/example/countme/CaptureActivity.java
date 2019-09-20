package com.example.countme;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity {


    private Button buttonChoose;
    File photoFile;
    ImageView mImageView;
    TextView classifiedText;
    private int PICK_IMAGE_REQUEST = 99;
    Bitmap bitmap;
    File image;
    Uri imageUri;
    String mCurrentPhotoPath;

    public Classifier classifier;
    public Executor executor = Executors.newSingleThreadExecutor();


    //setUp for tensorflow
    public static final int INPUT_SIZE = 224;
    public static final int IMAGE_MEAN = 128;
    public static final float IMAGE_STD = 128.0f;
    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "final_result";
    public static final String MODEL_FILE = "file:///android_asset/rounded_graph.pb";

    public static final String LABEL_FILE = "file:///android_asset/retrained_labels.txt";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        //Embed the tensor flow activity to count objects in an image
        initTensorFlowAndLoadModel();

        buttonChoose = findViewById(R.id.buttonChooseImage);
        mImageView = findViewById(R.id.imageView);
        classifiedText = findViewById(R.id.classifiedText);

        buttonChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });


    }


    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    public List<Classifier.Recognition> analyse(Bitmap bitmap)
    {
        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        return results;
    }


    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Gallery",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
        builder.setTitle("Add Photo!");
        AlertDialog.Builder cancel = builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(CaptureActivity.this);
                if (items[item].equals("Take Photo")) {
                    if (result)
                        captureImage();
                } else if (items[item].equals("Choose from Gallery")) {
                    if (result)
                        showFileChooser();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void captureImage() {

        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            photoFile = createImageFile();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        startActivityForResult(captureIntent, 100);
    }


    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Choose from Gallery"), PICK_IMAGE_REQUEST);

    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST) {
            imageUri = data.getData();
            if (imageUri == null) {
                Toast.makeText(this, "Missing Image", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Bitmap bitmaps = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmaps.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    mImageView.setImageBitmap(bitmaps);

                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    List<Classifier.Recognition> results = analyse(selectedImage);
                    classifiedText.setText(results.get(0).toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        if (resultCode == RESULT_OK && requestCode == 100) {


            // Get Extra from the intent
            Bundle extras = data.getExtras();
            // Get the returned image from extra
            Bitmap bmp = (Bitmap) extras.get("data");
            // Get the dimensions of the View
            int targetW = mImageView.getWidth();
            int targetH = mImageView.getHeight();

            bmp.getScaledHeight(targetH);
            bmp.getScaledWidth(targetW);

            mImageView.setImageBitmap(bitmap);
            mImageView.setImageBitmap(bmp);
            List<Classifier.Recognition> results = analyse(bmp);
            classifiedText.setText(results.get(0).toString());

        }

    }

    @Override
    protected void onPause() {
        mImageView.setImageURI(null);
        super.onPause();
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "VialData_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e("Getpath", "Cool" + mCurrentPhotoPath);

        return image;
    }




}


