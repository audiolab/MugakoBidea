package com.audiolab.mugakobidea;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.ScanResult;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;

public class SoundPoint extends Location {

    private float radius;
    private String folder = "";
    private String soundFile = "test.wav";
    private int id;
    private Context mContext;
    private int type;
    private String SSID;
    private int layer=0;
    private int destLayer=0;
    // Volumen
    public float volume=1;
    public float vol;
    public float tVolume;
    public int increment=10;
    public int fadeTime=500; // medio segundo de fade
    public int old_status;

    //Vibraciones
    private boolean vibrate=false;
    private Vibrator vibrator;

    // TIPO DE PUNTOS TRADICIONALES
    public static final int TYPE_PLAY_ONCE=0; // reproduce una vez el audio mientras esté en el radio
    public static final int TYPE_PLAY_LOOP=1; // reproduce en loop mientras esté en el radio
    public static final int TYPE_PLAY_UNTIL=2;
    // TIPOS DE PUNTOS DE ACCIÓN
    public static final int TYPE_TOGGLE=6; // Play/Stop segun el estado anterior del audio a que hace referencia
    public static final int TYPE_PLAY_START=3; // Ejecuta un audio
    public static final int TYPE_PLAY_STOP=4; // Para un audio
    // TIPOS DE PUNTOS DE WIFI
    public static final int TYPE_WIFI_PLAY_LOOP=5;

    // ESTADOS DE REPRODUCCIÓN
    public static final int STATUS_PLAYING=0;
    public static final int STATUS_STOPPED=2;
    public static final int STATUS_PAUSED=3;
    public static final int STATUS_ACTIVATE=4;
    public static final int STATUS_DEACTIVATE=5;
    public static final int STATUS_CHANGING_VOLUME=10;

    // INFO DE MEDIA PLAYER
    private MediaPlayer mp;
    private int status=STATUS_STOPPED;
    private boolean played=false;	 // Cuando ya lo hemos reproducido y no queremos volver a reproducirlo

    private boolean salido = false;
    private boolean completado = false;
    public boolean autofade = false;

    public static float MIN_ACCURACY = (float) 20.0;
    public CountDownTimer CountDown;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEssid(String SSID) {
        this.SSID = SSID;
    }

    public String getEssid() {
        return this.SSID;
    }

    public boolean hasEssid() {
        if (this.SSID != null ) return true;
        return false;

    }

    public int getStatus(){
        return this.status;
    }

