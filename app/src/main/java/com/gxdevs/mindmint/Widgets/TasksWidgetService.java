package com.gxdevs.mindmint.Widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class TasksWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TasksRemoteViewsFactory(this.getApplicationContext());
    }
}
