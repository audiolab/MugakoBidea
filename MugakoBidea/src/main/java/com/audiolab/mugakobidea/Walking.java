package com.audiolab.mugakobidea;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;


public class Walking extends Activity{

    private WalksOpenHelper wDB;
    private Cursor cursor;
    private String mID;
    private LocationManager lManager;
    private LocationListener lListener;
    private Paseo paseo;

    private WalkingFragment frg;
    private MusicIntentReceiver myReceiver;

    private boolean ready = false;
    private String state = "PAUSED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking);

        mID = getIntent().getStringExtra("wID");
        cursor = loadWalk();

        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        myReceiver = new MusicIntentReceiver();

        if(!am.isWiredHeadsetOn()){
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.walking_container, new CascosFragment())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        }else{
            ready = true;
            frg = new WalkingFragment();
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.walking_container, frg)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
            paseo = new Paseo(mID, this);
            String puntosJSON = paseo.loadJSONFile();
            if(puntosJSON != null){
                paseo.create_points(puntosJSON);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AREAGO","onStart");
        lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lListener = new PaseoLocationListener(frg, paseo);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        lManager.getLastKnownLocation(lManager.getBestProvider(crit, true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("AREAGO","onResume");
        if(ready){
            arrancarGPS();
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        if( myReceiver != null ) registerReceiver(myReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(ready){
            this.paseo.pause();
            this.paseo.stop();
            pararGPS();
        }
    }

    private void arrancarGPS(){
        lManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, lListener);
    }

    private void pararGPS(){
        try {
            lManager.removeUpdates(lListener);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.walking, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public Cursor loadWalk(){

        wDB = new WalksOpenHelper(this);

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

    public void cascosConectados()
    {
        frg = new WalkingFragment();
        ready = true;
        getFragmentManager().beginTransaction()
                    .replace(R.id.walking_container, frg)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();

        paseo = new Paseo(mID, this);
        String puntosJSON = paseo.loadJSONFile();
        if(puntosJSON != null){
            paseo.create_points(puntosJSON);
        }
        arrancarGPS();
    }

    public void cascosDesconectados(){
        if (!ready) return;

        ready = false;

        getFragmentManager().beginTransaction()
                .replace(R.id.walking_container, new CascosFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        this.paseo.pause();
        pararGPS();

    }


    private class PaseoLocationListener implements LocationListener {

        private WeakReference<WalkingFragment> wFrg;
        private WeakReference<Paseo> walk;

        public void onLocationChanged(Location location) {
            if (location != null) {
                ((TextView)findViewById(R.id.logger)).append(" Posición: " + location.getLatitude() + " / " + location.getLongitude() + " / " + location.getAccuracy());
                //((TextView)findViewById(R.id.status_gps)).setVisibility(View.GONE);
                SoundPoint nl = new SoundPoint(location);
                Log.d("AREAGO","Location changed");

                // Miramos si el punto actual está dentro del radio de acción de algun punto del paseo.
                // Si nos hemos movido
                Paseo w = walk.get();
                if(w != null){
                    w.check_collisions(nl);
                }
            }
        }

        private PaseoLocationListener(WalkingFragment frg, Paseo wl) {
            wFrg = new WeakReference<WalkingFragment>(frg);
            walk = new WeakReference<Paseo>(wl);

        }

        public void onProviderDisabled(String provider) {
            ((TextView)findViewById(R.id.logger)).append(provider + " desconectado");
            Log.d("AREAGO","GPS Disable");
            //((TextView)findViewById(R.id.logger)).setText("Dispositivo GPS desactivado");
            //((TextView)findViewById(R.id.status_gps)).setVisibility(View.VISIBLE);
            Paseo w = walk.get();
            if (w != null) w.location_pause();
        }

        public void onProviderEnabled(String provider) {
            ((TextView)findViewById(R.id.logger)).append("GPS Conectado: " + provider);
            //((TextView)findViewById(R.id.status_gps)).setText("Dispositivo GPS activado");
            //((TextView)findViewById(R.id.status_gps)).setVisibility(View.VISIBLE);
            Log.d("AREAGO","GPS Enabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String st = "";
            Paseo w = walk.get();
            switch (status) {
                case android.location.LocationProvider.AVAILABLE:
                    st=" Disponible";
                    break;
                case android.location.LocationProvider.OUT_OF_SERVICE:
                    st=" no disponible";
                    if (w != null) w.location_pause();
                    Log.d("AREAGO","Pausamos el paseo por fuera de servicio");
                    break;
                case android.location.LocationProvider.TEMPORARILY_UNAVAILABLE:
                    st=" Temporalmente no disponible";
                    if(w != null) w.location_pause();
                    Log.d("AREAGO","Pausamos el paseo por temporalmente no disponible");
                    break;
            }
            ((TextView)findViewById(R.id.logger)).append("GPS Status:" + st);
            Log.d("AREAGO","GPS Status: " + st);
            //((TextView)findViewById(R.id.status_gps)).setText(getString(R.string.dipositivo_gps)+st);
            //((TextView)findViewById(R.id.status_gps)).setVisibility(View.VISIBLE);
            Log.d("AREAGO","Status" + st);
        }

    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d("AREAGO", "Headset is unplugged");
                        cascosDesconectados();
                        break;
                    case 1:
                        Log.d("AREAGO", "Headset is plugged");
                        cascosConectados();
                        break;
                    default:
                        Log.d("AREAGO", "I have no idea what the headset state is");
                }
            }
        }
    }

    public class CascosFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_no_cascos, container, false);
            return rootView;
        }


    }

}
