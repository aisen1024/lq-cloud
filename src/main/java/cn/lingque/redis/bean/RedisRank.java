package cn.lingque.redis.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisRank {
    private String memberId;
    private Double score;
    private Long rank;
}