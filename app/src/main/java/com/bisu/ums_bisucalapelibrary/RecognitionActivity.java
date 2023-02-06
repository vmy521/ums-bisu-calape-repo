package com.bisu.ums_bisucalapelibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.bisu.ums_bisucalapelibrary.model.Monitoring;
import com.bisu.ums_bisucalapelibrary.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RecognitionActivity extends AppCompatActivity {

    private String purposeVisit;
    private PreviewView previewView;
    private Button camera_switch;
    private FirebaseFirestore db;
    private Helper helper;
    private TextView tv_school_id, tv_fullname, tv_gender, tv_bdate, tv_course, tv_time_in, tv_time_out, tv_purpose_visit, tv_result;
    private Uri imageUri;
    private ProgressDialog progressDialog;
    private FaceDetector detector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Interpreter tfLite;
    private CameraSelector cameraSelector;
    private float distance= 1.0f;
    private boolean start=true,flipX=false;
    private int cam_face=CameraSelector.LENS_FACING_BACK; //Default Back Camera
    private int[] intValues;
    private int inputSize=112;  //Input size for model
    private boolean isModelQuantized=false;
    private float[][] embeedings;
    private float IMAGE_MEAN = 128.0f;
    private float IMAGE_STD = 128.0f;
    private int OUTPUT_SIZE=192; //Output size of model
    private static int SELECT_PICTURE = 1;
    private ProcessCameraProvider cameraProvider;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private String modelFile="mobile_face_net.tflite"; //model name
    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>(); //saved Faces
    private SharedPreferences sharedPref;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        //Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //For UP button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentforBackButton = NavUtils.getParentActivityIntent(RecognitionActivity.this);
                intentforBackButton.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(RecognitionActivity.this, intentforBackButton);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        helper = new Helper(this);

        previewView = findViewById(R.id.previewView);
        camera_switch = findViewById(R.id.camera_switch);
        tv_school_id = findViewById(R.id.tv_school_id);
        tv_fullname = findViewById(R.id.tv_fullname);
        tv_gender = findViewById(R.id.tv_gender);
        tv_bdate = findViewById(R.id.tv_bdate);
        tv_course = findViewById(R.id.tv_course);
        tv_time_in = findViewById(R.id.tv_time_in);
        tv_time_out = findViewById(R.id.tv_time_out);
        tv_purpose_visit = findViewById(R.id.tv_purpose_visit);
        tv_result = findViewById(R.id.tv_result);
        registered = new HashMap<>();
        sharedPref = this.getSharedPreferences("umsbisucalapelibrary", Context.MODE_PRIVATE);
        distance = sharedPref.getFloat("distance",1.00f);
        progressDialog = new ProgressDialog(this);

        if(this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }

        //On-screen switch to toggle between Cameras.
        camera_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cam_face==CameraSelector.LENS_FACING_BACK) {
                    cam_face = CameraSelector.LENS_FACING_FRONT;
                    flipX=true;
                }else{
                    cam_face = CameraSelector.LENS_FACING_BACK;
                    flipX=false;
                }

                cameraProvider.unbindAll();
                cameraBind();
            }
        });

        //Load model
        try {
            tfLite = new Interpreter(loadModelFile(this, modelFile));
        } catch(IOException e) {
            e.printStackTrace();
        }

        //Initialize Face Detector
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        detector = FaceDetection.getClient(highAccuracyOpts);

        cameraBind();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_face_recognition, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.purpose_visit:
                setPurposeVisitListView();
                break;
        }
        return true;
    }

    private void setPurposeVisitListView() {
        Dialog dialog = new Dialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_select_purpose_visit, null);
        dialog.setContentView(view);

        ListView list_view = dialog.findViewById(R.id.list_view);
        List<String> purposeVisitList = Arrays.asList(getResources().getStringArray(R.array.purpose_visit));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, purposeVisitList);
        list_view.setAdapter(adapter);
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                purposeVisit = purposeVisitList.get(position);
                tv_purpose_visit.setText(purposeVisit);
                start = true;
                dialog.dismiss();
            }
        });

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MY_CAMERA_REQUEST_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(RecognitionActivity.this, "Camera permission granted", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(RecognitionActivity.this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    //Bind camera and preview view
    private void cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try{
                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);
            }catch(ExecutionException | InterruptedException e) {
                // No errors need to be handled for this in Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy imageProxy) {
                try{
                    Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
                }catch(InterruptedException e) {
                    e.printStackTrace();
                }

                InputImage image = null;

                @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                // Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)

                Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {
                    image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                }

                //Process acquired image to detect faces
                Task<List<Face>> result = detector.process(image)
                        .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                if(faces.size()!=0) {
                                    tv_result.setVisibility(View.INVISIBLE);

                                    Face face = faces.get(0); //Get first face from detected faces

                                    //mediaImage to Bitmap
                                    Bitmap frame_bmp = toBitmap(mediaImage);

                                    int rot = imageProxy.getImageInfo().getRotationDegrees();

                                    //Adjust orientation of Face
                                    Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false, false);

                                    //Get bounding box of face
                                    RectF boundingBox = new RectF(face.getBoundingBox());

                                    //Crop out bounding box from whole Bitmap(image)
                                    Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                                    if(flipX)
                                        cropped_face = rotateBitmap(cropped_face, 0, flipX, false);
                                    //Scale the acquired Face to 112*112 which is required input for model
                                    Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

                                    imageUri = helper.getImageUri(scaled);

                                    if(start){
                                        recognizeImage(scaled); //Send scaled bitmap to create face embeddings
                                    }
                                }else{
                                    tv_result.setVisibility(View.VISIBLE);
                                    tv_result.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                                    tv_result.setText("");
                                    tv_school_id.setText("");
                                    tv_fullname.setText("");
                                    tv_gender.setText("");
                                    tv_bdate.setText("");
                                    tv_course.setText("");
                                    tv_time_in.setText("");
                                    tv_time_out.setText("");
                                    tv_purpose_visit.setText("");

                                    if(registered.isEmpty()){
                                        tv_result.setText("No registered user");
                                    }else{
                                        tv_result.setText("No face detected");
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                            @Override
                            public void onComplete(Task<List<Face>> task) {
                                imageProxy.close(); //v.important to acquire next frame for analysis
                            }
                        });
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    private Bitmap toBitmap(Image image) {
        byte[] nv21=YUV_420_888toNV21(image);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    //IMPORTANT. If conversion not done ,the toBitmap conversion does not work on some devices.
    private static byte[] YUV_420_888toNV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert(image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if(rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        }else{
            long yBufferPos = -rowStride; // not an actual position
            for(; pos<ySize; pos+=width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert(rowStride == image.getPlanes()[1].getRowStride());
        assert(pixelStride == image.getPlanes()[1].getPixelStride());

        if(pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try{
                vBuffer.put(1, (byte)~savePixel);
                if(uBuffer.get(0) == (byte)~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            }catch(ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant

        for(int row=0; row<height/2; row++) {
            for(int col=0; col<width/2; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if(rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(), (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(new RectF(0, 0, cropRectF.width(), cropRectF.height()), paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        if(source != null && !source.isRecycled()) {
            source.recycle();
        }
        return resultBitmap;
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public void recognizeImage(final Bitmap bitmap) {
        if(!helper.isConnected()) {
            tv_result.setText("No internet connection");
            tv_result.setVisibility(View.VISIBLE);
            return;
        }

        tv_result.setText("");
        tv_result.setVisibility(View.INVISIBLE);

        // set Face to Preview
        //face_preview.setImageBitmap(bitmap);

        //Create ByteBuffer to store normalized image
        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[inputSize * inputSize];

        //get pixel values from Bitmap to normalize
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for(int i = 0; i < inputSize; ++i) {
            for(int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];

                if(isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                }else{ // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        //imgData is input to our model
        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();

        embeedings = new float[1][OUTPUT_SIZE]; //output of model will be stored in this variable
        outputMap.put(0, embeedings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model

        float distance_local = Float.MAX_VALUE;
        String id = "0";
        String label = "?";

        //Compare new face with saved Faces.
        if(registered.size() > 0) {
            final List<Pair<String, Float>> nearest = findNearest(embeedings[0]);//Find 2 closest matching face

            if(nearest.get(0) != null) {
                final String userId = nearest.get(0).first; //get userId and distance of closest matching face
                // label = userId;
                distance_local = nearest.get(0).second;

                //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
                if(distance_local < distance){
                    //Load user details from Firestore
                    db.collection("User")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(Task<DocumentSnapshot> task) {
                                    if(!task.isSuccessful()){
                                        Toast.makeText(RecognitionActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                        Log.e("App Error", task.getException().getMessage());
                                        return;
                                    }

                                    DocumentSnapshot document = task.getResult();
                                    final User user = document.toObject(User.class);

                                    tv_school_id.setText(user.getSchoolId());
                                    tv_fullname.setText(user.getFullName());
                                    tv_gender.setText(user.getGender());
                                    tv_bdate.setText(helper.formatDate(user.getBdate()));
                                    tv_course.setText(user.getCourse());

                                    //Check if user has time in already
                                    Query query = db.collection("Monitoring")
                                            .orderBy("timeIn", Query.Direction.DESCENDING)
                                            .whereEqualTo("schoolId", user.getSchoolId())
                                            .whereEqualTo("completed", false);

                                    query.get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(Task<QuerySnapshot> task) {
                                                    if(!task.isSuccessful()){
                                                        Toast.makeText(RecognitionActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                        Log.e("App Error", task.getException().getMessage());
                                                        return;
                                                    }

                                                    //Time Out
                                                    if(start == true && !task.getResult().isEmpty()){
                                                        start = false;
                                                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                                        Monitoring monitoring = document.toObject(Monitoring.class);

                                                        if(monitoring.getTimeIn() != null){
                                                            tv_time_in.setText(helper.formatDate(monitoring.getTimeIn()));
                                                        }

                                                        tv_time_out.setText(helper.formatDate(new Date()));
                                                        tv_purpose_visit.setText(purposeVisit);
                                                        timeOut(monitoring);
                                                    }else if(start == true && task.getResult().isEmpty()){
                                                        start = false;
                                                        tv_time_in.setText(helper.formatDate(new Date().getTime()));
                                                        tv_purpose_visit.setText(purposeVisit);
                                                        timeIn(user);
                                                    }
                                                }
                                            });
                                }
                            });
                }else{
                    tv_result.setVisibility(View.VISIBLE);
                    tv_result.setBackgroundColor(getResources().getColor(R.color.red));
                    tv_result.setText("Unknown");
                    tv_school_id.setText("");
                    tv_fullname.setText("");
                    tv_gender.setText("");
                    tv_bdate.setText("");
                    tv_course.setText("");
                    tv_time_in.setText("");
                    tv_time_out.setText("");
                    tv_purpose_visit.setText("");
                }
            }
        }
    }

    //Compare Faces by distance between face embeddings
    private List<Pair<String, Float>> findNearest(float[] emb) {
        List<Pair<String, Float>> neighbour_list = new ArrayList<Pair<String, Float>>();
        Pair<String, Float> ret = null; //to get closest match
        Pair<String, Float> prev_ret = null; //to get second closest match

        for(Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

            float distance = 0;
            for(int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff*diff;
            }

            distance = (float) Math.sqrt(distance);
            if(ret == null || distance < ret.second) {
                prev_ret=ret;
                ret = new Pair<>(name, distance);
            }
        }

        if(prev_ret==null) prev_ret = ret;
        neighbour_list.add(ret);
        neighbour_list.add(prev_ret);

        return neighbour_list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("User").get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(RecognitionActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(task.getResult().isEmpty()){
                            Toast.makeText(RecognitionActivity.this, "No registered user", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        registered.clear();
                        for(DocumentSnapshot document : task.getResult()){
                            User user = document.toObject(User.class);

                            float[][] newEmbeedings = new float[1][OUTPUT_SIZE];
                            for(int i=0; i<newEmbeedings.length; i++){
                                for(int j=0; j<user.getEmbeedings().size(); j++){
                                    newEmbeedings[i][j] = Float.parseFloat(user.getEmbeedings().get(j));
                                }
                            }

                            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                                    "0", "", -1f);
                            result.setExtra(newEmbeedings);
                            registered.put(user.getId(), result);
                        }
                    }
                });
    }

    private void timeIn(User user) {
        progressDialog.setTitle("Saving Time In");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        //Check if purpose of visit is not null
        if(purposeVisit == null){
            progressDialog.hide();
            start = true;
            tv_result.setVisibility(View.VISIBLE);
            tv_result.setBackgroundColor(getResources().getColor(R.color.red));
            tv_result.setText("Select purpose of visit");
            return;
        }

        tv_result.setVisibility(View.INVISIBLE);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("photoUrl", user.getPhotoUrl());
        map.put("schoolId", user.getSchoolId());
        map.put("fullName", user.getFullName());
        map.put("gender", user.getGender());
        map.put("bdate", user.getBdate());
        map.put("course", user.getCourse());
        map.put("purposeVisit", purposeVisit);
        map.put("timeIn", FieldValue.serverTimestamp());
        map.put("completed", false);

        //Remove empty or null values
        map.values().removeAll(Collections.singleton(null));

        db.collection("Monitoring")
                .add(map)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        progressDialog.dismiss();

                        if(task.isSuccessful()){
                            String time = helper.formatDate(new Date());
                            if(!isFinishing()) {
                                showSuccessMsgDialog("Time In Success", time);
                            }
                        }else{
                            Toast.makeText(RecognitionActivity.this, "Time In Failed", Toast.LENGTH_SHORT).show();
                            Log.e("App Error", task.getException().getMessage());
                        }
                    }
                });
    }

    private void timeOut(Monitoring monitoring) {
        progressDialog.setTitle("Saving Time Out");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.create();
        progressDialog.show();

        Map<String, Object> map = new HashMap<>();
        map.put("timeOut", FieldValue.serverTimestamp());
        map.put("completed", true);

        db.collection("Monitoring").document(monitoring.getId())
                .update(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();

                        if(task.isSuccessful()){
                            String time = helper.formatDate(new Date());
                            if(!isFinishing()) {
                                showSuccessMsgDialog("Time Out Success", time);
                            }
                        }else{
                            Toast.makeText(RecognitionActivity.this, "Time Out Failed", Toast.LENGTH_SHORT).show();
                            Log.e("App Error", task.getException().getMessage());
                        }
                    }
                });
    }

    private void reset(){
        tv_result.setVisibility(View.INVISIBLE);
        tv_result.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tv_result.setText("");
        tv_school_id.setText("");
        tv_fullname.setText("");
        tv_gender.setText("");
        tv_bdate.setText("");
        tv_course.setText("");
        tv_time_in.setText("");
        tv_time_out.setText("");
        tv_purpose_visit.setText("");
    }

    private void showSuccessMsgDialog(String msg, String time_in){
        Dialog dialog = new Dialog(this);
        dialog.setCancelable(false);
        View view = getLayoutInflater().inflate(R.layout.dialog_success_msg, null);
        dialog.setContentView(view);

        TextView tv_msg = dialog.findViewById(R.id.tv_msg);
        TextView tv_time = dialog.findViewById(R.id.tv_time);

        tv_msg.setText(msg);
        tv_time.setText(time_in);

        dialog.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        //Wait 5 secs for next recognition
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                reset();
                purposeVisit = null;
                start = true;
                dialog.dismiss();
            }
        }, 5000);
    }

}