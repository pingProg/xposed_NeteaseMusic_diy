package com.example.xposeddemo;

import static androidx.core.view.KeyEventDispatcher.dispatchKeyEvent;

import android.app.AndroidAppHelper;
import android.media.AudioManager;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals("com.netease.cloudmusic")){
            return;
        }
        monitorClickFunction(loadPackageParam);

        XposedHelpers.findAndHookMethod("com.netease.cloudmusic.activity.p7", loadPackageParam.classLoader, "A7", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                LogQuick("~~~~ BEFORE CLICK");
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                stopPlay();
                LogQuick("@@@@ AFTER CLICK");
            }
        });
    }

    public void monitorClickFunction(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        Class clazz = XposedHelpers.findClass("android.view.View", loadPackageParam.classLoader);
        XposedBridge.hookAllMethods(clazz, "performClick", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object listenerInfoObject = XposedHelpers.getObjectField(param.thisObject, "mListenerInfo");
                Object mOnClickListenerObject = XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener");
                String callbackType = mOnClickListenerObject.getClass().getName();
                LogQuick("CLICK FUNCTION : " + callbackType);
            }
        });
    }

    private void stopPlay() throws Throwable {
        LogQuick("%%%% stopPlay");
        Context context = (Context) AndroidAppHelper.currentApplication();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
    }

    private void LogQuick(String msg) throws Throwable {
        Log.d("XPOSED TEST", msg);
    }
}