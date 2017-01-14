package com.log.cyclone;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.log.cyclone.General.Chat;

public class ChatAdapter extends ArrayAdapter<Chat>{
	Context context;
	int id;
	LayoutInflater inflater;
	ArrayList<Chat> list;
	View v;
	Chat entity;
	public ChatAdapter(Context context, int textViewResourceId,ArrayList<Chat> objects){
		super(context, textViewResourceId, objects);
		this.context=context;
		id = textViewResourceId;	
		this.list = objects;
		inflater = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
		
	}
	private static class ViewHolder{
		TextView textLeft, textRight;
	}
	public void setViewHolder(ViewHolder holder){
		holder.textLeft = (TextView) v.findViewById(R.id.textLeft);
		
		holder.textRight=(TextView)v.findViewById(R.id.textRight);
	}


	@SuppressLint("NewApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		v = convertView;
		final ViewHolder holder;
		entity = list.get(position);
		if(convertView == null){
			v = inflater.inflate(id, null);
			holder = new ViewHolder();
			setViewHolder(holder);
			v.setTag(holder);
		}else{
			holder = (ViewHolder) v.getTag();
		}
		if(entity.getSent().equalsIgnoreCase("y")){
			holder.textLeft.setText(entity.getMessage());
			holder.textLeft.setVisibility(View.VISIBLE);
			holder.textRight.setVisibility(View.GONE);
		}
		else{
			holder.textRight.setText(entity.getMessage());
			holder.textLeft.setVisibility(View.GONE);
			holder.textRight.setVisibility(View.VISIBLE);
		}
		return v;
	}
}
