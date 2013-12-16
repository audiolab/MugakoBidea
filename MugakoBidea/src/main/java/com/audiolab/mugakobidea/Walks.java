package com.audiolab.mugakobidea;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
//import android.app.TaskStackBuilder;

import android.support.v4.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Walks extends Activity {

    String pref_url = "";
    String detailWalk;
    private long enqueue;
    private DownloadManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walks);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.walks_container, new ThumbFragment(), "listado")
                    .commit();
        }

        SharedPreferences shrPref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_url = shrPref.getString("url_preference", "http://www.mugakobidea.com");


        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = dm.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            String filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                            Uri downloadedUri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI)));
                            unZipDownloadedWalk(filename, downloadedUri.getLastPathSegment());
                            notifyDownloadFinish();
                        }
                    }
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));


    }

    public void unZipDownloadedWalk(String zipFile, String title) {
        if (!isExternalStorageWritable()) return;
        try {
            //Creamos el directorio con el ID donde se decargar el archivo
            File externalDir = getExternalFilesDir(null); // The external directory;
            String pathWalk = externalDir.getAbsolutePath() + "/" + title;
            File fold = new File(pathWalk);
            if (!fold.isDirectory()) fold.mkdir();

            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zin.getNextEntry();

            while (ze != null) {
                String filePath = pathWalk + File.separator + ze.getName();
                if (!ze.isDirectory()) {
                    // if the entry is a file, extracts it
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[4086];
                    int read = 0;
                    while ((read = zin.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zin.closeEntry();
                ze = zin.getNextEntry();
            }
            zin.close();
            finishedDownload(title);
            File zF = new File(zipFile);
            zF.delete();

        } catch (Exception e) {
            Log.e("AREAGO", "ERROR al descargar paseo" + e);
        }
        Log.e("AREAGO", "Paseo descargado");

    }

    ;

    public void walkRemove(Cursor cursor) {
        Log.d("AREAGO", "BORARRA PASEOOO");
        String id = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_ID));
        if (isExternalStorageWritable()) {
            //Significa que hay que descargar el paseo, así que cojo el id y se lo envío a la rutina de descarga
            //Chequear que puedo escribir en el disco externo.
            DialogFragment newFragment = new DeleteWalkDialogFragment(id);
            newFragment.show(getFragmentManager(), "confirmarBorrar");
        }
    }

    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void walkConfirmedRemove(String id) {
        WalksOpenHelper wDB;
        wDB = new WalksOpenHelper(this);
        SQLiteDatabase db = wDB.getWritableDatabase();

        String selection = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(id)};

        db.delete(WalkContract.WalkEntry.TABLE_NAME, selection, selectionArgs);
        File externalDir = getExternalFilesDir(null); // The external directory;
        String pathWalk = externalDir.getAbsolutePath() + "/" + id;
        DeleteRecursive(new File(pathWalk));


        ThumbFragment f = ((ThumbFragment) getFragmentManager().findFragmentByTag("listado"));

        if ((f != null)) {
            Log.d("AREAGO", "Hay que actualizar");
            f.refresh();
        }
        Context context = getApplicationContext();
        //TODO: Pasar textos como String
        CharSequence text = "Paseo eliminado";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void playClick(View v) {
        Log.d("AREAGO", "Hemos hehco click!!!");
        Log.d("AREAGO", "VIEW: " + v.toString());
        Intent i = new Intent(this, Walking.class);
        i.putExtra("wID", detailWalk);
        startActivity(i);
    }

    public void finishedDownload(String wID) {

        WalksOpenHelper wDB;
        wDB = new WalksOpenHelper(this);
        SQLiteDatabase db = wDB.getWritableDatabase();
        ContentValues cV = new ContentValues();
        cV.put(WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS, "OK");

        // Which row to update, based on the ID
        String selection = WalkContract.WalkEntry.COLUMN_NAME_WALK_ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(wID)};

        int count = db.update(
                WalkContract.WalkEntry.TABLE_NAME,
                cV,
                selection,
                selectionArgs);

        ThumbFragment f = ((ThumbFragment) getFragmentManager().findFragmentByTag("listado"));

        if ((f != null)) {
            Log.d("AREAGO", "Hay que actualizar");
            //WalksAdapter wAd = (WalksAdapter) f.getListAdapter();
            f.refresh();
            //wAd.notifyDataSetChanged();
        }

    }

    public void walkSelected(Cursor cursor) {
        //Si el paseo se tiene que descargar, voy a ejecutar el async de descarga de los archivos.
        String status = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_STATUS));
        String id = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_ID));
        String name = cursor.getString(cursor.getColumnIndex(WalkContract.WalkEntry.COLUMN_NAME_WALK_NAME));
        if (("NEW".equals(status) || "UPDATE".equals(status)) && isExternalStorageWritable()) {
            //Significa que hay que descargar el paseo, así que cojo el id y se lo envío a la rutina de descarga
            //Chequear que puedo escribir en el disco externo.

            //dW = new DownloadWalkAsync(this);
            //dW.execute(id);

            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(pref_url + "/soundwalk/descarga/" + id));
            request.setTitle(name);
            enqueue = dm.enqueue(request);
            Context context = getApplicationContext();
            //TODO: Pasar textos como String
            CharSequence text = "Descargando paseo";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        } else {
            //Abrimos el detalle
            getFragmentManager().beginTransaction()
                    .replace(R.id.walks_container, new WalkDetail(id), "detalle")
                    .addToBackStack(null)
                    .commit();
            detailWalk = id;
        }
    }

    private void notifyDownloadFinish() {
        //TODO: Poner los textos en Strings
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.icono)
                        .setContentTitle("Mugako Bidea")
                        .setContentText("Nuevos paseos descargados");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, Walks.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(Walks.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(3, mBuilder.getNotification());
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        boolean r = false;

        if (Environment.MEDIA_MOUNTED.equals(state)) r = true;
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) r = false;

        return r;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.walks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.walks_container, new PrefsFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DeleteWalkDialogFragment extends DialogFragment {

        private String mID;

        private DeleteWalkDialogFragment(String id) {
            this.mID = id;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View v = inflater.inflate(R.layout.pass_layout, null);
            builder.setView(v)
                    .setMessage(R.string.borrar_paseo)
                    .setPositiveButton(R.string.borrar, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            TextView p = (TextView) v.findViewById(R.id.password);
                            String pass = p.getText().toString();
                            if ("lorea".equals(pass)) {
                                walkConfirmedRemove(mID);
                            } else {
                                Context context = getApplicationContext();
                                CharSequence text = "Password incorrecto";
                                int duration = Toast.LENGTH_LONG;

                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                                DeleteWalkDialogFragment.this.getDialog().cancel();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            DeleteWalkDialogFragment.this.getDialog().cancel();
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }


}
