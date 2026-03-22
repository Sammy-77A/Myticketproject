package com.myticket.desktop.util;

/**
 * Implemented by controllers that need to receive data during scene navigation.
 * SceneManager calls initData(data) after the FXML has been loaded and
 * the standard initialize() lifecycle has already occurred.
 */
public interface DataReceiver {
    void initData(Object data);
}
