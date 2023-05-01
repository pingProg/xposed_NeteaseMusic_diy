package com.example.xposeddemo;

import android.app.AndroidAppHelper;
import android.media.AudioManager;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private ClassLoader classLoader = null;
    private boolean isStopPlayNext = false;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.netease.cloudmusic")) {
            return;
        }
        classLoader = loadPackageParam.classLoader;

        monitorClickFunction();

        testHandler_monitorPlayButton();

        playNextHandler();
    }

    public void playNextHandler() throws Throwable {
        // HOOK的方法：
        // [只在播放自然完成时调用] public void next()
        // [所有next逻辑都需要调用] public void next(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)

        final Class c = classLoader.loadClass("com.netease.cloudmusic.service.PlayService");
        XposedBridge.hookAllMethods(c, "next", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args.length != 0) {
                    return;
                }
                LogQuick("**** 播放完成");
                stopPlay();
            }
        });
    }

    public void testHandler_monitorPlayButton() throws Throwable {
        XposedHelpers.findAndHookMethod("com.netease.cloudmusic.activity.p7", classLoader, "m7", android.view.View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                int ret = getCurrentTimeMs(classLoader);
//                LogQuick(String.format("^^^^^^^ ret : %d", ret));
            }
        });
    }

    public void monitorClickFunction() throws Throwable {
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
    private int getCurrentTimeMs(){
        Class c = XposedHelpers.findClass("com.netease.cloudmusic.service.PlayService", classLoader);
        return (int) XposedHelpers.callStaticMethod(c, "getCurrentTime");
    }

    private int getDuration() throws Throwable {
        // NOTE : 使用反射，获取java的私有成员变量
        Class c = XposedHelpers.findClass("com.netease.cloudmusic.service.PlayService", classLoader);
        Object returnObjectOfInvoke = XposedHelpers.callStaticMethod(c, "getPlayingMusicInfo");
        Field fieldOfPrivateVariable = returnObjectOfInvoke.getClass().getDeclaredField("duration");
        fieldOfPrivateVariable.setAccessible(true);
        Object privateVariable = fieldOfPrivateVariable.get(returnObjectOfInvoke);
        return (int) privateVariable;
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