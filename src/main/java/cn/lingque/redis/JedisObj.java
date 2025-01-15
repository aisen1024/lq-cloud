package cn.lingque.redis;

import cn.lingque.util.TryCatch;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.Jedis;

public class JedisObj extends Jedis {

    private ConnectionPool pool = null;
    private Connection conn = null;
    public JedisObj(ConnectionPool pool,Connection conn) {
        super(conn);
        this.pool = pool;
        this.conn = conn;
    }

    public void returnResource(){
        TryCatch.trying(()-> pool.returnResource(conn),"回收Jedis资源");
    }
}
