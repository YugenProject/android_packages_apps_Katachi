/*
 * Copyright (C) 2026 The Yugen Project
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

package org.yugen.katachi;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Entry point of Katachi, Yugen's personalization surface.
 *
 * <p>The page is split into sections: one for the Quick Settings panel and the Liquid Glass
 * treatment, one for the power menu. The header tiles show the current choice and scroll to the
 * matching section, so the page stays a single screen as more features land.
 *
 * <p>Every value is stored in {@link Settings.Secure} so SystemUI can observe it without needing
 * a broadcast.
 */
public class KatachiDashboard extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_HEADER = "katachi_header";

    private static final String KEY_QS_PANEL_STYLE = "qs_panel_style";
    private static final String KEY_QS_CONNECTIVITY_FOLDER = "qs_connectivity_folder";
    private static final String KEY_QS_ONEUI_EXPERIMENTAL = "qs_oneui_experimental";
    private static final String KEY_LIQUID_GLASS = "penguin_liquid_glass";
    private static final String KEY_LIQUID_GLASS_STYLE = "penguin_liquid_glass_style";
    private static final String KEY_GLASS_TAB_BARS = "penguin_glass_tab_bars";
    private static final String KEY_POWER_MENU_STYLE = "power_menu_style";

    private static final String KEY_QS_CATEGORY = "katachi_qs_category";
    private static final String KEY_POWER_MENU_CATEGORY = "katachi_power_menu_category";

    /** Matches {@code QsPanelStyle.Penguin}, the style Yugen ships by default. */
    private static final int QS_PANEL_STYLE_DEFAULT = 1;

    /** Matches the rimmed glass style. */
    private static final int LIQUID_GLASS_STYLE_DEFAULT = 0;

    /** Matches {@code PowerMenuStyle.YUGEN}, the layout Yugen ships by default. */
    private static final int POWER_MENU_STYLE_DEFAULT = 0;

    private static final String VERSION_PROPERTY = "ro.lineage.build.version";
    private static final String FALLBACK_VERSION_PROPERTY = "ro.lineage.version";
    private static final String CODENAME_PROPERTY = "ro.yugen.version.codename";
    private static final String VERSION_FALLBACK = "Yugen";

    private ListPreference mQsPanelStyle;
    private SwitchPreferenceCompat mQsConnectivityFolder;
    private SwitchPreferenceCompat mQsOneUiExperimental;
    private SwitchPreferenceCompat mLiquidGlass;
    private ListPreference mLiquidGlassStyle;
    private SwitchPreferenceCompat mGlassTabBars;
    private ListPreference mPowerMenuStyle;

    private TextView mQsTileSummary;
    private TextView mPowerMenuTileSummary;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SYSTEM_CATEGORY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.katachi_dashboard;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        bindHeader();

        mQsPanelStyle = findPreference(KEY_QS_PANEL_STYLE);
        mQsConnectivityFolder = findPreference(KEY_QS_CONNECTIVITY_FOLDER);
        mQsOneUiExperimental = findPreference(KEY_QS_ONEUI_EXPERIMENTAL);
        mLiquidGlass = findPreference(KEY_LIQUID_GLASS);
        mLiquidGlassStyle = findPreference(KEY_LIQUID_GLASS_STYLE);
        mGlassTabBars = findPreference(KEY_GLASS_TAB_BARS);
        mPowerMenuStyle = findPreference(KEY_POWER_MENU_STYLE);

        mQsPanelStyle.setOnPreferenceChangeListener(this);
        mQsConnectivityFolder.setOnPreferenceChangeListener(this);
        mQsOneUiExperimental.setOnPreferenceChangeListener(this);
        mLiquidGlass.setOnPreferenceChangeListener(this);
        mLiquidGlassStyle.setOnPreferenceChangeListener(this);
        mGlassTabBars.setOnPreferenceChangeListener(this);
        mPowerMenuStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void bindHeader() {
        final Preference header = findPreference(KEY_HEADER);
        if (!(header instanceof LayoutPreference)) {
            return;
        }
        final LayoutPreference layoutHeader = (LayoutPreference) header;

        final TextView version = layoutHeader.findViewById(R.id.katachi_version);
        if (version != null) {
            version.setText(buildVersion());
        }

        mQsTileSummary = layoutHeader.findViewById(R.id.katachi_qs_tile_summary);
        mPowerMenuTileSummary = layoutHeader.findViewById(R.id.katachi_power_tile_summary);

        final View qsTile = layoutHeader.findViewById(R.id.katachi_qs_tile);
        if (qsTile != null) {
            qsTile.setOnClickListener(v -> scrollToPreference(KEY_QS_CATEGORY));
        }

        final View powerTile = layoutHeader.findViewById(R.id.katachi_power_tile);
        if (powerTile != null) {
            powerTile.setOnClickListener(v -> scrollToPreference(KEY_POWER_MENU_CATEGORY));
        }
    }

    private void updateState() {
        final Context context = requireContext();
        final ContentResolver resolver = context.getContentResolver();

        final int panelStyle = Settings.Secure.getIntForUser(
                resolver, KEY_QS_PANEL_STYLE, QS_PANEL_STYLE_DEFAULT, UserHandle.USER_CURRENT);
        final String panelStyleValue = sanitize(mQsPanelStyle, String.valueOf(panelStyle),
                String.valueOf(QS_PANEL_STYLE_DEFAULT));
        mQsPanelStyle.setValue(panelStyleValue);
        updateListSummary(mQsPanelStyle, panelStyleValue);
        setTileSummary(mQsTileSummary, entryOf(mQsPanelStyle, panelStyleValue));

        // SystemUI draws the folder for the Yugen style until the value is written, so show the
        // state the panel is actually in rather than an unchecked switch.
        updateConnectivityFolder(resolver, panelStyleValue);

        mQsOneUiExperimental.setChecked(Settings.Secure.getIntForUser(
                resolver, KEY_QS_ONEUI_EXPERIMENTAL, 0, UserHandle.USER_CURRENT) != 0);

        final boolean liquidGlass = Settings.Secure.getIntForUser(
                resolver, KEY_LIQUID_GLASS, 0, UserHandle.USER_CURRENT) != 0;
        mLiquidGlass.setChecked(liquidGlass);

        final int glassStyle = Settings.Secure.getIntForUser(
                resolver, KEY_LIQUID_GLASS_STYLE, LIQUID_GLASS_STYLE_DEFAULT,
                UserHandle.USER_CURRENT);
        final String glassStyleValue = sanitize(mLiquidGlassStyle, String.valueOf(glassStyle),
                String.valueOf(LIQUID_GLASS_STYLE_DEFAULT));
        mLiquidGlassStyle.setValue(glassStyleValue);
        updateListSummary(mLiquidGlassStyle, glassStyleValue);
        mLiquidGlassStyle.setEnabled(liquidGlass);

        mGlassTabBars.setChecked(Settings.Secure.getIntForUser(
                resolver, KEY_GLASS_TAB_BARS, 0, UserHandle.USER_CURRENT) != 0);

        final int powerMenuStyle = Settings.Secure.getIntForUser(
                resolver, KEY_POWER_MENU_STYLE, POWER_MENU_STYLE_DEFAULT,
                UserHandle.USER_CURRENT);
        final String powerMenuValue = sanitize(mPowerMenuStyle, String.valueOf(powerMenuStyle),
                String.valueOf(POWER_MENU_STYLE_DEFAULT));
        mPowerMenuStyle.setValue(powerMenuValue);
        updateListSummary(mPowerMenuStyle, powerMenuValue);
        setTileSummary(mPowerMenuTileSummary, entryOf(mPowerMenuStyle, powerMenuValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = requireContext().getContentResolver();
        final String key = preference.getKey();

        if (KEY_QS_PANEL_STYLE.equals(key) || KEY_LIQUID_GLASS_STYLE.equals(key)
                || KEY_POWER_MENU_STYLE.equals(key)) {
            final String value = (String) newValue;
            Settings.Secure.putIntForUser(resolver, key,
                    Integer.parseInt(value), UserHandle.USER_CURRENT);
            updateListSummary((ListPreference) preference, value);
            if (KEY_QS_PANEL_STYLE.equals(key)) {
                updateConnectivityFolder(resolver, value);
                setTileSummary(mQsTileSummary, entryOf(mQsPanelStyle, value));
            } else if (KEY_POWER_MENU_STYLE.equals(key)) {
                setTileSummary(mPowerMenuTileSummary, entryOf(mPowerMenuStyle, value));
            }
        } else {
            final boolean enabled = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver, key, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
            if (KEY_LIQUID_GLASS.equals(key)) {
                mLiquidGlassStyle.setEnabled(enabled);
            }
        }
        return true;
    }

    /**
     * SystemUI treats the folder as enabled until a value is written, but only for the Yugen
     * panel style; the other styles leave it off. Show the same state the panel draws.
     */
    private void updateConnectivityFolder(ContentResolver resolver, String panelStyleValue) {
        final boolean folderDefault =
                String.valueOf(QS_PANEL_STYLE_DEFAULT).equals(panelStyleValue);
        mQsConnectivityFolder.setChecked(Settings.Secure.getIntForUser(
                resolver, KEY_QS_CONNECTIVITY_FOLDER, folderDefault ? 1 : 0,
                UserHandle.USER_CURRENT) != 0);
    }

    private static CharSequence buildVersion() {
        String version = SystemProperties.get(VERSION_PROPERTY);
        if (TextUtils.isEmpty(version)) {
            version = SystemProperties.get(FALLBACK_VERSION_PROPERTY, VERSION_FALLBACK);
        }
        final String codename = SystemProperties.get(CODENAME_PROPERTY);
        return TextUtils.isEmpty(codename)
                ? VERSION_FALLBACK + " " + version
                : VERSION_FALLBACK + " " + version + " (" + codename + ")";
    }

    private static void setTileSummary(TextView summary, CharSequence text) {
        if (summary != null && text != null) {
            summary.setText(text);
        }
    }

    private static CharSequence entryOf(ListPreference preference, String value) {
        final int index = preference.findIndexOfValue(value);
        return index >= 0 ? preference.getEntries()[index] : null;
    }

    /**
     * List preferences cannot represent values that are missing from their entry list. Fall back
     * to {@code fallback} when the stored value is unknown, for example after switching from a
     * different ROM or when a value was written by another component.
     */
    private static String sanitize(ListPreference preference, String value, String fallback) {
        return preference.findIndexOfValue(value) >= 0 ? value : fallback;
    }

    private static void updateListSummary(ListPreference preference, String value) {
        final CharSequence entry = entryOf(preference, value);
        if (entry != null) {
            preference.setSummary(entry);
        }
    }
}
