package biz.rebirthble.geolocationpush;


import android.app.PendingIntent;
import android.content.Intent;
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

    private GoogleApiClient mGoogleApiClient;

    private GeofencingRequest mGeofenceRequest;

    @Override
    public void onMessageReceived(String from, Bundle data) {

        //ペイロードデータの取得
        if (data.containsKey("com.nifty.Data")) {
            try {
                JSONObject json = new JSONObject(data.getString("com.nifty.Data"));

                //Locationデータの取得
                NCMBObject point = new NCMBObject("Location");
                point.setObjectId(json.getString("location_id"));
                point.fetchObject();

                Log.d(TAG, "location name:" + point.getString("name"));

                //geofenceの作成
                createGeofenceRequest(point);

                //Google API Clientのビルドと接続
                connectGoogleApiClient();


            } catch (JSONException e) {
                //エラー処理
                Log.e(TAG, "error:" + e.getMessage());
            } catch (NCMBException e) {
                Log.e(TAG, "error:" + e.getMessage());
            }
        }

        //デフォルトの通知を実行する場合はsuper.onMessageReceivedを実行する
        //super.onMessageReceived(from, data);
    }


    protected synchronized void connectGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private void createGeofenceRequest(NCMBObject point) {

        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(point.getString("name"))
                .setCircularRegion(
                        point.getGeolocation("geo").getLatitude(),
                        point.getGeolocation("geo").getLongitude(),
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        mGeofenceRequest = builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {

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
                mGeofenceRequest,
                getGeofencePendingIntent()
        ).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed");
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
