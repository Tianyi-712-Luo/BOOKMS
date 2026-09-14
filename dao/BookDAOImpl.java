package com.bookms.dao;

import com.bookms.entity.Book;
import com.bookms.util.JDBCConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Book 实体 DAO。 */
public class BookDAOImpl implements IBaseDAO<Book> {

    @Override
    public List<Book> getList() {
        return getListByCategory(null);
    }

    /** 按类别筛选（null = 全部）。 */
    public List<Book> getListByCategory(String category) {
        List<Book> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT id, isbn, title, author, category, publisher, price, stock, status FROM book");
        if (category != null && !category.isEmpty()) {
            sb.append(" WHERE category = ?");
        }
        sb.append(" ORDER BY id ASC");
        String sql = sb.toString();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            if (category != null && !category.isEmpty()) {
                ps.setString(1, category);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Book b = new Book();
                b.setId(rs.getInt("id"));
                b.setIsbn(rs.getString("isbn"));
                b.setTitle(rs.getString("title"));
                b.setAuthor(rs.getString("author"));
                b.setCategory(rs.getString("category"));
                b.setPublisher(rs.getString("publisher"));
                b.setPrice(BigDecimal.valueOf(rs.getDouble("price")));
                b.setStock(rs.getInt("stock"));
                b.setStatus(rs.getString("status"));
                list.add(b);
            }
        } catch (SQLException e) {
            System.err.println("[BookDAOImpl] getList 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return list;
    }

    @Override
    public Book findById(int id) {
        String sql = "SELECT id, isbn, title, author, category, publisher, price, stock, status FROM book WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                Book b = new Book();
                b.setId(rs.getInt("id"));
                b.setIsbn(rs.getString("isbn"));
                b.setTitle(rs.getString("title"));
                b.setAuthor(rs.getString("author"));
                b.setCategory(rs.getString("category"));
                b.setPublisher(rs.getString("publisher"));
                b.setPrice(BigDecimal.valueOf(rs.getDouble("price")));
                b.setStock(rs.getInt("stock"));
                b.setStatus(rs.getString("status"));
                return b;
            }
        } catch (SQLException e) {
            System.err.println("[BookDAOImpl] findById 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return null;
    }

    @Override
    public boolean save(Book b) {
        String sql = "INSERT INTO book(isbn, title, author, category, publisher, price, stock, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, b.getIsbn());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getCategory());
            ps.setString(5, b.getPublisher());
            ps.setBigDecimal(6, b.getPrice());
            ps.setInt(7, b.getStock());
            ps.setString(8, b.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BookDAOImpl] save 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean update(Book b) {
        String sql = "UPDATE book SET isbn=?, title=?, author=?, category=?, publisher=?, price=?, stock=?, status=? WHERE id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, b.getIsbn());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getCategory());
            ps.setString(5, b.getPublisher());
            ps.setBigDecimal(6, b.getPrice());
            ps.setInt(7, b.getStock());
            ps.setString(8, b.getStatus());
            ps.setInt(9, b.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BookDAOImpl] update 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM book WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[BookDAOImpl] delete 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }
}