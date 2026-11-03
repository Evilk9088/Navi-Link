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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 分色丝带导航窗口。
 * 左侧实心色块承担「转向 + 距离」，右侧信息区显示路名、剩余概览与到达时间，
 * 车道线 / 区间测速 / TMC 细条在需要时才向下展开。
 */
public class RibbonNaviWindow extends BaseFloatingWindow {

    private static final int LIGHT_COLOR_GREEN = 0xFF1ABF54;
    private static final int LIGHT_COLOR_RED = 0xFFFF1744;
    private static final int LIGHT_COLOR_YELLOW = 0xFFFFD600;

    private View llRibbonLeft;
    private ImageView ivRibbonTurn;
    private TextView tvRibbonDistNum;
    private TextView tvRibbonDistUnit;
    private TextView tvRibbonRoad;
    private TextView tvRibbonSummary;
    private View layoutRibbonLight;
    private View vRibbonLightDot;
    private TextView tvRibbonLightCountdown;
    private LaneLineView laneLineView;
    private IntervalSpeedView intervalSpeedView;
    private TmcProgressBar tmcProgressBar;

    private int themeColor = 0xFF0099FF;
    private int mCurSpeed = 0;
    private String mOriginalRoadName = "";
    private String mExitDirection = "";

    public RibbonNaviWindow(Context context, View floatingView) {
        super(context, floatingView);
    }

    @Override
    protected void initViews() {
        llRibbonLeft = floatingView.findViewById(R.id.ll_ribbon_left);
        ivRibbonTurn = floatingView.findViewById(R.id.iv_ribbon_turn);
        tvRibbonDistNum = floatingView.findViewById(R.id.tv_ribbon_dist_num);
        tvRibbonDistUnit = floatingView.findViewById(R.id.tv_ribbon_dist_unit);
        tvRibbonRoad = floatingView.findViewById(R.id.tv_ribbon_road);
        tvRibbonSummary = floatingView.findViewById(R.id.tv_ribbon_summary);
        layoutRibbonLight = floatingView.findViewById(R.id.layout_ribbon_light);
        vRibbonLightDot = floatingView.findViewById(R.id.v_ribbon_light_dot);
        tvRibbonLightCountdown = floatingView.findViewById(R.id.tv_ribbon_light_countdown);
        laneLineView = floatingView.findViewById(R.id.lane_line_view_ribbon);
        intervalSpeedView = floatingView.findViewById(R.id.interval_speed_view_ribbon);
        tmcProgressBar = floatingView.findViewById(R.id.tmc_progress_bar_ribbon);

        if (tmcProgressBar != null) {
            boolean tmcEnabled = sp.getBoolean("normal_navi_tmc_enabled", true);
            tmcProgressBar.setVisibility(tmcEnabled ? View.VISIBLE : View.GONE);
        }
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
        this.mCurSpeed = curSpeed;
        if (intervalSpeedView != null) {
            intervalSpeedView.updateCurrentSpeed(curSpeed);
        }

        if (ivRibbonTurn != null) {
            int turnIconRes = getTurnIconRes(icon);
            if (turnIconRes != 0) {
                ivRibbonTurn.setImageResource(turnIconRes);
            }
            boolean shouldBlink = shouldBlinkTurnIcon(disNum, disUnit);
            ObjectAnimator animator = (ObjectAnimator) ivRibbonTurn.getTag();
            if (shouldBlink) {
                if (animator == null) {
                    ObjectAnimator newAnimator = ObjectAnimator.ofFloat(ivRibbonTurn, "alpha", 1f, 0.3f);
                    newAnimator.setDuration(500);
                    newAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    newAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    newAnimator.start();
                    ivRibbonTurn.setTag(newAnimator);
                }
            } else {
                if (animator != null) {
                    animator.cancel();
                    ivRibbonTurn.setTag(null);
                }
                ivRibbonTurn.setAlpha(1f);
            }
        }

        if (tvRibbonDistNum != null) {
            tvRibbonDistNum.setText(disNum);
        }
        if (tvRibbonDistUnit != null) {
            tvRibbonDistUnit.setText(disNumIsNow(disNum) ? "" : disUnit);
        }

        mOriginalRoadName = roadName;
        updateRoadView();

        if (tvRibbonSummary != null) {
            tvRibbonSummary.setText(buildSummary(summaryStr, eta));
        }
    }

    /** 剩余里程/时间 与 到达时间合成一行，跨天角标沿用基类上标样式 */
    private CharSequence buildSummary(String summaryStr, String eta) {
        String summary = summaryStr != null ? summaryStr.trim() : "";
        CharSequence etaText = formatEtaSpannable(eta);
        if (summary.isEmpty()) return etaText;
        if (etaText.length() == 0) return summary;
        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
        builder.append(summary).append(" · ").append(etaText);
        return builder;
    }

    private void updateRoadView() {
        if (tvRibbonRoad == null) return;
        String dir = mExitDirection != null ? mExitDirection.trim() : "";
        if (!dir.isEmpty()) {
            tvRibbonRoad.setText(dir);
        } else {
            tvRibbonRoad.setText(mOriginalRoadName != null ? mOriginalRoadName : "");
        }
    }

