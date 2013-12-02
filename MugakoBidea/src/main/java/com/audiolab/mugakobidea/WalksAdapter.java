package com.audiolab.mugakobidea;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class WalksAdapter extends CursorAdapter{

    private LayoutInflater mLayoutInflater;
    private Context mContext;

    public WalksAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView title = (TextView)view.findViewById(R.id.title);
        title.setText(cursor.getString(cursor.getColumnIndex((WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME))));
        String status = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS));
        String url = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC));
        ImageView im = (ImageView)view.findViewById(R.id.icon);
        ProgressBar pb = (ProgressBar)view.findViewById(R.id.picture_loading);
        LinearLayout lay = (LinearLayout)view.findViewById(R.id.row_layout);
        if ("UPDATE".equals(status) || "NEW".equals(status)){
            lay.setBackgroundResource(R.drawable.listitem_red);
            //Cuando tenemos que hacer un update, buscamos la imagen en el url
            new DownloadRemotePicture(im, pb).execute(url);
        }else{
            lay.setBackgroundResource(R.drawable.listitem_normal);
            String path = getImagePath(cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_ID)));
            Bitmap bm = BitmapFactory.decodeFile(path + "/icono.jpg");
            im.setImageBitmap(bm);
            pb.setVisibility(View.INVISIBLE);
        }
        Log.d("AREAGO", "dentro del cursoradapter");

    }

    private String getImagePath(String wID){
        File externalDir = mContext.getExternalFilesDir(null); // The external directory;
        String pathWalk = externalDir.getAbsolutePath() + "/" + wID;
        return pathWalk;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        //devuelvo el view del row
        return mLayoutInflater.inflate(R.layout.fragment_row, viewGroup, false);
    }


    private class DownloadRemotePicture extends AsyncTask<String, Integer, Drawable>
    {
        private final WeakReference<ImageView> imageReference;
        private final WeakReference<ProgressBar> progressReference;

        @Override
        protected void onPostExecute(Drawable d) {
            if (imageReference != null && d != null) {
                final ImageView imageView = imageReference.get();
                final ProgressBar progressView = progressReference.get();
                if (imageView != null) {
                    imageView.setImageDrawable(d);
                    progressView.setVisibility(View.INVISIBLE);
                }
            }
        }

        private DownloadRemotePicture(ImageView imageView, ProgressBar pb) {
            imageReference = new WeakReference<ImageView>(imageView);  //guardo la referencia al view
            progressReference = new WeakReference<ProgressBar>(pb);
        }

        @Override
        protected Drawable doInBackground(String... url) {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(url[0]);
            Drawable drawable = null;
            try{

                HttpResponse response = httpClient.execute(request);
                InputStream is = response.getEntity().getContent();
                TypedValue typedValue = new TypedValue();
                typedValue.density = TypedValue.DENSITY_NONE;
                drawable = Drawable.createFromResourceStream(null, typedValue, is, "src");

            }catch (IOException e){
                e.printStackTrace();
            }

            return drawable;
        }

    }


}
