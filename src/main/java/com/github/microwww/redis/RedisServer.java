package com.github.microwww.redis;

import com.github.microwww.redis.database.Schema;
import com.github.microwww.redis.logger.LogFactory;
import com.github.microwww.redis.logger.Logger;
import com.github.microwww.redis.protocal.AbstractOperation;
import com.github.microwww.redis.protocal.RedisRequest;
import com.github.microwww.redis.protocal.RequestSession;
import redis.clients.jedis.Protocol;
import redis.clients.util.RedisInputStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RedisServer extends SelectSocketsThreadPool {
    public static final Logger log = LogFactory.getLogger(RedisServer.class);

    private static final Executor pool = Executors.newFixedThreadPool(5);
    private final Map<SocketChannel, RequestSession> sessions = new ConcurrentHashMap<>();
    private Schema schema;

    public RedisServer() {
        super(pool);
    }

    public void configScheme(int size, AbstractOperation... operation) {
        if (this.schema == null) {
            this.schema = new Schema(size, operation);
        }
    }

    public void listener(String host, int port) throws IOException {
        Runnable config = this.config(host, port);

        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {//
                log.error("Thread runtime error {}", e);
            });
        }
        pool.execute(config);
    }

    @Override
    public Runnable config(String host, int port) throws IOException {
        Runnable run = super.config(host, port);
        return () -> {
            InetSocketAddress address = (InetSocketAddress) this.serverSocket.getLocalSocketAddress();
            log.info("Redis server start @ {}:{}", address.getHostName(), "" + address.getPort());
            // RUN and block !
            run.run();
        };
    }

    @Override
    protected void readChannel(SocketChannel channel, AwaitRead lock) throws IOException {
        RedisInputStream in = new RedisInputStream(new ChannelInputStream(channel, lock));
        while (in.available() > 0) {
            Object read = Protocol.read(in);
            ExpectRedisRequest[] req = ExpectRedisRequest.parseRedisData(read);
            this.getSchema().exec(new RedisRequest(this, channel, req));
        }
    }

    @Override
    protected void acceptHandler(SocketChannel channel) throws IOException {
        super.acceptHandler(channel);
        sessions.put(key(channel), new RequestSession());
    }

    public RequestSession getSession(SocketChannel channel) {
        return sessions.get(channel);
    }

    public Schema getSchema() {
        if (schema == null) {
            synchronized (this) {
                if (schema == null) {
                    schema = new Schema(Schema.DEFAULT_SCHEMA_SIZE);
                }
            }
        }
        return schema;
    }
}
