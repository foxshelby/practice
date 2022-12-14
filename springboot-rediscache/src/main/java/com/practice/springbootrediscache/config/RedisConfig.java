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
	 * ??????cacheManager
	 *
	 * @return
	 */
	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		//redisCacheManager???????????????????????????redisCacheWriter?????????redisCacheConfigurer
		RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
		//??????cache ????????????jsonSerializer
		RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer();
		RedisSerializationContext.SerializationPair<Object> pair = RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);
		RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(pair);
		//??????????????????????????????
		defaultCacheConfig.entryTtl(Duration.ofDays(1));
		//???????????????builder?????????
		//RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(defaultCacheConfig).transactionAware().build();
		return new RedisCacheManager(redisCacheWriter, defaultCacheConfig);
	}


	/**
	 * @author zdd
	 * @createTime 2022/10/14 11:12
	 * @desc ?????????redis???????????????
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

		// ??????redis
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
		// ??????redis
		// RedisSentinelConfiguration redisConfig = new RedisSentinelConfiguration();
		// ??????redis
		// RedisClusterConfiguration redisConfig = new RedisClusterConfiguration();
		redisConfig.setHostName(redisHost);
		redisConfig.setPassword(RedisPassword.of(redisAuth));
		redisConfig.setPort(redisPort);
		redisConfig.setDatabase(redisDb);

		return new JedisConnectionFactory(redisConfig, clientConfig);
	}


	/**
	 * ??????????????????redis???KeyGenerator???key???????????????
	 * ??????: ????????????????????????key???????????????,??????@Cacheable???????????????keyGenerator?????????????????????key????????????
	 * ???????????????????????????????????????????????????
	 * ???: @Cacheable(value = "key", keyGenerator = "cacheKeyGenerator")
	 */
	@Override
	@Bean
	public KeyGenerator keyGenerator() {
		/**
		 * target: ???
		 * method: ??????
		 * params: ????????????
		 */
		return (target, method, params) -> {
			//???????????????????????????????????????
			StringBuilder sb = new StringBuilder();
			sb.append(target.getClass().getSimpleName()).append(":");
			sb.append(method.getName()).append(":");
			//??????SimpleKey???key?????????
			Object key = SimpleKeyGenerator.generateKey(params);
			return sb.append(key);
		};
	}

}
