package com.dnbitstudio.locassacodetest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.dnbitstudio.locassacodetest.api.WeatherAPI;
import com.dnbitstudio.locassacodetest.bus.BusProvider;
import com.dnbitstudio.locassacodetest.model.Condition;
import com.squareup.otto.Bus;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class FetchWeatherService extends IntentService
{
    private static final String ACTION_FETCH_WEATHER =
            "com.dnbitstudio.locassacodetest.action.FETCH_WEATHER";
    private static final String EXTRA_LATITUDE = "com.dnbitstudio.locassacodetest.extra.LATITUDE";
    private static final String EXTRA_LONGITUDE = "com.dnbitstudio.locassacodetest.extra.LONGITUDE";

    private Bus bus;

    /**
     * Starts this service to perform action Fetch Weather with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchWeather(Context context, double latitude, double longitude)
    {
        Intent intent = new Intent(context, FetchWeatherService.class);
        intent.setAction(ACTION_FETCH_WEATHER);
        intent.putExtra(EXTRA_LATITUDE, latitude);
        intent.putExtra(EXTRA_LONGITUDE, longitude);
        context.startService(intent);
    }

    public FetchWeatherService()
    {
        super("FetchWeatherService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent != null)
        {
            final String action = intent.getAction();
            if (ACTION_FETCH_WEATHER.equals(action))
            {
                double latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0);
                double longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0);
                String baseUrl = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from" +
                        "%20weather.forecast%20where%20woeid%20in%20(SELECT%20woeid%20FROM" +
                        "%20geo.placefinder%20WHERE%20text%3D%22" + latitude + "%2C%20" +
                        longitude + "%22%20and%20gflags%3D%22R%22)and%20u=%22c%22&format=json&" +
                        "diagnostics=false&env=store%3A%2F%2Fdatatables.org%2" +
                        "Falltableswithkeys&callback=";
                Log.i("location", baseUrl);

                handleActionFetchWeather(baseUrl);
            }
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        bus = BusProvider.getBus();
    }

    /**
     * Handle action Fetch Weather in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchWeather(String baseUrl)
    {
        // Create our custom gson object for Condition using our custom deserializer
        Gson gson =
                new GsonBuilder().registerTypeAdapter(Condition.class, new ConditionDeserializer())
                        .create();

        // Create the retrofit object using our custom gson
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        WeatherAPI weatherAPI = retrofit.create(WeatherAPI.class);

        Call<Condition> call = weatherAPI.getWeather();
        call.enqueue(new Callback<Condition>()
        {
            @Override
            public void onResponse(Response<Condition> response, Retrofit retrofit)
            {
                bus.post(response);
            }

            @Override
            public void onFailure(Throwable t)
            {
                Log.i("location", "failure" + t);
                Condition condition = new Condition();
                bus.post(condition);
            }
        });
    }
}