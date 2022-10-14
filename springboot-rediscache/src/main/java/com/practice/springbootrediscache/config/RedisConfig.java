package com.practice.springbootrediscache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author kolento
 */
@EnableCaching
@Configuration
public class RedisConfig extends CachingConfigurerSupport {


	@Value("${spring.redis.host}")
	private String redisHost;

	@Value("${spring.redis.port}")
	private int redisPort;

	@Value("${spring.redis.timeout}")
	private int redisTimeout;

	@Value("${spring.redis.password}")
	private String redisAuth;

	@Value("${spring.redis.database}")
	private int redisDb;

	@Value("${spring.redis.jedis.pool.max-active}")
	private int maxActive;

	@Value("${spring.redis.jedis.pool.max-wait}")
	private int maxWait;

	@Value("${spring.redis.jedis.pool.max-idle}")
	private int maxIdle;

	@Value("${spring.redis.jedis.pool.min-idle}")
	private int minIdle;

	/**
	 * 配置cacheManager
	 *
	 * @return
	 */
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		//redisCacheManager构造器需要提供一个redisCacheWriter和一个redisCacheConfigurer
		RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
		//配置cache 序列化为jsonSerializer
		RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
		RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);
		RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
		//设置默认过期时间一天
		defaultCacheConfig.entryTtl(Duration.ofDays(1));
		//也可以通过builder来构建
		//RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(defaultCacheConfig).transactionAware().build();
		return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);
	}


	/**
	 * @author zdd
	 * @createTime 2022/10/14 11:12
	 * @desc 自定义redis的连接工厂
	 * @Param []
	 * @return org.springframework.data.redis.connection.RedisConnectionFactory
	 */
	@Bean
	public RedisConnectionFactory connectionFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(maxActive);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMaxWaitMillis(maxWait);
		poolConfig.setMinIdle(minIdle);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(false);
		poolConfig.setTestWhileIdle(true);
		JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
				.usePooling().poolConfig(poolConfig).and().readTimeout(Duration.ofMillis(redisTimeout)).build();

		// 单点redis
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
		// 哨兵redis
		// RedisSentinelConfiguration redisConfig = new RedisSentinelConfiguration();
		// 集群redis
		// RedisClusterConfiguration redisConfig = new RedisClusterConfiguration();
		redisConfig.setHostName(redisHost);
		redisConfig.setPassword(RedisPassword.of(redisAuth));
		redisConfig.setPort(redisPort);
		redisConfig.setDatabase(redisDb);

		return new JedisConnectionFactory(redisConfig, clientConfig);
	}


	/**
	 * 自定义缓存的redis的KeyGenerator【key生成策略】
	 * 注意: 该方法只是声明了key的生成策略,需在@Cacheable注解中通过keyGenerator属性指定具体的key生成策略
	 * 可以根据业务情况，配置多个生成策略
	 * 如: @Cacheable(value = "key", keyGenerator = "cacheKeyGenerator")
	 */
	@Override
	@Bean
	public KeyGenerator keyGenerator() {
		/**
		 * target: 类
		 * method: 方法
		 * params: 方法参数
		 */
		return (target, method, params) -> {
			//获取代理对象的最终目标对象
			StringBuilder sb = new StringBuilder();
			sb.append(target.getClass().getSimpleName()).append(":");
			sb.append(method.getName()).append(":");
			//调用SimpleKey的key生成器
			Object key = SimpleKeyGenerator.generateKey(params);
			return sb.append(key);
		};
	}

}
