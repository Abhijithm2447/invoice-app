package com.example.invoiceprinting;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.Result;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.example.invoiceprinting.R.layout.dialog_select_item;


public class BarCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler{

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    ZXingScannerView scannerView;
    ImageView imageViewCart;
    TextView textViewSearch;
    TextView textViewCount;
    SearchView searchView;
    MenuItem searchItem;
    FrameLayout layoutSearch;
    AVLoadingIndicatorView progressView;
    LinearLayout layoutCartTitle;
    DrawerLayout drawerLayout;
    TextView textViewShopName;
    ImageView imageViewLogo;
    FloatingActionButton fabPrint;

    final int UPI_PAYMENT = 0;


    PredictedAdapter predictedItemAdapter;

    private RecyclerView rViewListPredicteItems;
    private RecyclerView recyclerListCart;
    private RecyclerView recyclerViewSavedItems;

    private CartAdapter cartAdapter;
    private SavedAdapter savedAdapter;
    private List<M_detected_class> detected_objs;
    //on print click use this list
    private List<M_detected_class> carted_items=new ArrayList<>();

    EditText editTextQuantity;

    HashMap<String, List<M_detected_class>> resResult;
    private List<String> resError;

    AlertDialog alertDialogQuantity;

    ListView suggestionListView;

    BottomSheetBehavior behavior;

    private FirebaseAuth mAuth;



