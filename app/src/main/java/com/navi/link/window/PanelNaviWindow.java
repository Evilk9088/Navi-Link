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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 面板款导航窗口（全数据面板）。
 *
 * 结构自上而下：转向区（占一半高度）→ 红绿灯独立一行 → 车速/限速/剩余数值行 → 原版 TMC 路况条。
 * 红绿灯行无数据时整行隐藏，窗口高度由 FloatingWindowManager 重新测量。
 */
public class PanelNaviWindow extends BaseFloatingWindow {

    private static final int LIGHT_GREEN = 0xFF4CAF50;
    private static final int LIGHT_RED = 0xFFFF1744;
    private static final int LIGHT_YELLOW = 0xFFFFD600;

    private View llPanelTurnBox;
    private ImageView ivPanelTurn;
    private TextView tvPanelExitInfo;
    private TextView tvPanelDistNum;
    private TextView tvPanelDistUnit;
    private TextView tvPanelRoad;
    private LaneLineView laneLineView;
    private View llPanelLightRow;
    private View vPanelLightDivider;
    private ImageView ivPanelLightDir;
    private View vPanelLightDot;
    private TextView tvPanelLightLabel;
    private TextView tvPanelLightCount;
    private TextView tvPanelLightUnit;
    private TextView tvPanelSpeed;
    private TextView tvPanelLimit;
    private TextView tvPanelLimitLabel;
    private TextView tvPanelRemain;
    private TextView tvPanelRemainUnit;
    private TextView tvPanelEta;
    private TmcProgressBar tmcProgressBar;

    private int themeColor = 0xFF0099FF;
    private String mOriginalRoadName = "";
    private String mExitDirection = "";

    public PanelNaviWindow(Context context, View floatingView) {
        super(context, floatingView);
    }

    @Override
    protected void initViews() {
        llPanelTurnBox = floatingView.findViewById(R.id.ll_panel_turn_box);
        ivPanelTurn = floatingView.findViewById(R.id.iv_panel_turn);
        tvPanelExitInfo = floatingView.findViewById(R.id.tv_panel_exit_info);
        tvPanelDistNum = floatingView.findViewById(R.id.tv_panel_dist_num);
        tvPanelDistUnit = floatingView.findViewById(R.id.tv_panel_dist_unit);
        tvPanelRoad = floatingView.findViewById(R.id.tv_panel_road);
        laneLineView = floatingView.findViewById(R.id.lane_line_view_panel);
        llPanelLightRow = floatingView.findViewById(R.id.ll_panel_light_row);
        vPanelLightDivider = floatingView.findViewById(R.id.v_panel_light_divider);
        ivPanelLightDir = floatingView.findViewById(R.id.iv_panel_light_dir);
        vPanelLightDot = floatingView.findViewById(R.id.v_panel_light_dot);
        tvPanelLightLabel = floatingView.findViewById(R.id.tv_panel_light_label);
        tvPanelLightCount = floatingView.findViewById(R.id.tv_panel_light_count);
        tvPanelLightUnit = floatingView.findViewById(R.id.tv_panel_light_unit);
        tvPanelSpeed = floatingView.findViewById(R.id.tv_panel_speed);
        tvPanelLimit = floatingView.findViewById(R.id.tv_panel_limit);
        tvPanelLimitLabel = floatingView.findViewById(R.id.tv_panel_limit_label);
        tvPanelRemain = floatingView.findViewById(R.id.tv_panel_remain);
        tvPanelRemainUnit = floatingView.findViewById(R.id.tv_panel_remain_unit);
        tvPanelEta = floatingView.findViewById(R.id.tv_panel_eta);
        tmcProgressBar = floatingView.findViewById(R.id.tmc_progress_bar_panel);

        if (tmcProgressBar != null) {
            boolean tmcEnabled = sp.getBoolean("normal_navi_tmc_enabled", true);
            tmcProgressBar.setVisibility(tmcEnabled ? View.VISIBLE : View.GONE);
        }
        refreshTurnBlockStyle();
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
        if (ivPanelTurn != null) {
            int turnIconRes = getTurnIconRes(icon);
            if (turnIconRes != 0) {
                ivPanelTurn.setImageResource(turnIconRes);
            }
            boolean shouldBlink = shouldBlinkTurnIcon(disNum, disUnit);
            ObjectAnimator animator = (ObjectAnimator) ivPanelTurn.getTag();
            if (shouldBlink) {
                if (animator == null) {
                    ObjectAnimator newAnimator = ObjectAnimator.ofFloat(ivPanelTurn, "alpha", 1f, 0.3f);
                    newAnimator.setDuration(500);
                    newAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    newAnimator.setRepeatMode(ValueAnimator.REVERSE);
                    newAnimator.start();
                    ivPanelTurn.setTag(newAnimator);
                }
            } else {
                if (animator != null) {
                    animator.cancel();
                    ivPanelTurn.setTag(null);
                }
                ivPanelTurn.setAlpha(1f);
            }
        }

        if (tvPanelDistNum != null) tvPanelDistNum.setText(disNum);
        if (tvPanelDistUnit != null) tvPanelDistUnit.setText(disNumIsNow(disNum) ? "" : disUnit);

        mOriginalRoadName = roadName;
        updateRoadView();

        if (tvPanelSpeed != null) tvPanelSpeed.setText(String.valueOf(curSpeed));

        if (tvPanelLimit != null) {
            tvPanelLimit.setText(limitedSpeed > 0 ? String.valueOf(limitedSpeed) : "--");
        }
        if (tvPanelLimitLabel != null) {
            // 有电子眼时把第二列的副标题让给电子眼距离，限速数值本身仍是道路限速
            if (cameraDist > 0 && cameraSpeed > 0) {
                tvPanelLimitLabel.setText("电子眼 " + cameraDist + "m 限速" + cameraSpeed);
            } else {
                tvPanelLimitLabel.setText("道路限速");
            }
        }

        updateSummary(summaryStr, eta);
    }

