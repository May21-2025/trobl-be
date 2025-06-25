package com.may21.trobl._global.component;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class GlobalValues {
    @Getter
    private static final int MAIN_VERSION = 0;
    @Getter
    private static final int DEV_VERSION = 0;
    @Getter
    private static final int STAGE_VERSION = 2;


    public static String getBEVersion() {
        return MAIN_VERSION + "." + DEV_VERSION + "." + STAGE_VERSION;
    }


}
