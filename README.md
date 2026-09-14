# BookMS 图书管理系统

> 仿写自《Java 高级程序设计实战教程》第 9 章「基于 C/S 架构的餐饮管理系统」，
> 业务自拟为「图书借阅管理」，覆盖 7 项核心技术中的 5 项：集合 + 序列化 + 反射 + 数据库 + 网络编程。
> 形态：Swing 桌面 GUI；DB：MySQL/MariaDB。



技术覆盖（实验要求 3-5 个 → 已覆盖 5 个）

| 技术 | 用在哪里 |
|---|---|
| **集合（ArrayList/HashMap/Vector）** | DAO 返回的 `List<T>`；DefaultTableModel 用 Vector；本地统计用 HashMap 分组 |
| **泛型（Generic）** | `IBaseDAO<T>` 统一 4 个实体的 CRUD 签名 |
| **反射（Reflection）** | `EntityFactory` 通过 `entities.properties` + `Class.forName().newInstance()` 动态建实体；`inspect()` 反射探查类结构 |
| **序列化（Serializable）** | 4 个实体 `implements Serializable`；`SerializationUtil` 用 `ObjectOutputStream` 导出图书到 `.dat` 文件 |
| **数据库（JDBC）** | `JDBCConnection` + 4 个 DAO 用 `PreparedStatement` 做 CRUD；借还流程用事务 + `FOR UPDATE` 行锁 |
| **网络编程（Socket TCP）** | `ReportServer` 监听 9999 端口；`ReportClient` 发借阅统计；服务端落盘 `report.log`；收发独立线程不阻塞 GUI |
| **多线程** | ReportServer 每个客户端连接独立线程 |
| **常用类** | `String.format`、`SimpleDateFormat`（DateUtil）、`BigDecimal`（价格）、`Math.ceil`（借出天数）|

---

环境准备（本机实测 2026-09-14 / Windows 11 / PowerShell）

| 组件 | 本机实际 | 备注 |
|---|---|---|
| JDK | OpenJDK 25.0.3（IntelliJ IDEA 2026.2.1 自带 JBR） | 项目要求 17+，满足；路径 `E:\IntelliJ IDEA 2026.2.1\jbr` |
| 构建 | 直接用 `javac` 编译 | **本机没装 Maven**，无 `mvn` 命令 |
| 数据库 | MySQL 9.7.2，服务名 `MySQL97`，状态 Running | 库 `bookms` 已建好 |
| 驱动 | `lib\mysql-connector-j-26.7.0.jar` | 已验证可加载 |

> ⚠️ 本机 PATH 里**没有** `java` / `javac` / `mvn`，直接敲会报「无法将"java"项识别为 cmdlet」。
> 本教程用 IDEA 自带 JBR 的**完整路径**；想永久解决可二选一：
> ```powershell
> # 方案一：装一个正式 JDK（推荐）
> # 方案二：把 JBR 配成 JAVA_HOME（重开终端生效）
> [Environment]::SetEnvironmentVariable("JAVA_HOME", "E:\IntelliJ IDEA 2026.2.1\jbr", "User")
> [Environment]::SetEnvironmentVariable("Path", $env:Path + ";E:\IntelliJ IDEA 2026.2.1\jbr\bin", "User")
> ```

---

逐步启动教程（照抄即可跑通，共 8 步）

> 全程用 **PowerShell**，每一步都有「预期结果」，对不上就停下看该步的排查提示。
> 换机器时只需改第 0 步的 `$j`（JDK 路径）和第 2 步的数据库口令。

### 第 0 步：打开终端，进入项目目录，设好 JDK 路径

```powershell
cd "c:\Users\32127\Desktop\BookMS\BookMS"
$j = "E:\IntelliJ IDEA 2026.2.1\jbr\bin"
& "$j\java.exe" -version
```

**预期结果**：

```
openjdk version "25.0.3" 2026-04-21
OpenJDK Runtime Environment JBR-25.0.3+9-508.16-nomodOpenJDK 运行时环境 JBR-25.0.3+9-508.16-nomod
```

> `$j` 只在当前窗口有效，**新开窗口要重新设**。后续所有命令都依赖它。

### 第 1 步：确认 MySQL 已启动

```powershell
Get-Service MySQL*
```

**预期结果**：

```
Status   Name     DisplayName
------   ----     -----------
Running  MySQL97  MySQL97
```

若不是 `Running`，用**管理员** PowerShell 启动：

