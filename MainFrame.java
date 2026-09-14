package com.bookms.frame;

import com.bookms.entity.User;
import com.bookms.pane.BookManagePane;
import com.bookms.pane.BorrowManagePane;
import com.bookms.pane.ReaderManagePane;
import com.bookms.pane.ReportPane;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 系统主窗（参照教材第 9 章 MainFrame.java 风格）。
 *
 * <p>顶部 JMenuBar；中部 CardLayout 切换面板；底部状态栏显示当前用户。</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final User currentUser;
    private final CardLayout cards = new CardLayout();
    private final JPanel  cardPanel = new JPanel(cards);
    private final JLabel  statusBar = new JLabel(" ", SwingConstants.LEFT);

    public MainFrame(User user) {
        this.currentUser = user;

        setTitle("BookMS 图书管理系统 v1.0");
        setSize(1024, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 顶部欢迎条
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel title = new JLabel("欢迎使用 BookMS 图书管理系统");
        title.setFont(new Font(Font.SERIF, Font.BOLD, 22));
        title.setForeground(new Color(0x2c, 0x3e, 0x50));
        header.add(title);

        // 菜单
        JMenuBar bar = new JMenuBar();

        JMenu bookMenu    = new JMenu("图书管理");
        JMenu readerMenu  = new JMenu("读者管理");
        JMenu borrowMenu  = new JMenu("借还管理");
        JMenu reportMenu  = new JMenu("报表/网络");
        JMenu toolsMenu   = new JMenu("工具");
        JMenu aboutMenu   = new JMenu("关于");

        JMenuItem miBookManage    = new JMenuItem("图书增删改查");
        JMenuItem miBookExport    = new JMenuItem("序列化导出图书");

        JMenuItem miReaderManage  = new JMenuItem("读者增删改查");

        JMenuItem miBorrowList    = new JMenuItem("借阅记录");
        JMenuItem miBorrowNew     = new JMenuItem("借书/还书");
        JMenuItem miReportLocal   = new JMenuItem("本地统计报表");
        JMenuItem miReportSend    = new JMenuItem("上报服务端（TCP）");

        JMenuItem miReflect       = new JMenuItem("反射探查类结构");

        JMenuItem miAbout         = new JMenuItem("关于 BookMS");

        bookMenu.add(miBookManage);
        bookMenu.add(miBookExport);
        readerMenu.add(miReaderManage);
        borrowMenu.add(miBorrowList);
        borrowMenu.add(miBorrowNew);
        reportMenu.add(miReportLocal);
        reportMenu.add(miReportSend);
        toolsMenu.add(miReflect);
        aboutMenu.add(miAbout);

        bar.add(bookMenu);
        bar.add(readerMenu);
        bar.add(borrowMenu);
        bar.add(reportMenu);
        bar.add(toolsMenu);
        bar.add(aboutMenu);
        setJMenuBar(bar);

        // 卡片布局：4 张面板
        cardPanel.add(new BookManagePane(this),     "book");
        cardPanel.add(new ReaderManagePane(this),   "reader");
        cardPanel.add(new BorrowManagePane(this),   "borrow");
        cardPanel.add(new ReportPane(this),         "report");

        // 底部状态栏
        statusBar.setText(String.format(" 当前用户：%s   |   时间：%s",
                user.getUsername(),
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())));

        // 装配
        add(header, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // 默认显示图书面板
        cards.show(cardPanel, "book");

        // 菜单事件
        miBookManage.addActionListener(e -> { showCard("book");   statusBar.setText("切换到：图书管理"); });
        miBookExport.addActionListener(e -> showCard("book"));
        miReaderManage.addActionListener(e -> { showCard("reader"); statusBar.setText("切换到：读者管理"); });
        miBorrowList.addActionListener(e -> { showCard("borrow"); statusBar.setText("切换到：借还管理"); });
        miBorrowNew.addActionListener(e ->  { showCard("borrow"); statusBar.setText("切换到：借还管理"); });
        miReportLocal.addActionListener(e ->  { showCard("report"); statusBar.setText("切换到：本地统计报表"); });
        miReportSend.addActionListener(e ->   { showCard("report"); statusBar.setText("切换到：上报服务端"); });
        // 反射演示（包了 try/catch 因为主方法声明 throws）
        miReflect.addActionListener(e -> {
            try {
                com.bookms.reflection.ReflectionDemo.main(new String[]{});
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "反射演示异常: " + ex.getMessage(),
                        "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        miAbout.addActionListener(e -> javax.swing.JOptionPane.showMessageDialog(this,
                "BookMS 图书管理系统\n仿写自《Java 高级程序设计实战教程》第 9 章\n小组 4 人协作完成",
                "关于", javax.swing.JOptionPane.INFORMATION_MESSAGE));

        setVisible(true);
    }

    private void showCard(String name) {
        cards.show(cardPanel, name);
    }

    public User getCurrentUser() { return currentUser; }

    public static void main(String[] args) {
        // 自检：跳登录直接打开主窗（方便看 GUI 效果）
        javax.swing.SwingUtilities.invokeLater(() -> {
            User test = new User(0, "preview", "preview");
            new MainFrame(test);
        });
    }
}