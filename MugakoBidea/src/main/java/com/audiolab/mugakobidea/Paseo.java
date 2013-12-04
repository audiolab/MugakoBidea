package com.audiolab.mugakobidea;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

public class Paseo {

    private String id ;
    private String titulo;
    private String idioma;
    private String descripcion;
    private String excerpt;
    private String imagen = "none";
    private Bitmap img = null;
    private int grabaciones;
    private int tamano;
   // private SoundPoint pref = new SoundPoint("paseo_reference"); // Punto de referencia de inicio / lat-lon
    private String hash = "";
    private boolean update=false; // ya está descargado pero necesita ser actualizado porque el hash es diferente
    private boolean downlad=false; // necesita ser descargado?
    private String JsonPoints; // Listado de puntos del mapa en Json Array
    private ArrayList<SoundPoint> puntos = new ArrayList<SoundPoint>();
    private int layer = 0; // por defecto el paseo se inicia en la layer 0
    private Vibrator v;
    private boolean paused = true;

    private WeakReference<Walking> wAc;

    // Creadoras
    public Paseo(String id, Walking w) {
        this.id = id;
        this.layer = 0; // iniciamos el paseo en el layer 0
        this.wAc = new WeakReference<Walking>(w);  //guardamos una referencia a la actividad
    }

    // Modificadoras
    public void setTitle(String t) {
        this.titulo = t;
    }

    public void setDescription(String d) {
        this.descripcion = d;
    }

    public void setExcerpt(String e) {
        this.excerpt = e;
    }

    public void setIdioma(String lan) {
        this.idioma=lan;
    }