```powershell
Start-Service MySQL97
```

> 服务名各机器可能不同（如 `MySQL80`、`MySQL97`），以上一步查出来的为准。

### 第 2 步：核对 JDBC 连接参数

打开 `src/com/bookms/util/JDBCConnection.java`：

```java
private static final String URL      = "jdbc:mysql://localhost:3306/bookms?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true";
private static final String USERNAME = "root";
private static final String PASSWORD = "123456";   // ← 改成你的 MySQL 口令
```

本机默认 `root / 123456` 已可用。改过就必须**重新执行第 4 步编译**才生效。

### 第 3 步：导入建库脚本（首次运行才需要）

> ⚠️ **必须用 cmd 重定向导入**，不要用 PowerShell 的 `Get-Content` 管道！
> PowerShell 5.1 的 `Get-Content` 默认按系统 ANSI(GBK) 解码 UTF-8 文件，中文会变成 `???`，
> 导致 `INSERT INTO book` 语法报错（ERROR 1064），脚本**中断在半路**——
> 表现为 `user`、`book` 表建好了但没数据，`reader`、`borrow` 表压根不存在，GUI 打开一片空白。

```powershell
cmd /c 'cd /d "c:\Users\32127\Desktop\BookMS\BookMS" & mysql -u root -p --default-character-set=utf8mb4 < sql\bookms_fixed.sql'
```

输入 MySQL 口令回车。**预期结果**：除口令安全警告外无任何输出。

> - 该脚本会 `DROP DATABASE IF EXISTS bookms` 后重建，**库里原有数据会被清空**，重复导入 = 重置。
> - 只用 `sql\bookms_fixed.sql`。`sql\bookms.sql` 是教材原始版（双引号 + 缺分号），在 MySQL 8/9 下必然报错，仅作对照。

验证数据已就位（**必做**，别跳过）：

```powershell
mysql -u root -p123456 --default-character-set=utf8mb4 -e "use bookms; show tables; select count(*) as books from book; select count(*) as readers from reader; select count(*) as borrows from borrow; select count(*) as users from user;"
```

**预期结果**：

| 检查项 | 应有结果 |
|---|---|
| 表 | `book` / `borrow` / `reader` / `user` / `v_borrow_detail` **5 张齐全** |
| `books` | 5 |
| `readers` | 3 |
| `borrows` | 3 |
| `users` | 2 |

再确认中文没变问号：

```powershell
mysql -u root -p123456 --default-character-set=utf8mb4 -e "use bookms; select id,title,author,stock,status from book order by id;"
```

**预期结果**：看到「Java核心技术卷I」「深入理解Java虚拟机」「红楼梦」等正常中文，且 `status` 为「在架」。
若书名是 `???` 或 `Table 'bookms.reader' doesn't exist`，说明第 3 步没成功，重跑一次。

### 第 4 步：编译

```powershell
$src = (Get-ChildItem -Recurse -Path src -Filter *.java).FullName
& "$j\javac.exe" -encoding UTF-8 -cp "lib\*" -d out $src
Copy-Item src\entities.properties out\ -Force
```

**预期结果**：两条命令都没有输出。看到 `错误:` 就按提示修。

