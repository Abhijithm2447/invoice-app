package com.example.invoiceprinting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CartedItemAdapter extends RecyclerView.Adapter<CartedItemAdapter.MyViewHolder> {
    List<M_detected_class> detected_classes=new ArrayList();
    Context context;

    OnListClickListener listener;
    public CartedItemAdapter(Context ctx, OnListClickListener listener){
        context = ctx;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.list_item_carted_sell, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {

        final M_detected_class item=detected_classes.get(position);

        holder.textViewName.setText(item.item_name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detected_classes.remove(holder.getAdapterPosition());
                notifyItemRemoved(holder.getAdapterPosition());
            }
        });
    }


    @Override
    public int getItemCount() {
        return detected_classes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        ImageView objImage;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewName = (TextView) itemView.findViewById(R.id.textViewName);

        }
    }

    public void addItem(M_detected_class item)
    {
        detected_classes.add(item);
        notifyItemInserted(detected_classes.size()-1);
    }

    public void removeAll()
    {
        detected_classes=new ArrayList();;
        notifyDataSetChanged();
    }

    public List<M_detected_class> getCartedItems()
    {
        return detected_classes;
    }


    public interface OnListClickListener{
        void onListClick(M_detected_class value);
    }
}
