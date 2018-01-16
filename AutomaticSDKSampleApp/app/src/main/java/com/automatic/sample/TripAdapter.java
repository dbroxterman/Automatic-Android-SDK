package com.automatic.sample;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.automatic.net.responses.ResultSet;
import com.automatic.net.responses.Trip;
import java.util.List;

/**
 * Created by duncancarroll on 4/3/15.
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.BindingHolder> {

    private List<Trip> mTrips;

    public static class BindingHolder extends RecyclerView.ViewHolder {
        private ViewDataBinding binding;

        BindingHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }

        public ViewDataBinding getBinding() {
            return binding;
        }
    }

    public TripAdapter(ResultSet<Trip> trips) {
        mTrips = trips.results;
    }

    @Override
    public BindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new BindingHolder(v);
    }

    @Override
    public void onBindViewHolder(BindingHolder holder, int position) {
        final Trip trip = mTrips.get(position);
        holder.getBinding().setVariable(com.automatic.sample.BR.trip, trip);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mTrips.size();
    }

    public void updateTrips(List<Trip> trips) {
        this.mTrips = trips;
        notifyDataSetChanged();
    }
}
