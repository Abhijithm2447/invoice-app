package com.example.invoiceprinting;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionModel {
    private List<String> error;
    private  Suggestions result;

    public List<String> getError() {
        return error;
    }

    public void setError(List<String> error) {
        this.error = error;
    }

    public Suggestions getResult() {
        return result;
    }

    public void setResult(Suggestions result) {
        this.result = result;
    }

    static class Suggestions{
        private ArrayList<String> names;

       public ArrayList<String> getNames() {
           return names;
       }

       public void setNames(ArrayList<String> names) {
           this.names = names;
       }
   }
}
