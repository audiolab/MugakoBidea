package com.audiolab.mugakobidea;

import android.app.Activity;
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

import java.lang.ref.WeakReference;


public class Walking extends Activity {

    private WalksOpenHelper wDB;
    private Cursor cursor;
    private String mID;
    private LocationManager lManager;
    private LocationListener lListener;
    private Paseo paseo;
    private WalkingFragment frg;
    private MusicIntentReceiver myReceiver;
    private boolean ready = false;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking);

        mID = getIntent().getStringExtra("wID");
        cursor = loadWalk();

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        myReceiver = new MusicIntentReceiver();

        this.paseo = new Paseo(mID, this);
        String puntosJSON = paseo.loadJSONFile();
        if (puntosJSON != null) {
            this.paseo.create_points(puntosJSON);
        }

        if (!am.isWiredHeadsetOn()) {
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.walking_container, new CascosFragment())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        } else {
            ready = true;
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.walking_container, new PausedFragment(), "paused")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AREAGO", "onStart");
        lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lListener = new PaseoLocationListener(this.paseo);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        lManager.getLastKnownLocation(lManager.getBestProvider(crit, true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("AREAGO", "onResume");
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        if (myReceiver != null) registerReceiver(myReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ready) {
           // this.paseo.pause();
           // this.paseo.stop();
           // pararGPS();
        }
    }

    public void playPaseo(View v) {
        if (!this.playing) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.walking_container, new WalkingFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            this.playing = true;
            arrancarGPS();
            this.paseo.play();
        }
    }

    public void pausePaseo(View v) {
        if (this.playing) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.walking_container, new PausedFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            this.playing = false;
            this.paseo.pause();
        }
    }

    private void arrancarGPS() {
        lManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, lListener);
        this.paseo.play();
    }

    private void pararGPS() {
        try {
            lManager.removeUpdates(lListener);
            this.paseo.pause();
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

    public Cursor loadWalk() {

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
        String[] selectionArgs = {String.valueOf(mID)};

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

    public void cascosConectados() {

        ready = true;
        getFragmentManager().beginTransaction()
                .replace(R.id.walking_container, new PausedFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        this.playing = false;
        arrancarGPS();
        this.paseo.pause();
    }

    public void cascosDesconectados() {
        if (!ready) return;

        ready = false;

        getFragmentManager().beginTransaction()
                .replace(R.id.walking_container, new CascosFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        this.paseo.pause();
        this.playing = false;
        pararGPS();

    }

    private class PaseoLocationListener implements LocationListener {


        private WeakReference<Paseo> walk;

        private PaseoLocationListener(Paseo wl) {
            this.walk = new WeakReference<Paseo>(wl);

        }

        public void onLocationChanged(Location location) {
            if (location != null) {
                SoundPoint nl = new SoundPoint(location);
                Log.d("AREAGO", "Location changed");
                Paseo w = this.walk.get();
                if (w != null) {
                    Log.d("AREAGO", "vamos a chekear");
                    w.check_collisions(nl);
                }
            }
        }

        public void onProviderDisabled(String provider) {
            Log.d("AREAGO", "GPS Disable");
            Paseo w = walk.get();
            if (w != null) w.location_pause();
        }

        public void onProviderEnabled(String provider) {
            Log.d("AREAGO", "GPS Enabled");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String st = "";
            Paseo w = walk.get();
            switch (status) {
                case android.location.LocationProvider.AVAILABLE:
                    st = " Disponible";
                    break;
                case android.location.LocationProvider.OUT_OF_SERVICE:
                    st = " no disponible";
                    if (w != null) w.location_pause();
                    Log.d("AREAGO", "Pausamos el paseo por fuera de servicio");
                    break;
                case android.location.LocationProvider.TEMPORARILY_UNAVAILABLE:
                    st = " Temporalmente no disponible";
                    if (w != null) w.location_pause();
                    Log.d("AREAGO", "Pausamos el paseo por temporalmente no disponible");
                    break;
            }

        }

    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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

    private class CascosFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_no_cascos, container, false);
            return rootView;
        }


    }

    private class PausedFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_paused, container, false);
            return rootView;
        }
    }

    private class WalkingFragment extends Fragment {

        public WalkingFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_walking, container, false);
            return rootView;
        }


    }


}
