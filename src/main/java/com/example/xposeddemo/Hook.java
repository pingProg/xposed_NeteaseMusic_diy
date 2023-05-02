package com.example.xposeddemo;

import android.app.AndroidAppHelper;
import android.media.AudioManager;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Hook implements IXposedHookLoadPackage {
    private ClassLoader classLoader = null;
    private static final long maxShortSongsDuration_ms = 3 * 60 * 1000;
    private static final int REP_INIT = -1;
    private static final int REP_FALSE = 0;
    private static final int REP_TRUE = 1;
    private static final int REP_WILL_REPLAY = 2;
    private static final int REP_REPLAYED = 3;
    private int replay = REP_INIT;
    private boolean toggle = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.netease.cloudmusic")) {
            return;
        }
        classLoader = loadPackageParam.classLoader;

        monitorClickFunction();

        testHandler_monitorPlayButton();

        playPrevHandler();

        playNextHandler();

        toggleModuleHandler();


    }

    public void playNextHandler() throws Throwable {
        // HOOK的方法：
        // [只在播放自然完成时调用] public void next()
        // [所有next逻辑都需要调用] public void next(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)

        final Class c = classLoader.loadClass("com.netease.cloudmusic.service.PlayService");
        XposedBridge.hookAllMethods(c, "next", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if(!toggle){
                    return;
                }

                //自然完成播放
                if (param.args.length == 0) {
                    LogQuick("next：自然完成播放");
                    LogQuick(String.format("++++ beforeHookedMethod : isReplay : %d", replay));
                    if (isShortSongs()) {
                        LogQuick(" ++++ beforeHookedMethod : isShortSongs");
                        // 第一次启动时的shortSongs默认重放
                        switch (replay) {
                            case REP_INIT: {
                                LogQuick(" ++++ beforeHookedMethod : first time start");
                                replay = REP_TRUE;
                                break;
                            }
                            case REP_FALSE: {
                                LogQuick(" ++++ beforeHookedMethod : set replay");
                                replay = REP_TRUE;
                                break;
                            }
                            case REP_TRUE:
                            case REP_REPLAYED:
                            case REP_WILL_REPLAY:
                            default:
                                LogQuick("!!!! WARNING : UNEXPECTED replay : " + replay);
                        }
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if(!toggle){
                    return;
                }

                boolean isNeedReplay = false;
                //自然完成播放
                if (param.args.length == 0) {
                    LogQuick(String.format("++++ afterHookedMethod : isReplay : %d", replay));
                    switch (replay) {
                        case REP_TRUE: {
                            LogQuick(" ++++ afterHookedMethod : replay");
                            replay = REP_WILL_REPLAY;
                            isNeedReplay = true;
                            break;
                        }
                        case REP_WILL_REPLAY:
                        case REP_FALSE:
                        case REP_INIT:
                        case REP_REPLAYED:
                        default: {

                            LogQuick(" ++++ afterHookedMethod : reset replay");
                            replay = REP_FALSE;
                            break;
                        }
                    }

                    if (isNeedReplay) {
                        KeyPrevious();
                    } else {
                        KeyPlayPause();
                    }
                //所有next逻辑
                } else {
                    LogQuick(String.format("++++ afterHookedMethod ARGS 3 : isReplay : %d", replay));
                    if (replay == REP_REPLAYED) {
                        LogQuick(" ++++ afterHookedMethod SKIP : reset replay");
                        replay = REP_FALSE;
                    }
//                    else if(replay == REP_FALSE){
//                        KeyPlayPause();
//                    }
                }
            }
        });
    }

    public void playPrevHandler() throws Throwable {
        final Class c = classLoader.loadClass("com.netease.cloudmusic.service.PlayService");
        XposedBridge.hookAllMethods(c, "prev", new XC_MethodHook() {
            // HOOK的方法：
            // [所有prev逻辑都需要调用] public void prev(boolean z, boolean z2, @Nullable MusicEndConfig musicEndConfig)
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if(!toggle){
                    return;
                }

                //点击上一曲
                if (param.args.length != 3) {
                    return;
                }

                LogQuick(String.format("++++ playPrevHandler beforeHookedMethod : isReplay : %d", replay));
                switch (replay) {
                    case REP_WILL_REPLAY: {
                        LogQuick("prev：REP_WILL_REPLAY change to REP_REPLAYED");
                        replay = REP_REPLAYED;
                        break;
                    }
                    case REP_REPLAYED:
                    case REP_FALSE:
                    case REP_INIT:
                    case REP_TRUE:
                    default: {
                        LogQuick("prev：reset replay");
                        replay = REP_FALSE;
                    }
                }
            }
        });
    }

    public void toggleModuleHandler() throws Throwable {
        XposedHelpers.findAndHookMethod("com.netease.cloudmusic.module.hint.view.PlayerShareView$e", classLoader, "onClick", android.view.View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                toggle = !toggle;
                resetData();
                Toast.makeText(AndroidAppHelper.currentApplication(), String.format("xposed模块功能：%s", toggle ? "开启" : "关闭"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetData() {
        replay = REP_INIT;
    }

    public void testHandler_monitorPlayButton() throws Throwable {
        XposedHelpers.findAndHookMethod("com.netease.cloudmusic.activity.p7", classLoader, "m7", android.view.View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                int ret = getCurrentTimeMs();
//                LogQuick(String.format("^^^^^^^ ret : %d", ret));
            }
        });
    }

    public void monitorClickFunction() throws Throwable {
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.view.View", classLoader), "performClick", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if(!toggle){
                    return;
                }

                Object listenerInfoObject = XposedHelpers.getObjectField(param.thisObject, "mListenerInfo");
                Object mOnClickListenerObject = XposedHelpers.getObjectField(listenerInfoObject, "mOnClickListener");
                String callbackType = mOnClickListenerObject.getClass().getName();
                LogQuick("---- ---- ---- ---- CLICK FUNCTION : " + callbackType);
            }
        });
    }

    private static void KeyPlayPause() throws Throwable {
        LogQuick("%%%% KeyPlayPause");
        Context context = (Context) AndroidAppHelper.currentApplication();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
    }
    private static void KeyPrevious() throws Throwable {
        LogQuick("%%%% KeyPrevious");
        Context context = (Context) AndroidAppHelper.currentApplication();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
    }
    private int getCurrentTime_ms(){
        Class c = XposedHelpers.findClass("com.netease.cloudmusic.service.PlayService", classLoader);
        return (int) XposedHelpers.callStaticMethod(c, "getCurrentTime");
    }

    private int getDuration_ms() throws Throwable {
        // NOTE : 使用反射，获取java的私有成员变量
        Class c = XposedHelpers.findClass("com.netease.cloudmusic.service.PlayService", classLoader);
        Object returnObjectOfInvoke = XposedHelpers.callStaticMethod(c, "getPlayingMusicInfo");
        Field fieldOfPrivateVariable = returnObjectOfInvoke.getClass().getDeclaredField("duration");
        fieldOfPrivateVariable.setAccessible(true);
        Object privateVariable = fieldOfPrivateVariable.get(returnObjectOfInvoke);
        return (int) privateVariable;
    }

//    private boolean isPlayFinished() throws Throwable {
//        int delta_ms = 2 * 1000;
//        return Math.abs(getDuration_ms() - getCurrentTime_ms()) <= delta_ms;
//    }

//    private long getCurrentMusicID() throws Throwable {
//        Class c = XposedHelpers.findClass("com.netease.cloudmusic.service.PlayService", classLoader);
//        Object returnObjectOfInvoke = XposedHelpers.callStaticMethod(c, "getPlayingMusicInfo");
//        Field fieldOfPrivateVariable = returnObjectOfInvoke.getClass().getDeclaredField("id");
//        fieldOfPrivateVariable.setAccessible(true);
//        Object privateVariable = fieldOfPrivateVariable.get(returnObjectOfInvoke);
//        return (long) privateVariable;
//    }

    private boolean isShortSongs() throws Throwable {
        return getDuration_ms() <= maxShortSongsDuration_ms;
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