package com.example.invoiceprinting;

import com.google.gson.annotations.SerializedName;

public class M_detected_class {
    @SerializedName("shop_id") String shop_id;
    @SerializedName("item_id") String item_id;
    @SerializedName("item_name") String item_name;
    @SerializedName("brand") String brand;
    @SerializedName("image") String image;
    @SerializedName("price") String price;
    @SerializedName("measurement_unit") String measurement_unit;
    @SerializedName("description") String description;
    @SerializedName("validity") String validity;
    @SerializedName("quantity") String quantity;
}
