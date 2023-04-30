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
        ClassLoader classLoader = loadPackageParam.classLoader;

        monitorClickFunction(classLoader);

        final Class c = classLoader.loadClass("com.netease.cloudmusic.service.PlayService");
        XposedBridge.hookAllMethods(c, "next", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if(param.args.length != 3){
                    LogQuick("因为参数数量不符合，所以跳过这个函数，避免多个next重载函数的重复触发");
                    return;
                }
                //触发public void next(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)
                LogQuick("触发next函数");
                stopPlay();
            }
        });
    }

    public static void monitorClickFunction(ClassLoader classLoader) throws Throwable {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.view.View", classLoader), "performClick", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Object listenerInfoObject = XposedHelpers.getObjectField(param.thisObject, "mListenerInfo");
                Object mOnClickListenerObject = XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener");
                String callbackType = mOnClickListenerObject.getClass().getName();
                LogQuick("---- ---- ---- ---- CLICK FUNCTION : " + callbackType);
            }
        });
    }

    private static void stopPlay() throws Throwable {
        LogQuick("%%%% stopPlay");
        Context context = (Context) AndroidAppHelper.currentApplication();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
    }

    private static void LogQuick(String msg) throws Throwable {
        Log.d("XPOSED TEST", msg);
        XposedBridge.log(msg);
    }

    private static void printStackTrace() throws Throwable {
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        LogQuick("{{{{ {{{{ {{{{ STACK TRACK");
        for (int i = 0; i < stackElements.length; i++) {
            StackTraceElement element = stackElements[i];
            LogQuick("at " + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        LogQuick("}}}} }}}} }}}} STACK TRACK");
    }
}