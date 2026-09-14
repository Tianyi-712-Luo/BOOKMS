package com.bookms.pane;

import com.bookms.dao.ReaderDAOImpl;
import com.bookms.entity.Reader;
import com.bookms.frame.MainFrame;

import javax.swing.JButton;
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
import java.util.List;

/** 读者管理面板：增删改查。 */
public class ReaderManagePane extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MainFrame owner;
    private final ReaderDAOImpl readerDAO = new ReaderDAOImpl();

    private final String[] COLUMNS = {"ID", "借书证号", "姓名", "性别", "电话", "邮箱"};
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private final JTextField tfCardno = new JTextField(15);
    private final JTextField tfName   = new JTextField(15);
    private final JTextField tfSex    = new JTextField(15);
    private final JTextField tfTel    = new JTextField(15);
    private final JTextField tfEmail  = new JTextField(15);

    public ReaderManagePane(MainFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("刷新");
        top.add(btnRefresh);
        btnRefresh.addActionListener(e -> refreshTable(readerDAO.getList()));
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0) fillForm(row);
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(1000, 380));
        add(sp, BorderLayout.CENTER);

        JPanel form = buildForm();
        add(form, BorderLayout.SOUTH);

        refreshTable(readerDAO.getList());
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, c, row++, "借书证号：", tfCardno);
        addRow(form, c, row++, "姓名：",   tfName);
        addRow(form, c, row++, "性别：",   tfSex);
        addRow(form, c, row++, "电话：",   tfTel);
        addRow(form, c, row++, "邮箱：",   tfEmail);

        JButton btnAdd    = new JButton("新增");
        JButton btnUpdate = new JButton("更新");
        JButton btnDelete = new JButton("删除");
        JButton btnClear  = new JButton("清空");
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

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridwidth = 1;
        c.gridy = row;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
    }

    private void refreshTable(List<Reader> list) {
        tableModel.setRowCount(0);
        for (Reader r : list) {
            tableModel.addRow(new Object[]{r.getId(), r.getCardno(), r.getName(), r.getSex(), r.getTel(), r.getEmail()});
        }
    }

    private void fillForm(int row) {
        tfCardno.setText((String) tableModel.getValueAt(row, 1));
        tfName.setText((String)   tableModel.getValueAt(row, 2));
        tfSex.setText((String)    tableModel.getValueAt(row, 3));
        tfTel.setText((String)    tableModel.getValueAt(row, 4));
        tfEmail.setText((String)  tableModel.getValueAt(row, 5));
    }

    private void clearForm() {
        tfCardno.setText(""); tfName.setText(""); tfSex.setText("");
        tfTel.setText(""); tfEmail.setText("");
        table.clearSelection();
    }

    private Reader readForm() {
        Reader r = new Reader();
        r.setCardno(tfCardno.getText().trim());
        r.setName(tfName.getText().trim());
        r.setSex(tfSex.getText().trim());
        r.setTel(tfTel.getText().trim());
        r.setEmail(tfEmail.getText().trim());
        return r;
    }

    private void doAdd() {
        if (tfCardno.getText().isEmpty() || tfName.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "借书证号和姓名不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean ok = readerDAO.save(readForm());
        JOptionPane.showMessageDialog(this, ok ? "新增成功" : "新增失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) { refreshTable(readerDAO.getList()); clearForm(); }
    }

    private void doUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选中要更新的行"); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        Reader r = readForm();
        r.setId(id);
        boolean ok = readerDAO.update(r);
        JOptionPane.showMessageDialog(this, ok ? "更新成功" : "更新失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) refreshTable(readerDAO.getList());
    }

    private void doDelete() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选中要删除的行"); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        int r = JOptionPane.showConfirmDialog(this, "确认删除 ID=" + id + "？", "确认", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        boolean ok = readerDAO.delete(id);
        JOptionPane.showMessageDialog(this, ok ? "删除成功" : "删除失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) { refreshTable(readerDAO.getList()); clearForm(); }
    }
}