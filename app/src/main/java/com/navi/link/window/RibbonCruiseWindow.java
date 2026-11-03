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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 分色丝带巡航窗口。
 * 左侧实心色块以车速为主角（超速变红并闪烁），右侧显示路名与摄像头/红绿灯信息。
 */
public class RibbonCruiseWindow extends BaseFloatingWindow {

    private View llRibbonLeft;
    private TextView tvRibbonSpeed;
    private TextView tvRibbonSpeedUnit;
    private TextView tvRibbonCruiseRoad;
    private CameraWarningView llRibbonCamera;
    private LinearLayout llRibbonTrafficLights;
    private LaneLineView laneLineView;

    private int themeColor = 0xFF0099FF;
    private boolean isOverspeedBlinking = false;
    private int mLastCameraType = 0;
    private int mLastCameraSpeed = 0;
    private int mLastCameraDist = 0;

    public RibbonCruiseWindow(Context context, View floatingView) {
        super(context, floatingView);
    }

    @Override
    protected void initViews() {
        llRibbonLeft = floatingView.findViewById(R.id.ll_ribbon_left);
        tvRibbonSpeed = floatingView.findViewById(R.id.tv_ribbon_speed);
        tvRibbonSpeedUnit = floatingView.findViewById(R.id.tv_ribbon_speed_unit);
        tvRibbonCruiseRoad = floatingView.findViewById(R.id.tv_ribbon_cruise_road);
        llRibbonCamera = floatingView.findViewById(R.id.ll_ribbon_camera);
        llRibbonTrafficLights = floatingView.findViewById(R.id.ll_ribbon_traffic_lights);
        laneLineView = floatingView.findViewById(R.id.lane_line_view_ribbon);
        updateCameraCapsuleBackground(llRibbonCamera);
        refreshLeftBlockStyle();
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
        // 丝带巡航窗口不处理导航数据
    }

    @Override
    public void updateCruiseInfo(int speed, String roadName, int cameraType, int cameraSpeed, int cameraDist, int carDirection) {
        mLastCameraType = cameraType;
        mLastCameraSpeed = cameraSpeed;
        mLastCameraDist = cameraDist;

        if (tvRibbonSpeed != null) {
            boolean hideSpeed = sp.getBoolean("hide_normal_cruise_speed", false);
            tvRibbonSpeed.setVisibility(hideSpeed ? View.GONE : View.VISIBLE);
            tvRibbonSpeed.setText(String.valueOf(speed));

            int threshold = sp.getInt("overspeed_threshold", 0);
            double factor = 1.0 + threshold / 100.0;
            boolean isOverspeedWarningEnabled = sp.getBoolean("overspeed_warning_enabled", true);
            boolean overspeed = !hideSpeed && isOverspeedWarningEnabled
                    && cameraSpeed > 0 && speed > Math.round(cameraSpeed * factor);
            if (overspeed) {
                tvRibbonSpeed.setTextColor(Color.RED);
                ObjectAnimator animator = (ObjectAnimator) tvRibbonSpeed.getTag();
                if (animator == null) {
                    ObjectAnimator newAnimator = ObjectAnimator.ofFloat(tvRibbonSpeed, "alpha", 1f, 0.3f);
                    newAnimator.setDuration(500);
                    newAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    newAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    newAnimator.start();
                    tvRibbonSpeed.setTag(newAnimator);
                    isOverspeedBlinking = true;
                }
            } else {
                ObjectAnimator animator = (ObjectAnimator) tvRibbonSpeed.getTag();
                if (animator != null) {
                    animator.cancel();
                    tvRibbonSpeed.setTag(null);
                }
                tvRibbonSpeed.setAlpha(1f);
                isOverspeedBlinking = false;
                refreshLeftBlockStyle();
            }
        }

        if (tvRibbonCruiseRoad != null && roadName != null) {
            tvRibbonCruiseRoad.setText(roadName);
        }

        if (llRibbonCamera != null) {
            llRibbonCamera.updateCameraInfo(cameraType, cameraDist, cameraSpeed);
        }
        updateCameraAndLightsVisibility();
    }

