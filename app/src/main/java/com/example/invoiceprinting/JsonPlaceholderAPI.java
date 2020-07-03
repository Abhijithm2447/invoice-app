package com.example.invoiceprinting;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JsonPlaceholderAPI {
    @POST("rec_object/")
    Call<M_receive_image> recObject(@Body Map<String, String> parameters);

}
