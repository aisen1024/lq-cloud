package cn.lingque.redis;

import cn.lingque.config.LQProperties;
import cn.lingque.util.LQUtil;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RedisInstance {
    private RedisClient standaloneClient;
    private RedisClusterClient clusterClient;
    private StatefulRedisConnection<String, String> standaloneConnection;
    private StatefulRedisMasterReplicaConnection<String, String> sentinelConnection;
    private StatefulRedisClusterConnection<String, String> clusterConnection;
    private String mode;

    public RedisInstance(LQProperties redisPlusProperties) {
        try {
            switch (redisPlusProperties.getMode().toLowerCase()) {
                case "standalone":
                    initStandalone(redisPlusProperties);
                    break;
                case "sentinel":
                    initSentinel(redisPlusProperties);
                    break;
                case "cluster":
                    initCluster(redisPlusProperties);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Redis mode: " + redisPlusProperties.getMode());
            }
            mode = redisPlusProperties.getMode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Redis connection", e);
        }
    }

    private void initStandalone(LQProperties props) {
        ClientOptions clientOptions = createClientOptions(props);
        standaloneClient = RedisClient.create(createRedisURI(props));
        standaloneClient.setDefaultTimeout(Duration.ofMillis(props.getTimeout()));
        standaloneConnection = standaloneClient.connect();
    }

    private void initSentinel(LQProperties props) {
        List<RedisURI> sentinelUris = props.getSentinel().getSentinelNodes().stream()
                .map(node -> {
                    String[] parts = node.split(":");
                    return RedisURI.builder()
                            .withHost(parts[0])
                            .withPort(Integer.parseInt(parts[1]))
                            .withPassword(props.getPassword().toCharArray())
                            .build();
                })
                .collect(Collectors.toList());

        RedisClient redisClient = RedisClient.create();
        redisClient.setDefaultTimeout(Duration.ofMillis(props.getTimeout()));
        
        sentinelConnection = MasterReplica.connect(redisClient, 
                StringCodec.UTF8,
                sentinelUris);
        sentinelConnection.setReadFrom(ReadFrom.MASTER_PREFERRED);
    }

    private void initCluster(LQProperties props) {
        List<RedisURI> clusterNodes = props.getCluster().getClusterNodes().stream()
                .map(node -> {
                    String[] parts = node.split(":");
                    return RedisURI.builder()
                            .withHost(parts[0])
                            .withPort(Integer.parseInt(parts[1]))
                            .withPassword(props.getPassword().toCharArray())
                            .withDatabase(props.getDb())
                            .withTimeout(Duration.ofMillis(props.getTimeout()))
                            .build();
                })
                .collect(Collectors.toList());

        clusterClient = RedisClusterClient.create(clusterNodes);
        clusterClient.setDefaultTimeout(Duration.ofMillis(props.getTimeout()));
        clusterConnection = clusterClient.connect();
    }

    private ClientOptions createClientOptions(LQProperties props) {
        return ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofMillis(props.getTimeout()))
                        .keepAlive(true)
                        .build())
                .build();
    }

    private RedisURI createRedisURI(LQProperties props) {
        return RedisURI.builder()
                .withHost(props.getIp())
                .withPort(Integer.parseInt(props.getPort()))
                .withPassword(LQUtil.isEmpty(props.getPassword()) ?null:props.getPassword().toCharArray())
                .withDatabase(props.getDb())
                .withTimeout(Duration.ofMillis(props.getTimeout()))
                .build();
    }

    public RedisCommands<String, String> getRedisCommands() {
        switch (mode.toLowerCase()) {
            case "standalone":
                return standaloneConnection.sync();
            case "sentinel":
                return sentinelConnection.sync();
            case "cluster":
                return (RedisCommands<String, String>) clusterConnection.sync();
            default:
                throw new IllegalStateException("Unknown Redis mode: " + mode);
        }
    }

    public void close() {
        if (standaloneConnection != null) {
            standaloneConnection.close();
        }
        if (sentinelConnection != null) {
            sentinelConnection.close();
        }
        if (clusterConnection != null) {
            clusterConnection.close();
        }
        if (standaloneClient != null) {
            standaloneClient.shutdown();
        }
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
    }
}
