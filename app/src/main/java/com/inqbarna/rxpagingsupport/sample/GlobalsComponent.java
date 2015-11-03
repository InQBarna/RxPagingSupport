package com.inqbarna.rxpagingsupport.sample;

import javax.inject.Singleton;

import dagger.Component;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 3/11/15
 *
 */
@Singleton
@Component(modules = {DataModule.class})
public interface GlobalsComponent {
    DataConnection<DataItem> getDataConnection();
}