    public void setStatus(int status){
        this.status = status;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setAutofade(boolean af) {
        this.autofade = af;
    }

    public void setType(int type) {
        this.type = type;
        if (this.type == SoundPoint.TYPE_TOGGLE) this.status = SoundPoint.STATUS_DEACTIVATE; // lo iniciamos sin estar activado
        else if ( (this.type == SoundPoint.TYPE_PLAY_LOOP) || (this.type == SoundPoint.TYPE_PLAY_ONCE) || (this.type == SoundPoint.TYPE_PLAY_UNTIL) || (this.type == SoundPoint.TYPE_WIFI_PLAY_LOOP) ) { this.status = SoundPoint.STATUS_STOPPED; }
    }

    public int getType() {
        return this.type;
    }

    public void setVibrate(Vibrator v) {
        this.vibrator = v;
        this.vibrate = true;
    }

    public void unsetVibrate() {
        this.vibrate = false;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getSoundFile() {
        return soundFile;
    }

    public void setSoundFile(String soundFile) {
        this.soundFile = soundFile;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setChangeToLayer(int l) {
        this.destLayer = l;
    }

    public int getLayer() {
        return this.layer;
    }

    public int getLayerDestination() {
        return this.destLayer;
    }

    public boolean isExecuted() {
        if (this.status == SoundPoint.STATUS_DEACTIVATE) { return false; }
        else { return true; }
    }

    public SoundPoint(Location l) {
        super(l);
        // TODO Auto-generated constructor stub
    }

    public SoundPoint(String provider) {
        super(provider);
        // TODO Auto-generated constructor stub
    }

    public SoundPoint(Context mContext){
        this(LocationManager.GPS_PROVIDER);
        this.mContext=mContext;

    }

    // PRE: Solo puntos de locacalización de sonido
    // PRE: Los puntos de trigger asociacios a un radio geografico tb se gestionan por aquí
    // POST: Devuevle -1 si no hay cambio de capa / N si hay cambio a alguna capa nueva
    public int checkColision(Location l){


        if ( l.getAccuracy()>= (float) 20.0 )return -2;

        float distance=this.distanceTo(l);
        Log.d("AREAGO","[Distance]["+this.soundFile+"][Status:"+this.status+"]"+distance);

        if ( (distance<=this.radius) && ( l.getAccuracy() < SoundPoint.MIN_ACCURACY )){
            // Si la accuracy es inferior a 20 metros no hacemos gestión de los puntos por problemas de gestión con otros puntos
            // COLISIÓN DEL PUNTO
            Log.d("AREAGO","Colision["+this.soundFile+"]"+" Estado reproducc: "+this.status+" TYPE:"+this.type);

            // TIPO TOGGLE
            if (this.getType() == SoundPoint.TYPE_TOGGLE) {
                Log.d("AREAGO", "Cambio de Layer a : "+this.destLayer);
                return this.destLayer;
            }

            // TIPOS DE REPRODUCCIóN
            switch (this.status) {
                case SoundPoint.STATUS_PAUSED : case SoundPoint.STATUS_STOPPED :
                    switch (this.type){
                        case SoundPoint.TYPE_PLAY_ONCE:
                            if (!this.played) {
                                this.mediaPlay(l);
                                this.played=true;
                            }
                            break;
                        case SoundPoint.TYPE_PLAY_UNTIL: case SoundPoint.TYPE_PLAY_LOOP:
                            Log.d("AREAGO","Playing audio Loop");
                            this.mediaPlay(l);
                            break;
                    }
                    break;
                case SoundPoint.STATUS_PLAYING :
                    // Seguimos reproduciendo
                    if (this.autofade) changeVolume(calculateVolumen(l));
                    break;
            }
            // EN EL CASO QUE NO HAYA COLISIÓN EN ESE PUNTO
        } else {
            switch (status) {
                case SoundPoint.STATUS_STOPPED : case SoundPoint.STATUS_PAUSED :
                    // SIGUE TAL CUAL
                    break;
                case SoundPoint.STATUS_PLAYING :
                    // LO PARAMOS O LO DEJAMOS PAUSADO? No..
                    if (this.type!=SoundPoint.TYPE_PLAY_UNTIL) this.mediaStop(); // El play_until lo dejamos hasta que se finalice el audio
                    break;
            }
            this.played = false; // Marcamos el punto como no reproducido
        }
        return -2;
    }

    // PRE Solo puntos de sonidos en localizaciones WIFI
    public int checkColision(List<ScanResult> results) {
        // Revismaos puntos y si son de type WIFI comprobamos si alguno tiene el mismo ESSID
        // Deberíamos ver si el wifi de este punto está o no..

        for (ScanResult wifi : results) {
            // Si está aquí dentro debermos ejecutar el audio o cambiar el volumen

            Log.d("AREAGO", "[Level]["+wifi.SSID+"]"+wifi.level);
            if ( wifi.SSID.equals(this.SSID)) { // Estamos en el radio de acción del wifi
                if (this.type == SoundPoint.TYPE_WIFI_PLAY_LOOP) {
                    switch (this.status) {
                        case SoundPoint.STATUS_PAUSED :
                        case SoundPoint.STATUS_STOPPED :
                            this.mediaPlay(wifi);
                            break;
                        case SoundPoint.STATUS_PLAYING :
                            this.changeVolume(calculateVolumen(wifi));
                            break;
                    }
                } else if (this.type == SoundPoint.TYPE_TOGGLE) {
                    Log.d("AREAGO","wifi Cambio de capa"+this.destLayer);
                    return this.destLayer;
                }
                return -2; // no hay cambio de capa..
            }
        }
        // No hemos encontrado el ESSID en la lista de los wifis disponibles
        // Por lo tanto paramos si está ejecutado lo paramos o no hacemos nada
        switch(this.status){
            case SoundPoint.STATUS_PLAYING:
                this.mediaStop();
                break;
        }
        return -2; // no hay cambio de capa..
    }

    public void stopSoundFile() {
        if (this.status == STATUS_PLAYING || this.status == STATUS_PAUSED ) this.mediaStop();
    }

    private void mediaStop(){
        if (this.CountDown!=null) this.CountDown.cancel();
        this.mp.setOnCompletionListener(null);
        this.mp.stop();
        this.mp.release();
        this.mp = null;
        this.status=STATUS_STOPPED;
        Log.d("AREAGO","Stopping: "+this.getSoundFile());
    }

    private void pauseSoundFile() {
        if (this.CountDown!=null) this.CountDown.cancel();
        this.changeVolume(0);
        this.mp.pause();
        this.status=STATUS_PAUSED;
    }

    private void unpauseSoundFile() {
        this.mp.start();
        this.status=STATUS_PLAYING;
    }

    private void changeVolume(float dVolume) {
        Log.d("AREAGO","[Volumen] Cambio de volumen de "+this.volume+" A "+dVolume);
        fadeVolume(fadeTime,dVolume);
        this.volume=dVolume;
        Log.d("AREAGO","[Audio] Cambiando volumen a :"+this.volume);

    }

    private void mediaPlay(ScanResult wifi) {
        this.mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.volume=0;

        try {
            this.mp.setDataSource(this.folder + "/" + this.soundFile);
            this.mp.prepare();
            this.mp.setLooping(false);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.d("AREAGO","Iniciamos reprod co vol: "+this.volume);

        this.mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer arg0) {
                switch (type) {
                    case  SoundPoint.TYPE_WIFI_PLAY_LOOP:
                        if ( status == SoundPoint.STATUS_PLAYING ) {
                            Log.d("AREAGO","[WIFI] Se finalizó la reproducción de UN AUDIO LOOP: " + arg0.getAudioSessionId()+" con vol: "+volume+" status:"+status);
                            arg0.start();
                        } else {
                            Log.d("AREAGO","XXXXX [WIFI] Se finalizó la reproducción de UN AUDIO LOOP: " + arg0.getAudioSessionId()+" con vol: "+volume+" status:"+status);
                            arg0.release();
                        }
                        break;
                }
            }
        });

        this.status=SoundPoint.STATUS_PLAYING;
        this.mp.start();
        this.played=true;
        Log.d("AREAGO","Playing: "+this.folder + "/" + this.soundFile);
        this.changeVolume(calculateVolumen(wifi));
        Log.d("AREAGO","Despues reprod co vol: "+this.volume);
    }

    private float calculateVolumen(ScanResult wifi) {
        if (!this.autofade) {
            return 1;
        } else {
            // autoFADE
            // basandome en esto: http://stackoverflow.com/questions/8704186/android-scanresult-level-value
            float x = (float) (90+wifi.level);
            if (x<-1) x=0;
            if (x>90) x=90;
            volume = x/90;
            if (volume > 1.0) volume = (float) 1;
            if (volume < 0) volume = (float) 0;
            Log.d("AREAGO","Gestionamos el volumen a: " + volume);
            return volume;
        }
    }

    private void mediaPlay(Location l){
        this.mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.volume=0;

        Log.d("AREAGO","Playing: "+this.folder + "/" + this.soundFile);

        try {
            this.mp.setDataSource(this.folder + "/" + this.soundFile);
            this.mp.prepare();
            this.mp.setLooping(false);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer arg0) {
                if (status==SoundPoint.STATUS_PLAYING) {
                    switch (type) {
                        case SoundPoint.TYPE_PLAY_ONCE : // lo mismo que en UNTIL
                        case SoundPoint.TYPE_PLAY_UNTIL :
                            Log.d("AREAGO","Se finalizó la reproducción de: " + arg0.getAudioSessionId());
                            arg0.release();
                            status = SoundPoint.STATUS_STOPPED;
                            break;
                        case SoundPoint.TYPE_PLAY_LOOP:
                            Log.d("AREAGO","Se finalizó la reproducción de UN AUDIO LOOP: " + arg0.getAudioSessionId());
                            arg0.start();
                            status = SoundPoint.STATUS_PLAYING;
                            break;
                    }
                } else {
                    arg0.release();
                    status = SoundPoint.STATUS_STOPPED;
                }
            }
        });



