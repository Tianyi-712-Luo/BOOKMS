package com.bookms.dao;

import com.bookms.entity.User;
import com.bookms.util.JDBCConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** User 实体 DAO。 */
public class UserDAOImpl implements IBaseDAO<User> {

    @Override
    public List<User> getList() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password FROM user ORDER BY id ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                list.add(u);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] getList 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return list;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT id, username, password FROM user WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] findById 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return null;
    }

    /** 按用户名+密码查一条（登录校验）。 */
    public User findByLogin(String username, String password) {
        String sql = "SELECT id, username, password FROM user WHERE username = ? AND password = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] findByLogin 失败: " + e.getMessage());
        } finally {
            JDBCConnection.close(conn, ps, rs);
        }
        return null;
    }

    @Override
    public boolean save(User u) {
        String sql = "INSERT INTO user(username, password) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] save 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean update(User u) {
        String sql = "UPDATE user SET username = ?, password = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setInt(3, u.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] update 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = JDBCConnection.getConn();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAOImpl] delete 失败: " + e.getMessage());
            return false;
        } finally {
            JDBCConnection.close(conn, ps);
        }
    }
}