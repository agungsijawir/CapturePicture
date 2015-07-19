package com.agungandika.android.capturepicture;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.isExternalStorageRemovable;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;
    final int PIC_CROP = 2;

    private File outputImageFile = null;
    private File outputDirs;
    private String imageTimeStamp;
    private Intent cropIntent;

    // jpeg output quality
    private int JPEG_OUTPUT_QUALITY = 70;

    // debug log flag
    private String ERR_FAIL_TO_CROP = "FAIL_TO_CROP";
    private String DBG_SAVE_CROP = "SAVE_CROP";
    private String DBG_SAVE_CROP_ERROR = "SAVE_CROP_ERROR";
    private String DBG_COMPRESS_CROP = "COMPRESS_CROP";
    private String DBG_COMPRESS_ERROR = "COMPRESS_CROP_ERROR";
    private String DBG_CREATE_DIR_ERROR = "CREATE_DIRS_ERROR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //retrieve a reference to the UI button
        Button captureBtn = (Button) findViewById(R.id.capture_btn);
        //handle button clicks
        captureBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.capture_btn) {
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent("android.media.action.IMAGE_CAPTURE");

                //create instance of File with same name we created before to get image from storage
                try {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable()) {
                        outputDirs = new File(getExternalCacheDir() + File.separator + "KDG" + File.separator);
                    } else {
                        outputDirs = new File(getApplicationContext().getCacheDir().getPath() + File.separator + "KDG" + File.separator);
                    }
                } catch (Exception e) {
                    outputDirs = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator);
                    Toast.makeText(MainActivity.this, "Seems we cannot create directory on external storage, we will use public directory instead @ " + outputDirs.toString(), Toast.LENGTH_SHORT).show();
                }

                try {
                    if (!outputDirs.exists()) {
                        outputDirs.mkdirs();
                    }
                } catch (Exception e) {
                    Log.e(this.DBG_CREATE_DIR_ERROR, "Error create target directory!");
                    Toast.makeText(MainActivity.this, "Error create target directory @ " +
                            outputDirs.toString(), Toast.LENGTH_SHORT).show();
                }

                Log.v("CHECK_DIRS", "Target Dir is exists? " + outputDirs.exists());

                // image timestamp setup
                imageTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                outputImageFile = new File(outputDirs, "KDG_NEWS_" + imageTimeStamp + ".jpg");

                // put uri as extra in intent object
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputImageFile));
                Log.v(DBG_SAVE_CROP, "Preparing to launch camera. Target file is: " +
                        outputImageFile.toString());

                // start activity for result pass intent as argument and request code
                startActivityForResult(captureIntent, CAMERA_CAPTURE);

            } catch (ActivityNotFoundException anfe) {
                //display an error message
                String errorMessage = "Whoops - your device doesn't support capturing images!";
                Toast toast = Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // create file output
        OutputStream fOut = null;

        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if (requestCode == CAMERA_CAPTURE) {
                // create new instance with same name as saved pic before we start crop operation
                try {
                    File targetCropImg = new File(outputDirs, "KDG_NEWS_" + imageTimeStamp + ".jpg");

                    //Crop the captured image using an other intent
                    try {
                        // the user's device may not support cropping
                        cropCapturedImage(Uri.fromFile(targetCropImg));
                    } catch (ActivityNotFoundException aNFE) {
                        //display an error message if user device doesn't support
                        Log.e(DBG_SAVE_CROP_ERROR, "No Crop Activity on this device");
                        Toast.makeText(MainActivity.this, "No Crop Activity on this device",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(DBG_SAVE_CROP_ERROR, "Fail to save cropped image because: " +
                                e.getMessage().toString());
                        Toast.makeText(MainActivity.this, "Fail to save cropped image because: " +
                                e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Sorry, we cannot save cropped image for you", Toast.LENGTH_SHORT).show();
                    Log.e(DBG_SAVE_CROP_ERROR, "Failt to create output image because: " + e.getMessage().toString());
                }
            } else if (requestCode == PIC_CROP) {
                //get the returned data
                try {
                    Bundle extras = data.getExtras();
                    //get the cropped bitmap
                    Bitmap thePic = extras.getParcelable("data");

                    //retrieve a reference to the ImageView
                    ImageView picView = (ImageView) findViewById(R.id.picture);
                    //display the returned cropped image
                    picView.setImageBitmap(thePic);
                    picView.setDrawingCacheEnabled(true);

                    // save cropped image to external directory
                    Bitmap croppedImg = ((BitmapDrawable) picView.getDrawable()).getBitmap();

                    try {
                        fOut = new FileOutputStream(outputImageFile);

                        try {
                            croppedImg.compress(Bitmap.CompressFormat.JPEG, JPEG_OUTPUT_QUALITY, fOut);
                            fOut.flush();
                            fOut.close();

                            Log.d(DBG_COMPRESS_CROP, "Saved file: " + fOut.toString());
                            Toast.makeText(MainActivity.this, "Successful save cropped image @ " +
                                    fOut.toString(), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(DBG_COMPRESS_ERROR, "Fail to compress cropped image: " +
                                    e.getMessage().toString());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(DBG_SAVE_CROP, "Fail to save cropped image: " + e.getMessage().toString());
                        Toast.makeText(MainActivity.this, "Error occured. Please try again later.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(DBG_SAVE_CROP_ERROR, "Fail to save because no data/extra on intent!");
                    Log.e(DBG_SAVE_CROP_ERROR, "Error message: " + e.getMessage().toString());
                    Toast.makeText(MainActivity.this, "Fail to save because no data/extra on intent! " +
                            "Please check debug log for error details.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(MainActivity.this, "Action cancelled!", Toast.LENGTH_SHORT).show();
        }
    }

    private void cropCapturedImage(Uri picUri) {
        Log.d("CROPPING", "Starting to crop image " + picUri.toString());

        try {
            //call the standard crop action intent (the user device may not support it)
            cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            //indicate output X and Y for resize picture
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        } catch (ActivityNotFoundException anfe) {
            //display an error message
            Log.e(ERR_FAIL_TO_CROP, "Fail to Crop because: " + anfe.getMessage().toString());
            Toast.makeText(MainActivity.this, "Fail to crop, because your device seems doesn't support crop action!", Toast.LENGTH_SHORT).show();
        } catch (Exception foo) {
            Log.e(ERR_FAIL_TO_CROP, "Fail to crop :( -- " + foo.getMessage().toString());
            Toast.makeText(MainActivity.this, "Fail to crop: " + foo.getMessage().toString(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
