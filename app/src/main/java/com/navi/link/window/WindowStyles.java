package com.navi.link.window;

import com.navi.link.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 悬浮窗样式注册表 —— 增加 / 删除样式唯一需要改的地方。
 *
 * <h3>新增一款样式</h3>
 * <ol>
 *   <li>新增导航与巡航两个 layout；</li>
 *   <li>新增窗口实现类（继承 {@link BaseFloatingWindow}）；</li>
 *   <li>在 {@link #REGISTRY} 里追加一条记录即可。工厂、窗口管理器的布局解析、
 *       缩放记忆、设置页卡片都会自动跟上。</li>
 * </ol>
 *
 * <h3>删除一款样式</h3>
 * 直接删掉对应记录。用户偏好里残留的旧编码会在
 * {@link FloatingWindowManager#loadPreferences()} 中被归一化为
 * {@link #DEFAULT_ID}，不会崩溃也不会空白。
 *
 * <p>注意：{@code id} 是持久化编码，已发布的编号不要复用给别的样式。
 */
public final class WindowStyles {

    /** 兜底样式编码（常规），未注册的编码一律回退到这里 */
    public static final int DEFAULT_ID = 0;

    private static final List<WindowStyle> REGISTRY;

    static {
        List<WindowStyle> list = new ArrayList<>();

        list.add(new WindowStyle(
                0, "常规", "经典导航窗口",
                R.layout.layout_floating_navi_normal,
                R.layout.layout_floating_cruise_normal,
                "scale_normal", 12, false,
                (isNavi, context, view) -> isNavi
                        ? new NormalNaviWindow(context, view)
                        : new NormalCruiseWindow(context, view)));

        list.add(new WindowStyle(
                1, "灵动岛", "精简岛风",
                R.layout.layout_floating_navi_minimal,
                R.layout.layout_floating_cruise_minimal,
                "scale_minimal", 40, true,
                (isNavi, context, view) -> isNavi
                        ? new MinimalNaviWindow(context, view)
                        : new MinimalCruiseWindow(context, view)));

        list.add(new WindowStyle(
                2, "全数据", "竖版完整",
                R.layout.layout_floating_navi_full,
                R.layout.layout_floating_cruise_full,
                "scale_full", 12, true,
                (isNavi, context, view) -> isNavi
                        ? new FullNaviWindow(context, view)
                        : new FullCruiseWindow(context, view)));

        list.add(new WindowStyle(
                3, "丝带", "分色丝带",
                R.layout.layout_floating_navi_ribbon,
                R.layout.layout_floating_cruise_ribbon,
                "scale_ribbon", 12, true,
                (isNavi, context, view) -> isNavi
                        ? new RibbonNaviWindow(context, view)
                        : new RibbonCruiseWindow(context, view)));

        list.add(new WindowStyle(
                4, "面板", "全数据面板",
                R.layout.layout_floating_navi_panel,
                R.layout.layout_floating_cruise_panel,
                "scale_panel", 12, true,
                (isNavi, context, view) -> isNavi
                        ? new PanelNaviWindow(context, view)
                        : new PanelCruiseWindow(context, view)));

        REGISTRY = Collections.unmodifiableList(list);
    }

    private WindowStyles() {
    }

    /** 全部已注册样式，顺序即设置页展示顺序 */
    public static List<WindowStyle> all() {
        return REGISTRY;
    }

    public static int count() {
        return REGISTRY.size();
    }

    /** 按编码取样式；编码未注册（例如该样式已被删除）时回退到默认样式 */
    public static WindowStyle byId(int id) {
        for (WindowStyle style : REGISTRY) {
            if (style.id == id) return style;
        }
        return REGISTRY.get(0);
    }

    /** 编码在注册表中的下标，用于缩放数组；未注册时回退到 0 */
    public static int indexOf(int id) {
        for (int i = 0; i < REGISTRY.size(); i++) {
            if (REGISTRY.get(i).id == id) return i;
        }
        return 0;
    }
}
