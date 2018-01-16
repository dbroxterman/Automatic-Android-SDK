package com.automatic.sample;

import android.support.test.runner.AndroidJUnit4;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.exceptions.AutomaticSdkException;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.containsString;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class SdkNotInitializedTest extends BaseTestActivity {

    public SdkNotInitializedTest() {
        super();
    }

    @Test
    public void testSdkNotInit() {
        try {
            Automatic.get();
        } catch (AutomaticSdkException e) {
            assertThat(e.getMessage(), containsString("SDK has not been initialized"));
            return;
        }

        Assert.fail();
    }
}
