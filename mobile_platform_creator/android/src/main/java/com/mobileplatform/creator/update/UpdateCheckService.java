package com.mobileplatform.creator.update;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class UpdateCheckService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 非绑定服务
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: 实现检查更新的逻辑
        // ...
        // 检查完成后停止服务
        stopSelf();
        return START_NOT_STICKY;
    }
} 