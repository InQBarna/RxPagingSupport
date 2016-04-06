/*                                                                              
 *    Copyright 2015 InQBarna Kenkyuu Jo SL                                     
 *                                                                              
 *    Licensed under the Apache License, Version 2.0 (the "License");           
 *    you may not use this file except in compliance with the License.          
 *    You may obtain a copy of the License at                                   
 *                                                                              
 *        http://www.apache.org/licenses/LICENSE-2.0                            
 *                                                                              
 *    Unless required by applicable law or agreed to in writing, software       
 *    distributed under the License is distributed on an "AS IS" BASIS,         
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 *    See the License for the specific language governing permissions and       
 *    limitations under the License.                                            
 *                                                                              
 */
package com.inqbarna.rxpagingsupport;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class Settings {
    private static final int DEFAULT_PAGE_SPAN  = 5;
    public static final  int MAX_PAGE_SPAN      = 10;
    public static final  int MIN_PAGE_SPAN      = 3;
    private static final int DEFAULT_PAGE_SIZE  = 10;
    private static final int DEFAULT_FIRST_PAGE = 0;

    private int       pageSpan;
    private int       pageSize;
    private int       prefetchPages;
    private int       prefetchDistance;
    private boolean   fallbackToCacheAfterNetworkFail;
    private Scheduler deliveryScheduler;
    private Logger    logger;
    private int firstPageToRequest;

    public int getFirstPageToRequest() {
        return firstPageToRequest;
    }

    public interface Logger {
        void error(@NonNull String msg, @Nullable Throwable throwable);

        void info(@NonNull String msg, @Nullable Throwable throwable);

        void debug(@NonNull String msg, @Nullable Throwable throwable);
    }

    public int getPageSpan() {
        return pageSpan;
    }

    public int getPagesToPrefetch() {
        return prefetchPages;
    }

    public Scheduler getDeliveryScheduler() {
        return deliveryScheduler;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPrefetchDistance() {
        return prefetchDistance;
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
        prefetchPages = -1;
        fallbackToCacheAfterNetworkFail = true;
        prefetchDistance = -1;
        firstPageToRequest = DEFAULT_FIRST_PAGE;
    }


    public static Builder builder() {
        return new Builder(new Settings());
    }

    public Builder buildUpon() {
        return new Builder(this);
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

        public Builder prefetchPages(int pagesToPrefetch) {
            settings.prefetchPages = pagesToPrefetch;
            return this;
        }

        /** Set the scheduler page request will be done on. Default: Schedulers.io() */
        public Builder setDeliveryScheduler(Scheduler scheduler) {
            settings.deliveryScheduler = scheduler;
            return this;
        }

        public Builder setFirstPage(int firstPage) {
            settings.firstPageToRequest = Math.max(DEFAULT_FIRST_PAGE, firstPage);
            return this;
        }

        public Builder prefetchDistance(int numItems) {
            settings.prefetchDistance = numItems;
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

        if (-1 == prefetchPages) {
            prefetchPages = pageSpan;
        }

        final int MIN_PREFETCH = (int) ((double) pageSize * 0.2 + 0.5);
        prefetchDistance = Math.max(MIN_PREFETCH, prefetchDistance);

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