    private shopModel active_shop=null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);


        mAuth = FirebaseAuth.getInstance();


        Toolbar toolBar=findViewById(R.id.toolBarLayout);
        setSupportActionBar(toolBar);
        setTitle("");

        scannerView = findViewById(R.id.scannerView);
        textViewSearch = findViewById(R.id.textViewSearch);
        textViewCount = findViewById(R.id.textViewCount);
        layoutSearch = findViewById(R.id.layoutSearch);
        progressView=findViewById(R.id.avindicator);
        imageViewCart=findViewById(R.id.imageViewCart);
        layoutCartTitle=findViewById(R.id.layoutCartTitle);
        suggestionListView=findViewById(R.id.searchSuggestionList);
        drawerLayout=findViewById(R.id.drawerLayout);
        imageViewLogo=findViewById(R.id.imageViewLogo);
        textViewShopName=findViewById(R.id.textViewShopName);
        fabPrint=findViewById(R.id.fabPrint);



        initDrawer();



        textViewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchItem.expandActionView();
            }
        });


        // recycler view
        rViewListPredicteItems = (RecyclerView) findViewById(R.id.recyclerViewPredictedItems);
        rViewListPredicteItems.setLayoutManager(new LinearLayoutManager(BarCodeActivity.this, RecyclerView.HORIZONTAL,false));

        setUpCartRecyclerView();

        setUpSavedRecyclerView();


        LinearLayout layout_bottom_sheet=findViewById(R.id.layout_bottom_sheet);
        behavior = BottomSheetBehavior.from(layout_bottom_sheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED){
                    recyclerListCart.setVisibility(View.VISIBLE);
                    layoutCartTitle.setVisibility(View.GONE);
                }
                else{
                    recyclerListCart.setVisibility(View.GONE);
                    layoutCartTitle.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events
            }
        });

        findViewById(R.id.imageViewClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });



        suggestionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selected_suggestion = (String) suggestionListView.getItemAtPosition(i);
                if(active_shop!=null) {
                    getSearchResults(selected_suggestion);
                }
                suggestionListView.setAdapter(null);
                searchItem.collapseActionView();
            }
        });

        imageViewCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        fabPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(carted_items.size()>0) {
                    payUsingUpi(active_shop.owner_name,"jiniljoyz@okaxis","Invoice1232","1");
                }
            }
        });

    }


    void payUsingUpi(  String name,String upiId, String note, String amount) {
        Log.e("main ", "name "+name +"--up--"+upiId+"--"+ note+"--"+amount);
        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                //.appendQueryParameter("mc", "")
                //.appendQueryParameter("tid", "02125412")
                //.appendQueryParameter("tr", "25584584")
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                //.appendQueryParameter("refUrl", "blueapp")
                .build();
        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);
        // will always show a dialog to user to choose an app
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");
        // check if intent resolves
        if(null != chooser.resolveActivity(getPackageManager())) {
            startActivityForResult(chooser, UPI_PAYMENT);
        } else {
            Toast.makeText(this,"No UPI app found, please install one to continue",Toast.LENGTH_SHORT).show();
        }
    }

    private void initDrawer()
    {
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.nav_logout) {
                    mAuth.signOut();
                    SendUserToLoginActivity();
                }

                return false;
            }
        });
    }

    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(BarCodeActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            SendUserToLoginActivity();
        }else {
        }
    }



    @Override
    public void onResume() {
        super.onResume();

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (allPermissionsGranted()) {
                if(scannerView == null) {
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scannerView.stopCamera();
    }


    private void setUpCartRecyclerView()
    {
        // recycler view
        recyclerListCart = (RecyclerView) findViewById(R.id.recyclerViewCartItems);
        recyclerListCart.setLayoutManager(new LinearLayoutManager(BarCodeActivity.this,RecyclerView.HORIZONTAL,false));

        CartAdapter.OnListClickListener listener=new CartAdapter.OnListClickListener() {
            @Override
            public void onListClick(M_detected_class value) {

            }
        };
        cartAdapter=new CartAdapter(this,carted_items,listener);
        recyclerListCart.setAdapter(cartAdapter);
    }


    private void setUpSavedRecyclerView()
    {
        // recycler view
        recyclerViewSavedItems = (RecyclerView) findViewById(R.id.recyclerViewSavedItems);
        recyclerViewSavedItems.setLayoutManager(new LinearLayoutManager(BarCodeActivity.this,RecyclerView.VERTICAL,false));

        SavedAdapter.OnListClickListener listener=new SavedAdapter.OnListClickListener() {

            @Override
            public void onListClick(M_detected_class value, int position) {
                showEditQuantityDialog(value,position);
            }

            @Override
            public void onRemoveClicked(M_detected_class value, int position) {
                carted_items.remove(position);
                savedAdapter.notifyItemRemoved(position);
                cartAdapter.notifyItemRemoved(position);
                textViewCount.setText(String.valueOf(carted_items.size()));

            }
        };
        savedAdapter=new SavedAdapter(this,carted_items,listener);
        recyclerViewSavedItems.setAdapter(savedAdapter);
    }

    private void SentImageToServer(String image) {
        Log.d("camera","sending to server");
        progressView.show();
        if(predictedItemAdapter!=null) {
            detected_objs.clear();
            predictedItemAdapter.notifyDataSetChanged();
        }

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        Map<String,String> parameters = new HashMap<>();
        parameters.put("image", image);

        Call<M_receive_image> call = jsonPlaceHolderApi.recObject(parameters);
        call.enqueue(new Callback<M_receive_image>() {
            @Override
            public void onResponse(Call<M_receive_image> call, Response<M_receive_image> response) {
                progressView.hide();

                if(!response.isSuccessful()){
                    Toast.makeText(BarCodeActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("camera","sending successful");

                M_receive_image postResponse = response.body();
                Log.i("my_res", String.valueOf(postResponse));
                resResult =  postResponse.getResult() ;
                resError = postResponse.getError();
                Log.i("my_res", String.valueOf(resResult));
                detected_objs = resResult.get("detected_classes");

                PredictedAdapter.OnListClickListener listener=new PredictedAdapter.OnListClickListener() {
                    @Override
                    public void onListClick(M_detected_class item) {
                        //onlick listner
                        showAddtoCartDialog(item);
                    }
                };

                predictedItemAdapter = new PredictedAdapter(BarCodeActivity.this,detected_objs,listener);
                rViewListPredicteItems.setAdapter(predictedItemAdapter);
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                progressView.hide();
                Toast.makeText(BarCodeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("camera","sending failed");

            }
        });


    }



    private void getSuggestions(String query) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);

        Call<SearchSuggestionModel> call = jsonPlaceHolderApi.getSearchSuggestions(query,active_shop.shop_id);
        call.enqueue(new Callback<SearchSuggestionModel>() {
            @Override
            public void onResponse(Call<SearchSuggestionModel> call, Response<SearchSuggestionModel> response) {

                if(!response.isSuccessful()){
                    Toast.makeText(BarCodeActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(response.body()!=null) {
                    ArrayList<String> suggestions=response.body().getResult().getNames();
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(BarCodeActivity.this, android.R.layout.simple_list_item_1, suggestions);
                    suggestionListView.setAdapter(adapter);
                }



            }

            @Override
            public void onFailure(Call<SearchSuggestionModel> call, Throwable t) {
                Toast.makeText(BarCodeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }



    private void getSearchResults(String query) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        progressView.show();
        Call<M_receive_image> call = jsonPlaceHolderApi.searchQuery(query,active_shop.shop_id);
        call.enqueue(new Callback<M_receive_image>() {
            @Override
            public void onResponse(Call<M_receive_image> call, Response<M_receive_image> response) {
                progressView.hide();

                if(!response.isSuccessful()){
                    Toast.makeText(BarCodeActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("camera","sending successful");

                M_receive_image postResponse = response.body();
                Log.i("my_res", String.valueOf(postResponse));
                resResult =  postResponse.getResult() ;
                resError = postResponse.getError();
                Log.i("my_res", String.valueOf(resResult));
                detected_objs = resResult.get("detected_classes");

                PredictedAdapter.OnListClickListener listener=new PredictedAdapter.OnListClickListener() {
                    @Override
                    public void onListClick(M_detected_class item) {
                        //onlick listner
                        showAddtoCartDialog(item);
                    }
                };

                predictedItemAdapter = new PredictedAdapter(BarCodeActivity.this,detected_objs,listener);
                rViewListPredicteItems.setAdapter(predictedItemAdapter);
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                progressView.hide();
                Toast.makeText(BarCodeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("camera","sending failed");

            }
        });


    }


    private void getItemDetails(String id) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        progressView.show();
        Call<M_receive_image> call = jsonPlaceHolderApi.getItemDetails(id,active_shop.shop_id);
        call.enqueue(new Callback<M_receive_image>() {
            @Override
            public void onResponse(Call<M_receive_image> call, Response<M_receive_image> response) {
                progressView.hide();

                if(!response.isSuccessful()){
                    Toast.makeText(BarCodeActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                M_receive_image postResponse = response.body();
                Log.i("my_res", String.valueOf(postResponse));
                resResult =  postResponse.getResult() ;
                resError = postResponse.getError();
                Log.i("my_res", String.valueOf(resResult));
                detected_objs = resResult.get("detected_classes");

                if(detected_objs.size()>=1) {
                    showAddtoCartDialog(detected_objs.get(0));
                    scannerView.resumeCameraPreview(BarCodeActivity.this);
                }
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                progressView.hide();
                Toast.makeText(BarCodeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("camera","sending failed");

            }
        });


    }



    private void getShopDetails(String id) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        progressView.show();
        Call<ShopResultModel> call = jsonPlaceHolderApi.getShopDetails(id);
        call.enqueue(new Callback<ShopResultModel>() {
            @Override
            public void onResponse(Call<ShopResultModel> call, Response<ShopResultModel> response) {
                progressView.hide();

                if(!response.isSuccessful()){
                    Toast.makeText(BarCodeActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                ShopResultModel postResponse = response.body();
                Log.i("my_res", String.valueOf(postResponse));
                HashMap<String, shopModel> resResult =  postResponse.getResult() ;
                resError = postResponse.getError();
                Log.i("my_res", String.valueOf(resResult));
                active_shop = resResult.get("shop_details");

                imageViewLogo.setVisibility(View.GONE);
                textViewShopName.setVisibility(View.VISIBLE);
                textViewShopName.setText(active_shop.shop_name);
                scannerView.resumeCameraPreview(BarCodeActivity.this);

            }

            @Override
            public void onFailure(Call<ShopResultModel> call, Throwable t) {
                progressView.hide();
                Toast.makeText(BarCodeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("camera","sending failed");

            }
        });


    }

    private void showAddtoCartDialog(final M_detected_class item) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(dialog_select_item, viewGroup, false);
        builder.setView(dialogView);

        final EditText quantity=dialogView.findViewById(R.id.quantity);
        Button buttonCancel=dialogView.findViewById(R.id.buttonCancel);
        Button buttonAddToCart=dialogView.findViewById(R.id.buttonAddToCart);
        TextView object_name=dialogView.findViewById(R.id.object_name);
        TextView object_brand=dialogView.findViewById(R.id.object_brand);
        TextView object_price=dialogView.findViewById(R.id.object_price);
        TextView textViewDescription=dialogView.findViewById(R.id.textViewDescription);
        ImageView object_image=dialogView.findViewById(R.id.object_image);

        Glide.with(this).load(item.image).into(object_image);
        object_name.setText(item.item_name);
        object_brand.setText(item.brand);
        textViewDescription.setText(item.description);
        object_price.setText(item.price);
        quantity.setText("1");


        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(alertDialogQuantity!=null)
                {
                    alertDialogQuantity.dismiss();
                }
            }
        });
        buttonAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String qty =quantity.getText().toString().trim();

                item.quantity = qty;
                carted_items.add(0,item);
                cartAdapter.notifyItemInserted(0);
                savedAdapter.notifyItemInserted(0);
                recyclerListCart.smoothScrollToPosition(0);
                textViewCount.setText(String.valueOf(carted_items.size()));

                if(alertDialogQuantity!=null)
                {
                    alertDialogQuantity.dismiss();
                }
            }
        });

        alertDialogQuantity = builder.create();
        alertDialogQuantity.show();

    }




    private void showEditQuantityDialog(final M_detected_class item,final int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(dialog_select_item, viewGroup, false);
        builder.setView(dialogView);

        final EditText quantity=dialogView.findViewById(R.id.quantity);
        Button buttonCancel=dialogView.findViewById(R.id.buttonCancel);
        Button buttonAddToCart=dialogView.findViewById(R.id.buttonAddToCart);
        TextView object_name=dialogView.findViewById(R.id.object_name);
        TextView object_brand=dialogView.findViewById(R.id.object_brand);
        TextView object_price=dialogView.findViewById(R.id.object_price);
        TextView textViewDescription=dialogView.findViewById(R.id.textViewDescription);
        ImageView object_image=dialogView.findViewById(R.id.object_image);

        buttonAddToCart.setText("Save");

        Glide.with(this).load(item.image).into(object_image);
        object_name.setText(item.item_name);
        object_brand.setText(item.brand);
        textViewDescription.setText(item.description);
        object_price.setText(item.price);
        quantity.setText(item.quantity);


        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(alertDialogQuantity!=null)
                {
                    alertDialogQuantity.dismiss();
                }
            }
        });
        buttonAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String qty =quantity.getText().toString().trim();

                item.quantity = qty;
                cartAdapter.notifyItemChanged(position);
                savedAdapter.notifyItemChanged(position);

                if(alertDialogQuantity!=null)
                {
                    alertDialogQuantity.dismiss();
                }
            }
        });



        alertDialogQuantity = builder.create();
        alertDialogQuantity.show();

    }






    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
         searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        // Configure the search info and add any event listeners
        searchView.setIconifiedByDefault(true);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                searchItem.setVisible(true);
                layoutSearch.setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                searchItem.setVisible(false);
                layoutSearch.setVisibility(View.VISIBLE);
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(active_shop!=null) {
                    getSearchResults(query);
                }
                suggestionListView.setAdapter(null);
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.length()>=3)
                {
                    if(active_shop!=null) {
                        getSuggestions(newText);
                    }
                    return true;
                }
                else
                {
                    suggestionListView.setAdapter(null);
                    return false;
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){

            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }



    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(BarCodeActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void handleResult(Result result) {
        final String myResult = result.getText();
        Log.d("QRCodeScanner", result.getText());
        Log.d("QRCodeScanner", result.getBarcodeFormat().toString());


        if(active_shop!=null) {
            // getItemDetails(result.getText());
            getItemDetails("Arduino_Nano");
        }
        else
        {
            getShopDetails("shop2");
        }

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void onBackPressed() {
        if(behavior.getState()==BottomSheetBehavior.STATE_EXPANDED)
        {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId) {
            // Android home
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            // manage other entries if you have it ...
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("main ", "response "+resultCode );
        /*
       E/main: response -1
       E/UPI: onActivityResult: txnId=AXI4a3428ee58654a938811812c72c0df45&responseCode=00&Status=SUCCESS&txnRef=922118921612
       E/UPIPAY: upiPaymentDataOperation: txnId=AXI4a3428ee58654a938811812c72c0df45&responseCode=00&Status=SUCCESS&txnRef=922118921612
       E/UPI: payment successfull: 922118921612
         */
        switch (requestCode) {
            case UPI_PAYMENT:
                if ((RESULT_OK == resultCode) || (resultCode == 11)) {
                    if (data != null) {
                        String trxt = data.getStringExtra("response");
                        Log.e("UPI", "onActivityResult: " + trxt);
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add(trxt);
                        upiPaymentDataOperation(dataList);
                    } else {
                        Log.e("UPI", "onActivityResult: " + "Return data is null");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                } else {
                    //when user simply back without payment
                    Log.e("UPI", "onActivityResult: " + "Return data is null");
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add("nothing");
                    upiPaymentDataOperation(dataList);
                }
                break;
        }
    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if (isConnectionAvailable(this)) {
            String str = data.get(0);
            Log.e("UPIPAY", "upiPaymentDataOperation: "+str);
            String paymentCancel = "";
            if(str == null) str = "discard";
            String status = "";
            String approvalRefNo = "";
            String response[] = str.split("&");
            for (int i = 0; i < response.length; i++) {
                String equalStr[] = response[i].split("=");
                if(equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                        status = equalStr[1].toLowerCase();
                    }
                    else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase()) || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())) {
                        approvalRefNo = equalStr[1];
                    }
                }
                else {
                    paymentCancel = "Payment cancelled by user.";
                }
            }
            if (status.equals("success")) {
                //Code to handle successful transaction here.
                Toast.makeText(this, "Transaction successful.", Toast.LENGTH_SHORT).show();
                Log.e("UPI", "payment successfull: "+approvalRefNo);
            }
            else if("Payment cancelled by user.".equals(paymentCancel)) {
                Toast.makeText(this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
                Log.e("UPI", "Cancelled by user: "+approvalRefNo);
            }
            else {
                Toast.makeText(this, "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
                Log.e("UPI", "failed payment: "+approvalRefNo);
            }
        } else {
            Log.e("UPI", "Internet issue: ");
            Toast.makeText(this, "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }
    public static boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }
}
