package com.audiolab.mugakobidea;



import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class WalkingFragment extends Fragment {



    public WalkingFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_walking, container, false);
        servicesConnected();
        return rootView;
    }

    private boolean servicesConnected() {
        return true;
    }


}