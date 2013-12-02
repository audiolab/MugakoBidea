package com.audiolab.mugakobidea;

import android.provider.BaseColumns;

public final class WalkContract {

    public WalkContract() {}

    public static abstract class WalkEntry implements BaseColumns {
        public static final String TABLE_NAME = "walks";
        public static final String COLUMN_NAME_WALK_ID = "walkid";
        public static final String COLUMN_NAME_WALK_NAME = "name";
        public static final String COLUMN_NAME_WALK_EXCERPT = "excerpt";
        public static final String COLUMN_NAME_WALK_RECORDINGS = "recordings";
        public static final String COLUMN_NAME_WALK_LANG = "lang";
        public static final String COLUMN_NAME_WALK_PIC = "picture";
        public static final String COLUMN_NAME_WALK_HASH = "hash";
        public static final String COLUMN_NAME_WALK_STATUS = "status";
    }

}
