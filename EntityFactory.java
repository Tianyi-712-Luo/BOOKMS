package com.bookms.util;

import com.bookms.entity.Book;
import com.bookms.entity.BorrowRecord;
import com.bookms.entity.Reader;
import com.bookms.entity.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * 反射 + 配置文件 动态创建实体对象（反射技术点）。
 *
 * <p>思路：src/main/resources/entities.properties 里登记「表名 → 类全限定名」，
 * 业务侧只需传 tableName，工厂内部反射 Class.forName().newInstance()。</p>
 *
 * <pre>
 * # entities.properties
 * user = com.bookms.entity.User
 * book = com.bookms.entity.Book
 * reader = com.bookms.entity.Reader
 * borrow = com.bookms.entity.BorrowRecord
 * </pre>
 */
public class EntityFactory {

    private static final String DEFAULT_RESOURCE = "/entities.properties";
    private static final Properties CACHE = new Properties();
    private static boolean loaded = false;

    /** 加载 classpath 下的 entities.properties（默认资源）。 */
    public static synchronized void loadDefault() throws IOException {
        if (loaded) return;
        try (InputStream in = EntityFactory.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("找不到资源 " + DEFAULT_RESOURCE);
            }
            CACHE.load(in);
            loaded = true;
        }
    }

    /** 直接从外部文件加载（GUI「加载自定义映射」功能用）。 */
    public static synchronized void loadFromFile(String path) throws IOException {
        try (InputStream in = new FileInputStream(path)) {
            CACHE.clear();
            CACHE.load(in);
            loaded = true;
        }
    }

    /**
     * 根据 tableName 创建实体（无参构造）。
     * 例：create("book") → new com.bookms.entity.Book()
     */
    public static <T> T create(String tableName) throws Exception {
        if (!loaded) loadDefault();
        String fqcn = CACHE.getProperty(tableName);
        if (fqcn == null) {
            throw new IllegalArgumentException("未登记的表名: " + tableName);
        }
        // 反射 Class.forName → getDeclaredConstructor().newInstance()
        Class<?> clazz = Class.forName(fqcn);
        @SuppressWarnings("unchecked")
        Class<T> tClass = (Class<T>) clazz;
        Constructor<T> ctor = tClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    /** 反射打印某个类的字段名 + 类型（探查类结构用）。 */
    public static void inspect(String fqcn) throws Exception {
        Class<?> clazz = Class.forName(fqcn);
        System.out.println("=== " + fqcn + " ===");
        java.lang.Class<?> sup = clazz.getSuperclass();
        System.out.println("super = " + (sup == null ? "null" : sup.getName()));
        System.out.println("fields:");
        for (Field f : clazz.getDeclaredFields()) {
            System.out.printf("  %-12s %s%n", f.getType().getSimpleName(), f.getName());
        }
        System.out.println("methods (public):");
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().startsWith("get") || m.getName().startsWith("set")
                    || m.getName().equals("toString")) {
                System.out.printf("  %s%n", m);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // 自检：用反射创建 4 个实体对象
        Object u = create("user");
        Object b = create("book");
        Object r = create("reader");
        Object br = create("borrow");
        System.out.println("[EntityFactory] 反射创建:");
        System.out.println("    " + u.getClass().getSimpleName() + " → " + u);
        System.out.println("    " + b.getClass().getSimpleName() + " → " + b);
        System.out.println("    " + r.getClass().getSimpleName() + " → " + r);
        System.out.println("    " + br.getClass().getSimpleName() + " → " + br);

        // 探查 Book
        System.out.println();
        inspect(Book.class.getName());
    }
}