    /** 右侧只有一行空间：有红绿灯倒计时时优先显示灯，无灯时回退到摄像头胶囊 */
    private void updateCameraAndLightsVisibility() {
        boolean hasLights = llRibbonTrafficLights != null
                && llRibbonTrafficLights.getVisibility() == View.VISIBLE
                && llRibbonTrafficLights.getChildCount() > 0;
        if (llRibbonCamera != null) {
            boolean hasCamera = mLastCameraSpeed > 0 || mLastCameraType != 0 || mLastCameraDist > 0;
            llRibbonCamera.setVisibility(!hasLights && hasCamera ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void updateTrafficLight(int status, int dir, int countdown) {
        // 巡航使用 updateCruiseTrafficLights 处理多灯倒计时
    }

    @Override
    public void updateCruiseTrafficLights(JSONArray lightsArray) {
        LinearLayout container = llRibbonTrafficLights;
        if (container == null) return;

        int count = lightsArray != null ? lightsArray.length() : 0;
        int childCount = container.getChildCount();

        if (count == 0) {
            container.setVisibility(View.GONE);
            if (childCount > 0) container.removeAllViews();
            updateCameraAndLightsVisibility();
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
                    llLp.setMarginStart(dpToPx(4));
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
        updateCameraAndLightsVisibility();
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
        this.themeColor = themeColor;
        refreshLeftBlockStyle();
    }

    /** 左侧色块跟随主题色；超速闪烁期间不覆盖红色，避免主题色把告警色刷掉 */
    private void refreshLeftBlockStyle() {
        if (llRibbonLeft != null) {
            Drawable bg = llRibbonLeft.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg.mutate()).setColor(themeColor);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setColor(themeColor);
                float r = dpToPx(12);
                drawable.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
                llRibbonLeft.setBackground(drawable);
            }
        }
        int textColor = contrastTextColor(themeColor);
        if (tvRibbonSpeed != null && !isOverspeedBlinking) {
            tvRibbonSpeed.setTextColor(textColor);
        }
        if (tvRibbonSpeedUnit != null) {
            tvRibbonSpeedUnit.setTextColor(withAlpha(textColor, 0xCC));
        }
    }

    private int contrastTextColor(int bgColor) {
        int r = (bgColor >> 16) & 0xFF;
        int g = (bgColor >> 8) & 0xFF;
        int b = bgColor & 0xFF;
        double luminance = r * 0.299 + g * 0.587 + b * 0.114;
        return luminance > 150 ? 0xFF111111 : 0xFFFFFFFF;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public void applyDayNightTextColors(boolean isNightMode) {
        this.isNightMode = isNightMode;
        int textPrimary = getPrimaryTextColor(isNightMode);

        if (tvRibbonCruiseRoad != null) tvRibbonCruiseRoad.setTextColor(textPrimary);
        if (llRibbonCamera != null) llRibbonCamera.setTextColor(textPrimary);
        if (laneLineView != null) {
            int laneIconColor = isNightMode ? sp.getInt("lane_icon_color_night", 0xFFFFFFFF) : sp.getInt("lane_icon_color_day", 0xFFFFFFFF);
            laneLineView.setIconColor(laneIconColor);
        }
        refreshLeftBlockStyle();
    }

    @Override
    public void resetToDefaultTextColors() {
        if (tvRibbonCruiseRoad != null) tvRibbonCruiseRoad.setTextColor(TEXT_PRIMARY_DARK);
        if (llRibbonCamera != null) llRibbonCamera.setTextColor(TEXT_PRIMARY_DARK);
        if (laneLineView != null) laneLineView.setIconColor(0xFFFFFFFF);
        refreshLeftBlockStyle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (llRibbonTrafficLights != null) {
            llRibbonTrafficLights.removeAllViews();
        }
        if (tvRibbonSpeed != null) {
            ObjectAnimator animator = (ObjectAnimator) tvRibbonSpeed.getTag();
            if (animator != null) {
                animator.cancel();
                tvRibbonSpeed.setTag(null);
            }
            tvRibbonSpeed.setAlpha(1f);
        }
        isOverspeedBlinking = false;
    }
}
