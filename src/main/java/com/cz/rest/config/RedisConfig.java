package com.cz.rest.config;

import org.springframework.cache.annotation.*;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.*;
import javax.annotation.*;
import org.springframework.data.redis.cache.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.cache.*;
import org.springframework.cache.interceptor.*;
import java.lang.reflect.*;
import org.springframework.data.redis.connection.jedis.*;
import redis.clients.jedis.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.connection.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.*;
import org.springframework.data.redis.serializer.*;
import org.slf4j.*;

@Configuration
@ComponentScan(basePackages = { "com.cz" })
@EnableCaching(proxyTargetClass = true)
@PropertySource({ "classpath:redis.properties" })
public class RedisConfig implements CachingConfigurer
{
  private static Logger logger;
  @Resource
  private Environment env;

  @Bean
  public CacheManager cacheManager() {
    final RedisCacheManager manager = new RedisCacheManager((RedisOperations)this.redisTemplate());
    manager.setUsePrefix(true);
    manager.setCachePrefix((RedisCachePrefix)new RedisCachePrefix() {
      public byte[] prefix(final String cacheName) {
        return (cacheName + "_").getBytes();
      }
    });
    final Map<String, Long> expires = new HashMap<String, Long>();
    expires.put("tokenInfo", 21600L);
    expires.put("online", 1800L);
    expires.put("userInfo", 21600L);
    expires.put("user_ext", 21600L);
    expires.put("userBank", 21600L);
    expires.put("open_info_cur", 5L);
    expires.put("lottery_info", 1800L);
    expires.put("notice_win_message_t", 3600L);
    expires.put("activity", 28800L);
    manager.setExpires((Map)expires);
    return (CacheManager)manager;
  }

  @Bean
  public CacheResolver cacheResolver() {
    return (CacheResolver)new RedisCacheResolver();
  }

  @Bean
  public CacheErrorHandler errorHandler() {
    return (CacheErrorHandler)new CacheErrorHandler() {
      public void handleCacheGetError(final RuntimeException exception, final Cache cache, final Object key) {
        RedisConfig.logger.error("cache get :key=" + key + ",exception:" + exception);
      }

      public void handleCachePutError(final RuntimeException exception, final Cache cache, final Object key, final Object value) {
        RedisConfig.logger.error("cache put :key=" + key + ",exception:" + exception);
      }

      public void handleCacheEvictError(final RuntimeException exception, final Cache cache, final Object key) {
        RedisConfig.logger.error("cache evict :key=" + key + ",exception:" + exception);
      }

      public void handleCacheClearError(final RuntimeException exception, final Cache cache) {
        RedisConfig.logger.error("cache clear :" + exception);
      }
    };
  }

  @Bean
  public KeyGenerator keyGenerator() {
    return (KeyGenerator)new KeyGenerator() {
      public Object generate(final Object target, final Method method, final Object... params) {
        final StringBuilder sb = new StringBuilder();
        sb.append(target.getClass().getName());
        sb.append(method.getName());
        for (final Object obj : params) {
          sb.append(obj.toString());
        }
        return sb.toString();
      }
    };
  }

  @Bean
  public JedisConnectionFactory jedisConnectionFactory() {
    final JedisConnectionFactory jcf = new JedisConnectionFactory();
    jcf.setHostName(this.env.getRequiredProperty("redis.hostname"));
    jcf.setDatabase((int)Integer.valueOf(this.env.getRequiredProperty("redis.database")));
    jcf.setPassword(this.env.getRequiredProperty("redis.password"));
    jcf.setPort((int)Integer.valueOf(this.env.getRequiredProperty("redis.port")));
    jcf.setTimeout((int)Integer.valueOf(this.env.getRequiredProperty("redis.timeout")));
    jcf.setPoolConfig(this.jedisPoolConfig());
    return jcf;
  }

  @Bean
  public JedisPoolConfig jedisPoolConfig() {
    final JedisPoolConfig config = new JedisPoolConfig();
    return config;
  }

  @Bean
  public RedisTemplate redisTemplate() {
    final StringRedisTemplate template = new StringRedisTemplate((RedisConnectionFactory)this.jedisConnectionFactory());
    final Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer((Class)Object.class);
    final ObjectMapper om = new ObjectMapper();
    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    jackson2JsonRedisSerializer.setObjectMapper(om);
    template.setValueSerializer((RedisSerializer)jackson2JsonRedisSerializer);
    template.setHashValueSerializer((RedisSerializer)jackson2JsonRedisSerializer);
    template.afterPropertiesSet();
    return (RedisTemplate)template;
  }

  static {
    RedisConfig.logger = LoggerFactory.getLogger((Class)RedisConfig.class);
  }
}
