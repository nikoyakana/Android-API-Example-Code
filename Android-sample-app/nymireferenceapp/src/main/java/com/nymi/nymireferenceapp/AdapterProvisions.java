package com.nymi.nymireferenceapp;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nymi.api.NymiProvision;
import com.nymi.api.NymiDeviceInfo;

import java.util.ArrayList;
import java.util.HashMap;

public class AdapterProvisions extends BaseAdapter {

    private final Activity mActivity;
    private ArrayList<NymiProvision> mDevices;
    private HashMap<String, NymiDeviceInfo.PresenceState> mPresenceStates;

    @SuppressWarnings("unchecked")
    public AdapterProvisions(Activity activity) {
        mActivity = activity;
        mDevices = new ArrayList();
        mPresenceStates = new HashMap<>();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.layout_provision_row, null);
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.provision = (TextView) rowView.findViewById(R.id.layout_provision_row_provision);
            viewHolder.mIvDevice = (ImageView) rowView.findViewById(R.id.layout_provision_row_img_device);
            viewHolder.mIvDeviceUnclasped = (ImageView) rowView.findViewById(R.id.layout_provision_row_img_device_unclasped);
            viewHolder.mIvDeviceMaybe = (ImageView) rowView.findViewById(R.id.layout_provision_row_img_device_maybe);
            rowView.setTag(viewHolder);
        }

        // fill data
        final ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.provision.setText(mDevices.get(position).getId());

        NymiDeviceInfo.PresenceState state = mPresenceStates.get(mDevices.get(position).getId());
        holder.mIvDevice.setVisibility(View.INVISIBLE);
        holder.mIvDeviceMaybe.setVisibility(View.INVISIBLE);
        holder.mIvDeviceUnclasped.setVisibility(View.INVISIBLE);
        if (state == null) {
            holder.mIvDeviceMaybe.setVisibility(View.VISIBLE);
        }
        else {
            if (state.equals(NymiDeviceInfo.PresenceState.DEVICE_PRESENCE_MAYBE)) {
                holder.mIvDeviceMaybe.setVisibility(View.VISIBLE);
            } else if (state.equals(NymiDeviceInfo.PresenceState.DEVICE_PRESENCE_UNLIKELY) || 
                    state.equals(NymiDeviceInfo.PresenceState.DEVICE_PRESENCE_NO)) {
                holder.mIvDeviceUnclasped.setVisibility(View.VISIBLE);
            } else {
                holder.mIvDevice.setVisibility(View.VISIBLE);
            }
        }

        return rowView;
    }

    public void setProvisions(ArrayList<NymiProvision> devices) {
        if (devices != null) {
            mDevices = devices;
            notifyDataSetInvalidated();
        }
    }

    public void addProvision(NymiProvision device) {
        mDevices.add(device);
        mPresenceStates.put(device.getId(), NymiDeviceInfo.PresenceState.DEVICE_PRESENCE_YES);
        notifyDataSetInvalidated();
    }

    public void updateProvisionPresenceState(String provisionId, NymiDeviceInfo.PresenceState state) {
        mPresenceStates.put(provisionId, state);
        notifyDataSetInvalidated();
    }

    static class ViewHolder {
        public TextView provision;
        public ImageView mIvDevice;
        public ImageView mIvDeviceUnclasped;
        public ImageView mIvDeviceMaybe;
    }
}
