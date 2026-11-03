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
import android.view.View;

/**
 * 窗口工厂：按样式编码从 {@link WindowStyles} 注册表取描述符并创建实例。
 * 新增样式不需要改动本类。
 */
public class FloatingWindowFactory {
    public static BaseFloatingWindow createWindow(int currentMode, int styleMode, Context context, View floatingView) {
        boolean isNavi = currentMode == FloatingWindowManager.MODE_NAVI;
        return WindowStyles.byId(styleMode).create(isNavi, context, floatingView);
    }
}
