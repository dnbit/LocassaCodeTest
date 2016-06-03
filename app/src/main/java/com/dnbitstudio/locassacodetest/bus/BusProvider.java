package com.dnbitstudio.locassacodetest.bus;

import com.squareup.otto.Bus;

import android.os.Handler;
import android.os.Looper;

public class BusProvider
{
    private static Bus bus;

    public synchronized static Bus getBus()
    {

        if (bus == null)
        {
            bus = new MainThreadBus();
        }

        return bus;
    }

    public static class MainThreadBus extends Bus
    {

        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void post(final Object event)
        {
            if (Looper.myLooper() == Looper.getMainLooper())
            {
                super.post(event);
            } else
            {
                mainThreadHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        post(event);
                    }
                });
            }
        }
    }
}
