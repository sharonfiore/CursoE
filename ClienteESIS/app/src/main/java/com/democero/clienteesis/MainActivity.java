package com.democero.clienteesis;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    LatLng pos;
    GoogleMap mapa;

    Socket mSocket;

    MediaPlayer media;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        mSocket = App.getSocket();
        mSocket.on("taxiencontrado", taxiencontrado);
        mSocket.on("localizacion",localizacion);
        mSocket.on("Abordo",abordo);
        mSocket.on("taxicerca",taxicerca);
        mSocket.connect();
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        pos = new LatLng(-18.011737, -70.253529);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(pos,17));
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mapa.setMyLocationEnabled(true);
            mapa.getUiSettings().setZoomControlsEnabled(false);
            mapa.getUiSettings().setCompassEnabled(true);
        }
    }

    public void pedir(View view) {
        if (mapa.getMyLocation() != null) {
            JSONObject miubicacion = new JSONObject();
            try {
                miubicacion.put("latitude", mapa.getMyLocation().getLatitude());
                miubicacion.put("longitude", mapa.getMyLocation().getLongitude());
            } catch (JSONException e) {
                Log.e("JSONExceptionPresenter", e.toString());
            }
            mSocket.emit("pedirtaxi", miubicacion, new Ack() {
                @Override
                public void call(Object... args) {
                    String res = (String) args[0];
                    if (res.equals("OK")) Log.i("mimensaje", "Se envio correctamente");
                    else Log.i("mimensaje", "Hubo error en el envio");
                }
            });
        }
        else
            Toast.makeText(this,"no se ha encontrado su ubicación", Toast.LENGTH_SHORT).show();
    }

    String miconductor="";
    private Emitter.Listener taxiencontrado = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("misdatos","taxiencontrado");
            final JSONObject paramsRequest = (JSONObject) args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i("misdatos","check:"+paramsRequest);
                        miconductor = paramsRequest.getString("datotaxi");
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Felicidades el señor " + miconductor + " atendera su pedido")
                                .setTitle("Conductor encontrado")
                                .setCancelable(false)
                                .setNeutralButton("Aceptar",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } catch(
                            JSONException e)
                    {
                        Log.e("JSONException", e.toString());
                    }
                }
            });
        }
    };



    Marker mimarker;
    private Emitter.Listener localizacion = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("localizacion","nuevalocalizacion");
            JSONObject paramsRequest = (JSONObject) args[0];
            final Double latcond,loncond;
            try {
                Log.i("localizacion","nuevalocalizacion:"+paramsRequest.toString());
                latcond = paramsRequest.getDouble("lat");
                loncond = paramsRequest.getDouble("lon");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mimarker!=null)
                        {
                            mimarker.remove();
                        }
                        mimarker= mapa.addMarker(new MarkerOptions().position(new LatLng(latcond, loncond))
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)).title(miconductor));
                    }
                });
            } catch (JSONException e) {
                Log.e("JSONException", e.toString());
            }
        }
    };

    /**************************************************************/

    private Emitter.Listener taxicerca = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            media = MediaPlayer.create(MainActivity.this, R.raw.tonohuawei);

            Log.i("misdatos","taxicerca");
            final JSONObject paramsRequest = (JSONObject) args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.i("misdatos","check:"+paramsRequest);
                        miconductor = paramsRequest.getString("datotaxi");
                        media.start();
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("El señor " + miconductor + " se encuentra CERCA")
                                .setTitle("TaxiESIS")
                                .setCancelable(false)
                                .setNeutralButton("Aceptar",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } catch(
                            JSONException e)
                    {
                        Log.e("JSONException", e.toString());
                    }
                }
            });
        }
    };
    /***************************************************/

    private Emitter.Listener abordo = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Que tenga un buen viaje")
                            .setTitle("Gracias por su preferencia")
                            .setCancelable(false)
                            .setNeutralButton("Aceptar",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
        }
    };
}
