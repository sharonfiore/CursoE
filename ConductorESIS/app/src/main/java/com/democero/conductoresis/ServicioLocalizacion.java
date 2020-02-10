package com.democero.conductoresis;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.Socket;

public class ServicioLocalizacion extends Service {
    public static final String NOTIFICATION_CHANNEL_ID = "1000";
    public static final String NOTIFICATION_CHANNEL_NAME = "localizacion";
    private FusedLocationProviderClient mFusedLocationClient;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Socket mSocket;

    @Override public void onCreate() {
        mSocket = App.getSocket();
        if(!mSocket.connected())
            mSocket.connect();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        Log.i("milocalizacion","lat:"+wayLatitude+" lon:"+wayLongitude);
                        JSONObject misdatos = new JSONObject();
                        try {
                            misdatos.put("lat",wayLatitude);
                            misdatos.put("lon",wayLongitude);
                            misdatos.put("id",App.getidcliente());
                        } catch (JSONException e) {
                            Log.e("JSONExceptionPresenter", e.toString());
                        }
                        mSocket.emit("taxilocation", misdatos, new Ack() {
                            @Override
                            public void call(Object... args) {
                                String res = (String) args[0];
                                if (res.equals("OK")) Log.i("mimensaje", "Se envio correctamente");
                                else Log.i("mimensaje", "Hubo error en el envio");
                            }
                        });
                    }
                }

            }
        };
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    public int onStartCommand(Intent intenc, int flags, int idArranque) {
        Toast.makeText(this,"Servicio localizaciones arrancado ",
                Toast.LENGTH_SHORT).show();
        NotificationCompat.Builder notific = new NotificationCompat.Builder(this)
                .setContentTitle("Servicio de localizaciones")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("En un servicio");
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            NOTIFICATION_CHANNEL_ID,
                            NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
            notific.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        startForeground(101,notific.build()
        );
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intencion) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
