package com.markwal.sdl.healthcheck.config;

import org.apache.commons.lang3.text.StrLookup;

/**
 * Created by mvanderwal on 7/25/2016.
 */
public class EnvVarLookup extends StrLookup<Object> {

    @Override
    public String lookup(String key) {
        final String value = System.getenv(key);
        return value;
    }

}
