package com.automatic.sample;

import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.AutomaticLoginCallbacks;
import com.automatic.android.sdk.LoginButton;
import com.automatic.android.sdk.SdkError;
import junit.framework.Assert;
import org.junit.runner.RunWith;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class BaseTestActivity extends ActivityInstrumentationTestCase2<TestActivity> {

    private boolean mFinish = false;

    public BaseTestActivity() {
        super(TestActivity.class);
    }

    public void login() {
        mFinish = false;
        Automatic.get().setLoginCallbackListener(new AutomaticLoginCallbacks() {
            @Override
            public void onLoginSuccess() {
                mFinish = true;
            }

            @Override
            public void onLoginFailure(SdkError sdkError) {
                Assert.fail(sdkError.toString());
            }
        });
        Automatic.get().loginWithAutomatic(getActivity());

        while (!mFinish) {
            Util.threadWait();
        }
    }

    public void login1() {
        mFinish = false;
        Automatic.get().loginWithAutomatic(getActivity(), new AutomaticLoginCallbacks() {
            @Override
            public void onLoginSuccess() {
                mFinish = true;
            }

            @Override
            public void onLoginFailure(SdkError sdkError) {
                Assert.fail(sdkError.toString());
            }
        });

        while (!mFinish) {
            Util.threadWait();
        }
    }

    public void loginWithButton() {
        Automatic.get().logout();
        mFinish = false;
        LoginButton loginButton = new LoginButton(getActivity());
        Automatic.get().addLoginButton(loginButton, getActivity(), new AutomaticLoginCallbacks() {
            @Override
            public void onLoginSuccess() {
                mFinish = true;
            }

            @Override
            public void onLoginFailure(SdkError sdkError) {
                Assert.fail(sdkError.toString());
            }
        });

        loginButton.performClick();

        while (!mFinish) {
            Util.threadWait();
        }
    }
}