// ITVServiceAidlInterface.aidl
package com.kt.apps.media.mobile.ui.playback;

// Declare any non-default types here with import statements

interface ITVServiceAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void sendData(String aString);
    String getChannelListJson();
    String getChannelJson(String channelId);
    void writeJsonData(String jsonData);
}