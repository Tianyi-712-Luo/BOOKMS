package com.bookms.reflection;

import com.bookms.entity.Book;
import com.bookms.entity.Reader;
import com.bookms.util.EntityFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射技术点独立演示（控制台）：
 * - 用 EntityFactory 反射创建 Book / Reader
 * - 通过反射读写 Book 的字段（演示 setAccessible）
 * - 列出 Book 的所有 getter / setter
 *
 * <p>GUI 端也可以在主窗菜单「工具 → 反射探查」调用。</p>
 */
public class ReflectionDemo {

    public static void demoCreate() throws Exception {
        System.out.println("[ReflectionDemo] 用反射创建对象（EntityFactory）：");
        Book b = EntityFactory.create("book");
        b.setTitle("Java核心技术卷I");
        b.setAuthor("Cay S. Horstmann");
        b.setIsbn("9787111213826");
        b.setStock(5);
        b.setPrice(new java.math.BigDecimal("119.00"));
        b.setCategory("计算机");
        System.out.println("  创建后: " + b);

        Reader r = EntityFactory.create("reader");
        r.setName("张三");
        r.setCardno("R2024001");
        System.out.println("  创建后: " + r);
    }

    public static void demoFieldAccess() throws Exception {
        System.out.println("\n[ReflectionDemo] 反射读写字段（绕过 setter）：");
        Book b = new Book();
        Class<?> clazz = b.getClass();
        // title 是 private，必须 setAccessible(true)
        Field fTitle = clazz.getDeclaredField("title");
        fTitle.setAccessible(true);
        fTitle.set(b, "Java核心技术卷I（反射赋值）");
        System.out.println("  反射写入后: b.title = " + fTitle.get(b));
    }

    public static void demoListMethods() {
        System.out.println("\n[ReflectionDemo] Book 的 getter / setter 列表：");
        Class<?> clazz = Book.class;
        for (Method m : clazz.getDeclaredMethods()) {
            String name = m.getName();
            if (name.startsWith("get") || name.startsWith("set")) {
                System.out.printf("  %s -> %s%n", m, m.getReturnType().getSimpleName());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        demoCreate();
        demoFieldAccess();
        demoListMethods();
    }
}