package com.example.invoiceprinting;

import java.util.HashMap;
import java.util.List;

public class M_receive_image {
    private List<String> error;
    private HashMap<String, List<HashMap<String, String>>> result;

    public M_receive_image() {
    }

    public M_receive_image(List<String> error, HashMap<String, List<HashMap<String, String>>> result) {
        this.error = error;
        this.result = result;
    }

    public List<String> getError() {
        return error;
    }

    public HashMap<String, List<HashMap<String, String>>> getResult() {
        return result;
    }
}