        this.status=SoundPoint.STATUS_PLAYING;
        this.mp.start();
        changeVolume(calculateVolumen(l));


    }

    private float calculateVolumen(Location l) {
        // Gestión de volumen!
        // autoFADE disabled
        if (!this.autofade) {
            //this.mp.setVolume(1.0f, 1.0f);
            return 1;
        } else {
            // autoFADE
            float volume = (float) 1.0 - (float) this.distanceTo(l)/this.radius;
            if (volume > 1.0) volume = (float) 1.0;
            if (volume < 0) volume = (float) 0.0;
            return volume;
        }

    }

    public void prepareSoundFile(){
        this.mp = new MediaPlayer();

        try {
            this.mp.setDataSource(this.folder + "/" + this.soundFile);
            this.mp.prepareAsync();
            this.mp.setLooping(false);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void pausePlaying(){
        try {
            if (this.mp != null && this.status==SoundPoint.STATUS_PLAYING) {
                if (this.mp.isPlaying()) {
                    this.status=SoundPoint.STATUS_PAUSED;
                    this.mp.pause();
                }
            }
        } catch (IllegalStateException e) {
            Log.e("AREAGO","Error IllegalState al pausar. ");
            e.printStackTrace();
        } catch (NullPointerException ne) {
            Log.e("AREAGO","Error NullPointer al pausar. ");
            ne.printStackTrace();
        }
    }

    // De un valor V (el actual en this.volume) a un valor
    //TODO: testear distintos tiempos para ver la reacción del audio..
    //TODO: Gestionar la muerte del proceso en el stop del activity!!!
    public void fadeVolume(int duration,float dVolume)
    {
        Log.d("AREAGO","STATUS: "+this.status);
        this.old_status = this.status;
        vol = dVolume; // volumen final
        tVolume = this.volume; // volumen inicial
        float rVolume = vol - tVolume;
        float steps = (float) duration/increment; // numero de veces que hace el cambio en tantos milisegundos
        final float vIncrement = rVolume/steps; //sera positivo si es FadeIN o negativo si FadeOut

        CountDown = new CountDownTimer(duration, increment)
        {
            public void onFinish()
            {
                try {
                    if (mp.isPlaying()) mp.setVolume(vol, vol);
                } catch (IllegalStateException e) {
                    try {
                        mp.stop();
                        mp.release();
                        mp = null;
                        cancel();
                    } catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                } catch (NullPointerException io) {
                    io.printStackTrace();
                }
                status=old_status;
            }
            public void onTick(long millisUntilFinished)
            {
                tVolume += vIncrement;
                try {
                    if (mp != null) {
                        if (mp.isPlaying()) {
                            mp.setVolume(tVolume, tVolume);
                        }
                    }
                } catch (IllegalStateException e) {
                    try {
                        if (mp != null) {
                            mp.stop();
                            mp.release();
                            mp = null;
                        }
                        cancel();
                    } catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                } catch (NullPointerException nex) {
                    nex.printStackTrace();
                }
            }
        };
        CountDown.start();
    }

}