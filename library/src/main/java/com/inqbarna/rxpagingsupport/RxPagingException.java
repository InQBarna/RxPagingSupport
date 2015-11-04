package com.inqbarna.rxpagingsupport;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 4/11/15
 */
public class RxPagingException extends Exception {
    private PageRequest request;

    public RxPagingException(String detailMessage, PageRequest request) {
        super(detailMessage);
        this.request = request;
    }

    public PageRequest getRequest() {
        return request;
    }
}
