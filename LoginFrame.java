package com.bookms.frame;

import com.bookms.dao.UserDAOImpl;
import com.bookms.entity.User;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 登录窗（参照教材第 9 章 LoginFrame.java 风格：GridBagLayout）。
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int WIDTH  = 300;
    static final int HEIGHT = 200;

    private JTextField  usernameInput;
    private JPasswordField passwordInput;

    public LoginFrame() {
        setTitle("欢迎进入 BookMS 图书管理系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // GridBagLayout：按网格布局
        GridBagLayout lay = new GridBagLayout();
        JPanel panel = new JPanel(lay);
        add(panel);

        JLabel title    = new JLabel("BookMS 图书管理系统");
        JLabel nameLbl  = new JLabel("用户名：");
        JLabel pwdLbl   = new JLabel("密  码：");
        usernameInput   = new JTextField(15);
        passwordInput   = new JPasswordField(15);
        JButton ok      = new JButton("登录");
        JButton cancel  = new JButton("取消");

        // GridBagConstraints 控制每个组件位置和占格
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.weighty = 1;

        // 第 0 行：标题（跨 2 列）
        c.gridwidth = 2;
        panel.add(title, c);
        // 第 1 行：用户名
        c.gridwidth = 1;
        panel.add(nameLbl, c);
        c.anchor = GridBagConstraints.WEST;
        panel.add(usernameInput, c);
        // 第 2 行：密码
        c.anchor = GridBagConstraints.EAST;
        panel.add(pwdLbl, c);
        c.anchor = GridBagConstraints.WEST;
        panel.add(passwordInput, c);
        // 第 3 行：按钮
        c.anchor = GridBagConstraints.CENTER;
        panel.add(ok, c);
        panel.add(cancel, c);

        // 居中显示
        setSize(WIDTH, HEIGHT);
        java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - WIDTH) / 2, (screen.height - HEIGHT) / 2);

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    /** 登录校验：调 UserDAOImpl.findByLogin()。 */
    private void login() {
        String u = usernameInput.getText().trim();
        String p = new String(passwordInput.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UserDAOImpl dao = new UserDAOImpl();
        User user = dao.findByLogin(u, p);
        if (user != null) {
            JOptionPane.showMessageDialog(this, "登录成功！欢迎 " + u, "成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            // 启动主窗
            new MainFrame(user);
        } else {
            JOptionPane.showMessageDialog(this, "用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // 自检：直接打开登录窗
        new LoginFrame();
    }
}