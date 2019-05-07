package com.young.serialportsample.observable;

import android.database.Observable;
import android.os.Handler;
import android.os.Looper;

public class FrequencyBandObservable extends Observable<FrequencyBandObservable.FrequencyBandObserver> {

    private static int sBand1;
    private static int sBand2;
    private static FrequencyBandObservable sFrequencyBandObservable;

    private FrequencyBandObservable() {

    }

    public static FrequencyBandObservable getInstance() {
        if (sFrequencyBandObservable == null) {
            synchronized (FrequencyBandObservable.class) {
                if (sFrequencyBandObservable == null) {
                    sFrequencyBandObservable = new FrequencyBandObservable();
                }
            }
        }
        return sFrequencyBandObservable;
    }

    public void notifyDataChange(int band1, int band2) {
        sBand1 = band1;
        sBand2 = band2;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (FrequencyBandObserver observer : mObservers) {
                    observer.onFrequencyBandChange();
                }
            }
        });
    }


    public int getBand1() {
        return sBand1;
    }

    public int getBand2() {
        return sBand2;
    }


    public interface FrequencyBandObserver {
        public void onFrequencyBandChange();
    }
}
