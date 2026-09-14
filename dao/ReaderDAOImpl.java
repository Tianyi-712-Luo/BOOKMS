package com.bookms.dao;

import com.bookms.entity.Reader;
import com.bookms.util.JDBCConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reader 实体 DAO。 */
public class ReaderDAOImpl implements IBaseDAO<Reader> {

    @Override
    public List<Reader> getList() {
        List<Reader> result = new ArrayList<>();
        String sql = "SELECT id, cardno, name, sex, tel, email FROM reader ORDER BY id ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Reader r = new Reader();
                r.setId(rs.getInt("id"));
                r.setCardno(rs.getString("cardno"));
                r.setName(rs.getString("name"));
                r.setSex(rs.getString("sex"));
                r.setTel(rs.getString("tel"));
                r.setEmail(rs.getString("email"));
                result.add(r);
            }
        } catch (SQLException e) {
            System.err.println("[ReaderDAOImpl] getList 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return result;
    }

    @Override
    public Reader findById(int id) {
        String sql = "SELECT id, cardno, name, sex, tel, email FROM reader WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                Reader r = new Reader();
                r.setId(rs.getInt("id"));
                r.setCardno(rs.getString("cardno"));
                r.setName(rs.getString("name"));
                r.setSex(rs.getString("sex"));
                r.setTel(rs.getString("tel"));
                r.setEmail(rs.getString("email"));
                return r;
            }
        } catch (SQLException e) {
            System.err.println("[ReaderDAOImpl] findById 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return null;
    }

    @Override
    public boolean save(Reader r) {
        String sql = "INSERT INTO reader(cardno, name, sex, tel, email) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, r.getCardno());
            ps.setString(2, r.getName());
            ps.setString(3, r.getSex());
            ps.setString(4, r.getTel());
            ps.setString(5, r.getEmail());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReaderDAOImpl] save 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean update(Reader r) {
        String sql = "UPDATE reader SET cardno=?, name=?, sex=?, tel=?, email=? WHERE id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, r.getCardno());
            ps.setString(2, r.getName());
            ps.setString(3, r.getSex());
            ps.setString(4, r.getTel());
            ps.setString(5, r.getEmail());
            ps.setInt(6, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReaderDAOImpl] update 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM reader WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ReaderDAOImpl] delete 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }
}