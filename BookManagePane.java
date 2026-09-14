package com.bookms.pane;

import com.bookms.dao.BookDAOImpl;
import com.bookms.entity.Book;
import com.bookms.frame.MainFrame;
import com.bookms.util.SerializationUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 图书管理面板：增删改查 + 序列化导出。 */
public class BookManagePane extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MainFrame owner;
    private final BookDAOImpl bookDAO = new BookDAOImpl();

    private final String[] COLUMNS = {"ID", "ISBN", "书名", "作者", "类别", "出版社", "价格", "库存", "状态"};
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private final JComboBox<String> cbCategory = new JComboBox<>(new String[]{"全部", "计算机", "文学", "历史", "科学"});
    private final JTextField tfTitleFilter = new JTextField(15);

    private final JTextField tfIsbn      = new JTextField(15);
    private final JTextField tfTitle     = new JTextField(15);
    private final JTextField tfAuthor    = new JTextField(15);
    private final JTextField tfCategory  = new JTextField(15);
    private final JTextField tfPublisher = new JTextField(15);
    private final JTextField tfPrice     = new JTextField(15);
    private final JTextField tfStock     = new JTextField(15);

    public BookManagePane(MainFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        refreshTable(bookDAO.getList());
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("筛选类别："));
        top.add(cbCategory);
        top.add(new JLabel("  模糊查书名："));
        top.add(tfTitleFilter);
        JButton btnQuery = new JButton("查询");
        JButton btnRefresh = new JButton("刷新");
        JButton btnExport = new JButton("序列化导出 (.dat)");
        top.add(btnQuery);
        top.add(btnRefresh);
        top.add(btnExport);

        btnQuery.addActionListener(e -> doQuery());
        btnRefresh.addActionListener(e -> {
            cbCategory.setSelectedIndex(0);
            tfTitleFilter.setText("");
            refreshTable(bookDAO.getList());
        });
        btnExport.addActionListener(e -> doExport());
        return top;
    }

    private JScrollPane buildTablePanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        // 点击行 → 回填表单
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) fillForm(row);
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(1000, 380));
        return sp;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addFormRow(form, c, row++, "ISBN：", tfIsbn);
        addFormRow(form, c, row++, "书名：", tfTitle);
        addFormRow(form, c, row++, "作者：", tfAuthor);
        addFormRow(form, c, row++, "类别：", tfCategory);
        addFormRow(form, c, row++, "出版社：", tfPublisher);
        addFormRow(form, c, row++, "价格：", tfPrice);
        addFormRow(form, c, row++, "库存：", tfStock);

        JButton btnAdd    = new JButton("新增");
        JButton btnUpdate = new JButton("更新");
        JButton btnDelete = new JButton("删除");
        JButton btnClear  = new JButton("清空表单");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(btnClear, c);
        c.gridx = 2;
        form.add(btnAdd, c);
        form.add(btnUpdate, c);
        form.add(btnDelete, c);

        btnAdd.addActionListener(e -> doAdd());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnClear.addActionListener(e -> clearForm());

        return form;
    }

    private void addFormRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridwidth = 1;
        c.gridy = row;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
    }

    // ---------- 行为 ----------

    private void doQuery() {
        String category = (String) cbCategory.getSelectedItem();
        String title = tfTitleFilter.getText().trim();
        List<Book> all = bookDAO.getList();
        List<Book> filtered = new ArrayList<>();
        for (Book b : all) {
            boolean catOk = "全部".equals(category) || (b.getCategory() != null && b.getCategory().equals(category));
            boolean titleOk = title.isEmpty() || (b.getTitle() != null && b.getTitle().contains(title));
            if (catOk && titleOk) filtered.add(b);
        }
        refreshTable(filtered);
    }

    private void refreshTable(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                    b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getCategory(),
                    b.getPublisher(), b.getPrice(), b.getStock(), b.getStatus()
            });
        }
    }

    private void fillForm(int row) {
        tfIsbn.setText((String) tableModel.getValueAt(row, 1));
        tfTitle.setText((String) tableModel.getValueAt(row, 2));
        tfAuthor.setText((String) tableModel.getValueAt(row, 3));
        tfCategory.setText((String) tableModel.getValueAt(row, 4));
        tfPublisher.setText((String) tableModel.getValueAt(row, 5));
        Object price = tableModel.getValueAt(row, 6);
        tfPrice.setText(price == null ? "" : price.toString());
        tfStock.setText(String.valueOf(tableModel.getValueAt(row, 7)));
    }

    private void clearForm() {
        tfIsbn.setText(""); tfTitle.setText(""); tfAuthor.setText("");
        tfCategory.setText(""); tfPublisher.setText(""); tfPrice.setText(""); tfStock.setText("");
        table.clearSelection();
    }

    private Book readForm() {
        Book b = new Book();
        b.setIsbn(tfIsbn.getText().trim());
        b.setTitle(tfTitle.getText().trim());
        b.setAuthor(tfAuthor.getText().trim());
        b.setCategory(tfCategory.getText().trim());
        b.setPublisher(tfPublisher.getText().trim());
        try { b.setPrice(new BigDecimal(tfPrice.getText().trim())); } catch (NumberFormatException e) { b.setPrice(BigDecimal.ZERO); }
        try { b.setStock(Integer.parseInt(tfStock.getText().trim())); } catch (NumberFormatException e) { b.setStock(0); }
        b.setStatus("在架");
        return b;
    }

    private void doAdd() {
        if (tfIsbn.getText().isEmpty() || tfTitle.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "ISBN 和书名不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = bookDAO.save(readForm());
        JOptionPane.showMessageDialog(this, ok ? "新增成功" : "新增失败（检查 DB）", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) {
            refreshTable(bookDAO.getList());
            clearForm();
        }
    }

    private void doUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中要更新的行", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Book b = readForm();
        b.setId(id);
        boolean ok = bookDAO.update(b);
        JOptionPane.showMessageDialog(this, ok ? "更新成功" : "更新失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) refreshTable(bookDAO.getList());
    }

    private void doDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中要删除的行", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        int r = JOptionPane.showConfirmDialog(this, "确认删除 ID=" + id + "？", "确认", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        boolean ok = bookDAO.delete(id);
        JOptionPane.showMessageDialog(this, ok ? "删除成功" : "删除失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) {
            refreshTable(bookDAO.getList());
            clearForm();
        }
    }

    /** 序列化导出当前列表的所有 Book 到 .dat 文件（演示 ObjectOutputStream）。 */
    private void doExport() {
        List<Book> all = bookDAO.getList();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "当前没有图书数据", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("books.dat"));
        int r = fc.showSaveDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            int n = SerializationUtil.serializeBooks(all, f);
            JOptionPane.showMessageDialog(this,
                    "序列化导出成功！共 " + n + " 条 → " + f.getAbsolutePath() + "\n\n" +
                    "提示：可用 SerializationUtil.deserializeBooks() 读回。",
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}