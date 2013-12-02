package com.audiolab.mugakobidea;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WalksOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "MugakoBidea.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + WalkContract.WalkEntry.TABLE_NAME + " (" +
                    WalkContract.WalkEntry._ID + " INTEGER PRIMARY KEY," +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_LANG + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_RECORDINGS + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC + TEXT_TYPE + COMMA_SEP +
                    WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WalkContract.WalkEntry.TABLE_NAME;

    public WalksOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }
}
