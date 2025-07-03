package com.may21.trobl._global.component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GlobalValues {
    private static final int MAIN_VERSION = 0;
    private static final int DEV_VERSION = 0;
    private static final int STAGE_VERSION = 2;


    public static final String USER_PROFILE_IMAGE_PATH = "public/thumbnails/users/";
    public static String getBEVersion() {
        return MAIN_VERSION + "." + DEV_VERSION + "." + STAGE_VERSION;
    }

    private static String cdnKey;

    @Getter private static String cdnUrl;

    private static void initUrl() {
        if (cdnKey != null) {
            cdnUrl = "https://" + cdnKey + "/";
        }
    }

    @Value("${CDN_LB_IP}")
    public void setCdUrl(String cdnKey) {
        GlobalValues.cdnKey = cdnKey;
        initUrl();
    }
}
