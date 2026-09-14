package com.bookms.util;

import com.bookms.entity.Book;
import com.bookms.entity.BorrowRecord;
import com.bookms.entity.Reader;
import com.bookms.entity.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 序列化工具（演示用）：
 * - serializeBooks() 把 List<Book> 写到 .dat（ObjectOutputStream）
 * - deserializeBooks() 从 .dat 读回
 *
 * <p>同时也是「序列化」技术点的展示：所有实体都 implements Serializable。</p>
 */
public class SerializationUtil {

    /** 序列化 List<Book> 到指定文件。返回写入条数。 */
    public static int serializeBooks(List<Book> books, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(books);
            oos.flush();
            return books == null ? 0 : books.size();
        }
    }

    /** 反序列化 List<Book>。 */
    @SuppressWarnings("unchecked")
    public static List<Book> deserializeBooks(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Book>) ois.readObject();
        }
    }

    /** 序列化为字符串（用 Base64 编码，方便嵌入到 GUI 文本框演示）。 */
    public static String toBase64String(Object obj) throws IOException {
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    public static void main(String[] args) throws Exception {
        // 自检
        List<Book> books = new ArrayList<>();
        books.add(new Book("9787111213826", "Java核心技术卷I", "Cay S. Horstmann",
                "计算机", "机械工业出版社", new java.math.BigDecimal("119.00"), 5, "在架"));
        books.add(new Book("9787020002207", "红楼梦", "曹雪芹",
                "文学", "人民文学出版社", new java.math.BigDecimal("59.70"), 6, "在架"));

        File f = new File(System.getProperty("java.io.tmpdir"), "bookms_books_test.dat");
        int n = serializeBooks(books, f);
        System.out.println("[SerializationUtil] 已写入 " + n + " 条到 " + f);

        List<Book> readBack = deserializeBooks(f);
        System.out.println("[SerializationUtil] 读回 " + readBack.size() + " 条:");
        for (Book b : readBack) {
            System.out.println("    " + b);
        }

        // 其他三个实体也声明 Serializable，这里仅编一个自检用来验证类加载链路无 MissingClass
        User u = new User("admin", "admin");
        Reader r = new Reader("R2024001", "张三", "男", "13800001111", "zs@example.com");
        BorrowRecord br = new BorrowRecord(1, 1, new java.util.Date(), null, "在借");
        System.out.println("[SerializationUtil] 类链路 OK: " + u + " / " + r + " / " + br);
    }
}