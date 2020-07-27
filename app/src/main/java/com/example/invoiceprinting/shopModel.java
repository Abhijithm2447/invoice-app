package com.example.invoiceprinting;

import java.util.HashMap;
import java.util.List;

public class shopModel {
    String shop_id;
   String address;
   String description;
   String shop_type;
   String shop_name;
   String pincode;
   String owner_name;
   String upi_id;


    public shopModel(String address, String description, String shop_type, String shop_name, String pincode,String owner_name, String shop_id,String upi_id) {
        this.address = address;
        this.description = description;
        this.shop_type = shop_type;
        this.shop_name = shop_name;
        this.pincode = pincode;
        this.owner_name = owner_name;

        this.shop_id = shop_id;
        this.upi_id = upi_id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShop_type() {
        return shop_type;
    }

    public void setShop_type(String shop_type) {
        this.shop_type = shop_type;
    }

    public String getShop_name() {
        return shop_name;
    }

    public void setShop_name(String shop_name) {
        this.shop_name = shop_name;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public String getShop_id() {
        return shop_id;
    }

    public void setShop_id(String shop_id) {
        this.shop_id = shop_id;
    }

    public String getUpi_id() {
        return upi_id;
    }

    public void setUpi_id(String upi_id) {
        this.upi_id = upi_id;
    }
}
