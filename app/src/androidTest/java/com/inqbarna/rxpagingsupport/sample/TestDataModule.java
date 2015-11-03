package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.RxDataConnection;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 */
public class TestDataModule extends DataModule {

    private RxDataConnection<DataItem> rxDataConnection;

    public TestDataModule(RxDataConnection<DataItem> rxDataConnection) {
        this.rxDataConnection = rxDataConnection;
    }


    @Override
    public RxDataConnection<DataItem> provideRxDataConnection() {
        return rxDataConnection;
    }

}
