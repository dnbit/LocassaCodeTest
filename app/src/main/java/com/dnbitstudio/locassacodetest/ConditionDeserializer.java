package com.dnbitstudio.locassacodetest;


import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import com.dnbitstudio.locassacodetest.model.Condition;

import java.lang.reflect.Type;

public class ConditionDeserializer implements JsonDeserializer<Condition> {
    @Override
    public Condition deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
            throws JsonParseException {
        // Get the "condition" element from the parsed JSON
        JsonElement condition = je.getAsJsonObject()
                .get("query")
                .getAsJsonObject()
                .get("results")
                .getAsJsonObject()
                .get("channel")
                .getAsJsonObject()
                .get("item")
                .getAsJsonObject()
                .get("condition");

        // Deserialize it. You use a new instance of Gson to avoid infinite recursion
        // to this deserializer
        return new Gson().fromJson(condition, Condition.class);

    }
}