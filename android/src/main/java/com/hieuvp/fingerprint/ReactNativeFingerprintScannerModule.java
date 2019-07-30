package com.hieuvp.fingerprint;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class ReactNativeFingerprintScannerModule extends ReactContextBaseJavaModule
        implements LifecycleEventListener, ActivityEventListener {
    public static final int MAX_AVAILABLE_TIMES = Integer.MAX_VALUE;
    private static final int DEVICE_CREDENTIAL_CONFIRMATION_CODE = 8819;

    private final ReactApplicationContext mReactContext;
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

    @ReactMethod
    public void isKeyguardSecure(final Promise promise) {
        KeyguardManager keyguardManager = (KeyguardManager) mReactContext.getSystemService(mReactContext.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()) {
            promise.resolve(true);
        } else {
            promise.reject("KeyguardNotSecure", "KeyguardNotSecure");
        }
    }

    @ReactMethod
    public void confirmDeviceCredential(final String title, final String description, final Promise promise) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
          promise.reject("E_ACTIVITY_DOES_NOT_EXIST", "Activity doesn't exist");
          return;
        }

        KeyguardManager keyguardManager = (KeyguardManager) mReactContext.getSystemService(mReactContext.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            this.pendingPromise = promise;
            Intent credentialConfirmationIntent = keyguardManager.createConfirmDeviceCredentialIntent(title, description);
            currentActivity.startActivityForResult(credentialConfirmationIntent, DEVICE_CREDENTIAL_CONFIRMATION_CODE);
        } else {
            promise.reject("KeyguardNotSecure", "KeyguardNotSecure");
        }
    }
}
