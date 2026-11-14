package com.jfvag.aibox;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only home-screen widget. Uses BridgeService's existing EXLAP snapshot. */
public class VehicleWidget extends AppWidgetProvider {
    private static final String[] FIELDS = {
        "vehicleSpeed", "engineSpeed", "outsideTemperature",
        "batteryVoltage", "coolantTemperature", "currentTorque"
    };
    private static final int[] VALUE_IDS = {
        R.id.widget_value_1, R.id.widget_value_2, R.id.widget_value_3,
        R.id.widget_value_4, R.id.widget_value_5, R.id.widget_value_6
    };
    private static final String[] UNITS = {"km/h", "rpm", "°C", "V", "°C", "Nm"};

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    @Override public void onEnabled(Context context) { refresh(context); }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) refresh(context);
    }

    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, VehicleWidget.class));
        if (ids.length != 0) update(context, manager, ids);
    }

    private static void update(Context context, AppWidgetManager manager, int[] ids) {
        boolean fresh = BridgeService.running && BridgeService.connected
            && BridgeService.lastGoodData > 0
            && System.currentTimeMillis() - BridgeService.lastGoodData < 15000;
        Map<String,Object> snapshot = BridgeService.values;
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.vehicle_widget);
            for (int i=0;i<FIELDS.length;i++) {
                Long fieldTime = BridgeService.fieldUpdatedMs.get(FIELDS[i]);
                boolean fieldFresh = fieldTime != null && System.currentTimeMillis() - fieldTime < 15000;
                String reading = (fresh && fieldFresh) ? number(snapshot.get(FIELDS[i]), FIELDS[i]) : null;
                v.setTextViewText(VALUE_IDS[i], reading == null ? "—" : reading);
            }
            v.setTextViewText(R.id.widget_status, fresh ? "● 已連線" : "○ 開啟 JF App 連線");
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent click = PendingIntent.getActivity(context, 0, open, flags);
            v.setOnClickPendingIntent(R.id.widget_root, click);
            manager.updateAppWidget(id, v);
        }
    }

    private static String number(Object field, String key) {
        if (!(field instanceof List)) return null;
        for (Object row : (List<?>)field) {
            if (!(row instanceof Map)) continue;
            Map<?,?> map=(Map<?,?>)row;
            Object obj=map.get("value");
            if (obj==null) continue;
            String s=String.valueOf(obj).trim();
            if (s.isEmpty() || "nodata".equalsIgnoreCase(s) || "valid".equalsIgnoreCase(s)) continue;
            // Prefer genuine numerical payloads; never interpret the metadata field as a temperature.
            try {
                double d=Double.parseDouble(s);
                if (!Double.isFinite(d)) continue;
                int digits=("vehicleSpeed".equals(key)||"engineSpeed".equals(key))?0:1;
                return String.format(Locale.US, digits==0?"%.0f":"%.1f",d);
            } catch (NumberFormatException ignored) { /* nonnumerical EXLAP status */ }
        }
        return null;
    }
}
