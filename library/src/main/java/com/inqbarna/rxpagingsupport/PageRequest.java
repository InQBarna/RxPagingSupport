package com.inqbarna.rxpagingsupport;

/**
 * Created by david on 14/10/15.
 */
public class PageRequest {

    static PageRequest createFromPageAndSize(Type type, int page, int pageSize) {
        PageRequest request = new PageRequest();
        // first page is 0
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be smaller than 0");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be a positive number");
        }

        // for this to be true, pageSize must be same for all pages!
        request.type = type;
        request.offset = page * pageSize;
        request.size = pageSize;
        return request;
    }

    static PageRequest createFromOffsetEnd(Type type, int offset, int end) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }

        if (end <= offset) {
            throw new IllegalArgumentException("End must be greater than offset");
        }

        PageRequest request = new PageRequest();
        request.type = type;
        request.offset = offset;
        request.size = end - offset + 1;
        return request;
    }

    public int getPage() {
        return offset / size;
    }

    public enum Type {
        /** Just get contents from network for later request. */
        Prefetch,
        /** Full request, return first any available disk info, then retrieve from network for updates */
        Network,
        /** Request page only on local storage */
        Disk
    }

    private Type type;
    private int offset;
    private int size;



    private PageRequest() {
    }

    public Type getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public int getEnd() {
        return offset + size - 1;
    }
}
