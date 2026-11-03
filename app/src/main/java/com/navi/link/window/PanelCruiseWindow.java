package com.navi.link.window;
import com.navi.link.R;
import com.navi.link.BuildConfig;
import com.navi.link.activity.*;
import com.navi.link.delegate.*;
import com.navi.link.window.*;
import com.navi.link.view.*;
import com.navi.link.receiver.*;
import com.navi.link.service.*;
import com.navi.link.utils.*;


import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 面板款巡航窗口。
 *
 * 结构自上而下：速度区（占一半高度）→ 红绿灯独立一行 → 车道线。
 * 红绿灯行无数据时整行隐藏。
 */
public class PanelCruiseWindow extends BaseFloatingWindow {

    private TextView tvPanelSpeed;
    private TextView tvPanelSpeedUnit;
    private TextView tvPanelCruiseRoad;
    private CameraWarningView llPanelCruiseCamera;
    private LinearLayout llPanelCruiseLights;
    private View vPanelCruiseLightDivider;
    private LaneLineView laneLineView;

    private boolean isOverspeedBlinking = false;
    private int mLastCameraType = 0;
    private int mLastCameraSpeed = 0;
    private int mLastCameraDist = 0;

    public PanelCruiseWindow(Context context, View floatingView) {
        super(context, floatingView);
    }

    @Override
    protected void initViews() {
        tvPanelSpeed = floatingView.findViewById(R.id.tv_panel_speed);
        tvPanelSpeedUnit = floatingView.findViewById(R.id.tv_panel_speed_unit);
        tvPanelCruiseRoad = floatingView.findViewById(R.id.tv_panel_cruise_road);
        llPanelCruiseCamera = floatingView.findViewById(R.id.ll_panel_cruise_camera);
        llPanelCruiseLights = floatingView.findViewById(R.id.ll_panel_cruise_lights);
        vPanelCruiseLightDivider = floatingView.findViewById(R.id.v_panel_cruise_light_divider);
        laneLineView = floatingView.findViewById(R.id.lane_line_view_panel);
        updateCameraCapsuleBackground(llPanelCruiseCamera);
    }

    @Override
    public void updateNaviInfo(
            int icon, String disNum, String disUnit, String actionStr,
            String roadName, String summaryStr, String eta,
            int progress, int curSpeed,
            int limitedSpeed, int cameraType, int cameraDist, int cameraSpeed,
            String endPoiName, int totalLightNum, int remainLightNum,
            String curRoadName, int carDirection
    ) {
        // 面板巡航窗口不处理导航数据
    }

    @Override
    public void updateCruiseInfo(int speed, String roadName, int cameraType, int cameraSpeed, int cameraDist, int carDirection) {
        mLastCameraType = cameraType;
        mLastCameraSpeed = cameraSpeed;
        mLastCameraDist = cameraDist;

        if (tvPanelSpeed != null) {
            boolean hideSpeed = sp.getBoolean("hide_normal_cruise_speed", false);
            tvPanelSpeed.setVisibility(hideSpeed ? View.GONE : View.VISIBLE);
            if (tvPanelSpeedUnit != null) {
                tvPanelSpeedUnit.setVisibility(hideSpeed ? View.GONE : View.VISIBLE);
            }
            tvPanelSpeed.setText(String.valueOf(speed));

            int threshold = sp.getInt("overspeed_threshold", 0);
            double factor = 1.0 + threshold / 100.0;
            boolean isOverspeedWarningEnabled = sp.getBoolean("overspeed_warning_enabled", true);
            boolean overspeed = !hideSpeed && isOverspeedWarningEnabled
                    && cameraSpeed > 0 && speed > Math.round(cameraSpeed * factor);
            if (overspeed) {
                tvPanelSpeed.setTextColor(Color.RED);
                ObjectAnimator animator = (ObjectAnimator) tvPanelSpeed.getTag();
                if (animator == null) {
                    ObjectAnimator newAnimator = ObjectAnimator.ofFloat(tvPanelSpeed, "alpha", 1f, 0.3f);
                    newAnimator.setDuration(500);
                    newAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    newAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    newAnimator.start();
                    tvPanelSpeed.setTag(newAnimator);
                    isOverspeedBlinking = true;
                }
            } else {
                ObjectAnimator animator = (ObjectAnimator) tvPanelSpeed.getTag();
                if (animator != null) {
                    animator.cancel();
                    tvPanelSpeed.setTag(null);
                }
                tvPanelSpeed.setAlpha(1f);
                isOverspeedBlinking = false;
                tvPanelSpeed.setTextColor(getPrimaryTextColor(isNightMode));
            }
        }

        if (tvPanelCruiseRoad != null && roadName != null) {
            tvPanelCruiseRoad.setText(roadName);
        }

        if (llPanelCruiseCamera != null) {
            llPanelCruiseCamera.updateCameraInfo(cameraType, cameraDist, cameraSpeed);
        }
    }

    @Override
    public void updateTrafficLight(int status, int dir, int countdown) {
        // 巡航使用 updateCruiseTrafficLights 处理多灯倒计时
    }

