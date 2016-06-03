package com.dnbitstudio.locassacodetest.api;

import com.dnbitstudio.locassacodetest.model.Condition;

import retrofit.Call;
import retrofit.http.GET;

public interface WeatherAPI
{
    @GET(" ")
    Call<Condition> getWeather();
}
