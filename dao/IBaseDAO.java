package com.bookms.dao;

import java.util.List;

/**
 * 泛型 DAO 接口：所有实体 DAO 都继承此接口，统一 CRUD。
 *
 * <pre>
 * public class BookDAOImpl implements IBaseDAO&lt;Book&gt; {
 *     public List&lt;Book&gt; getList() { ... }
 *     public Book findById(int id) { ... }
 *     public boolean save(Book t) { ... }
 *     public boolean update(Book t) { ... }
 *     public boolean delete(int id) { ... }
 * }
 * </pre>
 *
 * @param <T> 实体类型
 */
public interface IBaseDAO<T> {

    /** 返回该表全部记录（按 id 升序）。 */
    List<T> getList();

    /** 按主键查一条。 */
    T findById(int id);

    /** 新增一条，返回是否成功。 */
    boolean save(T t);

    /** 更新一条（按主键），返回是否成功。 */
    boolean update(T t);

    /** 按主键删除一条，返回是否成功。 */
    boolean delete(int id);
}