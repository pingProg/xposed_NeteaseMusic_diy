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
        //HOOK的方法：public void next(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)

        final Class c = classLoader.loadClass("com.netease.cloudmusic.service.PlayService");
        XposedBridge.hookAllMethods(c, "next", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args.length != 3) {
                    LogQuick("因为参数数量不符合，所以跳过这个函数，避免多个next重载函数的重复触发");
                    return;
                }
                if(isPlayingFinished()){
                    isStopPlayNext = true;
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args.length != 3) {
                    LogQuick("因为参数数量不符合，所以跳过这个函数，避免多个next重载函数的重复触发");
                    return;
                }
                //触发public void next(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)
                if(isStopPlayNext){
                    isStopPlayNext = false;
                    stopPlay();
                }
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

    private boolean isPlayingFinished()  throws Throwable {
        int progress = getCurrentTimeMs();
        int duration = getDuration();
        LogQuick(String.format( "isPlayingFinished() : %d, %d", progress, duration));
        //进度的差值在deviation之间算作完成播放
        final int deviation = 1500;
        return getDuration() - getCurrentTimeMs() <= deviation;
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