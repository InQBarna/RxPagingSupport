package com.inqbarna.rxpagingsupport.sample;

import com.inqbarna.rxpagingsupport.PageManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mock;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 1/12/15
 */
public class DataModuleRule implements TestRule {

    private DataModuleClient testClass;

    public DataModuleRule(DataModuleClient testClass) {
        this.testClass = testClass;
    }

    public interface DataModuleClient {

        void mockSetup();
        void regularSetup();
    }

    public @interface DataModuleMode {
        boolean useMock() default false;
    }

    @Override
    public Statement apply(Statement base, Description description) {

        DataModuleMode mode = description.getAnnotation(DataModuleMode.class);
        if (null == mode || !mode.useMock()) {
            testClass.regularSetup();
        } else {
            testClass.mockSetup();
        }

        return base;
    }
}
