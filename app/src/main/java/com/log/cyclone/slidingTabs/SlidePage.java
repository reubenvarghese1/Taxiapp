package com.log.cyclone.slidingTabs;

import com.log.cyclone.R;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


/**
 * Created by Logan on 4/9/2016.
 */
public class SlidePage extends Fragment {

    private ImageView   ivImage;
    private int         imageResID;

    public SlidePage() {

    }

    @SuppressLint("ValidFragment")
    public SlidePage(int imageResID) {
        super();
        this.imageResID = imageResID;
    }

    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_slide, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivImage = (ImageView) view.findViewById(R.id.iv_image);
        ivImage.setImageResource(imageResID);
    }

    public void setImageResID(int imageResID) {
        this.imageResID = imageResID;
    }
}
