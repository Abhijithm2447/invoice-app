package com.example.invoiceprinting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import static com.example.invoiceprinting.R.layout.dialog_select_item;

public class PredictedItemAdapter extends RecyclerView.Adapter<PredictedItemAdapter.MyViewHolder> {
    List<M_detected_class> detected_classes;
    Context context;

    OnListClickListener listener;
    public PredictedItemAdapter(Context ctx, List<M_detected_class> detected_classes,OnListClickListener listener){
        context = ctx;
        this.detected_classes = detected_classes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.predicted_item_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        final M_detected_class item=detected_classes.get(position);

        holder.myTitle.setText(item.item_name);
        holder.myPrice.setText(item.price);
/*
        holder.objImage.setImageResource(images[position]);
*/
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onListClick(item);
            }
        });
    }


    @Override
    public int getItemCount() {
        return detected_classes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView myTitle, myPrice;
        ImageView objImage;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            myTitle = (TextView) itemView.findViewById(R.id.object_name);
            myPrice = (TextView) itemView.findViewById(R.id.object_price);
            objImage = (ImageView) itemView.findViewById(R.id.object_image);
        }
    }


    public interface OnListClickListener{
        void onListClick(M_detected_class value);
    }
}
