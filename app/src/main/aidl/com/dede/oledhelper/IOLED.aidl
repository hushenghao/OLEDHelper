// IOLED.aidl
package com.dede.oledhelper;

// Declare any non-default types here with import statements

interface IOLED {
    void show();
    void dismiss();
    boolean isShow();
    void toggle();
    float getAlpha();
    void updateAlpha(float alpha);
}
