package com.automatic.sample;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;
import com.automatic.android.sdk.Automatic;
import com.automatic.net.AutomaticClientPublic;
import com.automatic.net.responses.ResultSet;
import com.automatic.net.responses.Trip;
import com.automatic.net.responses.User;
import com.automatic.net.responses.Vehicle;
import com.automatic.sample.databinding.ActivityTripViewerBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripViewerActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private final static String TAG = TripViewerActivity.class.getSimpleName();

    private RecyclerView mTripList;
    private ObservableArrayList<Trip> mTrips = new ObservableArrayList<>();
    private TripAdapter tripAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private User mUser;
    private String mVehicleId;
    private AutomaticClientPublic mAutomaticRestApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityTripViewerBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_trip_viewer);

        binding.setTrips(mTrips);
        mTripList = binding.tripList;
        mTripList.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(this);

        mAutomaticRestApiClient = Automatic.get().restApi();
        testOtherCalls();
    }

    private void testOtherCalls() {
        mAutomaticRestApiClient.getUser().enqueue(new com.automatic.net.Callback<User>() {

            @Override
            public void success(User user) {
                mUser = user;
                Log.d("AutomaticRestApi", "getUser() Success!");
            }

            @Override
            public void failure(Call<User> call, Response<User> response, Throwable throwable) {
                Log.e("AutomaticRestApi", "getUser() Failed!");
                Toast.makeText(TripViewerActivity.this, "Couldn't get user!  Not getting trips.", Toast.LENGTH_LONG).show();
            }
        });

        mAutomaticRestApiClient.getMyVehicles().enqueue(new Callback<ResultSet<Vehicle>>() {
            @Override
            public void onResponse(Call<ResultSet<Vehicle>> call, Response<ResultSet<Vehicle>> response) {
                if (response.isSuccessful()) {
                    ResultSet<Vehicle> vehicles = response.body();
                    if ((vehicles != null) && (vehicles.results != null) && !vehicles.results.isEmpty()) {
                        mVehicleId = vehicles.results.get(0).id;
                        getTrips();

                        getVehicle(mVehicleId);
                    }
                } else {
                    Log.e("AutomaticRestApi", "getVehicles() Failed!");
                }
            }

            @Override
            public void onFailure(Call<ResultSet<Vehicle>> call, Throwable throwable) {
                Log.e("AutomaticRestApi", "getVehicles() Failed!");
            }
        });
    }

    @Override
    public void onRefresh() {
        getTrips();
    }

    private void getTrips() {
        if (mVehicleId != null) {
            mAutomaticRestApiClient.getTrips(mVehicleId, 1, 100).enqueue(new Callback<ResultSet<Trip>>() {
                @Override
                public void onResponse(Call<ResultSet<Trip>> call, Response<ResultSet<Trip>> response) {
                    if (response.isSuccessful()) {
                        ResultSet<Trip> trips = response.body();
                        Log.d(TAG, "Got " + trips.results.size() + " trips!");

                        mTrips.clear();
                        mTrips.addAll(trips.results);

                        if (tripAdapter == null) {
                            tripAdapter = new TripAdapter(trips);
                            mTripList.setAdapter(tripAdapter);
                        } else {
                            tripAdapter.updateTrips(trips.results);
                        }
                        if (trips.results.size() > 0) {
                            getTrip(trips.results.get(0).id);
                        }
                    } else {
                        Log.e(TAG, "Couldn't get trips with status " + response.code());
                    }

                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onFailure(Call<ResultSet<Trip>> call, Throwable throwable) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(TripViewerActivity.this, "Error fetching trips.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void getTrip(String tripId) {
        mAutomaticRestApiClient.getTrip(tripId).enqueue(new com.automatic.net.Callback<Trip>() {
            @Override
            public void success(Trip trip) {
                Log.d("AutomaticRestApi", "getTrip() Success!");
            }

            @Override
            public void failure(Call<Trip> call, Response<Trip> response, Throwable throwable) {
                Log.e("AutomaticRestApi", "getTrip() Failed!");
            }
        });
    }

    private void getVehicle(String vehicleId) {
        mAutomaticRestApiClient.getVehicle(vehicleId).enqueue(new com.automatic.net.Callback<Vehicle>() {
            @Override
            public void success(Vehicle vehicle) {
                Log.d("AutomaticRestApi", "getVehicle() Success!");
            }

            @Override
            public void failure(Call<Vehicle> call, Response<Vehicle> response, Throwable throwable) {
                Log.e("AutomaticRestApi", "getVehicle() Failed!");
            }
        });
    }
}
