package com.audiolab.mugakobidea;


import android.app.Fragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class WalkDetail extends Fragment{

    private WalksOpenHelper wDB;
    private Cursor cursor;
    private String mID;


    public WalkDetail(String wID) {
        mID = wID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail_walk, container, false);

        cursor = loadWalk();
        if (cursor != null){
            TextView t = (TextView)rootView.findViewById(R.id.detail_title);
            t.setText(cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME)));
            TextView ex = (TextView)rootView.findViewById(R.id.detail_excerpt);
            ex.setText(cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT)));
            ImageView im = (ImageView)rootView.findViewById(R.id.detail_picture);
            Bitmap bm = BitmapFactory.decodeFile(getImagePath(mID) + "/icono.jpg");
            im.setImageBitmap(bm);
        }

        return rootView;
    }

    private String getImagePath(String wID){
        File externalDir = getActivity().getExternalFilesDir(null); // The external directory;
        String pathWalk = externalDir.getAbsolutePath() + "/" + wID;
        return pathWalk;
    }

    private Cursor loadWalk(){

        wDB = new WalksOpenHelper(getActivity());

        SQLiteDatabase rDB = wDB.getReadableDatabase();
        String[] projection = {
                WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_RECORDINGS,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_LANG,
        };

        String selection = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " == ?";
        String[] selectionArgs = { String.valueOf(mID) };

        Cursor c = rDB.query(
                WalkContract.WalkEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        if (c.getCount() == 0) return null;

        c.moveToFirst();
        return c;

    }

}
