package com.nymi.nymireferenceapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;
import com.nymi.api.NymiAdapter;
import com.nymi.api.NymiProvision;
import com.nymi.api.NymiDeviceInfo;
import com.nymi.api.NymiRandomNumber;
import com.nymi.api.NymiPublicKey;
import com.nymi.api.NymiSymmetricKey;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.BitSet;


public class MainActivity extends Activity {

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int LEDS_NUMBER = 5;

    private NymiAdapter mNymiAdapter;

    private AdapterProvisions mAdapterProvisions;
    private Switch mSwitchDiscovery;
    private ListView mListViewProvisions;

    private RadioButton mLeds[];

    private Button mButtonAccept;
    private Button mButtonReject;

    private static final String TOTP_SAMPLE_KEY = "48656c6c6f21deadbeef";
    private static final String NEA_NAME = "NymiExample"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwitchDiscovery = (Switch) findViewById(R.id.layout_main_discovery_switch);

        mLeds = new RadioButton[LEDS_NUMBER];
        mLeds[0] = (RadioButton) findViewById(R.id.layout_main_led0);
        mLeds[1] = (RadioButton) findViewById(R.id.layout_main_led1);
        mLeds[2] = (RadioButton) findViewById(R.id.layout_main_led2);
        mLeds[3] = (RadioButton) findViewById(R.id.layout_main_led3);
        mLeds[4] = (RadioButton) findViewById(R.id.layout_main_led4);

        mButtonAccept = (Button) findViewById(R.id.layout_main_button_accept);
        mButtonReject = (Button) findViewById(R.id.layout_main_button_reject);
        mListViewProvisions = (ListView) findViewById(R.id.layout_main_provision_list);

        mAdapterProvisions = new AdapterProvisions(this);

        /** Step 1 - Get instance of the Nymi Adapter*/
        mNymiAdapter = NymiAdapter.getDefaultAdapter();