    @Override
    public void updateCruiseTrafficLights(JSONArray lightsArray) {
        LinearLayout container = llPanelCruiseLights;
        if (container == null) return;

        int count = lightsArray != null ? lightsArray.length() : 0;
        int childCount = container.getChildCount();

        if (count == 0) {
            container.setVisibility(View.GONE);
            if (vPanelCruiseLightDivider != null) vPanelCruiseLightDivider.setVisibility(View.GONE);
            if (childCount > 0) container.removeAllViews();
            return;
        }

        if (count != childCount) {
            container.removeAllViews();
            float scale = getWindowScale();
            for (int i = 0; i < count; i++) {
                try {
                    JSONObject lightObj = lightsArray.getJSONObject(i);
                    TrafficLightView lightView = new TrafficLightView(context);
                    lightView.setCompact(count >= 3);
                    LinearLayout.LayoutParams llLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    if (i > 0) llLp.setMarginStart(dpToPx(8));
                    lightView.setLayoutParams(llLp);
                    if (scale != 1.0f) {
                        scaleViewRecursive(lightView, scale);
                    }
                    int status = lightObj.getInt("status");
                    int countdown = lightObj.getInt("countdown");
                    int dir = lightObj.getInt("dir");
                    if (countdown > 0) {
                        lightView.setData(status, dir, countdown, false);
                    } else {
                        lightView.setVisibility(View.GONE);
                    }
                    container.addView(lightView);
                } catch (Exception ignored) {
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                try {
                    TrafficLightView lightView = (TrafficLightView) container.getChildAt(i);
                    JSONObject lightObj = lightsArray.getJSONObject(i);
                    int status = lightObj.getInt("status");
                    int countdown = lightObj.getInt("countdown");
                    int dir = lightObj.getInt("dir");
                    if (countdown > 0) {
                        lightView.setVisibility(View.VISIBLE);
                        lightView.setData(status, dir, countdown, false);
                    } else {
                        lightView.setVisibility(View.GONE);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        container.setVisibility(View.VISIBLE);

        boolean allGone = true;
        for (int i = 0; i < container.getChildCount(); i++) {
            if (container.getChildAt(i).getVisibility() == View.VISIBLE) {
                allGone = false;
                break;
            }
        }
        if (allGone) {
            container.setVisibility(View.GONE);
        }
        if (vPanelCruiseLightDivider != null) {
            vPanelCruiseLightDivider.setVisibility(
                    container.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void updateLaneLines(String driveWayJson) {
        if (laneLineView == null) return;
        if (sp.getBoolean("normal_navi_lane_enabled", false)) {
            laneLineView.updateLanes(driveWayJson);
        } else {
            laneLineView.clear();
        }
    }

    @Override
    public void updateExitInfo(String exitName, String exitDirection) {
        // 巡航无出口信息
    }

    @Override
    public void applyThemeColor(int themeColor) {
        // 面板巡航款以车速为唯一强调对象，颜色沿用主文字色，不需要主题色块
        if (tvPanelSpeed != null && !isOverspeedBlinking) {
            tvPanelSpeed.setTextColor(getPrimaryTextColor(isNightMode));
        }
    }

    @Override
    public void applyDayNightTextColors(boolean isNightMode) {
        this.isNightMode = isNightMode;
        int textPrimary = getPrimaryTextColor(isNightMode);
        int textSecondary = getSecondaryTextColor(isNightMode);

        if (tvPanelCruiseRoad != null) tvPanelCruiseRoad.setTextColor(textPrimary);
        if (llPanelCruiseCamera != null) llPanelCruiseCamera.setTextColor(textPrimary);
        if (tvPanelSpeedUnit != null) tvPanelSpeedUnit.setTextColor(textSecondary);
        if (tvPanelSpeed != null && !isOverspeedBlinking) tvPanelSpeed.setTextColor(textPrimary);
        if (laneLineView != null) {
            int laneIconColor = isNightMode ? sp.getInt("lane_icon_color_night", 0xFFFFFFFF) : sp.getInt("lane_icon_color_day", 0xFFFFFFFF);
            laneLineView.setIconColor(laneIconColor);
        }
        // 分隔线按昼夜取色，浅色底用黑色半透明
        int dividerColor = isNightMode ? 0x12FFFFFF : 0x14000000;
        View dividerTop = floatingView.findViewById(R.id.v_panel_cruise_divider_top);
        if (dividerTop != null) dividerTop.setBackgroundColor(dividerColor);
        if (vPanelCruiseLightDivider != null) vPanelCruiseLightDivider.setBackgroundColor(dividerColor);
    }

    @Override
    public void resetToDefaultTextColors() {
        if (tvPanelCruiseRoad != null) tvPanelCruiseRoad.setTextColor(TEXT_PRIMARY_DARK);
        if (llPanelCruiseCamera != null) llPanelCruiseCamera.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelSpeedUnit != null) tvPanelSpeedUnit.setTextColor(TEXT_SECONDARY_DARK);
        if (tvPanelSpeed != null) tvPanelSpeed.setTextColor(TEXT_PRIMARY_DARK);
        if (laneLineView != null) laneLineView.setIconColor(0xFFFFFFFF);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (llPanelCruiseLights != null) {
            llPanelCruiseLights.removeAllViews();
        }
        if (tvPanelSpeed != null) {
            ObjectAnimator animator = (ObjectAnimator) tvPanelSpeed.getTag();
            if (animator != null) {
                animator.cancel();
                tvPanelSpeed.setTag(null);
            }
            tvPanelSpeed.setAlpha(1f);
        }
        isOverspeedBlinking = false;
    }
}