> `-encoding UTF-8` 不能省，源码含中文；`entities.properties` 是反射映射配置，必须一起复制到 `out\`，否则 `EntityFactory` 加载失败。

### 第 5 步：数据库连通性自检（强烈建议）

```powershell
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-Dstdout.encoding=UTF-8","-cp","out;lib/*","com.bookms.util.JDBCConnection")
```

**预期结果**：

```
[JDBC] 驱动加载成功: com.mysql.cj.jdbc.Driver
[JDBC] URL = jdbc:mysql://localhost:3306/bookms?...
[JDBC] 自检：连接成功 → com.mysql.cj.jdbc.ConnectionImpl@35d176f7
```

看到「连接成功」再往下走；失败就先把 DB 搞定，别急着开 GUI。

> `-D` 参数**必须用数组 `@(...)`** 传。写成 `& java -Dfile.encoding=UTF-8 ...` 会被 PowerShell 拆坏，报 `ClassNotFoundException: /encoding=UTF-8`。

### 第 6 步：启动 TCP 统计服务端

**另开一个 PowerShell 窗口**常驻（它要一直跑着接收上报）：

```powershell
cd "c:\Users\32127\Desktop\BookMS\BookMS"
$j = "E:\IntelliJ IDEA 2026.2.1\jbr\bin"
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-Dstdout.encoding=UTF-8","-cp","out;lib/*","com.bookms.net.ReportServer")
```

**预期结果**：

```
[ReportServer] 监听端口 9999 ...（按 Ctrl+C 退出）
```

> 这步可选——不启动服务端，GUI 照样能用，只是「上报借阅统计」按钮会失败。

### 第 7 步：启动主程序（GUI）

```powershell
Start-Process "$j\javaw.exe" -ArgumentList @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.Main") -WorkingDirectory "c:\Users\32127\Desktop\BookMS\BookMS"
```

**预期结果**：弹出「欢迎进入 BookMS 图书管理系统」登录窗，终端不阻塞。

用 `javaw`（不是 `java`）就是为了后台启动、不留黑窗。确认进程在跑：

```powershell
Get-Process javaw,java | Select-Object Id,ProcessName,StartTime
Get-NetTCPConnection -LocalPort 9999 -State Listen   # 第 6 步跑起来后应有监听
```

### 第 8 步：登录并开始使用

在登录窗输入：

| 用户名 | 密码 |
|---|---|
| `admin` | `admin` |

**预期结果**：弹「登录成功！欢迎 admin」→ 进入主窗，含图书 / 读者 / 借阅 / 统计 4 个面板。

功能自测建议：图书管理查列表 → 借阅管理借一本书（走事务 + 行锁）→ 点「上报借阅统计」（第 6 步服务端窗口会打印记录，并写入 `report.log`）→ 图书管理点「序列化导出 (.dat)」。

---

关闭程序

```powershell
Get-Process javaw,java | Select-Object Id,ProcessName,StartTime
Stop-Process -Id <PID>          # 按 PID 结束，别用 Stop-Process java 一把梭
```

服务端窗口直接 `Ctrl+C` 即可。

---

其它自检入口（可选，控制台运行）

```powershell
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.reflection.ReflectionDemo")   # 综合反射演示
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.util.EntityFactory")          # 反射动态建对象
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.util.SerializationUtil")      # 序列化导出
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.util.DateUtil")               # 日期工具
& "$j\java.exe" @("-Dfile.encoding=UTF-8","-cp","out;lib/*","com.bookms.frame.MainFrame")             # 跳过登录直开主窗
```

---

排错速查

| 现象 | 原因 / 处理 |
|---|---|
| `无法将"java"项识别为 cmdlet` | PATH 没有 JDK，用第 0 步的 `$j` 完整路径 |
| `[JDBC] 自检：连接失败` / `Access denied for user` | MySQL 没启动，或第 2 步口令不对 |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | classpath 没带 `lib\*`；Windows 分隔符是 `;` 不是 `:` |
| `ClassNotFoundException: /encoding=UTF-8` | `-D` 被 PowerShell 拆开，改用 `@("-Dxxx=yyy", ...)` 数组 |
| 编译报 `错误: 编码 UTF-8 的不可映射字符` | `javac` 少了 `-encoding UTF-8` |
| `Address already in use: 9999` | 服务端已启动过；`Get-NetTCPConnection -LocalPort 9999` 查 PID 后结束 |
| 界面中文乱码 | 补 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` |
| 登录后表格空白 / 无初始书籍 | 种子数据没导入，回第 3 步重跑并核对 5 张表与 5 本书 |
| `ERROR 1064 ... near '????'` 导入报错 | `Get-Content` 把 UTF-8 读成乱码，改用第 3 步的 `cmd /c` 重定向 |
| `Table 'bookms.reader' doesn't exist` | 建库脚本中途报错中断，重跑第 3 步（不要用 `sql\bookms.sql`） |
| 书名显示为 `???` | 导入时没加 `--default-character-set=utf8mb4`，重跑第 3 步 |
| `EntityFactory` 报找不到映射 | `out\entities.properties` 没复制，补第 4 步第二条命令 |

---

（备选）Maven 方式

本机没装 Maven；装好之后可走：

```bash
mvn clean package
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar target/BookMS.jar
```

---

## 五、模块自检一览

每个含 `main` 方法的类都可独立运行（按 Maven/Java 方式）：

| 入口 | 自检内容 |
|---|---|
| `com.bookms.Main` | GUI 启动 → 登录 → 主窗 + 4 面板 |
| `com.bookms.frame.LoginFrame` | 仅打开登录窗 |
| `com.bookms.frame.MainFrame` | 跳过登录直接打开主窗（预览用）|
| `com.bookms.util.JDBCConnection` | Class.forName 加载驱动 + 连接自检 |
| `com.bookms.util.DateUtil` | 日期格式化 + 借出天数计算 |
| `com.bookms.util.SerializationUtil` | 序列化 List<Book> → 读回验证 |
| `com.bookms.util.EntityFactory` | 反射创建 4 个实体 + inspect 探查 |
| `com.bookms.reflection.ReflectionDemo` | 综合反射演示（控制台）|
| `com.bookms.net.ReportClient` | 发测试报到本地 ReportServer |

---

## 六、Git 协作流程

### 1. 初始化

```bash
cd BookMS
git init
git config user.name "admin"      # 全局账号配置
git checkout -b main
```

### 2. 推荐分支约定

- `main`：稳定分支，只接受合并
- `feature/db`：DAO + 工具类
- `feature/gui`：Swing 框架 + 面板
- `feature/network`：网络 + 序列化演示
- `feature/main`：主程 + 反射 + pom + README

```
## 六、关键技术点详解

### 1. 泛型 DAO 接口

```java
public interface IBaseDAO<T> {
    List<T> getList();
    T findById(int id);
    boolean save(T t);
    boolean update(T t);
    boolean delete(int id);
}

public class BookDAOImpl implements IBaseDAO<Book> { ... }
```

### 2. 反射 + 配置文件动态建对象

`src/entities.properties` 登记映射：

```properties
book = com.bookms.entity.Book
```

`EntityFactory.create("book")` 内部走：

```java
Class<?> clazz = Class.forName(fqcn);
Constructor<T> ctor = clazz.getDeclaredConstructor();
ctor.setAccessible(true);
return ctor.newInstance();
```

### 3. 序列化导出图书（演示 `ObjectOutputStream`）

`BookManagePane` 的「序列化导出 (.dat)」按钮 → `SerializationUtil.serializeBooks()` → 写到用户选择的 `.dat` 文件。

### 4. JDBC 借还事务

`BorrowDAOImpl.borrowBook()` 用 `Connection.setAutoCommit(false)` + `SELECT ... FOR UPDATE` + 三步（INSERT borrow + UPDATE book）+ commit/rollback，保证库存数据一致性。

### 5. TCP 网络上报

GUI `BorrowManagePane` 的「上报借阅统计」按钮 → `ReportClient.sendReport()` → `127.0.0.1:9999` → `ReportServer` 接收并追加到 `report.log`。

---

## 八、目录结构

```
BookMS/
├── pom.xml
├── README.md
├── .gitignore
├── sql/
│   ├── bookms.sql           # 教材风格原始版（含笔法错误，仅作对照）
│   └── bookms_fixed.sql     # 修正版：单引号 + 分号 + use + 完整种子数据
├── src/
│   ├── entities.properties  # 反射映射配置
│   └── com/bookms/
│       ├── Main.java
│       ├── entity/          (4 个实体，都 implements Serializable)
│       ├── util/            (JDBC + Date + Serialization + Reflection Factory)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + Date + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)│ ├── util/ (JDBC + 日期 + 序列化 + 反射工厂)
│       ├── dao/             (IBaseDAO<T> + 4 个实现)
│       ├── reflection/      (ReflectionDemo 控制台演示)
│       ├── net/             (TCP Server + Client)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)│ ├── net/ (TCP服务器 + 客户端)
│       ├── frame/           (LoginFrame + MainFrame)│ ├── frame/ (登录界面 + 主界面)
│       └── pane/            (4 个管理面板)
├── lib/
│   └── mysql-connector-j-8.4.0.jar
└── doc/
    └── 工作安排.md
```

---

## 九、已知边界

- 本机（2026-09-14 实测）MySQL 9.7.2 已安装并启动，`bookms` 库与种子数据已导入，
  全部模块（GUI + JDBC + 反射 + 序列化 + TCP）均已跑通，详见上文「逐步启动教程」。
- 本机**未安装独立 JDK/Maven**，当前使用 IntelliJ IDEA 自带 JBR（`E:\IntelliJ IDEA 2026.2.1\jbr`）；换机器时替换该路径即可。
- `bookms_fixed.sql` 中的数据是 MySQL 兼容的，如用 H2/MariaDB 可直接复用。
- GUI 字体在中文 Windows 下默认黑体，能正常显示；其它系统若有乱码，加 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`。