        if (!mNymiAdapter.isInitialized()) {

            //NOTE: we cannot always use BuildConfig.LOCAL_IP to set the IP since the host can have 
            //a few network interfaces, and the interface could be bound to more than one IP address.
            // in typical dev environments, the nymulator will be run on the
            // same machine as your build machine, so this is a sensible default.
            // If that's not the case for you, update this host field.

            /** Step 2 - Set the Nymulator ip */
            String nymulatorHost = BuildConfig.LOCAL_IP;
            mNymiAdapter.setNymulator(nymulatorHost);

            /** Step 3 - Initialize the Nymi adapter */
            //Context supplied to NymiAdapter here will be preserved throughout the lifetime of the app 
            mNymiAdapter.init(ApplicationContextProvider.getContext(),
                    NEA_NAME, new NymiAdapter.InitCallback() {
                        @Override
                        public void onInitResult(int status, String message) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            if (status == NymiAdapter.InitCallback.INIT_SUCCESS) {

                                /** Step 4 - Get the provisions (Provisions are available only if the Adapter has been initialized) */
                                updateProvisions();

                                /** Step 5 - Set callbacks to receive agreement and new provision events */
                                setAreementAndProvisionCallbacks();

                                /** Step 6 - Start provisioning Nymi bands nearby*/
                                mNymiAdapter.startProvision();

                                //discovery enable state can be changed only if NymiAdapter has been initialized
                                mSwitchDiscovery.setEnabled(true);
                                
                            } else if (status == NymiAdapter.InitCallback.INIT_FAIL) {
                                Toast.makeText(MainActivity.this, "Init failed", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });

        } else {
            /** Step 4 - Get the provisions (Provisions are available only if the Adapter has been initialized) */
            updateProvisions();

            /** Step 5 - Set callbacks to receive agreement and new provision events */
            setAreementAndProvisionCallbacks();

            //discovery enable state can be changed only if NymiAdapter has been initialized
            mSwitchDiscovery.setEnabled(true);
        }

        /** Step 7 - Update the UI with the provisions */
        mListViewProvisions.setAdapter(mAdapterProvisions);

        /** Step 8 - Set notifications callbacks */
        setNotifications();

        /** Step 9 - Init UI  */
        mSwitchDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mSwitchDiscovery.setEnabled(false);
                mNymiAdapter.setDiscoveryEnabled(isChecked, new NymiAdapter.DiscoveryModeChangeCallback() {
                    @Override
                    public void onDiscoveryModeChange(int status) {
                        mSwitchDiscovery.setEnabled(true);
                        if (status == NYMI_DISCOVERY_CHANGE_ERROR) {
                            Toast.makeText(MainActivity.this, "Error changing discovery mode", Toast.LENGTH_LONG).show();
                        } else {
                            mSwitchDiscovery.setChecked(status == NYMI_DISCOVERY_ON);
                        }
                    }
                });
            }
        });

        mButtonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BitSet bitSet = new BitSet(LEDS_NUMBER);
                bitSet.clear();
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    if (mLeds[i].isChecked()) {
                        bitSet.set(i);
                    }
                    mLeds[i].setChecked(false);
                    mLeds[i].setEnabled(false);
                }

                // Calling setPattern is your application's way of saying
                // "Yes, I really want to provision with this band."
                // After setPattern, you'll get a callback with a NymiProvision
                // instance with which your application can interact.
                mNymiAdapter.setPattern(bitSet);
                mButtonAccept.setEnabled(false);
                mButtonReject.setEnabled(false);
            }
        });

        mButtonReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    mLeds[i].setChecked(false);
                    mLeds[i].setEnabled(false);
                }
                mButtonAccept.setEnabled(false);
                mButtonReject.setEnabled(false);
                mNymiAdapter.rejectAgreement();
                Toast.makeText(MainActivity.this, "Device rejected", Toast.LENGTH_SHORT).show();
            }
        });

        mListViewProvisions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.popup_menu_notify_positive:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).sendNotification(NymiProvision.Notification.NOTIFICATION_POSITIVE, new NymiProvision.NotificationCallback() {
                                    @Override
                                    public void onNotificationResult(int status, NymiProvision.Notification nymiNotification) {
                                        if (status == NymiProvision.NotificationCallback.NOTIFICATION_SUCCESS) {
                                            Toast.makeText(MainActivity.this, nymiNotification.toString() + " notification completed", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Notification failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_notify_negative:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).sendNotification(NymiProvision.Notification.NOTIFICATION_NEGATIVE, new NymiProvision.NotificationCallback() {
                                    @Override
                                    public void onNotificationResult(int status, NymiProvision.Notification nymiNotification) {
                                        if (status == NymiProvision.NotificationCallback.NOTIFICATION_SUCCESS) {
                                            Toast.makeText(MainActivity.this, nymiNotification.toString() + " notification completed", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Notification failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_get_random:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getRandom(new NymiProvision.RandomCallback() {
                                    @Override
                                    public void onRandomResult(int status, NymiRandomNumber nymiRandomNumber) {
                                        if (status == NymiProvision.RandomCallback.RANDOM_SUCCESS) {
                                            // Of course a real application will want to make use of this value otherwise,
                                            // but this is to demonstrate flow.
                                            Toast.makeText(MainActivity.this, "Obtained random: " + nymiRandomNumber.toString(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Random failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                break;
                            case R.id.popup_menu_sign:
                                if (null != mAdapterProvisions.getItem(position) &&
                                        null != ((NymiProvision) mAdapterProvisions.getItem(position)).getKeys() &&
                                        !((NymiProvision) mAdapterProvisions.getItem(position)).getKeys().isEmpty()) {
                                    ((NymiProvision) mAdapterProvisions.getItem(position)).sign("Message to be signed",
                                            new NymiProvision.SignCallback() {
                                                @Override
                                                public void onMessageSigned(int status, String algorithm, String signature, String key) {
                                                    if (status == NymiProvision.SignCallback.SIGN_LOCAL_SUCCESS) {
                                                        // Of course your code will want to make use of this value otherwise.
                                                        // This code intends to demonstrate flow.
                                                        Toast.makeText(MainActivity.this, "Sign (on a dummy message) returned: " + signature, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Sign failed", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                } else {
                                    Toast.makeText(MainActivity.this, "Error retrieving keys", Toast.LENGTH_SHORT).show();
                                }
                                break;

                            case R.id.popup_menu_pair_with_partner:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).addPartner(Constants.PARTNER_PUBLIC_KEY,
                                        new NymiProvision.PartnerAddedCallback() {
                                            @Override
                                            public void onPartnerAdded(int status, final NymiPublicKey key) {
                                                Toast.makeText(MainActivity.this, "partner added keyId=" + key.getId(), Toast.LENGTH_SHORT).show();
                                                new Thread() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            URL url = new URL("http://" + Constants.PARTNER_HOST + ":" + Constants.PARTNER_PORT +
                                                                    "/signup/" + key.getId() + "/" + key.getKey() + "/" + Constants.USER_ID + "/" + Constants.WIEGAND);

                                                            URLConnection urlConnection = url.openConnection();
                                                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                                            try {
                                                                BufferedReader reader = new BufferedReader(
                                                                        new InputStreamReader(in));

                                                                Gson gson = new Gson();
                                                                GServerSignupResponse response = gson.fromJson(reader, GServerSignupResponse.class);

                                                                reader.close();
                                                                if (response != null &&
                                                                        response.Ok != null &&
                                                                        response.Ok.equals("yes")) {
                                                                    Toast.makeText(MainActivity.this, "Registered user " + Constants.USER_ID, Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(MainActivity.this, "Failed to register user " + Constants.USER_ID, Toast.LENGTH_SHORT).show();
                                                                }
                                                            } finally {
                                                                in.close();
                                                            }
                                                        } catch (Exception e) {
                                                            Log.e(LOG_TAG, e.toString());
                                                        }
                                                    }
                                                }.start();
                                            }
                                        });
                                break;

                            case R.id.popup_menu_get_symmetric_key:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getSymmetricKey(new NymiProvision.SymmetricKeyCallback() {
                                    @Override
                                    public void onNymiSymmetricKeyResult(int status, NymiSymmetricKey nymiSymmetricKey) {
                                        if (status == SYMMETRIC_KEY_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Got symmetric key id: "
                                                    + nymiSymmetricKey.getId() + " value: " + nymiSymmetricKey.getKey(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Symmetric key failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_set_totp_key:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).setTotpKey(TOTP_SAMPLE_KEY, true, new NymiProvision.TotpSetKeyCallback() {
                                    @Override
                                    public void onTotpKeySet(int status) {
                                        if (status == TOTP_SET_KEY_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Set totp key succeeded", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Get totp failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_get_totp:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getTotp(new NymiProvision.TotpGetCallback() {
                                    @Override
                                    public void onTotpGet(int status, String totp) {
                                        if (status == TOTP_GET_SUCCESS) {
                                            Toast.makeText(MainActivity.this, "Got totp: " + totp, Toast.LENGTH_SHORT).show();
                                        }
                                        else if (status == TOTP_GET_REFUSED) {
                                            Toast.makeText(MainActivity.this, "Get totp failed. Have you set totp key?", Toast.LENGTH_LONG).show();
                                        }

                                        else {
                                            Toast.makeText(MainActivity.this, "Get totp failed", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });

                                break;
                            case R.id.popup_menu_device_info:
                                ((NymiProvision) mAdapterProvisions.getItem(position)).getDeviceInfo(
                                        new NymiProvision.DeviceInfoCallback() {
                                            @Override
                                            public void onDeviceInfo(int status, NymiDeviceInfo info) {
                                                if (status == DEVICE_INFO_SUCCESS) {
                                                    Toast.makeText(MainActivity.this, "Device presence state: "
                                                            + info.getPresenceState(), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Get device info failed", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                break;

                            default:
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /** Step 10 - Clear all callbacks: some might contain references to Views, this step is needed to avoid memory leaks */
        mNymiAdapter.clearCallbacks();
    }

    private void setNotifications() {
        mNymiAdapter.setDeviceApproachedCallback(new NymiAdapter.DeviceApproachedCallback() {
            @Override
            public void onDeviceApproached(String provisionId) {
                Log.d(LOG_TAG, "onDeviceApproached provisionId=" + provisionId);
            }
        });

        mNymiAdapter.setDeviceDetectedCallback(new NymiAdapter.DeviceDetectedCallback() {
            @Override
            public void onDeviceDetected(String provisionId, boolean connectable, int rssi, int smoothedRssi) {
                Log.d(LOG_TAG, "onDeviceDetected provisionId=" + provisionId +
                        " connectable=" + connectable +
                        " rssi=" + rssi +
                        " smoothedRssi=" + smoothedRssi);
            }
        });

        mNymiAdapter.setDeviceFoundCallback(new NymiAdapter.DeviceFoundCallback() {
            @Override
            public void onDeviceFound(String provisionId, boolean connectable, int rssi, int smoothedRssi, boolean strong) {
                Log.d(LOG_TAG, "onDeviceFound provisionId=" + provisionId +
                        " connectable=" + connectable +
                        " rssi=" + rssi +
                        " smoothedRssi=" + smoothedRssi +
                        " strong=" + strong);
            }
        });

        mNymiAdapter.setDeviceFoundStatusChangeCallback(new NymiAdapter.DeviceFoundStatusChangeCallback() {
            @Override
            public void onDeviceFoundStatusChange(String provisionId,
                                                  NymiAdapter.DeviceFoundStatusChangeCallback.FoundStatus before,
                                                  NymiAdapter.DeviceFoundStatusChangeCallback.FoundStatus after) {
                Log.d(LOG_TAG, "onDeviceFoundStatusChange provisionId=" + provisionId +
                        " before=" + before +
                        " after=" + after);
            }
        });

        mNymiAdapter.setDevicePresenceChangeCallback(new NymiAdapter.DevicePresenceChangeCallback() {
            @Override
            public void onDevicePresenceChange(String provisionId, NymiDeviceInfo.PresenceState before, NymiDeviceInfo.PresenceState after) {
                mAdapterProvisions.updateProvisionPresenceState(provisionId, after);
                Log.d(LOG_TAG, "onDevicePresenceChange provisionId=" + provisionId + " before=" + before +
                        " after=" + after);
            }
        });

        mNymiAdapter.setProximityEstimateChangeCallback(new NymiAdapter.ProximityEstimateChangeCallback() {
            @Override
            public void onDeviceProximityEstimateChange(String provisionId, NymiProvision.ProximityState before, NymiProvision.ProximityState after) {
                Log.d(LOG_TAG, "onDeviceProximityEstimateChange provisionId=" + provisionId + " before=" + before +
                        " after=" + after);
            }
        });

        mNymiAdapter.setFirmwareVersionCallback(new NymiAdapter.FirmwareVersionCallback() {
            @Override
            public void onFirmwareVersion(String provisionId, String fwVersion, int basicVersionCode, int imageCompatibilityCode, int nymiBandVersion) {
                Log.d(LOG_TAG, "onFirmwareVersion provisionId=" + provisionId +
                        " fwVersion=" + fwVersion +
                        " basicVersionCode=" + basicVersionCode +
                        " imageCompatibilityCode=" + imageCompatibilityCode +
                        " nymiBandVersio=" + nymiBandVersion);
            }
        });
    }

    private void setAreementAndProvisionCallbacks() {
        mNymiAdapter.setAgreementCallback(new NymiAdapter.AgreementCallback() {
            @Override
            public void onAgreement(BitSet pattern) {
                for (int i = 0; i < LEDS_NUMBER; i++) {
                    mLeds[i].setChecked(pattern.get(i));
                    mLeds[i].setEnabled(true);
                }
                mButtonAccept.setEnabled(true);
                mButtonReject.setEnabled(true);
            }
        });

        mNymiAdapter.setNewProvisionCallback(new NymiAdapter.NewProvisionCallback() {
            @Override
            public void onNewProvision(int status, NymiProvision provision) {
                if (status == NymiAdapter.NewProvisionCallback.PROVISION_SUCCESS) {
                    mAdapterProvisions.addProvision(provision);
                } else {
                    // Provisioning can fail due to connectivity problems.
                    // Unfortunately, your applications only recovery is to
                    // start the provisioning process over. You'll need to
                    // instruct the user to put their band back into provisioning mode.
                    Toast.makeText(MainActivity.this, "Error completing provision.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateProvisions() {
        mAdapterProvisions.setProvisions(NymiAdapter.getDefaultAdapter().getProvisions());

        //Get presence state of each provision to update UI
        for (final NymiProvision provision : NymiAdapter.getDefaultAdapter().getProvisions()) {
            provision.getDeviceInfo(new NymiProvision.DeviceInfoCallback() {
                @Override
                public void onDeviceInfo(int status, NymiDeviceInfo info) {
                    mAdapterProvisions.updateProvisionPresenceState(provision.getId(), info.getPresenceState());
                }
            });
        }
    }

    private class GServerSignupResponse {
        public String Ok;
    }
}