    /** summaryStr 形如「12.6公里 · 14分钟」，拆成剩余里程与剩余时间两段 */
    private void updateSummary(String summaryStr, String eta) {
        String summary = summaryStr != null ? summaryStr.trim() : "";
        String distPart = summary;
        String timePart = "";
        int sep = summary.indexOf('·');
        if (sep >= 0) {
            distPart = summary.substring(0, sep).trim();
            timePart = summary.substring(sep + 1).trim();
        }

        String distNum = distPart;
        String distUnit = "";
        if (distPart.endsWith("公里")) {
            distNum = distPart.substring(0, distPart.length() - 2);
            distUnit = "公里";
        } else if (distPart.endsWith("米")) {
            distNum = distPart.substring(0, distPart.length() - 1);
            distUnit = "米";
        }
        if (tvPanelRemain != null) tvPanelRemain.setText(distNum.isEmpty() ? "--" : distNum);
        if (tvPanelRemainUnit != null) tvPanelRemainUnit.setText(distUnit);

        if (tvPanelEta != null) {
            String etaText = formatEta(eta);
            StringBuilder sb = new StringBuilder();
            if (!timePart.isEmpty()) sb.append(timePart);
            if (!etaText.isEmpty()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(etaText);
            }
            tvPanelEta.setText(sb.length() > 0 ? sb.toString() : "--");
        }
    }

    private void updateRoadView() {
        if (tvPanelRoad == null) return;
        String dir = mExitDirection != null ? mExitDirection.trim() : "";
        if (!dir.isEmpty()) {
            tvPanelRoad.setText(dir);
        } else {
            tvPanelRoad.setText(mOriginalRoadName != null ? mOriginalRoadName : "");
        }
    }

    @Override
    public void updateCruiseInfo(int speed, String roadName, int cameraType, int cameraSpeed, int cameraDist, int carDirection) {
        // 面板导航窗口不处理巡航数据
    }

