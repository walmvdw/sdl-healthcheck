package com.markwal.sdl.healthcheck.config;

import org.apache.commons.lang3.text.StrSubstitutor;

/**
 * Created by mvanderwal on 7/25/2016.
 */
public class EnvVarSubstitutor extends StrSubstitutor {

    public EnvVarSubstitutor() {
        super(new EnvVarLookup());
    }
}
