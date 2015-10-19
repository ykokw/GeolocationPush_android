package biz.rebirthble.geolocationpush;


import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBGcmListenerService;
import com.nifty.cloud.mb.core.NCMBObject;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomGcmListenerService extends NCMBGcmListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback
{

    protected static final String TAG = "CustomListenerService";

    protected static final int GEOFENCE_RADIUS_IN_METERS = 500;

    protected static final int GEOFENCE_EXPIRATION_IN_HOURS = 1;

    protected static final int GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    private PendingIntent mGeofencePendingIntent;

    private GoogleApiClient mGoogleApiClient;

    private NCMBObject geoFenceCenter;

    private GeofencingRequest.Builder requestBuilder;

    private boolean connectFlag;

    @Override
    public void onMessageReceived(String from, Bundle data) {

        //プッシュ通知受信時の挙動をカスタマイズ

        //ペイロードデータの取得
        if (data.containsKey("com.nifty.Data")) {

            try {
                JSONObject json = new JSONObject(data.getString("com.nifty.Data"));

                //Locationデータの取得
                NCMBObject point = new NCMBObject("Location");
                point.setObjectId(json.getString("location_id"));
                try {
                    point.fetchObject();
                } catch (NCMBException e) {
                    e.printStackTrace();
                }

                //geofenceの設定
                geoFenceCenter = point;

                Location centerPoint = geoFenceCenter.getGeolocation("geo");

                Geofence geofence = new Geofence.Builder()
                        // Set the request ID of the geofence. This is a string to identify this
                        // geofence.
                        .setRequestId(geoFenceCenter.getString("name"))

                        .setCircularRegion(
                                centerPoint.getLatitude(),
                                centerPoint.getLongitude(),
                                GEOFENCE_RADIUS_IN_METERS
                        )
                        .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build();

                GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                builder.addGeofence(geofence);
                requestBuilder = builder;

                Location geolocation = point.getGeolocation("geo");

                Log.d(TAG, "location name:" + point.getString("name"));
                Log.d(TAG, "lat:" + geolocation.getLatitude() + " lng:" + geolocation.getLongitude());


                buildGoogleApiClient();

                mGoogleApiClient.connect();

            } catch (JSONException e) {
                //エラー処理
                Log.e(TAG, "error:" + e.getMessage());
            }
        }

        //デフォルトの通知を実行する場合はsuper.onMessageReceivedを実行する
        //super.onMessageReceived(from, data);
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        connectFlag = false;
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connection Succeeded.");

        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                requestBuilder.build(),
                getGeofencePendingIntent()
        ).setResultCallback(this);

        connectFlag = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended");

        connectFlag = true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed");

        connectFlag = true;
    }

    @Override
    public void onResult(Result result) {
        Log.d(TAG, "onResult:" + result.toString());

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Gcm service is destroyed...");
        super.onDestroy();
    }
}
