/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.network;

import static com.android.settings.network.MobilePlanPreferenceController.MANAGE_MOBILE_PLAN_DIALOG_ID;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.MobilePlanPreferenceController.MobilePlanPreferenceHost;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiPrimarySwitchPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceHost {

    private static final String TAG = "NetworkDashboardFrag";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (Utils.isProviderModelEnabled(getContext())) {
            return R.xml.network_provider_internet;
        } else {
            return R.xml.network_and_internet;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!Utils.isProviderModelEnabled(context)) {
            use(MultiNetworkHeaderController.class).init(getSettingsLifecycle());
        }
        use(AirplaneModePreferenceController.class).setFragment(this);
        getSettingsLifecycle().addObserver(use(AllInOneTetherPreferenceController.class));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        use(AllInOneTetherPreferenceController.class).initEnabler(getSettingsLifecycle());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), mMetricsFeatureProvider,
                this /* fragment */, this /* mobilePlanHost */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, MetricsFeatureProvider metricsFeatureProvider, Fragment fragment,
            MobilePlanPreferenceHost mobilePlanHost) {
        final MobilePlanPreferenceController mobilePlanPreferenceController =
                new MobilePlanPreferenceController(context, mobilePlanHost);
        final WifiPrimarySwitchPreferenceController wifiPreferenceController =
                Utils.isProviderModelEnabled(context)
                        ? null
                        : new WifiPrimarySwitchPreferenceController(
                                context,
                                metricsFeatureProvider);
        final InternetPreferenceController internetPreferenceController =
                Utils.isProviderModelEnabled(context)
                        ? new InternetPreferenceController(context, lifecycle)
                        : null;

        final VpnPreferenceController vpnPreferenceController =
                new VpnPreferenceController(context);
        final PrivateDnsPreferenceController privateDnsPreferenceController =
                new PrivateDnsPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(mobilePlanPreferenceController);
            if (wifiPreferenceController != null) {
                lifecycle.addObserver(wifiPreferenceController);
            }
            lifecycle.addObserver(vpnPreferenceController);
            lifecycle.addObserver(privateDnsPreferenceController);
        }

        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        controllers.add(new MobileNetworkSummaryController(context, lifecycle));
        controllers.add(new TetherPreferenceController(context, lifecycle));
        controllers.add(vpnPreferenceController);
        controllers.add(new ProxyPreferenceController(context));
        controllers.add(mobilePlanPreferenceController);
        if (wifiPreferenceController != null) {
            controllers.add(wifiPreferenceController);
        }
        if (internetPreferenceController != null) {
            controllers.add(internetPreferenceController);
        }
        controllers.add(privateDnsPreferenceController);
        if (Utils.isProviderModelEnabled(context)) {
            controllers.add(new NetworkProviderCallsSmsController(context, lifecycle));
        }
        ConnectivityCheckPreferenceController connectivityCheck =
                new ConnectivityCheckPreferenceController(context);
        controllers.add(connectivityCheck);
        return controllers;
    }

    @Override
    public void showMobilePlanMessageDialog() {
        showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Log.d(TAG, "onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                final MobilePlanPreferenceController controller =
                        use(MobilePlanPreferenceController.class);
                return new AlertDialog.Builder(getActivity())
                        .setMessage(controller.getMobilePlanDialogMessage())
                        .setCancelable(false)
                        .setPositiveButton(com.android.internal.R.string.ok,
                                (dialog, id) -> controller.setMobilePlanDialogMessage(null))
                        .create();
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (MANAGE_MOBILE_PLAN_DIALOG_ID == dialogId) {
            return SettingsEnums.DIALOG_MANAGE_MOBILE_PLAN;
        }
        return 0;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.network_and_internet) {

                @Override
                // TODO(b/167474581): Should remove this method when Provider Model finished.
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    if (Utils.isProviderModelEnabled(context)) {
                        final SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.network_provider_internet;
                        return Arrays.asList(sir);
                    }
                    return super.getXmlResourcesToIndex(context, enabled);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* metricsFeatureProvider */, null /* fragment */,
                            null /* mobilePlanHost */);
                }
            };
}
