package com.michaelarc.volttrigger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public final class SettingsUtils {

    private SettingsUtils() {
    }

    public static ComponentName accessibilityComponent(Context context) {
        return new ComponentName(context, TriggerAccessibilityService.class);
    }

    public static boolean isAccessibilityEnabled(Context context) {
        ComponentName expected = accessibilityComponent(context);
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);

        if (accessibilityEnabled == 1 && !TextUtils.isEmpty(enabledServices)) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(enabledServices);
            while (splitter.hasNext()) {
                ComponentName component = ComponentName.unflattenFromString(splitter.next());
                if (expected.equals(component)) {
                    return true;
                }
            }
        }

        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<android.accessibilityservice.AccessibilityServiceInfo> services =
                    manager.getEnabledAccessibilityServiceList(
                            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            if (services != null) {
                String packageName = context.getPackageName();
                for (android.accessibilityservice.AccessibilityServiceInfo info : services) {
                    ComponentName component = info.getResolveInfo() == null
                            ? null
                            : new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                    info.getResolveInfo().serviceInfo.name);
                    if (component != null && packageName.equals(component.getPackageName())
                            && component.getClassName().endsWith(
                                    TriggerAccessibilityService.class.getSimpleName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void openBatterySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {
            openAppSettings(context);
        }
    }

    public static String appLabel(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            ApplicationInfo info = manager.getApplicationInfo(context.getPackageName(), 0);
            return manager.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException ignored) {
            return "Volt Trigger";
        }
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