    @Override
    public void updateTrafficLight(int status, int dir, int countdown) {
        if (llPanelLightRow == null) return;
        if (countdown <= 0) {
            llPanelLightRow.setVisibility(View.GONE);
            if (vPanelLightDivider != null) vPanelLightDivider.setVisibility(View.GONE);
            return;
        }

        int color;
        String label;
        if (status == 4) {
            color = LIGHT_GREEN;
            label = "绿灯";
        } else if (status == 1) {
            color = LIGHT_RED;
            label = "红灯";
        } else {
            color = LIGHT_YELLOW;
            label = "黄灯";
        }

        if (vPanelLightDot != null) {
            vPanelLightDot.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (ivPanelLightDir != null) {
            ivPanelLightDir.setImageResource(getNaviLightDirRes(dir));
        }
        if (tvPanelLightLabel != null) {
            tvPanelLightLabel.setText(label);
            tvPanelLightLabel.setTextColor(color);
        }
        if (tvPanelLightCount != null) {
            tvPanelLightCount.setText(String.valueOf(countdown));
            tvPanelLightCount.setTextColor(color);
        }
        if (tvPanelLightUnit != null) {
            tvPanelLightUnit.setTextColor(withAlpha(color, 0xB0));
        }
        // 整行底色跟随灯色，用极低透明度做状态暗示，不喧宾夺主
        llPanelLightRow.setBackgroundColor(withAlpha(color, 0x1A));

        llPanelLightRow.setVisibility(View.VISIBLE);
        if (vPanelLightDivider != null) vPanelLightDivider.setVisibility(View.VISIBLE);
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
        if (tvPanelExitInfo != null) {
            String name = exitName != null ? exitName.trim() : "";
            if (name.isEmpty()) {
                tvPanelExitInfo.setVisibility(View.GONE);
            } else {
                tvPanelExitInfo.setText(name);
                tvPanelExitInfo.setVisibility(View.VISIBLE);
            }
        }
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
    public void applyThemeColor(int themeColor) {
        this.themeColor = themeColor;
        refreshTurnBlockStyle();
    }

    /** 转向图标底板跟随主题色：填充 15% 透明度、描边 50% */
    private void refreshTurnBlockStyle() {
        if (llPanelTurnBox == null) return;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(withAlpha(themeColor, 0x26));
        drawable.setStroke(dpToPx(1), withAlpha(themeColor, 0x80));
        drawable.setCornerRadius(dpToPx(16));
        llPanelTurnBox.setBackground(drawable);
    }

    @Override
    public void applyDayNightTextColors(boolean isNightMode) {
        this.isNightMode = isNightMode;
        int textPrimary = getPrimaryTextColor(isNightMode);
        int textSecondary = getSecondaryTextColor(isNightMode);

        if (tvPanelRoad != null) tvPanelRoad.setTextColor(textPrimary);
        if (tvPanelDistNum != null) tvPanelDistNum.setTextColor(textPrimary);
        if (tvPanelDistUnit != null) tvPanelDistUnit.setTextColor(textSecondary);
        if (tvPanelSpeed != null) tvPanelSpeed.setTextColor(textPrimary);
        if (tvPanelLimit != null) tvPanelLimit.setTextColor(textPrimary);
        if (tvPanelRemain != null) tvPanelRemain.setTextColor(textPrimary);
        if (tvPanelLimitLabel != null) tvPanelLimitLabel.setTextColor(textSecondary);
        if (tvPanelEta != null) tvPanelEta.setTextColor(textSecondary);
        if (tvPanelRemainUnit != null) tvPanelRemainUnit.setTextColor(textSecondary);
        if (laneLineView != null) {
            int laneIconColor = isNightMode ? sp.getInt("lane_icon_color_night", 0xFFFFFFFF) : sp.getInt("lane_icon_color_day", 0xFFFFFFFF);
            laneLineView.setIconColor(laneIconColor);
        }
        // 分隔线：深色底用白色半透明，浅色底用黑色半透明，否则白天模式看不见
        int dividerColor = isNightMode ? 0x12FFFFFF : 0x14000000;
        setDividerColor(R.id.v_panel_divider_top, dividerColor);
        setDividerColor(R.id.v_panel_divider_kpi1, dividerColor);
        setDividerColor(R.id.v_panel_divider_kpi2, dividerColor);
        setDividerColor(R.id.v_panel_light_divider, dividerColor);
        // 转向图标跟随昼夜取色：用户自定义过就用设置值，没设置过则白天深色、夜间白色
        if (ivPanelTurn != null) {
            String colorKey = isNightMode ? "normal_turn_icon_color_night" : "normal_turn_icon_color_day";
            int turnIconColor = sp.contains(colorKey)
                    ? sp.getInt(colorKey, 0xFFFFFFFF)
                    : (isNightMode ? 0xFFFFFFFF : 0xFF111111);
            ivPanelTurn.setColorFilter(turnIconColor);
        }
        refreshTurnBlockStyle();
    }

    private void setDividerColor(int viewId, int color) {
        View divider = floatingView.findViewById(viewId);
        if (divider != null) divider.setBackgroundColor(color);
    }

    @Override
    public void resetToDefaultTextColors() {
        if (tvPanelRoad != null) tvPanelRoad.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelDistNum != null) tvPanelDistNum.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelDistUnit != null) tvPanelDistUnit.setTextColor(TEXT_SECONDARY_DARK);
        if (tvPanelSpeed != null) tvPanelSpeed.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelLimit != null) tvPanelLimit.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelRemain != null) tvPanelRemain.setTextColor(TEXT_PRIMARY_DARK);
        if (tvPanelLimitLabel != null) tvPanelLimitLabel.setTextColor(TEXT_SECONDARY_DARK);
        if (tvPanelEta != null) tvPanelEta.setTextColor(TEXT_SECONDARY_DARK);
        if (tvPanelRemainUnit != null) tvPanelRemainUnit.setTextColor(TEXT_SECONDARY_DARK);
        if (laneLineView != null) laneLineView.setIconColor(0xFFFFFFFF);
        setDividerColor(R.id.v_panel_divider_top, 0x12FFFFFF);
        setDividerColor(R.id.v_panel_divider_kpi1, 0x12FFFFFF);
        setDividerColor(R.id.v_panel_divider_kpi2, 0x12FFFFFF);
        setDividerColor(R.id.v_panel_light_divider, 0x12FFFFFF);
        if (ivPanelTurn != null) ivPanelTurn.clearColorFilter();
        refreshTurnBlockStyle();
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ivPanelTurn != null) {
            ObjectAnimator animator = (ObjectAnimator) ivPanelTurn.getTag();
            if (animator != null) {
                animator.cancel();
                ivPanelTurn.setTag(null);
            }
            ivPanelTurn.setAlpha(1f);
        }
    }
}
