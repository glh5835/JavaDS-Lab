package com.javadslab.persist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 极简手写数据库连接池（固定容量 + 阻塞队列借还）。
 * - 所有连接打开时统一执行 PRAGMA foreign_keys=ON，保证外键约束生效；
 * - borrow()/release() 支持连接复用；closePool() 释放全部物理连接；
 * - DAO 层禁止自建连接，一律经由本池。
 */
public final class Db implements AutoCloseable {

    private final BlockingQueue<Connection> pool;
    private final String url;

    public Db(String sqlitePath, int poolSize) {
        if (poolSize < 1) throw new IllegalArgumentException("池容量必须 >= 1");
        this.url = "jdbc:sqlite:" + sqlitePath;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            try {
                Connection c = DriverManager.getConnection(url);
                try (var st = c.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
                pool.add(c);
            } catch (SQLException e) {
                throw new DataAccessException("初始化连接池失败", e);
            }
        }
    }

    /** 借连接；poolSize 秒内拿不到则抛异常。 */
    public Connection borrow() {
        try {
            Connection c = pool.poll(10, TimeUnit.SECONDS);
            if (c == null) throw new DataAccessException("借连接超时：池已耗尽");
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("借连接被中断", e);
        }
    }

    public void release(Connection c) {
        if (c == null) return;
        try {
            if (c.isClosed()) return;
            if (!c.getAutoCommit()) c.setAutoCommit(true);
        } catch (SQLException ignore) {
            // 连接已异常，放弃归还
            return;
        }
        pool.offer(c);
    }

    /** 事务模板：fn 内的所有语句在同一事务中，异常自动回滚。 */
    public <T> T inTransaction(SqlWork<T> work) {
        Connection c = borrow();
        try {
            c.setAutoCommit(false);
            T result = work.apply(c);
            c.commit();
            return result;
        } catch (Exception e) {
            try {
                c.rollback();
            } catch (SQLException re) {
                e.addSuppressed(re);
            }
            throw e instanceof DataAccessException dae ? dae : new DataAccessException("事务失败，已回滚", e);
        } finally {
            release(c);
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T apply(Connection c) throws Exception;
    }

    @Override
    public void close() {
        Connection c;
        while ((c = pool.poll()) != null) {
            try {
                c.close();
            } catch (SQLException ignore) {
                // 忽略关闭异常
            }
        }
    }

    /** 统一的数据访问异常。 */
    public static class DataAccessException extends RuntimeException {
        public DataAccessException(String msg) { super(msg); }

        public DataAccessException(String msg, Throwable cause) { super(msg, cause); }
    }
}
