package com.example.invoiceprinting;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
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
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.example.invoiceprinting.R.layout.dialog_select_item;


public class CameraActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    CameraView cameraView;
    ImageView btn_capture;
    ImageView imageViewCart;
    TextView textViewSearch;
    TextView textViewCount;
    SearchView searchView;
    MenuItem searchItem;
    FrameLayout layoutSearch;
    AVLoadingIndicatorView progressView;
    LinearLayout layoutCartTitle;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Toolbar toolBar=findViewById(R.id.toolBarLayout);
        setSupportActionBar(toolBar);
        setTitle("");

        cameraView = findViewById(R.id.cameraView);
        btn_capture = findViewById(R.id.imageViewCapture);
        textViewSearch = findViewById(R.id.textViewSearch);
        textViewCount = findViewById(R.id.textViewCount);
        layoutSearch = findViewById(R.id.layoutSearch);
        progressView=findViewById(R.id.avindicator);
        imageViewCart=findViewById(R.id.imageViewCart);
        layoutCartTitle=findViewById(R.id.layoutCartTitle);
        suggestionListView=findViewById(R.id.searchSuggestionList);

        if(allPermissionsGranted()){
            cameraView.setLifecycleOwner(this);
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cameraView.takePicture();
            }
        });

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);

                Log.d("camera","taken_image");
                BitmapCallback callback=new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                        Log.d("camera","bitmap ready");

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream .toByteArray();
                        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        SentImageToServer(encoded);
                    }
                };

                result.toBitmap(400, 600, callback);

            }
        });

        textViewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchItem.expandActionView();
            }
        });


        // recycler view
        rViewListPredicteItems = (RecyclerView) findViewById(R.id.recyclerViewPredictedItems);
        rViewListPredicteItems.setLayoutManager(new LinearLayoutManager(CameraActivity.this, RecyclerView.HORIZONTAL,false));

        setUpCartRecyclerView();

        setUpSavedRecyclerView();


        LinearLayout layout_bottom_sheet=findViewById(R.id.layout_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(layout_bottom_sheet);
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
                getSearchResults(selected_suggestion);
                suggestionListView.setAdapter(null);
                searchItem.collapseActionView();
            }
        });

    }






    private void setUpCartRecyclerView()
    {
        // recycler view
        recyclerListCart = (RecyclerView) findViewById(R.id.recyclerViewCartItems);
        recyclerListCart.setLayoutManager(new LinearLayoutManager(CameraActivity.this,RecyclerView.HORIZONTAL,false));

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
        recyclerViewSavedItems.setLayoutManager(new LinearLayoutManager(CameraActivity.this,RecyclerView.VERTICAL,false));

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
                    Toast.makeText(CameraActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
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

                predictedItemAdapter = new PredictedAdapter(CameraActivity.this,detected_objs,listener);
                rViewListPredicteItems.setAdapter(predictedItemAdapter);
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                progressView.hide();
                Toast.makeText(CameraActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d("camera","sending failed");

            }
        });


    }



    private void getSuggestions(String query) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);

        Call<SearchSuggestionModel> call = jsonPlaceHolderApi.getSearchSuggestions(query);
        call.enqueue(new Callback<SearchSuggestionModel>() {
            @Override
            public void onResponse(Call<SearchSuggestionModel> call, Response<SearchSuggestionModel> response) {

                if(!response.isSuccessful()){
                    Toast.makeText(CameraActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(response.body()!=null) {
                    ArrayList<String> suggestions=response.body().getResult().getNames();
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(CameraActivity.this, android.R.layout.simple_list_item_1, suggestions);
                    suggestionListView.setAdapter(adapter);
                }



            }

            @Override
            public void onFailure(Call<SearchSuggestionModel> call, Throwable t) {
                Toast.makeText(CameraActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }



    private void getSearchResults(String query) {

        Retrofit retrofit =RetrofitClientInstance.getRetrofitInstance();

        JsonPlaceholderAPI jsonPlaceHolderApi = retrofit.create(JsonPlaceholderAPI.class);
        progressView.show();
        Call<M_receive_image> call = jsonPlaceHolderApi.searchQuery(query);
        call.enqueue(new Callback<M_receive_image>() {
            @Override
            public void onResponse(Call<M_receive_image> call, Response<M_receive_image> response) {
                progressView.hide();

                if(!response.isSuccessful()){
                    Toast.makeText(CameraActivity.this, "Code: "+ response.code(), Toast.LENGTH_SHORT).show();
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

                predictedItemAdapter = new PredictedAdapter(CameraActivity.this,detected_objs,listener);
                rViewListPredicteItems.setAdapter(predictedItemAdapter);
            }

            @Override
            public void onFailure(Call<M_receive_image> call, Throwable t) {
                progressView.hide();
                Toast.makeText(CameraActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
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
                getSearchResults(query);
                suggestionListView.setAdapter(null);
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(newText.length()>=3)
                {
                    getSuggestions(newText);
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
                cameraView.setLifecycleOwner(this);
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }



}
