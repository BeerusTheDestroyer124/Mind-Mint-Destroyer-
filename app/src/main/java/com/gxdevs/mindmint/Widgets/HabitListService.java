package com.gxdevs.mindmint.Widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class HabitListService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new HabitRemoteViewsFactory(this.getApplicationContext());
    }
}
