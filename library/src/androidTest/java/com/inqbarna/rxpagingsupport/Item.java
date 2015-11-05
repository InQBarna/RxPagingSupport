package com.inqbarna.rxpagingsupport;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 5/11/15
 */
public class Item {
    final int page;
    final int absIds;

    public Item(int page, int absIds) {
        this.page = page;
        this.absIds = absIds;
    }
}
