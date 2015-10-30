package com.inqbarna.rxpagingsupport;

/**
 * Created by david on 14/10/15.
 */
public class PageRequest {
    public enum Type {
        /** Just get contents from network for later request. */
        Prefetch,
        /** Full request, return first any available disk info, then retrieve from network for updates */
        Network,
        /** Request page only on local storage */
        Disk
    }
}
