package com.bookms.dao;

import com.bookms.entity.BorrowRecord;
import com.bookms.util.JDBCConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * BorrowRecord 实体 DAO。
 *
 * <p>借书 / 还书是核心流程，事务性要求高：
 * borrow() 在一个事务里同时 INSERT borrow + UPDATE book.status + UPDATE book.stock；
 * returnBook() 同样在一个事务里 UPDATE borrow.status + UPDATE book.status + UPDATE book.stock。
 * </p>
 */
public class BorrowDAOImpl implements IBaseDAO<BorrowRecord> {

    @Override
    public List<BorrowRecord> getList() {
        return findAll(null);
    }

    /** 按状态筛选（null = 全部；"在借" / "已还"）。 */
    public List<BorrowRecord> findAll(String status) {
        List<BorrowRecord> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT id, readerid, bookid, borrowdate, returndate, status FROM borrow");
        if (status != null && !status.isEmpty()) sb.append(" WHERE status = ?");
        sb.append(" ORDER BY id ASC");
        String sql = sb.toString();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            if (status != null && !status.isEmpty()) ps.setString(1, status);
            rs = ps.executeQuery();
            while (rs.next()) {
                BorrowRecord br = new BorrowRecord();
                br.setId(rs.getInt("id"));
                br.setReaderId(rs.getInt("readerid"));
                br.setBookId(rs.getInt("bookid"));
                br.setBorrowDate(rs.getTimestamp("borrowdate"));
                br.setReturnDate(rs.getTimestamp("returndate"));
                br.setStatus(rs.getString("status"));
                list.add(br);
            }
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl] findAll 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return list;
    }

    @Override
    public BorrowRecord findById(int id) {
        String sql = "SELECT id, readerid, bookid, borrowdate, returndate, status FROM borrow WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                BorrowRecord br = new BorrowRecord();
                br.setId(rs.getInt("id"));
                br.setReaderId(rs.getInt("readerid"));
                br.setBookId(rs.getInt("bookid"));
                br.setBorrowDate(rs.getTimestamp("borrowdate"));
                br.setReturnDate(rs.getTimestamp("returndate"));
                br.setStatus(rs.getString("status"));
                return br;
            }
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl] findById 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return null;
    }

    /**
     * 借书事务：插入 borrow + 减库存 + 改状态。
     * 失败任意一步回滚。
     *
     * @return true 借成功；false 失败（库存不足 / 书不存在 / 已被借出）
     */
    public boolean borrowBook(int readerId, int bookId) {
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;
        boolean ok = false;
        try {
            conn = JDBCConnection.getConn();
            conn.setAutoCommit(false);

            // 1) 查图书当前状态 + 库存
            psCheck = conn.prepareStatement(
                    "SELECT stock, status FROM book WHERE id = ? FOR UPDATE");
            psCheck.setInt(1, bookId);
            rs = psCheck.executeQuery();
            if (!rs.next()) {
                System.err.println("[BorrowDAOImpl.borrowBook] 图书不存在 id=" + bookId);
                conn.rollback();
                return false;
            }
            int stock = rs.getInt("stock");
            String status = rs.getString("status");
            if (stock <= 0) {
                System.err.println("[BorrowDAOImpl.borrowBook] 库存不足");
                conn.rollback();
                return false;
            }
            if ("借出".equals(status)) {
                System.err.println("[BorrowDAOImpl.borrowBook] 图书已被借出");
                conn.rollback();
                return false;
            }

            // 2) INSERT borrow
            psInsert = conn.prepareStatement(
                    "INSERT INTO borrow(readerid, bookid, borrowdate, status) VALUES (?, ?, ?, '在借')");
            psInsert.setInt(1, readerId);
            psInsert.setInt(2, bookId);
            psInsert.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
            psInsert.executeUpdate();

            // 3) UPDATE book：库存 -1，状态 借出
            psUpdate = conn.prepareStatement(
                    "UPDATE book SET stock = stock - 1, status = '借出' WHERE id = ?");
            psUpdate.setInt(1, bookId);
            psUpdate.executeUpdate();

            conn.commit();
            ok = true;
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl.borrowBook] 失败: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            JDBCConnection.close(conn, psUpdate, null);
            JDBCConnection.close(null, psInsert, null);
            JDBCConnection.close(null, psCheck, rs);
        }
        return ok;
    }

    /**
     * 还书事务：更新 borrow.returndate + 改 borrow.status + 加库存 + 改状态 在架。
     */
    public boolean returnBook(int borrowId) {
        Connection conn = null;
        PreparedStatement psFind = null;
        PreparedStatement psUpdateBorrow = null;
        PreparedStatement psUpdateBook = null;
        ResultSet rs = null;
        boolean ok = false;
        try {
            conn = JDBCConnection.getConn();
            conn.setAutoCommit(false);

            // 1) 查 borrow 记录
            psFind = conn.prepareStatement(
                    "SELECT bookid, status FROM borrow WHERE id = ? FOR UPDATE");
            psFind.setInt(1, borrowId);
            rs = psFind.executeQuery();
            if (!rs.next()) {
                System.err.println("[BorrowDAOImpl.returnBook] 借阅记录不存在 id=" + borrowId);
                conn.rollback();
                return false;
            }
            String status = rs.getString("status");
            int bookId = rs.getInt("bookid");
            if ("已还".equals(status)) {
                System.err.println("[BorrowDAOImpl.returnBook] 记录已归还");
                conn.rollback();
                return false;
            }

            // 2) UPDATE borrow：填 returndate + 改状态
            psUpdateBorrow = conn.prepareStatement(
                    "UPDATE borrow SET returndate = ?, status = '已还' WHERE id = ?");
            psUpdateBorrow.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
            psUpdateBorrow.setInt(2, borrowId);
            psUpdateBorrow.executeUpdate();

            // 3) UPDATE book：库存 +1，状态 在架
            psUpdateBook = conn.prepareStatement(
                    "UPDATE book SET stock = stock + 1, status = '在架' WHERE id = ?");
            psUpdateBook.setInt(1, bookId);
            psUpdateBook.executeUpdate();

            conn.commit();
            ok = true;
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl.returnBook] 失败: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            JDBCConnection.close(conn, psUpdateBook, null);
            JDBCConnection.close(null, psUpdateBorrow, null);
            JDBCConnection.close(null, psFind, rs);
        }
        return ok;
    }

    @Override
    public boolean save(BorrowRecord br) {
        // 直接 INSERT 借用记录（不更新 book 表，谨慎使用）
        String sql = "INSERT INTO borrow(readerid, bookid, borrowdate, returndate, status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, br.getReaderId());
            ps.setInt(2, br.getBookId());
            ps.setTimestamp(3, br.getBorrowDate() == null ? null : new java.sql.Timestamp(br.getBorrowDate().getTime()));
            ps.setTimestamp(4, br.getReturnDate() == null ? null : new java.sql.Timestamp(br.getReturnDate().getTime()));
            ps.setString(5, br.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl] save 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean update(BorrowRecord br) {
        String sql = "UPDATE borrow SET readerid=?, bookid=?, borrowdate=?, returndate=?, status=? WHERE id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, br.getReaderId());
            ps.setInt(2, br.getBookId());
            ps.setTimestamp(3, br.getBorrowDate() == null ? null : new java.sql.Timestamp(br.getBorrowDate().getTime()));
            ps.setTimestamp(4, br.getReturnDate() == null ? null : new java.sql.Timestamp(br.getReturnDate().getTime()));
            ps.setString(5, br.getStatus());
            ps.setInt(6, br.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl] update 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM borrow WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BorrowDAOImpl] delete 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }
}