package com.example.invoiceprinting;

import java.util.HashMap;
import java.util.List;

public class ShopResultModel {
    private List<String> error;
        private HashMap<String, shopModel> result;

    public ShopResultModel() {
    }

    public ShopResultModel(List<String> error, HashMap<String, shopModel> result) {
        this.error = error;
        this.result = result;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public HashMap<String, shopModel> getResult() {
        return result;
    }

    public void setResult(HashMap<String, shopModel> result) {
        this.result = result;
    }
}
