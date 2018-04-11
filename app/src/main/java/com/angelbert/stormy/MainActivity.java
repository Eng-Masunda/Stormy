package com.angelbert.stormy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
/*
* Stormy retrieves JSON weather data from Darksky weather forecast API and shows it on a simple UI.
* Location is hardcoded to Califonia.
* Json data is stored in CurrentWeather class(model)
* */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

   //CurrentWeather Object.
    private CurrentWeather mCurrentWeather;

    //Fields for our xml layout.
    private TextView mTimeLabel;
    private TextView mTemperatureLabel;
    private TextView mHumidityValue;
    private TextView mPrecipValue;
    private TextView mSummaryLabel;
    private ImageView mIconImageView;
    private ImageView mRefreshImageView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Linking layout views to our fields.
        mTimeLabel = (TextView)findViewById(R.id.timeLabel);
        mTemperatureLabel = (TextView)findViewById(R.id.temperatureLabel);
        mHumidityValue = (TextView)findViewById(R.id.humidityValue);
        mPrecipValue = (TextView)findViewById(R.id.precipValue);
        mSummaryLabel = (TextView)findViewById(R.id.summaryLabel);
        mIconImageView = (ImageView)findViewById(R.id.iconImageView);
        mRefreshImageView = (ImageView)findViewById(R.id.refreshImageView);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        final double latitude = 37.8267;
        final double longitude = -122.4233;

        //Hiding our spinning progressbar when our app launches.
        mProgressBar.setVisibility(View.INVISIBLE);

        //Refresh button with onClick listener to refresh data.
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getForecast(latitude, longitude);
            }
        });
        //Weather data updated on app launch.
        getForecast(latitude, longitude);

        Log.d(TAG, "Main thread executing here.");

    }
        //getForecst will give weather data after passing it coordinates.
    private void getForecast(double latitude, double longitude) {
        //API key from darksky.net
        String apiKey = "d2920b04e084e0fce455f6cc70338058";
        String forecastUrl = "https://api.darksky.net/forecast/" + apiKey + "/"+ latitude + "," + longitude;
        if(isNetworkAvailable()) {
            //Will show progress bar
            toggleRefresh();
            //Building weather data request.
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().
                    url(forecastUrl).
                    build();
            //Making a call to server. enque method will run call on seperate thread away from main UI thread.
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        }
                        else {
                            alertUserAboutError();
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                }
            }
        });
        }
        else {
            Toast.makeText(MainActivity.this, R.string.network_unavailable_message,Toast.LENGTH_LONG).show();
        }
    }
    //Will show progress bar in different network conditions.
    private void toggleRefresh() {

        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }

    }
    //Update layout with data
    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mTimeLabel.setText("At" + mCurrentWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());

        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);

    }
    //Method will fill up Current Weather object with data from CurrentWeather class.
    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException{
        JSONObject foreCast = new JSONObject(jsonData);
        String timezone = foreCast.getString("timezone");
        Log.i(TAG,"From Json " + timezone );

        JSONObject currently = foreCast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTimezone(timezone);

        Log.i(TAG, currentWeather.getFormattedTime());

        return currentWeather;
    }
    //Test for network
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return  isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }
}
