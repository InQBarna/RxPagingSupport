package com.inqbarna.rxpagingsupport.sample;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class DataItem {
    private int ownerPage;
    private int absIdx;

    private String showText;

    public DataItem(int ownerPage, int absIdx) {
        this.ownerPage = ownerPage;
        this.absIdx = absIdx;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Item of page: ").append(ownerPage)
                     .append(", with abs idx: ").append(absIdx);

        this.showText = stringBuilder.toString();
    }

    public int getOwnerPage() {
        return ownerPage;
    }

    public int getAbsIdx() {
        return absIdx;
    }

    public String getShowText() {
        return showText;
    }
}
