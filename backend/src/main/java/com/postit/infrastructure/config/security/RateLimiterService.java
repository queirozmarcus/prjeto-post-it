package com.postit.infrastructure.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter em memória por IP usando Bucket4j.
 * Configuração: 5 requisições por minuto por IP.
 * Em produção, considere usar armazenamento distribuído (Redis) para ambientes multi-instância.
 */
@Service
public class RateLimiterService {

    // ConcurrentHashMap para segurança em ambientes multi-thread
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Tenta consumir 1 token do bucket associado ao IP.
     *
     * @param ip endereço IP do cliente
     * @return {@code true} se o token foi consumido (requisição permitida),
     *         {@code false} se o limite foi excedido
     */
    public boolean tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createNewBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
