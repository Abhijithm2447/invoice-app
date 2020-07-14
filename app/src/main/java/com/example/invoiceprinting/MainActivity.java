package com.example.invoiceprinting;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.invoiceprinting.MyService;
import com.example.invoiceprinting.MyService.MyLocalBinder;

import static com.example.invoiceprinting.R.layout.dialog_select_item;

public class MainActivity extends AppCompatActivity {
    final private static String BASEURL = "http://192.168.1.10:2424/";

    private RecyclerView rViewListPredicteItems;
    private RecyclerView recyclerListCart;

    private CartedItemAdapter cartAdapter;
    private Button btnCapture, btnPrint;
    private TextureView textureView;
    private List<M_detected_class> detected_objs;

    EditText editTextQuantity;
    // search bar
    SearchView searchView;
    ListView myList;

    ArrayList<String> list;
    ArrayAdapter<String> adapter;


    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    HashMap<String, List<M_detected_class>> resResult;
    private List<String> resError;
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };

    MyService myService;
    boolean isbound = false;

    public void showTime(View view){
        String currenttime = myService.getCurrentTime();
        Toast.makeText(MainActivity.this, currenttime, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView)findViewById(R.id.textureView);
        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btnCapture = (Button)findViewById(R.id.btnCapture);
        btnPrint = (Button)findViewById(R.id.btn_print_carted);

        // search bar
        searchView = (SearchView) findViewById(R.id.searchView);
        searchView = (SearchView) findViewById(R.id.searchView);
        myList = (ListView) findViewById(R.id.serachList);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

        // recycler view
        rViewListPredicteItems = (RecyclerView) findViewById(R.id.recyclerListItem);
        rViewListPredicteItems.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        setUpCartRecyclerView();

        Intent intent = new Intent(this, MyService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        searchBarListner();
    }

    private void searchBarListner() {
        list = new ArrayList<String>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        myList.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
//                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                if(list.contains("Monday")) {
                    list.remove("Monday");
                    adapter.notifyDataSetChanged();
                }else {
                    list.add("Monday");
                    adapter.notifyDataSetChanged();
                }
//                adapter.getFilter().filter(s);
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
//        showTime();
    }

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyLocalBinder myLocalBinder = (MyLocalBinder) service;
            myService = myLocalBinder.getService();
            isbound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isbound = false;
        }
    };



    private void setUpCartRecyclerView()
    {
        // recycler view
        recyclerListCart = (RecyclerView) findViewById(R.id.recyclerListCart);
        recyclerListCart.setLayoutManager(new LinearLayoutManager(MainActivity.this,RecyclerView.HORIZONTAL,false));

        CartedItemAdapter.OnListClickListener listener=new CartedItemAdapter.OnListClickListener() {
            @Override
            public void onListClick(M_detected_class value) {

            }
        };
        cartAdapter=new CartedItemAdapter(this,listener);
        recyclerListCart.setAdapter(cartAdapter);
    }

    private void SentImageToServer(String image) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        Map<String,String> parameters = new HashMap<>();
        parameters.put("image", image);

        Call<M_receive_image> call = jsonPlaceHolderApi.recObject(parameters);
        call.enqueue(new Callback<M_receive_image>() {
            @Override
            public void onResponse(Call<M_receive_image> call, Response<M_receive_image> response) {
                if(!response.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                M_receive_image postResponse = response.body();
                Log.i("my_res", String.valueOf(postResponse));
                resResult =  postResponse.getResult() ;
                resError = postResponse.getError();
                Log.i("my_res", String.valueOf(resResult));
                detected_objs = resResult.get("detected_classes");

                PredictedItemAdapter.OnListClickListener listener=new PredictedItemAdapter.OnListClickListener() {
                    @Override
                    public void onListClick(M_detected_class item) {
                        //onlick listner
                        CreateAlertBox(item);
                    }
                };

                PredictedItemAdapter predictedItemAdapter = new PredictedItemAdapter(MainActivity.this,detected_objs,listener);
                rViewListPredicteItems.setAdapter(predictedItemAdapter);
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void CreateAlertBox(final M_detected_class item) {
//        ItemSelectDialog itemSelectDialog = new ItemSelectDialog();
//        itemSelectDialog.show();
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Please add quantity");
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View view = layoutInflater.inflate(dialog_select_item, null);
        builder1.setView(view);
        builder1.setCancelable(true);



        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String quantity = editTextQuantity.getText().toString();
                        item.quantity = quantity;
                        cartAdapter.addItem(item);
                        //getall carted items
                        //List<M_detected_class>=cartAdapter.getCartedItems();
                        // sat visibility of print button
                        btnPrint.setVisibility(View.VISIBLE);
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        editTextQuantity = view.findViewById(R.id.quantity);
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void takePicture() {
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
//            if(jpegSizes != null && jpegSizes.length > 0)
//            {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//            }
            final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

            file = new File(Environment.getDataDirectory()+"/"+UUID.randomUUID().toString()+".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];

                        buffer.get(bytes);
                        save(bytes);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        {
                            if(image != null)
                                image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try{
                        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
                        SentImageToServer(encoded);
//                        outputStream = new FileOutputStream(file);
//                        outputStream.write(bytes);
                    }finally {
//                        Toast.makeText(MainActivity.this, "Cant sent image", Toast.LENGTH_SHORT).show();
                        if(outputStream != null)
                            outputStream.close();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(MainActivity.this, "Saved "+file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert  texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updatePreview() {
        if(cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    public void setBtnPrintCarted(View view){
        List<M_detected_class> m_detected_classes = cartAdapter.getCartedItems();
//        Toast.makeText(this, "len " + String.valueOf(m_detected_classes.size()), Toast.LENGTH_SHORT).show();
        // Add carted items to DB
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        Map<String,List<M_detected_class>> parameters = new HashMap<>();
        parameters.put("carted_items", m_detected_classes);

        Call<M_add_carted_items> call = jsonPlaceHolderApi.addCartedItem(parameters);
        call.enqueue(new Callback<M_add_carted_items>() {
            @Override
            public void onResponse(Call<M_add_carted_items> call, Response<M_add_carted_items> response) {
                if(!response.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                M_add_carted_items postResponse = response.body();

                HashMap<String, String> result =  postResponse.getResult() ;
                resError = postResponse.getError();

                String invoice_num = result.get("invoice_num");

                Toast.makeText(MainActivity.this, "Invoice num: " + invoice_num, Toast.LENGTH_SHORT).show();
                cartAdapter.removeAll();
                // set visibility of print button
                btnPrint.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<M_add_carted_items> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
}