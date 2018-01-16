package com.automatic.sample;

import android.support.test.runner.AndroidJUnit4;
import com.automatic.android.sdk.Automatic;
import com.automatic.net.AutomaticClientPublic;
import com.automatic.net.responses.Device;
import com.automatic.net.responses.ResultSet;
import com.automatic.net.responses.Trip;
import com.automatic.net.responses.User;
import com.automatic.net.responses.Vehicle;
import java.io.IOException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import retrofit2.Response;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestApiTest extends BaseTestActivity {

    private static AutomaticClientPublic mAutomaticClientPublic;
    private static String mDeviceId;
    private static String mVehicleId;
    private static String mTripId;
    private static String mUserId;
    private static boolean mFirstTest = true;
    private static boolean mLastTest = false;

    public RestApiTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (mFirstTest) {
            login();
            assertEquals(true, Automatic.get().isLoggedIn());

            mAutomaticClientPublic = Automatic.get().restApi();
            assertNotNull(mAutomaticClientPublic);

            mFirstTest = false;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mLastTest) {
            super.tearDown();
        }
    }

    @Test
    public void test1GetDevices() throws IOException {
        Response<ResultSet<Device>> response = mAutomaticClientPublic.getDevices().execute();
        if (response.isSuccessful()) {
            ResultSet<Device> deviceResultSet = response.body();
            mDeviceId = deviceResultSet.results.get(0).id;
            assertNotNull(mDeviceId);
            assertEquals(false, mDeviceId.isEmpty());
        } else {
            fail();
        }
    }

    @Test
    public void test1GetMyVehicles() throws IOException {
        Response<ResultSet<Vehicle>> response = mAutomaticClientPublic.getMyVehicles().execute();
        if (response.isSuccessful()) {
            ResultSet<Vehicle> vehicleResultSet = response.body();
            mVehicleId = vehicleResultSet.results.get(0).id;
            assertNotNull(mVehicleId);
        } else {
            fail();
        }
    }

    @Test
    public void test1GetUser() throws IOException {
        Response<User> response = mAutomaticClientPublic.getUser().execute();
        if (response.isSuccessful()) {
            User user = response.body();
            mUserId = user.id;
            assertNotNull(mUserId);
        } else {
            fail();
        }
    }

    @Test
    public void test2GetDevice() throws IOException {
        assertNotNull(mDeviceId);
        assertEquals(false, mDeviceId.isEmpty());

        Response<Device> response = mAutomaticClientPublic.getDevice(mDeviceId).execute();
        if (response.isSuccessful()) {

        } else {
            fail();
        }
    }

    @Test
    public void test2GetTrips() throws IOException {
        Response<ResultSet<Trip>> response = mAutomaticClientPublic.getTrips(mVehicleId, 1, 10).execute();
        if (response.isSuccessful()) {
            ResultSet<Trip> tripResultSet = response.body();
            mTripId = tripResultSet.results.get(0).id;
            assertNotNull(mTripId);
        } else {
            fail();
        }
    }

    @Test
    public void test2GetTripsAfter() throws IOException {
        Response<ResultSet<Trip>> response = mAutomaticClientPublic.getTripsAfter(Util.oneWeekAgo(), mVehicleId, false, 1, 10).execute();
        if (response.isSuccessful()) {

        } else {
            fail();
        }
    }

    @Test
    public void test2GetTripsBefore() throws IOException {
        Response<ResultSet<Trip>> response = mAutomaticClientPublic.getTripsBefore(Util.oneWeekAgo(), mVehicleId, false, 1, 10).execute();
        if (response.isSuccessful()) {

        } else {
            fail();
        }
    }

    @Test
    public void test2GetTripsBetween() throws IOException {
        Response<ResultSet<Trip>> response = mAutomaticClientPublic.getTripsBetween(Util.twoWeekAgo(), Util.oneWeekAgo(), false, mVehicleId, 1, 10).execute();
        if (response.isSuccessful()) {

        } else {
            fail();
        }
    }

    @Test
    public void test2GetUserById() throws IOException {
        Response<User> response = mAutomaticClientPublic.getUser(mUserId).execute();
        if (response.isSuccessful()) {

        } else {
            fail();
        }
    }

    @Test
    public void test3GetTrip() throws IOException {
        assertNotNull(mTripId);
        assertEquals(false, mDeviceId.isEmpty());

        Response<Trip> response = mAutomaticClientPublic.getTrip(mTripId).execute();
        if (response.isSuccessful()) {
            assertNotNull(response.body());
        } else {
            fail();
        }

        mLastTest = true;
    }
}
