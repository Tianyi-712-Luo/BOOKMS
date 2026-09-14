package com.bookms.pane;

import com.bookms.dao.BookDAOImpl;
import com.bookms.dao.BorrowDAOImpl;
import com.bookms.dao.ReaderDAOImpl;
import com.bookms.entity.Book;
import com.bookms.entity.BorrowRecord;
import com.bookms.entity.Reader;
import com.bookms.frame.MainFrame;
import com.bookms.util.DateUtil;
import com.bookms.util.EntityFactory;
import com.bookms.util.SerializationUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表面板：展示本地统计 + 提供序列化/反射/上报按钮。
 */
public class ReportPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MainFrame owner;
    private final BorrowDAOImpl borrowDAO = new BorrowDAOImpl();
    private final BookDAOImpl   bookDAO   = new BookDAOImpl();
    private final ReaderDAOImpl readerDAO = new ReaderDAOImpl();
    private final JTextArea     reportArea = new JTextArea();

    public ReportPane(MainFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh     = new JButton("刷新统计");
        JButton btnExportBooks = new JButton("序列化导出图书");
        JButton btnInspectBook = new JButton("反射探查 Book");
        top.add(btnRefresh);
        top.add(btnExportBooks);
        top.add(btnInspectBook);
        add(top, BorderLayout.NORTH);

        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> rebuild());
        btnExportBooks.addActionListener(e -> exportBooks());
        btnInspectBook.addActionListener(e -> inspectBook());

        rebuild();
    }

    private void rebuild() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== BookMS 系统概览 =====\n");
        sb.append("生成时间: ").append(DateUtil.format(new java.util.Date())).append("\n\n");

        // 图书统计
        List<Book> books = bookDAO.getList();
        Map<String, Integer> catCount = new HashMap<>();
        for (Book b : books) catCount.merge(b.getCategory(), 1, Integer::sum);
        sb.append("[图书] 总数: ").append(books.size()).append('\n');
        for (Map.Entry<String, Integer> e : catCount.entrySet()) {
            sb.append("  - ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }

        // 读者统计
        List<Reader> readers = readerDAO.getList();
        sb.append("\n[读者] 总数: ").append(readers.size()).append('\n');

        // 借阅统计
        List<BorrowRecord> all = borrowDAO.getList();
        Map<String, Integer> statusCount = new HashMap<>();
        Map<Integer, Integer> readerBorrowCount = new HashMap<>();
        for (BorrowRecord r : all) {
            statusCount.merge(r.getStatus(), 1, Integer::sum);
            readerBorrowCount.merge(r.getReaderId(), 1, Integer::sum);
        }
        sb.append("\n[借阅] 总数: ").append(all.size()).append('\n');
        sb.append("  - 在借: ").append(statusCount.getOrDefault("在借", 0)).append('\n');
        sb.append("  - 已还: ").append(statusCount.getOrDefault("已还", 0)).append('\n');
        sb.append("按读者 ID 借阅次数:\n");
        for (Map.Entry<Integer, Integer> e : readerBorrowCount.entrySet()) {
            sb.append("  - 读者 ").append(e.getKey()).append(": ").append(e.getValue()).append(" 次\n");
        }

        reportArea.setText(sb.toString());
    }

    private void exportBooks() {
        List<Book> all = bookDAO.getList();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "无图书数据", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File f = new File("books.dat");
        try {
            int n = SerializationUtil.serializeBooks(all, f);
            JOptionPane.showMessageDialog(this, "序列化成功：共 " + n + " 条 → " + f.getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inspectBook() {
        StringBuilder sb = new StringBuilder();
        try {
            Book b = EntityFactory.create("book");
            b.setTitle("示例");
            sb.append("[反射] 用 EntityFactory.create(\"book\") → ").append(b).append('\n');
            EntityFactory.inspect(Book.class.getName());
        } catch (Exception ex) {
            sb.append("[反射] 失败: ").append(ex.getMessage());
        }
        reportArea.append("\n===== 反射探查 =====\n" + sb);
    }
}