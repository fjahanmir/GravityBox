/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.gm2.gravitybox;

import java.util.ArrayList;

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.BatteryManager;

public class BatteryInfoManager {
    private BatteryData mBatteryData;
    private ArrayList<BatteryStatusListener> mListeners;
    private Context mGbContext;
    private boolean mChargedSoundEnabled;
    private boolean mPluggedSoundEnabled;

    class BatteryData {
        boolean charging;
        int level;
        int powerSource;
    }

    public interface BatteryStatusListener {
        void onBatteryStatusChanged(BatteryData batteryData);
    }

    public BatteryInfoManager(Context gbContext) {
        mGbContext = gbContext;
        mBatteryData = new BatteryData();
        mBatteryData.charging = false;
        mBatteryData.level = 0;
        mBatteryData.powerSource = 0;
        mListeners = new ArrayList<BatteryStatusListener>();
        mChargedSoundEnabled = false;
        mPluggedSoundEnabled = false;
    }

    public void registerListener(BatteryStatusListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (BatteryStatusListener listener : mListeners) {
            listener.onBatteryStatusChanged(mBatteryData);
        }
    }

    public void updateBatteryInfo(Intent intent) {
        int newLevel = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int newPowerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean newCharging = newPowerSource != 0;

        if (mBatteryData.level != newLevel || mBatteryData.charging != newCharging ||
                mBatteryData.powerSource != newPowerSource) {
            if (mChargedSoundEnabled && newLevel == 100 && mBatteryData.level == 99) {
                playSound(R.raw.battery_charged);
            }

            if (mPluggedSoundEnabled && mBatteryData.powerSource != newPowerSource) {
                if (newPowerSource == 0) {
                    playSound(R.raw.charger_unplugged);
                } else if (newPowerSource != BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    playSound(R.raw.charger_plugged);
                }
            }

            mBatteryData.level = newLevel;
            mBatteryData.charging = newCharging;
            mBatteryData.powerSource = newPowerSource;
            notifyListeners();
        }
    }

    public void setChargedSoundEnabled(boolean enabled) {
        mChargedSoundEnabled = enabled;
    }

    public void setPluggedSoundEnabled(boolean enabled) {
        mPluggedSoundEnabled = enabled;
    }

    private void playSound(int soundResId) {
        try {
            MediaPlayer mp = MediaPlayer.create(mGbContext, soundResId);
            mp.start();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
