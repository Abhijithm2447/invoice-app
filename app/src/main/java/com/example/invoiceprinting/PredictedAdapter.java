package com.example.invoiceprinting;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PredictedAdapter extends RecyclerView.Adapter<PredictedAdapter.MyViewHolder> {
    List<M_detected_class> detected_classes;
    Context context;

    OnListClickListener listener;
    public PredictedAdapter(Context ctx, List<M_detected_class> detected_classes, OnListClickListener listener){
        context = ctx;
        this.detected_classes = detected_classes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.predicted_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        final M_detected_class item=detected_classes.get(position);

        holder.myTitle.setText(item.item_name);
        holder.myPrice.setText(item.price);

        Glide.with(holder.context).load(item.image).placeholder(R.color.dark_grey).into(holder.objImage);


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
        Context context;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            myTitle = (TextView) itemView.findViewById(R.id.object_name);
            myPrice = (TextView) itemView.findViewById(R.id.object_price);
            objImage = (ImageView) itemView.findViewById(R.id.object_image);
            context=itemView.getContext();

        }
    }


    public interface OnListClickListener{
        void onListClick(M_detected_class value);
    }
}
