package com.inqbarna.rxpagingsupport;

/**
 * Created by david on 14/10/15.
 */
public class Settings {
    private boolean usePrefetch;

    public boolean isUsePrefetch() {
        return usePrefetch;
    }

    private Settings() {
        usePrefetch = true;
    }


    public static Builder builder() {
        return new Builder(new Settings());
    }

    public static class Builder {
        private Settings settings;
        private Builder(Settings settings) {
            this.settings = settings;
        }

        public Builder disablePrefetch() {
            settings.usePrefetch = false;
            return this;
        }
    }
}