    @Override
    public void updateCruiseInfo(int speed, String roadName, int cameraType, int cameraSpeed, int cameraDist, int carDirection) {
        // 丝带导航窗口不处理巡航数据
    }

    @Override
    public void updateTrafficLight(int status, int dir, int countdown) {
        if (layoutRibbonLight == null) return;
        if (countdown <= 0) {
            layoutRibbonLight.setVisibility(View.GONE);
            return;
        }
        int dotColor;
        if (status == 4) {
            dotColor = LIGHT_COLOR_GREEN;
        } else if (status == 1) {
            dotColor = LIGHT_COLOR_RED;
        } else {
            dotColor = LIGHT_COLOR_YELLOW;
        }
        if (vRibbonLightDot != null) {
            vRibbonLightDot.setBackgroundTintList(ColorStateList.valueOf(dotColor));
        }
        if (tvRibbonLightCountdown != null) {
            tvRibbonLightCountdown.setText(countdown + "s");
        }
        layoutRibbonLight.setVisibility(View.VISIBLE);
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
        mExitDirection = exitDirection;
        updateRoadView();
    }

    @Override
    public void updateTmcData(String tmcJson) {
        if (tmcProgressBar == null) return;
        if (sp.getBoolean("normal_navi_tmc_enabled", true)) {
            tmcProgressBar.setVisibility(View.VISIBLE);
            tmcProgressBar.updateTmcData(tmcJson);
        } else {
            tmcProgressBar.setVisibility(View.GONE);
            tmcProgressBar.clear();
        }
    }

    @Override
    public void updateIntervalSpeed(int startDist, String startDistText, int avgSpeed, String endDistText, int limitSpeed) {
        if (intervalSpeedView == null) return;
        if (startDist <= 0 && (startDistText == null || startDistText.isEmpty() || "0m".equals(startDistText) || "0.0m".equals(startDistText))
                && avgSpeed <= 0 && (endDistText == null || endDistText.isEmpty()) && limitSpeed <= 0) {
            intervalSpeedView.hide();
            return;
        }
        boolean isInside = (endDistText != null && !endDistText.trim().isEmpty()) || avgSpeed > 0;
        if (isInside) {
            intervalSpeedView.setInsideState(mCurSpeed, limitSpeed, avgSpeed, endDistText);
        } else {
            String distText = (startDistText != null && !startDistText.isEmpty()) ? startDistText : (startDist + "m");
            intervalSpeedView.setApproachingState(mCurSpeed, distText, limitSpeed);
        }
    }

    @Override
    public void applyThemeColor(int themeColor) {
        this.themeColor = themeColor;
        refreshLeftBlockStyle();
    }

    /** 左侧色块跟随主题色，文字/图标按色块明度自动切换黑白以保证可读性 */
    private void refreshLeftBlockStyle() {
        int textColor = contrastTextColor(themeColor);
        if (llRibbonLeft != null) {
            Drawable bg = llRibbonLeft.getBackground();
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg.mutate()).setColor(themeColor);
            } else {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setColor(themeColor);
                float r = dpToPx(12);
                drawable.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, 0, 0});
                llRibbonLeft.setBackground(drawable);
            }
        }
        if (tvRibbonDistNum != null) tvRibbonDistNum.setTextColor(textColor);
        if (tvRibbonDistUnit != null) tvRibbonDistUnit.setTextColor(withAlpha(textColor, 0xCC));
        if (ivRibbonTurn != null) ivRibbonTurn.setColorFilter(textColor);
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
        int textSecondary = getSecondaryTextColor(isNightMode);

        if (tvRibbonRoad != null) tvRibbonRoad.setTextColor(textPrimary);
        if (tvRibbonSummary != null) tvRibbonSummary.setTextColor(textSecondary);
        if (tvRibbonLightCountdown != null) tvRibbonLightCountdown.setTextColor(textPrimary);
        if (laneLineView != null) {
            int laneIconColor = isNightMode ? sp.getInt("lane_icon_color_night", 0xFFFFFFFF) : sp.getInt("lane_icon_color_day", 0xFFFFFFFF);
            laneLineView.setIconColor(laneIconColor);
        }
        refreshLeftBlockStyle();
    }

    @Override
    public void resetToDefaultTextColors() {
        if (tvRibbonRoad != null) tvRibbonRoad.setTextColor(TEXT_PRIMARY_DARK);
        if (tvRibbonSummary != null) tvRibbonSummary.setTextColor(TEXT_SECONDARY_DARK);
        if (tvRibbonLightCountdown != null) tvRibbonLightCountdown.setTextColor(TEXT_PRIMARY_DARK);
        if (laneLineView != null) laneLineView.setIconColor(0xFFFFFFFF);
        refreshLeftBlockStyle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ivRibbonTurn != null) {
            ObjectAnimator animator = (ObjectAnimator) ivRibbonTurn.getTag();
            if (animator != null) {
                animator.cancel();
                ivRibbonTurn.setTag(null);
            }
            ivRibbonTurn.setAlpha(1f);
        }
    }
}