    public void setGrabaciones(int grabaciones) {
        this.grabaciones = grabaciones;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setUpdated() {
        this.update=true;
    }

    public void setOutdated() {
        this.update=false;
    }

    public void setDownloaded() {
        this.downlad=true;
    }

    public void setNotDownloaded() {
        this.downlad=false;
    }

    public void setPoints(String points) {
        this.JsonPoints = points;
    }

    public void setImage(String uri) {
        this.imagen = uri;
    }

    public void setBitmap(String file) {
        //Crea un bitmap desde un path
        File imgFile = new  File(file);
        if(imgFile.exists()){
            this.img = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        } // Gestionar si no hay icono.jpg??
    }

    public void setBitmap(Bitmap bitmap) {
        this.img = bitmap;
    }

    public void setVibrator(Vibrator v) {
        this.v = v;
    }

    // Consultoras

    //TODO: Buscar gestión de la imagen para no hacer dos tipología distintas...
    public Bitmap getBitmap() {
        return this.img;
    }

    public String getImage() {
        return this.imagen;
    }

    public String getTitle() {
        return this.titulo;
    }

    public String getId() {
        return this.id;
    }

    public String getHash() {
        return this.hash;
    }

    public String getPoints() {
        return this.JsonPoints;
    }

    public String getDescription() {
        return this.descripcion;
    }

    public String getExcerpt() {
        return this.excerpt;
    }

    public String getIdioma() {
        return this.idioma;
    }

    public int getLayer() {
        return this.layer;
    }

    public boolean isUpdate() { //esta actualizado? false no está actualizado
        return this.update;
    }

    public boolean isDownload() { // esta descargado?
        return this.downlad;
    }

    public boolean hasImage() {
        if (this.imagen!="none") return true;
        if (this.img!=null) return true;
        else { return false; }
    }

    public String getPathImage() {
        return "/sdcard/Areago/"+this.id+"/icono.jpg";
    }

    // Acciones
    public void stop() {
        for (int i = 0; i<this.puntos.size(); i++) {
            this.puntos.get(i).stopSoundFile();
            Log.d("AREAGO","Parando punto "+i);
        }
        this.paused = true;
    }

    public void pause() {
        for (int i=0; i<this.puntos.size(); i++) {
            try {
                if (this.puntos.get(i).getType() != SoundPoint.TYPE_TOGGLE) this.puntos.get(i).pausePlaying();
            } catch (NullPointerException e) {
                Log.e("AREAGO","Error NullPointer al pausar el punto");
            }
            Log.d("AREAGO","Pausando punto "+i);
        }
        this.paused = true;

    }

    public void play(){
        this.paused = false;
    }

    public void location_pause() {
        // Solo  pausamos los puntos relacionados con el location
        for (int i=0; i<this.puntos.size(); i++) {
            try {
                if (this.puntos.get(i).getType() != SoundPoint.TYPE_WIFI_PLAY_LOOP || this.puntos.get(i).getType() != SoundPoint.TYPE_TOGGLE || this.puntos.get(i).getType() != SoundPoint.TYPE_PLAY_UNTIL ) this.puntos.get(i).pausePlaying();
            } catch (NullPointerException e) {
                Log.e("AREAGO","Error NullPointer al pausar el punto");
            }
            Log.d("AREAGO","Pausando punto "+i);
        }
    }

    public void create_points(String str) {
        // Crear los puntos a partir del Objeto Points del GeoJSON
        //

        JSONObject points;
        try {

            points = new JSONObject(str);
            //String type = points.getString("type"); no lo uso
            JSONArray features = points.getJSONArray("features"); // aquí van los puntos
            String _path = getPaseoPath(this.id);

            for (int i = 0; i<features.length();i++) {

                SoundPoint p = new SoundPoint(LocationManager.GPS_PROVIDER);

                // Cargamos cada uno de los obj con los puntos
                JSONObject jO = features.getJSONObject(i);
                JSONObject properties = jO.getJSONObject("properties");

                if (properties.has("type")) p.setType(properties.getInt("type")); // Dentro de properties
                if (properties.has("layer")) {p.setLayer(properties.getInt("layer")); } else {p.setLayer(0);} // por defecto en la 0
                if (properties.has("vibrate")) {p.setVibrate(this.v);} else {p.unsetVibrate();}


                if (p.getType()==SoundPoint.TYPE_TOGGLE) {
                    if (properties.has("tolayer")) {p.setChangeToLayer(properties.getInt("tolayer")); } else {p.setChangeToLayer(0);} // por defecto dirige a la 0
                    if (properties.has("essid")) p.setEssid(properties.getString("essid")); // Es posible que hagamos toogle a partir de los wifis..
                }

                //Checkeo del audio/fade/.. para tipos que no sean toggle
                if (p.getType()!=SoundPoint.TYPE_TOGGLE) {
                    if (properties.has("autofade")) {p.setAutofade(properties.getBoolean("autofade"));} else {p.setAutofade(true);} // por defecto le dejo activado el autofade..
                    if (properties.has("file")) p.setSoundFile(properties.getString("file"));
                    // TODO: Hacer una variable global en sharedPreferences para guarda el lugar de descarga..
                    p.setFolder(_path);
                }

                // Geometria solo cuando es tipo GPS o Toggle..
                if (p.getType()!=SoundPoint.TYPE_WIFI_PLAY_LOOP) {
                    JSONObject geometry = jO.getJSONObject("geometry");
                    JSONArray geo_coord = geometry.getJSONArray("coordinates");
                    if (!geo_coord.isNull(1)) p.setLatitude(geo_coord.getDouble(1));
                    if (!geo_coord.isNull(0)) p.setLongitude(geo_coord.getDouble(0));
                    if (geometry.has("radius")) p.setRadius((float) geometry.getDouble("radius"));
                }

                if (p.getType()==SoundPoint.TYPE_WIFI_PLAY_LOOP) {
                    if (properties.has("essid")) p.setEssid(properties.getString("essid")); // TODO: Será obligatorio tener ESSID?? aunque sea ""
                }

                this.puntos.add(p);
                Log.d("AREAGO", "Todos los puntos cargados");
            }

        } catch (JSONException e) {
            //e.printStackTrace();
            Log.d("AREAGO","Error al cargar los puntos del paseo: "+this.getTitle()+" @ "+str);
        }
    }

    public void addRefPoint(double lat, double lon) {
        //this.pref.setLatitude(lat);
        //this.pref.setLongitude(lon);
    }

    public void check_collisions(Location l) {
        // Recorre los puntos del mapa y revisa si estamos dentro del radio de uno de ellos
        String p = "";

        if (this.paused) return;

        for (int i = 0; i<this.puntos.size(); i++){
            int type = this.puntos.get(i).getType();
            int lay = this.puntos.get(i).getLayer();
            Log.d("AREAGO","[LAYER] " + this.layer);
            if ( (type == SoundPoint.TYPE_PLAY_LOOP) || (type == SoundPoint.TYPE_PLAY_ONCE) || (type == SoundPoint.TYPE_PLAY_UNTIL) || (type==SoundPoint.TYPE_TOGGLE) ) {
                if (checkLayer(lay)) {
                    int r = this.puntos.get(i).checkColision(l);
                    if (r>=-1) { this.layer = r; }
                } // se reproduce algun tipo de sonido
                else { this.puntos.get(i).stopSoundFile(); } // Debemos parar la reproducción de los archivos que ya no deberían estar sonando
            }
           // p = p + " | "+this.puntos.get(i).getFolder()+"/"+this.puntos.get(i).getSoundFile();
        }
        //Log.d("AREAGO",p);
    }

    public void check_collisions(List<ScanResult> wifis) {
        for (int i=0; i<this.puntos.size(); i++) {
            //Log.d("AREAGO","["+this.puntos.get(i).getEssid()+"]"+this.puntos.get(i).getLayer());
            int type = this.puntos.get(i).getType();
            int layer = this.puntos.get(i).getLayer();
            if (type==SoundPoint.TYPE_WIFI_PLAY_LOOP || type==SoundPoint.TYPE_TOGGLE) {
                if (checkLayer(layer)) {
                    //Log.d("AREAGO","[En el layer actual]["+this.puntos.get(i).getEssid()+"]"+this.puntos.get(i).getLayer());
                    if (this.puntos.get(i).hasEssid()) {
                        int r = this.puntos.get(i).checkColision(wifis);
                        Log.d("AREAGO","["+this.puntos.get(i).getEssid()+"] La layer de respuesta es: "+r);
                        if (r>=-1) { this.layer = r;}
                    }
                }
                else {this.puntos.get(i).stopSoundFile();}
            }
        }
    }

    public boolean checkLayer(int l){
        // Si es la capa actual o es -1 se ejecuta
        if (l==this.layer || l==-1) return true;
        return false;
    }

    public boolean exist(HashMap<Integer,Paseo> walks) {
        if (walks.get(this.id) != null) { // ya está mapeado un paseo con ese ID
            Paseo p = (Paseo) walks.get(this.id);
            if (this.getHash() != "") { // si tiene hash...
                if (!p.getHash().equals(this.getHash())) { // se debe actualizar
                    p.setOutdated(); // marcamos como no actualizado
                  //  walks.put(this.getId(), p); // actualizamos el paseo en el hash
                }

            }
            return true;
        } else { return false; }
    }

    public String loadJSONFile(){
        String path = getPaseoPath(this.id);
        if (path == null) return null;

        File fold = new File(getPaseoPath(this.id));
        if (!fold.isDirectory()) return null;

        File d = new File(path + '/' + "info.json");
        if (!d.exists()) return null;

        Writer writer = new StringWriter();
        try {
            char[] buffer = new char[1024];
            Reader reader = new BufferedReader(new FileReader(d));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer,0,n);
            }
            reader.close();

        } catch (Exception e) {
            Log.d("AREAGO","Error: "+e);
            return null;
        }
        String JSONString = "";
        JSONString = writer.toString();
        Log.d("AREAGO","JSON Info: " + JSONString);
        return JSONString;
    }

    private String getPaseoPath(String id){
        final Walking ac = wAc.get();
        if(ac != null){
            File externalDir = ac.getExternalFilesDir(null); // The external directory;
            String pathWalk = externalDir.getAbsolutePath() + "/" + id;
            return pathWalk;
        }
        return null;
    }


}