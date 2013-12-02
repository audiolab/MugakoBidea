package com.audiolab.mugakobidea;

import android.app.ListFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import java.util.ListIterator;


public class ThumbFragment extends ListFragment {

    private WalksOpenHelper wDB;
    private Cursor cursor;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        cursor = loadDB();
        WalksAdapter wAd = new WalksAdapter(getActivity(), cursor, false);
        setListAdapter(wAd);
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        cursor.moveToPosition(position);
        Log.d("AREAGO", cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME)));
        ((Walks)getActivity()).walkSelected(cursor);
    }


    @Override
    public void onStart() {
        super.onStart();
        ListView lV = getListView();
        lV.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                cursor.moveToPosition(i);
                ((Walks)getActivity()).walkRemove(cursor);
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        return rootView;
    }

    private Cursor loadDB(){

        wDB = new WalksOpenHelper(getActivity());
        SQLiteDatabase db = wDB.getReadableDatabase();
        String[] projection = {
                WalkContract.WalkEntry._ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_LANG,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_RECORDINGS
        };
        String sortOrder = WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME + " DESC";
        Cursor c = db.query(
                WalkContract.WalkEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        //int u =  c.getCount();
        return c;
    }//loadDB

}
