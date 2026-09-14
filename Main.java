package com.bookms;

import com.bookms.frame.LoginFrame;
import com.bookms.util.EntityFactory;

import javax.swing.SwingUtilities;

/**
 * 系统入口：先加载反射映射 → 启动登录窗。
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // 1) 反射映射配置
        EntityFactory.loadDefault();

        // 2) 启动 GUI（事件派发线程）
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}