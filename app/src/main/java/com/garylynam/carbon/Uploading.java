package com.garylynam.carbon;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.garylynam.util.PostReq;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.control.TroubleCodesObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FindFuelTypeObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelConsumptionRateObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;


public class Uploading extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static String reg;
    GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    Location mLastLocation;
    BluetoothSocket socket = null;
    String url = "http://chiloutus.pythonanywhere.com/new/timestamp";
    JSONObject data = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildGoogleApiClient();
        selectBlueTooth();
        setContentView(R.layout.activity_uploading);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        reg = getIntent().getStringExtra("reg");


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

//            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));

            TextView textViewToChange = (TextView) findViewById(R.id.gpsbox);
            textViewToChange.setText(String.valueOf(mLastLocation.getLatitude() + "," + String.valueOf(mLastLocation.getLongitude())));


        }

    }

    public void selectBlueTooth() {

        ArrayList deviceStrs = new ArrayList<String>();
        final ArrayList devices = new ArrayList<String>();
        BluetoothDevice blueDevice;


        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {

        }
        Set pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (Object device : pairedDevices) {
                blueDevice = (BluetoothDevice) device;
                deviceStrs.add(blueDevice.getName() + "\n" + blueDevice.getAddress());
                devices.add(blueDevice.getAddress());
            }
        }

        // show list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                String deviceAddress = devices.get(position).toString();
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                updateTimerThread.run();


                //begin to upload data
                PostReq p = new PostReq(data);

                p.execute(url);
            }
        });
        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }






    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_uploading, container, false);
            return rootView;
        }
    }

    private Runnable updateTimerThread = new Runnable()
    {
        public void run()
        {
            //write here whaterver you want to repeat
            DelayHandler.postDelayed(1000);
            try {
                socket.connect();

                new EchoOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());

                new LineFeedOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());

                new TimeoutObdCommand(100).run(socket.getInputStream(), socket.getOutputStream());

                new SelectProtocolObdCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

                EngineRPMObdCommand engineRpmCommand = new EngineRPMObdCommand();
                SpeedObdCommand speedCommand = new SpeedObdCommand();
                FindFuelTypeObdCommand fuelTypeObdCommand = new FindFuelTypeObdCommand();
                FuelConsumptionRateObdCommand fuelConsumptionRateObdCommand = new FuelConsumptionRateObdCommand();
                FuelEconomyObdCommand fuelEconomyObdCommand = new FuelEconomyObdCommand();
                FuelLevelObdCommand fuelLevelObdCommand = new FuelLevelObdCommand();

                TroubleCodesObdCommand troubleCodesObdCommand = new TroubleCodesObdCommand();

                while (!Thread.currentThread().isInterrupted())
                {
                    engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                    speedCommand.run(socket.getInputStream(), socket.getOutputStream());
                    fuelTypeObdCommand.run(socket.getInputStream(), socket.getOutputStream());
                    fuelConsumptionRateObdCommand.run(socket.getInputStream(), socket.getOutputStream());
                    fuelEconomyObdCommand.run(socket.getInputStream(), socket.getOutputStream());
                    fuelLevelObdCommand.run(socket.getInputStream(), socket.getOutputStream());
                    troubleCodesObdCommand.run(socket.getInputStream(), socket.getOutputStream());

                    // TODO handle commands result

                }
            }
            catch (IOException e) {
                    e.printStackTrace();
                }
            catch (InterruptedException e){
                    e.printStackTrace();
            }

        }
    };
    private class GPSListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            String longitude = "Longitude: " + loc.getLongitude();
            String latitude = "Latitude: " + loc.getLatitude();

    /*----------to get City-Name from coordinates ------------- */
            String cityName = null;
            List<Address> addresses;
            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.getDefault());
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                if (addresses.size() > 0)
                    System.out.println(addresses.get(0).getLocality());
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String s = longitude + "\n" + latitude +
                    "\n\nMy Currrent City is: " + cityName;


        }


        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }


}
