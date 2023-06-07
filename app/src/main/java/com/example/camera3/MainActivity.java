package com.example.camera3;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import static java.lang.Math.sqrt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.icu.number.Scale;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    //---------------------
    private static final int SCALING_FACTOR = 2;//6
    private static final String TAG = "FACE_DETECT_TAG";
    private FaceDetector detector;
    private Button detectFacesBtn;

    private Button Clearbutton;
    private ImageView croppedImageIv;
    private ImageView croppedImageIv1;
    //---------------------
    private String currentPhotoPath;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set click listener for the button
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fileName = "photo";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                try {
                    // Create a temporary image file
                    File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);

                    currentPhotoPath = imageFile.getAbsolutePath();

                    // Get the content URI for the image file using a FileProvider
                    Uri imageUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.camera3.fileprovider", imageFile);

                    // Create an intent to capture an image and save it to the specified imageUri
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, 1);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });


        //----
        croppedImageIv = findViewById(R.id.croppedImageIv);
        croppedImageIv1 = findViewById(R.id.croppedImageIv1);
        detectFacesBtn = findViewById(R.id.detecFacesBtn);
        Clearbutton = findViewById(R.id.Clearbutton);

        // Configure the FaceDetectorOptions for face detection
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setMinFaceSize(0.8f)//0.15f
                        .enableTracking()
                        .build();

        // Initialize the FaceDetector object
        detector = FaceDetection.getClient(options);

        // Set click listener for the detectFacesBtn
        detectFacesBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Get the drawable from the imageView and convert it to a Bitmap
                Drawable drawable = imageView.getDrawable();
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                // Analyze the photo for face detection
                analyzePhoto(bitmap);
            }
        });

        Clearbutton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                imageView.setImageDrawable(null);
                Toast.makeText(getApplicationContext(), "Image removed", Toast.LENGTH_SHORT).show();

            }
        });


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Decode the image file into a Bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

            // Find the imageView by its ID and set the captured photo as the image
            imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);

            // Rotate the bitmap by the specified rotation angle
            int rotation =  270;
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedPhoto;
            rotatedPhoto = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            imageView.setImageBitmap(rotatedPhoto);
        }
    }

    //--------------------------------------------------
    private void analyzePhoto(Bitmap bitmap){
        Log.d(TAG,"analyzePhoto: ");

        //Get smaller bitmap to do analyze process faster
        Bitmap smallerBitmap = Bitmap.createScaledBitmap(
                bitmap,
                bitmap.getWidth()/SCALING_FACTOR,
                bitmap.getHeight()/SCALING_FACTOR,
                true);

        //Get input image using bitmap, you may use fromUri method
        InputImage inputImage = InputImage.fromBitmap(smallerBitmap, 0);

        //start detection process
        detector.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces)
                    {
                        //There can be multiple faces detected from on image, manage them using loop from List<Face> faces faces
                        Log.d(TAG,"onSuccess: No of faces detected: "+faces.size());
                        for (Face face:faces)
                        {
                            Bitmap analyzedBitmap = bitmap.copy(bitmap.getConfig(), true);

                            Canvas canvas = new Canvas(analyzedBitmap);
                            Paint paint = new Paint();
                            paint.setColor(Color.GREEN);
                            paint.setStyle(Paint.Style.STROKE); // Set the paint style to stroke empty inside
                            paint.setStrokeWidth(6);

                            // Create a path object from the eye contour points
                            //-------------------------------shrink the eye region for right eye ------------------
                            List<PointF> rightEyePoints = face.getContour(FaceContour.RIGHT_EYE).getPoints();
                            List<PointF> rightEyePoints1 = face.getContour(FaceContour.RIGHT_EYE).getPoints();

                            float rightMiddleX = 0;
                            float rightMiddleY = 0;
                            int rightA = 0;

                            Path rightEyePath1 = new Path();
                            for (int i = 0; i < rightEyePoints1.size(); i++) {
                                PointF point = rightEyePoints1.get(i);
                                if (i == 0) {
                                    //--
                                    rightMiddleX += point.x * SCALING_FACTOR;
                                    rightMiddleY += point.y * SCALING_FACTOR;
                                    rightA++;
                                    //--
                                    rightEyePath1.moveTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR);
                                } else
                                {
                                    //--
                                    rightMiddleX += point.x * SCALING_FACTOR;
                                    rightMiddleY += point.y * SCALING_FACTOR;
                                    rightA++;
                                    //--
                                    rightEyePath1.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR);
                                }
                            }
                            rightEyePath1.close();

                            RectF bounds3 = new RectF();
                            rightEyePath1.computeBounds(bounds3, true);

                            int RshrinkWidth = (int) (bounds3.width() * 0.88);
                            int RshrinkHeight = (int) (bounds3.height() * 0.96);

                            int RshrinkX =  (int)(bounds3.width() - RshrinkWidth);
                            int RshrinkY =  (int)(bounds3.height() - RshrinkHeight);

                            rightMiddleX = rightMiddleX / rightA;
                            rightMiddleY = rightMiddleY / rightA;

                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                            PointF rightEyePos = new PointF(rightEye.getPosition().x, rightEye.getPosition().y);

                            float distRMtoRE = (float) sqrt(Math.pow(rightMiddleX - rightEyePos.x * SCALING_FACTOR, 2)
                                    + Math.pow(rightMiddleY - rightEyePos.y * SCALING_FACTOR, 2));

                            float distRMXtoREX = Math.abs(rightEyePos.x * SCALING_FACTOR - rightMiddleX);
                            float distRMYtoREY = Math.abs(rightEyePos.y * SCALING_FACTOR - rightMiddleY);

                            Path rightEyePath = new Path();

                            if (distRMtoRE > Math.abs(bounds3.width() - bounds3.height()) / 5)
                            {
                                for (int i = 0; i < rightEyePoints.size(); i++) {
                                    PointF point = rightEyePoints.get(i);
                                    switch (i) {
                                        case 0:
                                            rightEyePath.moveTo(point.x * SCALING_FACTOR + RshrinkX + distRMXtoREX, point.y * SCALING_FACTOR + distRMYtoREY);
                                            break;
                                        case 1:
                                        case 2:
                                        case 15:
                                        case 14:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR + RshrinkX + distRMXtoREX, point.y * SCALING_FACTOR + distRMYtoREY);
                                            break;
                                        case 3:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR + distRMXtoREX, point.y * SCALING_FACTOR + RshrinkY + distRMYtoREY);
                                            break;
                                        case 4:
                                        case 5:
                                        case 6:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR + distRMXtoREX, point.y * SCALING_FACTOR + RshrinkY + distRMYtoREY);
                                            break;
                                        case 7:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR - RshrinkX + distRMXtoREX, point.y * SCALING_FACTOR + RshrinkY + distRMYtoREY);
                                            break;
                                        case 8:
                                        case 9:
                                        case 10:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR - RshrinkX + distRMXtoREX, point.y * SCALING_FACTOR - RshrinkY + distRMYtoREY);
                                            break;
                                        case 11:
                                        case 12:
                                        case 13:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR + distRMXtoREX, point.y * SCALING_FACTOR - RshrinkY + distRMYtoREY);
                                            break;
                                    }
                                }
                                rightEyePath.close();
                            }

                            else
                            {
                                for (int i = 0; i < rightEyePoints.size(); i++) {
                                    PointF point = rightEyePoints.get(i);
                                    switch (i) {
                                        case 0:
                                            rightEyePath.moveTo(point.x * SCALING_FACTOR + RshrinkX, point.y * SCALING_FACTOR);
                                            break;
                                        case 1:
                                        case 2:
                                        case 15:
                                        case 14:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR + RshrinkX, point.y * SCALING_FACTOR);
                                            break;
                                        case 3:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR + RshrinkY);
                                            break;
                                        case 4:
                                        case 5:
                                        case 6:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR + RshrinkY);
                                            break;
                                        case 7:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR - RshrinkX, point.y * SCALING_FACTOR + RshrinkY);
                                            break;
                                        case 8:
                                        case 9:
                                        case 10:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR - RshrinkX, point.y * SCALING_FACTOR - RshrinkY);
                                            break;
                                        case 11:
                                        case 12:
                                        case 13:
                                            rightEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR - RshrinkY);
                                            break;
                                    }
                                }
                                rightEyePath.close();
                            }
                            //-------------------------------shrink the eye region for right eye ------------------


                            //-------------------------------shrink the eye region for left eye ------------------

                            List<PointF> leftEyePoints = face.getContour(FaceContour.LEFT_EYE).getPoints();
                            List<PointF> leftEyePoints1 = face.getContour(FaceContour.LEFT_EYE).getPoints();

                            float leftMiddleX = 0;
                            float leftMiddleY = 0;
                            int leftA = 0;

                            Path leftEyePath1 = new Path();
                            for (int i = 0; i < leftEyePoints1.size(); i++) {
                                PointF point = leftEyePoints1.get(i);
                                if (i == 0) {
                                    //--
                                    leftMiddleX += point.x * SCALING_FACTOR;
                                    leftMiddleY += point.y * SCALING_FACTOR;
                                    leftA++;
                                    //--
                                    leftEyePath1.moveTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR);
                                } else {
                                    //--
                                    leftMiddleX += point.x * SCALING_FACTOR;
                                    leftMiddleY += point.y * SCALING_FACTOR;
                                    leftA++;
                                    //--
                                    leftEyePath1.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR);
                                }
                            }
                            leftEyePath1.close();

                            RectF bounds2 = new RectF();
                            leftEyePath1.computeBounds(bounds2, true);
                            int LshrinkWidth = (int) (bounds2.width() * 0.88);
                            int LshrinkHeight = (int) (bounds2.height() * 0.96);

                            int LshrinkX =  (int)(bounds2.width() - LshrinkWidth);
                            int LshrinkY =  (int)(bounds2.height() - LshrinkHeight);

                            leftMiddleX = leftMiddleX / leftA;
                            leftMiddleY = leftMiddleY / leftA;

                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                            PointF leftEyePos = new PointF(leftEye.getPosition().x, leftEye.getPosition().y);

                            float distLMtoLE = (float) sqrt(Math.pow(leftMiddleX - leftEyePos.x * SCALING_FACTOR, 2)
                                    + Math.pow(leftMiddleY - leftEyePos.y * SCALING_FACTOR, 2));

                            float distLMXtoLEX = Math.abs(leftEyePos.x * SCALING_FACTOR - leftMiddleX);
                            float distLMYtoLEY = Math.abs(leftEyePos.y * SCALING_FACTOR - leftMiddleY);

                            Path leftEyePath = new Path();

                            if (distLMtoLE > Math.abs(bounds2.width() - bounds2.height()) / 5)
                            {
                                for (int i = 0; i < leftEyePoints.size(); i++) {
                                    PointF point = leftEyePoints.get(i);
                                    switch (i) {
                                        case 0:
                                            leftEyePath.moveTo(point.x * SCALING_FACTOR + LshrinkX + distLMXtoLEX, point.y * SCALING_FACTOR + distLMYtoLEY);
                                            break;
                                        case 1:
                                        case 2:
                                        case 15:
                                        case 14:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR + LshrinkX + distLMXtoLEX, point.y * SCALING_FACTOR + distLMYtoLEY);
                                            break;
                                        case 3:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR + distLMXtoLEX, point.y * SCALING_FACTOR + LshrinkY + distLMYtoLEY);
                                            break;
                                        case 4:
                                        case 5:
                                        case 6:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR + distLMXtoLEX, point.y * SCALING_FACTOR + LshrinkY + distLMYtoLEY);
                                            break;
                                        case 7:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR - LshrinkX + distLMXtoLEX, point.y * SCALING_FACTOR + LshrinkY + distLMYtoLEY);
                                            break;
                                        case 8:
                                        case 9:
                                        case 10:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR - LshrinkX + distLMXtoLEX, point.y * SCALING_FACTOR - LshrinkY + distLMYtoLEY);
                                            break;
                                        case 11:
                                        case 12:
                                        case 13:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR + distLMXtoLEX, point.y * SCALING_FACTOR - LshrinkY + distLMYtoLEY);
                                            break;
                                    }
                                }
                                leftEyePath.close();
                            }
                            else {
                                for (int i = 0; i < leftEyePoints.size(); i++) {
                                    PointF point = leftEyePoints.get(i);
                                    switch (i) {
                                        case 0:
                                            leftEyePath.moveTo(point.x * SCALING_FACTOR + LshrinkX, point.y * SCALING_FACTOR);
                                            break;
                                        case 1:
                                        case 2:
                                        case 15:
                                        case 14:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR + LshrinkX, point.y * SCALING_FACTOR);
                                            break;
                                        case 3:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR + LshrinkY);
                                            break;
                                        case 4:
                                        case 5:
                                        case 6:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR + LshrinkY);
                                            break;
                                        case 7:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR - LshrinkX, point.y * SCALING_FACTOR + LshrinkY);
                                            break;
                                        case 8:
                                        case 9:
                                        case 10:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR - LshrinkX, point.y * SCALING_FACTOR - LshrinkY);
                                            break;
                                        case 11:
                                        case 12:
                                        case 13:
                                            leftEyePath.lineTo(point.x * SCALING_FACTOR, point.y * SCALING_FACTOR - LshrinkY);
                                            break;
                                    }
                                }
                                leftEyePath.close();
                            }
                            //-------------------------------shrink the eye region for left eye ------------------

                            // Create a bitmap with the size of the eye region
                            RectF bounds = new RectF();
                            rightEyePath.computeBounds(bounds, true);
                            int width = (int) bounds.width();
                            int height = (int) bounds.height();
                            Bitmap eyeBitmap = Bitmap.createBitmap(bitmap, (int) bounds.left
                                    ,(int)bounds.top, width , height);

                            // left eye !!!
                            RectF bounds1 = new RectF();
                            leftEyePath.computeBounds(bounds1, true);
                            int width1 = (int) bounds1.width();
                            int height1 = (int) bounds1.height();
                            Bitmap eyeBitmap1 = Bitmap.createBitmap(bitmap, (int) bounds1.left
                                    ,(int)bounds1.top, width1, height1);

                            // Draw the eye region into the bitmap
                            Canvas canvas1 = new Canvas(eyeBitmap);
                            canvas1.drawPath(rightEyePath, new Paint());

                            // left eye
                            Canvas canvas2 = new Canvas(eyeBitmap1);
                            canvas2.drawPath(leftEyePath, new Paint());

                            // Create a mask bitmap with a white background and the eye region in black
                            Bitmap eyeMaskBitmap = bitmap.copy(bitmap.getConfig(), true);
                            Canvas maskCanvas = new Canvas(eyeMaskBitmap);
                            maskCanvas.drawColor(Color.WHITE);
                            Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
                            paint1.setColor(Color.BLACK);
                            paint1.setStyle(Paint.Style.FILL);
                            maskCanvas.drawPath(rightEyePath, paint1);
                            //croppedImageIv.setImageBitmap(eyeMaskBitmap);

                            // left eyee
                            Bitmap eyeMaskBitmap1 = bitmap.copy(bitmap.getConfig(), true);
                            Canvas maskCanvas1 = new Canvas(eyeMaskBitmap1);
                            maskCanvas1.drawColor(Color.WHITE);
                            Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                            paint2.setColor(Color.BLACK);
                            paint2.setStyle(Paint.Style.FILL);
                            maskCanvas1.drawPath(leftEyePath, paint2);

                            // Apply the mask to the original bitmap using PorterDuff mode to keep only the pixels within the eye region
                            Bitmap maskedBitmap = bitmap.copy(bitmap.getConfig(), true);
                            Canvas maskedCanvas = new Canvas(maskedBitmap);
                            paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                            maskedCanvas.drawBitmap(eyeMaskBitmap, 0,0, null);
                            maskedCanvas.drawBitmap(bitmap, 0,0, paint1);
                            paint1.setXfermode(null);

                            // leftt eyeeee
                            Bitmap maskedBitmap1 = bitmap.copy(bitmap.getConfig(), true);
                            Canvas maskedCanvas1 = new Canvas(maskedBitmap1);
                            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
                            maskedCanvas1.drawBitmap(eyeMaskBitmap1, 0,0, null);
                            maskedCanvas1.drawBitmap(bitmap, 0,0, paint2);
                            paint2.setXfermode(null);

                            //croppedImageIv.setImageBitmap(maskedBitmap);

                            // Convert the masked bitmap to grayscale
                            Bitmap grayscaleBitmap = bitmap.copy(bitmap.getConfig(), true);
                            Canvas grayscaleCanvas = new Canvas(grayscaleBitmap);
                            ColorMatrix grayscaleMatrix = new ColorMatrix();
                            grayscaleMatrix.setSaturation(0);
                            Paint grayscalePaint = new Paint();
                            grayscalePaint.setColorFilter(new ColorMatrixColorFilter(grayscaleMatrix));
                            grayscaleCanvas.drawBitmap(maskedBitmap, 0, 0, grayscalePaint);

                            // leftt eyee

                            Bitmap grayscaleBitmap1 = bitmap.copy(bitmap.getConfig(), true);
                            Canvas grayscaleCanvas1 = new Canvas(grayscaleBitmap1);
                            ColorMatrix grayscaleMatrix1 = new ColorMatrix();
                            grayscaleMatrix1.setSaturation(0); //0,-10,-25
                            Paint grayscalePaint1 = new Paint();
                            grayscalePaint1.setColorFilter(new ColorMatrixColorFilter(grayscaleMatrix1));
                            grayscaleCanvas1.drawBitmap(maskedBitmap1, 0, 0, grayscalePaint1);


                            // Convert the grayscale bitmap to binary bitmap (only white and black colors)
                            Bitmap binaryBitmap = Bitmap.createBitmap(grayscaleBitmap,(int)bounds.left
                                    ,(int)bounds.top, width, height);

                            int threshold = 2;

                            int a = 0;
                            float pupilCenterX = 0;
                            float pupilCenterY = 0;

                            float eyeCenterX = 0;
                            float eyeCenterY = 0;
                            int b = 0;

                            while(a < 20 && threshold < 100) {
                                binaryBitmap = Bitmap.createBitmap(grayscaleBitmap,(int)bounds.left
                                        ,(int)bounds.top, width, height);
                                a = 0;
                                pupilCenterX = 0;
                                pupilCenterY = 0;
                                eyeCenterX = 0;
                                eyeCenterY = 0;
                                b = 0;

                                for (int x = 0; x < width; x++) {
                                    for (int y = 0; y < height; y++) {
                                        int pixel = binaryBitmap.getPixel(x, y);
                                        int red = Color.red(pixel);
                                        int green = Color.green(pixel);
                                        int blue = Color.blue(pixel);
                                        int alpha = Color.alpha(pixel);
                                        int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                                        int newPixel = 0;

                                        eyeCenterX += x;
                                        eyeCenterY += y;
                                        b++;

                                        if (gray > threshold) {
                                            newPixel = Color.argb(alpha, 255, 255, 255); // white
                                        }
                                        else {
                                            newPixel = Color.argb(alpha, 0, 0, 0); // black
                                            binaryBitmap.setPixel(x, y, Color.BLACK);

                                            eyeCenterX -= x;
                                            eyeCenterY -= y;
                                            b--;

                                            pupilCenterX += x;
                                            pupilCenterY += y;
                                            a++;
                                        }
                                        binaryBitmap.setPixel(x, y, newPixel);
                                    }
                                }
                                threshold += 2;
                            }

                            float centroidX = (float) (pupilCenterX / a);
                            float centroidY = (float) (pupilCenterY / a);

                            eyeCenterX = (float) (eyeCenterX / b);
                            eyeCenterY = (float) (eyeCenterY / b);

                            Canvas pupilCanvas = new Canvas(binaryBitmap);

                            Paint pupilPaint = new Paint();
                            pupilPaint.setStyle(Paint.Style.STROKE);
                            pupilPaint.setStrokeWidth(5);

                            pupilPaint.setColor(Color.BLUE);
                            pupilCanvas.drawPoint(eyeCenterX, eyeCenterY, pupilPaint);
                            pupilPaint.setColor(Color.GREEN);
                            pupilCanvas.drawPoint(centroidX, centroidY, pupilPaint);

                            //canvas.drawPoint(centroidX + (int)bounds.left,centroidY + (int)bounds.top,paint);

                            // left eyeee
                            // Convert the grayscale bitmap to binary bitmap (only white and black colors)
                            Bitmap binaryBitmap1 = Bitmap.createBitmap(grayscaleBitmap1,(int)bounds1.left
                                    ,(int)bounds1.top, width1, height1);

                            int threshold1 = 2;

                            int a1 = 0;
                            float pupilCenterX1 = 0;
                            float pupilCenterY1 = 0;

                            float eyeCenterX1 = 0;
                            float eyeCenterY1 = 0;
                            int b1 = 0;

                            while(a1 < 20 && threshold1 < 100) {
                                binaryBitmap1 = Bitmap.createBitmap(grayscaleBitmap1,(int)bounds1.left
                                        ,(int)bounds1.top, width1, height1);
                                a1 = 0;
                                pupilCenterX1 = 0;
                                pupilCenterY1 = 0;
                                eyeCenterX1 = 0;
                                eyeCenterY1 = 0;
                                b1 = 0;

                                for (int x = 0; x < binaryBitmap1.getWidth(); x++) {
                                    for (int y = 0; y < binaryBitmap1.getHeight(); y++) {
                                        int pixel = binaryBitmap1.getPixel(x, y);
                                        int red = Color.red(pixel);
                                        int green = Color.green(pixel);
                                        int blue = Color.blue(pixel);
                                        int alpha = Color.alpha(pixel);
                                        int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                                        eyeCenterX1 += x;
                                        eyeCenterY1 += y;
                                        b1++;

                                        int newPixel = 0;
                                        if (gray > threshold1) {
                                            newPixel = Color.argb(alpha, 255, 255, 255); // white
                                        } else {
                                            newPixel = Color.argb(alpha, 0, 0, 0); // black
                                            binaryBitmap1.setPixel(x, y, Color.BLACK);

                                            eyeCenterX1 -= x;
                                            eyeCenterY1 -= y;
                                            b1--;

                                            pupilCenterX1 += x;
                                            pupilCenterY1 += y;
                                            a1++;
                                        }
                                        binaryBitmap1.setPixel(x, y, newPixel);
                                    }
                                }

                                threshold1 += 2;
                            }

                            float centroidX1 = (float) (pupilCenterX1 / a1);
                            float centroidY1 = (float) (pupilCenterY1 / a1);

                            eyeCenterX1 = (float) (eyeCenterX1 / b1);
                            eyeCenterY1 = (float) (eyeCenterY1 / b1);

                            Canvas pupilCanvas1 = new Canvas(binaryBitmap1);

                            Paint pupilPaint1 = new Paint();
                            pupilPaint1.setStyle(Paint.Style.STROKE);
                            pupilPaint1.setStrokeWidth(5);

                            pupilPaint1.setColor(Color.BLUE);
                            pupilCanvas1.drawPoint(eyeCenterX1, eyeCenterY1, pupilPaint1);
                            pupilPaint1.setColor(Color.GREEN);
                            pupilCanvas1.drawPoint(centroidX1, centroidY1, pupilPaint1);

                            //canvas.drawPoint(centroidX1 + (int)bounds1.left,centroidY1 + (int)bounds1.top,paint);

                            //------------------------------------------
                            //center of eye
                            float RCx = (eyeCenterX + (int)bounds.left);
                            float RCy = (eyeCenterY + (int)bounds.top);
                            float LCx = (eyeCenterX1 + (int)bounds1.left);
                            float LCy = (eyeCenterY1 + (int)bounds1.top);
                            //---------------------

                            paint.setColor(Color.BLUE);
                            //canvas.drawPoint(LCx, LCy, paint);
                            //canvas.drawPoint(RCx, RCy, paint);


                            //center of the pupil
                            float leftx = (centroidX1 + (int)bounds1.left);
                            float lefty = (centroidY1 + (int)bounds1.top);

                            float rightx = centroidX + (int)bounds.left;
                            float righty = centroidY + (int)bounds.top;


                            //slope and constant of vector
                            float mLeft =(LCy - lefty) / (LCx - leftx);
                            float ConstantLeft = lefty - mLeft*leftx;
                            float EndLeftx = 0;
                            float EndLefty = 0;

                            float mRight =(RCy - righty) / (RCx - rightx);
                            float ConstantRight = righty - mRight*rightx;
                            float EndRightx = 0;
                            float EndRighty = 0;

                            //distance between center of the pupil and eye
                            float distLCtoLP = (float) sqrt(Math.pow(LCx - leftx, 2) + Math.pow(LCy - lefty, 2));
                            float distRCtoRP = (float) sqrt(Math.pow(RCx - rightx, 2) + Math.pow(RCy - righty, 2));

                            paint.setColor(Color.GREEN);


                            //marks for gaze point calculation
                            int llu = 0;//(Left eye/Left/Up Direction = llu) ...
                            int lru = 0;
                            int lld = 0;
                            int lrd = 0;

                            int rlu = 0;
                            int rru = 0;
                            int rld = 0;
                            int rrd = 0;

                            int mark = 0;

                            // Create a vector for each eye
                            if ((distLCtoLP < Math.abs(width1 - height1) / 4.2) && (distRCtoRP < Math.abs(width - height) / 4.2))
                            {
                                //if distance between eye center and pupil center is so close to each other
                                //we will estimate that person is looking probably center
                                mark = 1;
                                paint.setColor(Color.RED);
                            }
                            else {
                                if (LCx > leftx && LCy > lefty) {
                                    EndLeftx = (leftx)*(-2);
                                    EndLefty = EndLeftx * mLeft + ConstantLeft;
                                    llu = 1;
                                    canvas.drawLine(LCx, LCy, EndLeftx, EndLefty, paint);
                                } else if (LCx > leftx && LCy < lefty) {
                                    EndLeftx = (leftx)*(-2);
                                    EndLefty = EndLeftx * mLeft + ConstantLeft;
                                    lld = 1;
                                    canvas.drawLine(LCx, LCy, EndLeftx, EndLefty, paint);
                                } else if (LCx < leftx && LCy > lefty) {
                                    EndLeftx = (leftx) * 2;
                                    EndLefty = EndLeftx * mLeft + ConstantLeft;
                                    lru = 1;
                                    canvas.drawLine(LCx, LCy, EndLeftx, EndLefty, paint);
                                } else if (LCx < leftx && LCy < lefty) {
                                    EndLeftx = (leftx) * 2;
                                    EndLefty = EndLeftx * mLeft + ConstantLeft;
                                    lrd = 1;
                                    canvas.drawLine(LCx, LCy, EndLeftx, EndLefty, paint);
                                }

                                if (RCx > rightx && RCy > righty) {
                                    EndRightx = (rightx)*(-2);
                                    EndRighty = EndRightx * mRight + ConstantRight;
                                    rlu = 1;
                                    canvas.drawLine(RCx, RCy, EndRightx, EndRighty, paint);
                                } else if (RCx > rightx && RCy < righty) {
                                    EndRightx = (rightx)*(-2);
                                    EndRighty = EndRightx * mRight + ConstantRight;
                                    rld = 1;
                                    canvas.drawLine(RCx, RCy, EndRightx, EndRighty, paint);
                                } else if (RCx < rightx && RCy > righty) {
                                    EndRightx = (rightx) * 2;
                                    EndRighty = EndRightx * mRight + ConstantRight;
                                    rru = 1;
                                    canvas.drawLine(RCx, RCy, EndRightx, EndRighty, paint);
                                } else if (RCx < rightx && RCy < righty) {
                                    EndRightx = (rightx) * 2;
                                    EndRighty = EndRightx * mRight + ConstantRight;
                                    rrd = 1;
                                    canvas.drawLine(RCx, RCy, EndRightx, EndRighty, paint);
                                }
                            }

                            //calculation for intersection point of two vectors
                            float denominator = ((RCx - EndRightx) * (LCy - EndLefty)) - ((RCy - EndRighty) * (LCx - EndLeftx));
                            float xIntersection = (((RCx * EndRighty) - (RCy * EndRightx)) * (LCx - EndLeftx) - (RCx - EndRightx) * ((LCx * EndLefty) - (LCy * EndLeftx))) / denominator;
                            float yIntersection = (((RCx * EndRighty) - (RCy * EndRightx)) * (LCy - EndLefty) - (RCy - EndRighty) * ((LCx * EndLefty) - (LCy * EndLeftx))) / denominator;

                            float centerEyeX = (LCx + RCx) / 2;
                            float centerEyeY = (LCy + RCy) / 2;

                            float LgazePointX = 0;
                            float LgazePointY = 0;
                            float RgazePointX = 0;
                            float RgazePointY = 0;

                            float gazePointX = 0;
                            float gazePointY = 0;

                            // Calculate the gaze point
                            if (mark == 1)
                            {
                                gazePointX = centerEyeX;
                                gazePointY = centerEyeY;
                                canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                            }
                            else
                            {
                                if (lru == 1 && rrd == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------

                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                } else if (lrd == 1 && rru == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty < yIntersection || righty < yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                } else if (llu == 1 && rld == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------

                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                }else if (lld == 1 && rlu == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                }
                                else if (lld == 1 && rld == 1) {//++++
                                    if (leftx < xIntersection || rightx < xIntersection ||
                                            lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                else if (lrd == 1 && rrd == 1) {//++++
                                    if (leftx > xIntersection || rightx > xIntersection
                                            || lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                else if (lru == 1 && rru == 1) {//++++
                                    if (leftx > xIntersection || rightx > xIntersection
                                            || lefty < yIntersection || righty < yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------
                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                else if (llu == 1 && rlu == 1) {//++++
                                    if (leftx < xIntersection || rightx < xIntersection ||
                                            lefty < yIntersection || righty < yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                //-----
                                else if (lru == 1 && rlu == 1) {//++++
                                    //otherwise is impossible
                                    if (bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                else if (llu == 1 && rru == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty < yIntersection || righty < yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection) {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            } else {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------

                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            } else {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------
                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY, Math.abs(height - width), paint);
                                    }
                                }
                                else if (lrd == 1 && rld == 1) {//++++
                                    //otherwise is impossible
                                    if (bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                    else
                                    {
                                        canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                    }
                                }
                                else if (lld == 1 && rrd == 1) {//++++
                                    if (leftx < xIntersection || rightx > xIntersection
                                            || lefty > yIntersection || righty > yIntersection ||
                                            bitmap.getWidth() <  xIntersection ||  0 > xIntersection ||
                                            bitmap.getWidth() <  yIntersection ||  0 > yIntersection)
                                    {
                                        //-------left
                                        if ((EndLeftx > bitmap.getWidth() || EndLeftx < 0) || (EndLefty > bitmap.getHeight() || EndLefty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mLeft + ConstantLeft;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mLeft + ConstantLeft;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }

                                                EndLeftx = LgazePointX;
                                                EndLefty = EndLeftx * mLeft + ConstantLeft;
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                LgazePointY = EndLefty;
                                                if (bitmap.getHeight() < LgazePointY) {
                                                    LgazePointY = bitmap.getHeight();
                                                } else if (0 > LgazePointY) {
                                                    LgazePointY = 0;
                                                }

                                                EndLefty = LgazePointY;
                                                EndLeftx = (EndLefty - ConstantLeft) / mLeft;
                                                LgazePointX = EndLeftx;
                                                if (bitmap.getWidth() < LgazePointX) {
                                                    LgazePointX = bitmap.getWidth();
                                                } else if (0 > LgazePointX) {
                                                    LgazePointX = 0;
                                                }
                                            }
                                        }

                                        else
                                        {
                                            LgazePointX = EndLeftx;
                                            if (bitmap.getWidth() < LgazePointX) {
                                                LgazePointX = bitmap.getWidth();
                                            } else if (0 > LgazePointX) {
                                                LgazePointX = 0;
                                            }

                                            EndLeftx = LgazePointX;
                                            EndLefty = EndLeftx * mLeft + ConstantLeft;
                                            LgazePointY = EndLefty;
                                            if (bitmap.getHeight() < LgazePointY) {
                                                LgazePointY = bitmap.getHeight();
                                            } else if (0 > LgazePointY) {
                                                LgazePointY = 0;
                                            }
                                        }

                                        //-------right
                                        //--------------------------------------------
                                        if ((EndRightx > bitmap.getWidth() || EndRightx < 0) || (EndRighty > bitmap.getHeight() || EndRighty < 0))
                                        {
                                            float test1x = bitmap.getWidth(); // for right of the rectangle
                                            float test1y = test1x * mRight + ConstantRight;

                                            float testx = 0; // for left of the rectangle
                                            float testy = testx * mRight + ConstantRight;

                                            if ((testy < bitmap.getHeight() && testy > 0) || (test1y < bitmap.getHeight() && test1y > 0))
                                            {//width
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }

                                                EndRightx = RgazePointX;
                                                EndRighty = EndRightx * mRight + ConstantRight;
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }
                                            }
                                            else
                                            {//height
                                                RgazePointY = EndRighty;
                                                if (bitmap.getHeight() < RgazePointY) {
                                                    RgazePointY = bitmap.getHeight();
                                                } else if (0 > RgazePointY) {
                                                    RgazePointY = 0;
                                                }

                                                EndRighty = RgazePointY;
                                                EndRightx = (EndRighty - ConstantRight) / mRight;
                                                RgazePointX = EndRightx;
                                                if (bitmap.getWidth() < RgazePointX) {
                                                    RgazePointX = bitmap.getWidth();
                                                } else if (0 > RgazePointX) {
                                                    RgazePointX = 0;
                                                }
                                            }
                                        }
                                        //------------------------------------

                                        else
                                        {
                                            RgazePointX = EndRightx;
                                            if (bitmap.getWidth() < RgazePointX) {
                                                RgazePointX = bitmap.getWidth();
                                            } else if (0 > RgazePointX) {
                                                RgazePointX = 0;
                                            }

                                            EndRightx = RgazePointX;
                                            EndRighty = EndRightx * mRight + ConstantRight;
                                            RgazePointY = EndRighty;
                                            if (bitmap.getHeight() < RgazePointY) {
                                                RgazePointY = bitmap.getHeight();
                                            } else if (0 > RgazePointY) {
                                                RgazePointY = 0;
                                            }
                                        }
                                        //-----

                                        gazePointX = (LgazePointX + RgazePointX) / 2;
                                        gazePointY = (LgazePointY + RgazePointY) / 2;

                                        canvas.drawCircle(gazePointX, gazePointY , Math.abs(height-width), paint);
                                    }
                                }
                                else
                                {
                                    canvas.drawCircle(xIntersection, yIntersection, Math.abs(height-width), paint);
                                }
                            }
                            imageView.setImageBitmap(analyzedBitmap);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Detection failed
                        Log.d(TAG,"onFailure: ",e);
                        Toast.makeText(MainActivity.this, "Detection failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}