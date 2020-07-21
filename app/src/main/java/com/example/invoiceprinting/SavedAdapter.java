package com.example.invoiceprinting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.MyViewHolder> {
    List<M_detected_class> detected_classes;
    Context context;

    OnListClickListener listener;
    public SavedAdapter(Context ctx,List<M_detected_class> list, OnListClickListener listener){
        context = ctx;
        this.listener = listener;
        detected_classes=list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.list_item_saved, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        final M_detected_class item=detected_classes.get(position);

        Glide.with(context).load(item.image).placeholder(R.color.grey).into(holder.object_image);
        holder.object_brand.setText(item.brand);
        holder.object_name.setText(item.item_name);
        holder.object_price.setText(item.price);
        holder.object_quantity.setText(item.quantity);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onListClick(item,holder.getAdapterPosition());
            }
        });

        holder.textViewRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onRemoveClicked(item,holder.getAdapterPosition());
            }
        });
    }


    @Override
    public int getItemCount() {
        return detected_classes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView object_image;
        TextView object_name,object_brand,object_price,object_quantity,textViewRemove;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            object_image = (ImageView) itemView.findViewById(R.id.object_image);
            object_name = (TextView) itemView.findViewById(R.id.object_name);
            object_brand = (TextView) itemView.findViewById(R.id.object_brand);
            object_price = (TextView) itemView.findViewById(R.id.object_price);
            object_quantity = (TextView) itemView.findViewById(R.id.object_quantity);
            textViewRemove = (TextView) itemView.findViewById(R.id.textViewRemove);

        }
    }




    public interface OnListClickListener{
        void onListClick(M_detected_class value,int position);
        void onRemoveClicked(M_detected_class value,int position);
    }
}
