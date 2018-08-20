package com.hackwithinfy.one.hoarding;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

class RecyclerViewAdapter extends RecyclerView.Adapter {
    private ArrayList<String> values;
    private ArrayList<String> image;
    public Context context;
    RecyclerViewAdapter(ArrayList<String> values,ArrayList<String> image,Context context) {
        this.values = values;
        this.context = context;
        this.image = image;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SampleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_list_items,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((SampleViewHolder)holder).name.setText(values.get(position));
        Glide.with(context).load(image.get(position)).into(((SampleViewHolder)holder).img);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    class SampleViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public ImageView img;
        SampleViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textTitle);
            img =  itemView.findViewById(R.id.imageBanner);
        }
    }
}


