package com.bookms.pane;

import com.bookms.dao.BookDAOImpl;
import com.bookms.dao.BorrowDAOImpl;
import com.bookms.dao.ReaderDAOImpl;
import com.bookms.entity.Book;
import com.bookms.entity.BorrowRecord;
import com.bookms.entity.Reader;
import com.bookms.frame.MainFrame;
import com.bookms.net.ReportClient;
import com.bookms.util.DateUtil;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 借还管理面板：借书 + 还书 + 上报服务端（TCP）。
 */
public class BorrowManagePane extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MainFrame owner;
    private final BorrowDAOImpl  borrowDAO  = new BorrowDAOImpl();
    private final BookDAOImpl    bookDAO    = new BookDAOImpl();
    private final ReaderDAOImpl  readerDAO  = new ReaderDAOImpl();

    private final String[] COLUMNS = {"借阅ID", "读者ID", "图书ID", "借出时间", "归还时间", "状态"};
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private final JComboBox<Reader> cbReader = new JComboBox<>();
    private final JComboBox<Book>   cbBook   = new JComboBox<>();
    private final JComboBox<String> cbFilter = new JComboBox<>(new String[]{"全部", "在借", "已还"});

    public BorrowManagePane(MainFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        // 顶部：读者/图书下拉 + 借/还按钮 + 筛选 + 刷新 + 上报
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("读者："));
        top.add(cbReader);
        top.add(new JLabel("  图书："));
        top.add(cbBook);
        JButton btnBorrow  = new JButton("借书");
        JButton btnReturn  = new JButton("还书");
        JButton btnRefresh = new JButton("刷新");
        top.add(btnBorrow);
        top.add(btnReturn);
        top.add(new JLabel("  筛选："));
        top.add(cbFilter);
        top.add(btnRefresh);
        add(top, BorderLayout.NORTH);

        // 中部：表格
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(1000, 360));
        add(sp, BorderLayout.CENTER);

        // 底部：上报 + 统计文本框
        JPanel bottom = buildBottomPanel();
        add(bottom, BorderLayout.SOUTH);

        // 事件
        btnBorrow.addActionListener(e -> doBorrow());
        btnReturn.addActionListener(e -> doReturn());
        btnRefresh.addActionListener(e -> refreshAll());
        cbFilter.addActionListener(e -> refreshTable(borrowDAO.findAll((String) cbFilter.getSelectedItem())));

        refreshAll();
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnReportSend = new JButton("上报借阅统计 → TCP 服务端");
        btnRow.add(btnReportSend);
        bottom.add(btnRow, BorderLayout.NORTH);

        JTextArea reportArea = new JTextArea(8, 60);
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.append(buildLocalReport());
        bottom.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        btnReportSend.addActionListener(e -> {
            List<String> lines = new ArrayList<>();
            String report = reportArea.getText();
            for (String l : report.split("\\n")) {
                if (!l.trim().isEmpty()) lines.add(l);
            }
            try {
                String ack = ReportClient.sendReport(lines, "127.0.0.1", ReportClient.DEFAULT_PORT, 3000);
                JOptionPane.showMessageDialog(this, "已上报！服务端 ACK：\n" + ack, "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "上报失败：\n" + ex.getMessage() +
                                "\n\n提示：先在另一终端运行 java com.bookms.net.ReportServer 启动服务端。",
                        "失败", JOptionPane.ERROR_MESSAGE);
            }
        });
        return bottom;
    }

    private void refreshAll() {
        // 刷新读者下拉
        DefaultComboBoxModel<Reader> rm = new DefaultComboBoxModel<>();
        for (Reader r : readerDAO.getList()) rm.addElement(r);
        cbReader.setModel(rm);
        // 刷新图书下拉（只显示在架的）
        DefaultComboBoxModel<Book> bm = new DefaultComboBoxModel<>();
        for (Book b : bookDAO.getList()) {
            if ("在架".equals(b.getStatus())) bm.addElement(b);
        }
        cbBook.setModel(bm);
        // 刷新表格
        cbFilter.setSelectedIndex(0);
        refreshTable(borrowDAO.getList());
    }

    private void refreshTable(List<BorrowRecord> records) {
        tableModel.setRowCount(0);
        for (BorrowRecord r : records) {
            tableModel.addRow(new Object[]{
                    r.getId(), r.getReaderId(), r.getBookId(),
                    DateUtil.format(r.getBorrowDate()),
                    r.getReturnDate() == null ? "" : DateUtil.format(r.getReturnDate()),
                    r.getStatus()
            });
        }
    }

    private void doBorrow() {
        Reader r = (Reader) cbReader.getSelectedItem();
        Book   b = (Book)   cbBook.getSelectedItem();
        if (r == null || b == null) {
            JOptionPane.showMessageDialog(this, "请先选择读者和图书", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = borrowDAO.borrowBook(r.getId(), b.getId());
        JOptionPane.showMessageDialog(this, ok ? "借书成功！" : "借书失败（库存不足 / 书已被借出 / DB 错误）",
                "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) refreshAll();
    }

    private void doReturn() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先在表格里选中一条要归还的记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 5);
        if ("已还".equals(status)) {
            JOptionPane.showMessageDialog(this, "该记录已归还，无需重复操作", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "确认还书？借阅 ID=" + id, "确认", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        boolean ok = borrowDAO.returnBook(id);
        JOptionPane.showMessageDialog(this, ok ? "还书成功！" : "还书失败", "结果",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) refreshAll();
    }

    /** 本地统计（按读者 + 状态分组）。 */
    private String buildLocalReport() {
        List<BorrowRecord> all = borrowDAO.getList();
        if (all.isEmpty()) return "[暂无借阅数据]\n";

        Map<Integer, Integer> borrowCount  = new HashMap<>();
        Map<Integer, Integer> readerIdName = new HashMap<>();
        Map<String, Integer>  statusCount  = new HashMap<>();
        for (BorrowRecord r : all) {
            borrowCount.merge(r.getReaderId(), 1, Integer::sum);
            readerIdName.put(r.getReaderId(), r.getReaderId());
            statusCount.merge(r.getStatus(), 1, Integer::sum);
        }
        int totalAll = all.size();
        int totalBorrowed = statusCount.getOrDefault("在借", 0);
        int totalReturned  = statusCount.getOrDefault("已还", 0);

        StringBuilder sb = new StringBuilder();
        sb.append("===== 借阅统计 (生成于 ").append(DateUtil.format(new Date())).append(") =====\n");
        sb.append("总记录: ").append(totalAll).append(" | 在借: ").append(totalBorrowed)
          .append(" | 已还: ").append(totalReturned).append('\n');
        sb.append("按读者 ID 分组:\n");
        for (Map.Entry<Integer, Integer> e : borrowCount.entrySet()) {
            sb.append("  读者 ").append(e.getKey()).append(": ").append(e.getValue()).append(" 次\n");
        }
        return sb.toString();
    }
}