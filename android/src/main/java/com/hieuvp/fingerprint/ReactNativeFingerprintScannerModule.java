package com.hieuvp.fingerprint;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.content.Intent;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.FingerprintIdentifyExceptionListener;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.FingerprintIdentifyListener;

public class ReactNativeFingerprintScannerModule extends ReactContextBaseJavaModule
        implements LifecycleEventListener, ActivityEventListener {
    public static final int MAX_AVAILABLE_TIMES = Integer.MAX_VALUE;
    private static final int DEVICE_CREDENTIAL_CONFIRMATION_CODE = 8819;

    private final ReactApplicationContext mReactContext;
    private FingerprintIdentify mFingerprintIdentify;
    private Promise pendingPromise = null;

    public ReactNativeFingerprintScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mReactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "ReactNativeFingerprintScanner";
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        this.release();
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_CREDENTIAL_CONFIRMATION_CODE && this.pendingPromise != null) {
            if (resultCode == Activity.RESULT_OK) {
                this.pendingPromise.resolve(true);
            } else {
                this.pendingPromise.reject("UserCancel", "UserCancel");
            }
            this.pendingPromise = null;
        }
    }

    private FingerprintIdentify getFingerprintIdentify() {
        if (mFingerprintIdentify != null) {
            return mFingerprintIdentify;
        }
        mReactContext.addLifecycleEventListener(this);
        mFingerprintIdentify = new FingerprintIdentify(getCurrentActivity(),
                new FingerprintIdentifyExceptionListener() {
                    @Override
                    public void onCatchException(Throwable exception) {
                        mReactContext.removeLifecycleEventListener(
                                ReactNativeFingerprintScannerModule.this);
                    }
                });
        return mFingerprintIdentify;
    }

    private String getErrorMessage() {
        if (!getFingerprintIdentify().isHardwareEnable()) {
            return "FingerprintScannerNotSupported";
        } else if (!getFingerprintIdentify().isRegisteredFingerprint()) {
            return "FingerprintScannerNotEnrolled";
        } else if (!getFingerprintIdentify().isFingerprintEnable()) {
            return "FingerprintScannerNotAvailable";
        }
        return null;
    }

    @ReactMethod
    public void authenticate(final Promise promise) {
        final String errorMessage = getErrorMessage();
        if (errorMessage != null) {
            promise.reject(errorMessage, errorMessage);
            ReactNativeFingerprintScannerModule.this.release();
            return;
        }

        getFingerprintIdentify().resumeIdentify();
        getFingerprintIdentify().startIdentify(MAX_AVAILABLE_TIMES, new FingerprintIdentifyListener() {
            @Override
            public void onSucceed() {
                promise.resolve(true);
                ReactNativeFingerprintScannerModule.this.release();
            }

            @Override
            public void onNotMatch(int availableTimes) {
                mReactContext.getJSModule(RCTDeviceEventEmitter.class)
                        .emit("FINGERPRINT_SCANNER_AUTHENTICATION", "AuthenticationNotMatch");
            }

            @Override
            public void onFailed(boolean isDeviceLocked) {
                if(isDeviceLocked){
                    promise.reject("AuthenticationFailed", "DeviceLocked");
                } else {
                    promise.reject("AuthenticationFailed", "AuthenticationFailed");
                }
                ReactNativeFingerprintScannerModule.this.release();
            }

            @Override
            public void onStartFailedByDeviceLocked() {
                // the first start failed because the device was locked temporarily
                promise.reject("AuthenticationFailed", "DeviceLocked");
            }
        });
    }

    @ReactMethod
    public void release() {
        getFingerprintIdentify().cancelIdentify();
        mFingerprintIdentify = null;
        mReactContext.removeLifecycleEventListener(this);
    }

    @ReactMethod
    public void isSensorAvailable(final Promise promise) {
        String errorMessage = getErrorMessage();
        if (errorMessage != null) {
            promise.reject(errorMessage, errorMessage);
        } else {
            promise.resolve("Fingerprint");
        }
    }

    @ReactMethod
    public void confirmDeviceCredential(final String title, final String description, final Promise promise) {
        KeyguardManager keyguardManager = (KeyguardManager) mReactContext.getSystemService(mReactContext.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            this.pendingPromise = promise;
            Intent credentialConfirmationIntent = keyguardManager.createConfirmDeviceCredentialIntent(title, description);
            mReactContext.startActivityForResult(credentialConfirmationIntent, DEVICE_CREDENTIAL_CONFIRMATION_CODE, Bundle.EMPTY);
        } else {
            promise.reject("KeyguardNotSecure", "KeyguardNotSecure");
        }
    }
}
