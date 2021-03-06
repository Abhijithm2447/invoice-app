package com.example.invoiceprinting;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface JsonPlaceholderAPI {
    @POST("rec_object/")
    Call<M_receive_image> recObject(@Body Map<String, String> parameters);

    @POST("add_carted_items/")
    Call<M_add_carted_items> addCartedItem(@Body Map<String, List<M_detected_class>> parameters);

    @FormUrlEncoded
    @POST("searchbar_suggestions/")
    Call<SearchSuggestionModel> getSearchSuggestions(@Field("query") String query);

    @FormUrlEncoded
    @POST("search/")
    Call<M_receive_image> searchQuery(@Field("query") String query);
}
