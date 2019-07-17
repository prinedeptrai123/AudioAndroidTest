package com.connecteven.NativeModule;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.connecteven.CallBack.IFFTValue;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.connecteven.FFTRecorder.FFTRecorder;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class FFTRecorderModule extends ReactContextBaseJavaModule {

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";
    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";

    FFTRecorder myRecorder;
    private Callback mMeasureBuffer;

    private ReactContext reactContext;


    public FFTRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Log.d("check", "init1: ");
        myRecorder= new FFTRecorder();
        myRecorder.initRecorder();
        Log.d("check", "init3: ");
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
        constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @Override
    public String getName() {
        return "FFTRecorder";
    }

    @ReactMethod
    public void show(String message, int duration) {
        Toast.makeText(getReactApplicationContext(), message, duration).show();

        for(int i=0;i<10;i++){
            WritableMap params1 = Arguments.createMap();
            params1.putDouble("type", 1.2+i);
            sendEvent(reactContext, "qui", params1);
        }
    }

    @ReactMethod
    public void startRecording() {
        Toast.makeText(getReactApplicationContext(), "kfkk", Toast.LENGTH_LONG).show();

        IFFTValue ifftValue = new IFFTValue() {
            @Override
            public void sendSum(double value) {

                WritableMap params1 = Arguments.createMap();
                if(params1==null){
                    Log.d("check", "null: ");
                }else {
                    params1.putDouble("type", value);
                    sendEvent(reactContext, "qui", params1);
                }

                Log.d("check", value+"");

            }
        };

        if(myRecorder == null){
            Log.d("check", "fall to init: ");
            myRecorder= new FFTRecorder();
            myRecorder.initRecorder();
        }

        Log.d("check", "true to init: ");
        myRecorder.startRecording(ifftValue);
    }

    @ReactMethod
    public void show2(Promise promise) {
        promise.resolve("jkfsjfskfksjkfjskfjkskfjs");
    }

    @ReactMethod
    public void measureLayout(
            int tag,
            int ancestorTag,
            Callback successCallback) {
        try {
            measureLayout(tag, ancestorTag, mMeasureBuffer);

            successCallback.invoke(3, 3, 3, 3);
        } catch (IllegalViewOperationException e) {

        }
    }

    private void measureLayout(int tag, int ancestorTag) {

    }
}
