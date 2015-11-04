package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * Created by david on 14/10/15.
 */
public class Settings {
    private static final int DEFAULT_PAGE_SPAN = 5;
    public static final  int MAX_PAGE_SPAN     = 10;
    public static final  int MIN_PAGE_SPAN     = 3;
    private static final int DEFAULT_PAGE_SIZE = 10;


    private int       pageSpan;
    private int       pageSize;
    private boolean   fallbackToCacheAfterNetworkFail;
    private Scheduler deliveryScheduler;
    private Logger    logger;

    public interface Logger {
        void error(@NonNull String msg, @Nullable Throwable throwable);

        void info(@NonNull String msg, @Nullable Throwable throwable);

        void debug(@NonNull String msg, @Nullable Throwable throwable);
    }

    public int getPageSpan() {
        return pageSpan;
    }

    public Scheduler getDeliveryScheduler() {
        return deliveryScheduler;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean hasCacheFallbackEnabled() {
        return fallbackToCacheAfterNetworkFail;
    }

    public Logger getLogger() {
        return logger;
    }

    private Settings() {
        pageSpan = DEFAULT_PAGE_SPAN;
        pageSize = DEFAULT_PAGE_SIZE;
        fallbackToCacheAfterNetworkFail = true;
    }


    public static Builder builder() {
        return new Builder(new Settings());
    }

    public static class Builder {
        private Settings settings;
        private Builder(Settings settings) {
            this.settings = settings;
        }

        public Builder setPageSpan(int span) {
            settings.pageSpan = Math.max(MIN_PAGE_SPAN, Math.min(span, MAX_PAGE_SPAN));
            return this;
        }

        public Builder disableFallbackToCacheOnNetworkFailure() {
            settings.fallbackToCacheAfterNetworkFail = false;
            return this;
        }

        public Builder setPageSize(int pageSize) {
            settings.pageSize = pageSize;
            return this;
        }

        /** Set the scheduler page request will be done on. Default: Schedulers.io() */
        public Builder setDeliveryScheduler(Scheduler scheduler) {
            settings.deliveryScheduler = scheduler;
            return this;
        }

        public Builder setLogger(Logger logger) {
            settings.logger = logger;
            return this;
        }

        public Settings build() {
            settings.initializeEmpty();
            return settings;
        }
    }

    private void initializeEmpty() {
        if (null == deliveryScheduler) {
            deliveryScheduler = Schedulers.io();
        }

        if (null == logger) {
            logger = new Logger() {
                @Override
                public void error(@NonNull String msg, @Nullable Throwable throwable) {
                    // no-op... default logging is not enabled
                }

                @Override
                public void info(@NonNull String msg, @Nullable Throwable throwable) {
                    // no-op... default logging is not enabled
                }

                @Override
                public void debug(@NonNull String msg, @Nullable Throwable throwable) {
                    // no-op... default logging is not enabled
                }
            };
        }
    }

}
