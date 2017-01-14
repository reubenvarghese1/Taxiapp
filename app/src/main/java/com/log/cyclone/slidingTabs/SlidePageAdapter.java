package com.log.cyclone.slidingTabs;

import android.support.v4.app.FragmentPagerAdapter;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by LoganPC on 6/4/2016.
 */
public class SlidePageAdapter extends FragmentPagerAdapter {
    List<SlidePage> fragmentList = new ArrayList<SlidePage>();

    public SlidePageAdapter(android.support.v4.app.FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public SlidePage getItem(int position) {
        if (position < fragmentList.size()) {
            return fragmentList.get(position);
        }
        return null;
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    public void add(SlidePage page) {
        fragmentList.add(page);
    }

    public void clear() {
        for (int i = 0; i < fragmentList.size(); i++)
            fragmentList.remove(i);
        fragmentList.clear();
    }
}
