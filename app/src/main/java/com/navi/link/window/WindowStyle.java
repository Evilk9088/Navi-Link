package com.navi.link.window;

import android.content.Context;
import android.view.View;

/**
 * 悬浮窗样式描述符。
 *
 * 一个样式的全部差异（名称、两套布局、缩放持久化 key、窗口圆角、窗口实现类）
 * 都收敛在这里；新增或删除样式只改 {@link WindowStyles} 的注册表，不需要再动
 * 工厂、管理器或设置页。
 */
public final class WindowStyle {

    /** 窗口实例构造器：isNavi 为 true 表示导航模式，否则巡航模式 */
    public interface Creator {
        BaseFloatingWindow create(boolean isNavi, Context context, View floatingView);
    }

    /** 样式编码，持久化在 style_mode / cruise_style_mode 中，必须保持稳定 */
    public final int id;
    /** 设置页主文案，如「常规」 */
    public final String name;
    /** 设置页副文案，如「经典窗口」 */
    public final String subtitle;
    public final int naviLayoutRes;
    public final int cruiseLayoutRes;
    /** 该样式独立记忆缩放值的偏好 key */
    public final String scaleKey;
    /** 窗口圆角（dp）。灵动岛是胶囊形 40dp，其余 12dp */
    public final int cornerDp;
    /** 是否走内容物理缩放（除「常规」外的导航样式都支持） */
    public final boolean scalable;

    private final Creator creator;

    public WindowStyle(int id,
                       String name,
                       String subtitle,
                       int naviLayoutRes,
                       int cruiseLayoutRes,
                       String scaleKey,
                       int cornerDp,
                       boolean scalable,
                       Creator creator) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.naviLayoutRes = naviLayoutRes;
        this.cruiseLayoutRes = cruiseLayoutRes;
        this.scaleKey = scaleKey;
        this.cornerDp = cornerDp;
        this.scalable = scalable;
        this.creator = creator;
    }

    public int layoutRes(boolean isNavi) {
        return isNavi ? naviLayoutRes : cruiseLayoutRes;
    }

    public BaseFloatingWindow create(boolean isNavi, Context context, View floatingView) {
        return creator.create(isNavi, context, floatingView);
    }
}
