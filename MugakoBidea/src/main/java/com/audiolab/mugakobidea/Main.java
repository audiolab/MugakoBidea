package com.audiolab.mugakobidea;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Main extends Activity {


    String pref_url="";
    WalksOpenHelper wDB;
    boolean gpsDialog = false;
    boolean loadingWalks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SplashFragment())
                    .commit();
        }

        SharedPreferences shrPref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_url = shrPref.getString("url_preference", "http://www.mugakobidea.com");
        wDB = new WalksOpenHelper(this);

        if (!isGPSEnabled()){
            EnableGPS();
        }
        if (isDataConnected()){
            loadingWalks = true;
            new DownloadWalksTask().execute(pref_url);
        }

        ifIsReadyContinue();

    }
    public void openGpsSettingsActivity(){
        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
    }
    public void downloadFinish(String paseos){
        loadingWalks = false;
        Cursor local = loadLocalWalks();
        try{
            JSONArray jremote= new JSONArray(paseos);
            Log.d("AREAGO", "Contador: " + local.getCount());
            if (local.getCount() == 0){
                addWalksToDB(jremote);
            }else{
               compareHashes(jremote);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

        ifIsReadyContinue();
    }

    public void compareHashes(JSONArray jr){
        // Compara los ids remotos y locales. Si son iguales, comprar el hash. Si es igual, pasa, si es diferente, lo marca con UPDATE.
        JSONArray toUpdate = new JSONArray();
        JSONArray toAdd = new JSONArray();
        try {
            for (int i = 0; i<jr.length();i++) {
                JSONObject jObject = jr.getJSONObject(i);
                String wID = jObject.getString("id");
                String hash = getHashFromID(wID);
                if(hash!=null){
                    String rHash = jObject.getString("hash");
                    if (!rHash.equals(hash)){
                        toUpdate.put(jObject);
                    }
                }else{
                    toAdd.put(jObject);
                }

            }//for
            updateWalksToDB(toUpdate);
            addWalksToDB(toAdd);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private String getHashFromID(String id){
        SQLiteDatabase rDB = wDB.getReadableDatabase();
        String[] projection = {
                WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH
        };
        String selection = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };

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
        return c.getString(0);
    }

    public void closedGPS(){
        gpsDialog = false;
        ifIsReadyContinue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        gpsDialog = false;
        ifIsReadyContinue();
    }
    public void ifIsReadyContinue(){
        if (gpsDialog == false && loadingWalks == false){
            Intent intent = new Intent(this, Walks.class);
            startActivity(intent);
        }
    }
    private boolean isDataConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();
        if (mWifi != null) return mWifi.isConnected();
        return false;
    }
    private boolean isGPSEnabled() {
        // TODO Auto-generated method stub

        LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ( !locManager.isProviderEnabled("gps") ) return false;
        return true;
    }
    public void EnableGPS () {
        gpsDialog = true;
        DialogFragment enableGpsDialog = new GpsDialog();
        enableGpsDialog.show(getFragmentManager(), "enablegps");
    }

    public void onSplashClick(View v){
        ifIsReadyContinue();
    }

    public void updateWalksToDB(JSONArray walks){
        SQLiteDatabase db = wDB.getWritableDatabase();
        Log.d("AREAGO", "Entramos en la base de datos");
        try {
            for (int i = 0; i<walks.length();i++) {
                JSONObject jObject = walks.getJSONObject(i);
                ContentValues cV = new ContentValues();
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME, jObject.getString("nombre"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT, jObject.getString("resumen"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_RECORDINGS, jObject.getString("grabaciones"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_LANG, jObject.getString("idioma"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC, jObject.getString("imagen"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH, jObject.getString("hash"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS, "UPDATE");

                // Which row to update, based on the ID
                String selection = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " LIKE ?";
                String[] selectionArgs = { String.valueOf(jObject.getString("id")) };

                int count = db.update(
                        WalkContract.WalkEntry.TABLE_NAME,
                        cV,
                        selection,
                        selectionArgs);
            }//for
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("AREAGO", "Error en la base de datos");
        }

    }
    public void addWalksToDB(JSONArray walks){
        SQLiteDatabase db = wDB.getWritableDatabase();
        try {
            for (int i = 0; i<walks.length();i++) {
                JSONObject jObject = walks.getJSONObject(i);
                ContentValues cV = new ContentValues();
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_ID, jObject.getString("id"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME, jObject.getString("nombre"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_EXCERPT, jObject.getString("resumen"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_RECORDINGS, jObject.getString("grabaciones"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_LANG, jObject.getString("idioma"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_PIC, jObject.getString("imagen"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH, jObject.getString("hash"));
                cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS, "NEW");
                long newRowId;
                newRowId = db.insert(
                        WalkContract.WalkEntry.TABLE_NAME,
                        null,
                        cV);
            }//for
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Cursor loadLocalWalks(){

        SQLiteDatabase db = wDB.getReadableDatabase();
        String[] projection = {
                WalkContract.WalkEntry._ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_ID,
                WalkContract.WalkEntry.COLUMN_NAME_WALK_HASH
        };
        String sortOrder = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " DESC";
        Cursor c = db.query(
                WalkContract.WalkEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        return c;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.splash, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new PrefsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class SplashFragment extends Fragment {

        public SplashFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_splash, container, false);
            return rootView;
        }
    }


    public class GpsDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.gps_dialog)
                    .setPositiveButton(R.string.activar, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ((Main)getActivity()).openGpsSettingsActivity();
                        }
                    })
                    .setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ((Main)getActivity()).closedGPS();
                        }
                    });
            return builder.create();
        }
    }

    private class DownloadWalksTask extends AsyncTask<String, Integer, String>
    {
        @Override
        protected void onPostExecute(String s) {
            downloadFinish(s);
        }

        @Override
        protected String doInBackground(String... url) {
            String paseos = "";
            try {
                Log.d("AREAGO", "Estoy en los paseos");
                paseos = checkRemote();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("AREAGO", paseos);
            return paseos;
        }

        private String checkRemote(){
            Log.d("AREAGO", "Estoy en checkRemote");
            int BUFFER_SIZE = 2000;
            String str = "";
            InputStream in = null;
            try{
                in = OpenHttpConnection(pref_url + "/soundwalk/listado");
                Log.d("AREAGO", "Abriendo conexión..");
            }catch (IOException e){
                Log.d("AREAGO",e.getLocalizedMessage());
                return str;
            }

            InputStreamReader isr = new InputStreamReader(in);
            int charRead;
            char[] inputBuffer = new char[BUFFER_SIZE];
            try{
                while ((charRead = isr.read(inputBuffer))>0){
                    String readString = String.copyValueOf(inputBuffer, 0, charRead);
                    str += readString;
                    inputBuffer = new char[BUFFER_SIZE];
                }
                in.close();
            }catch (IOException e){
                Log.d("AREAGO", e.getLocalizedMessage());
                str="";
                return str;
            }
            return str;
        }

        private InputStream OpenHttpConnection (String urlString) throws IOException {
            InputStream in = null;
            int response = -1;
            URL url = new URL(urlString);
            HttpURLConnection conn =(HttpURLConnection) url.openConnection();
            try {

                Log.d("AREAGO", "sfdsfsdf");
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.connect();
                Log.d("AREAGO", "djfusjskdofofiskdjfhsksncjcjdd");
                response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK){
                    in = conn.getInputStream();
                }
            } catch (IOException ex) {
                Log.d("AREAGO", ex.getLocalizedMessage());
                throw new IOException("Error connecting");
            }
            return in;
        }
    }
}
