package com.urbanairship.datacube.dbharnesses;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Row;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class WithHTable {
    private static final Logger log = LogManager.getLogger(WithHTable.class);
    
    /**
     * Take an htable from the pool, use it with the given HTableRunnable, and return it to
     * the pool. This is the "loan pattern" where the htable resource is used temporarily by
     * the runnable.
     */
    public static <T> T run(HTablePool pool, byte[] tableName, HTableRunnable<T> runnable) 
            throws IOException {
        HTableInterface hTable = null;
        try {
            hTable = pool.getTable(tableName);
            return runnable.runWith(hTable);
        } catch(Exception e) {
            if(e instanceof IOException) {
                throw (IOException)e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if(hTable != null) {
                pool.putTable(hTable);
            }
        }
    }
    
    public static interface HTableRunnable<T> {
        public T runWith(HTableInterface hTable) throws IOException;
    }

    /**
     * Do an HBase put and return null.
     * @throws IOException if the underlying HBase operation throws an IOException
     */
    public static void put(HTablePool pool, byte[] tableName, final Put put) throws IOException {
        run(pool, tableName, new HTableRunnable<Object>() {
            @Override
            public Object runWith(HTableInterface hTable) throws IOException {
                hTable.put(put);
                return null;
            }
        });
    }

    public static Result get(HTablePool pool, byte[] tableName, final Get get) throws IOException {
        return run(pool, tableName, new HTableRunnable<Result>() {
            @Override
            public Result runWith(HTableInterface hTable) throws IOException {
                return hTable.get(get);
            }
        });
    }

    public static long increment(HTablePool pool, byte[] tableName, final byte[] row,
            final byte[] cf, final byte[] qual, final long amount) throws IOException {
        return run(pool, tableName, new HTableRunnable<Long> () {
            @Override
            public Long runWith(HTableInterface hTable) throws IOException {
                return hTable.incrementColumnValue(row, cf, qual, amount);
            }
        });
    }
    
    public static boolean checkAndPut(HTablePool pool, byte[] tableName, final byte[] row,
            final byte[] cf, final byte[] qual, final byte[] value, final Put put) 
                    throws IOException {
        return run(pool, tableName, new HTableRunnable<Boolean>() {
            @Override
            public Boolean runWith(HTableInterface hTable) throws IOException {
                return hTable.checkAndPut(row, cf, qual, value, put);
            }
        });
    }

    public static boolean checkAndDelete(HTablePool pool, byte[] tableName, final byte[] row,
            final byte[] cf, final byte[] qual, final byte[] value, final Delete delete) 
                    throws IOException {
        return run(pool, tableName, new HTableRunnable<Boolean>() {
            @Override
            public Boolean runWith(HTableInterface hTable) throws IOException {
                return hTable.checkAndDelete(row, cf, qual, value, delete);
            }
        });
    }
    
    public static Object[] batch(HTablePool pool, byte[] tableName, final List<Row> actions) 
            throws IOException {
        return run(pool, tableName, new HTableRunnable<Object[]>() {
            @Override
            public Object[] runWith(HTableInterface hTable) throws IOException {
                try {
                    return hTable.batch(actions);
                } catch (InterruptedException e) {
                    log.error("Interrupted while running hbase batch, " +
                            "re-throwing as IOException", e);
                    throw new IOException(e);
                }
            }
        });
    }
    
    public static Result[] get(HTablePool pool, byte[] tableName, final List<Get> gets) 
            throws IOException {
        return run(pool, tableName, new HTableRunnable<Result[]>() {
            @Override
            public Result[] runWith(HTableInterface hTable) throws IOException {
                return hTable.get(gets);
            }
        });
    }
}
