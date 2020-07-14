package com.example.invoiceprinting;

import java.util.HashMap;
import java.util.List;

public class M_add_carted_items {
    private List<String> error;
    private HashMap<String, String> result;

    public M_add_carted_items() {
    }

    public M_add_carted_items(List<String> error, HashMap<String, String> result) {
        this.error = error;
        this.result = result;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public HashMap<String, String> getResult() {
        return result;
    }

    public void setResult(HashMap<String, String> result) {
        this.result = result;
    }
}
