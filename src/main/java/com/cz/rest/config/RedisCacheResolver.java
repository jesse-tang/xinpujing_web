package com.cz.rest.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

public class RedisCacheResolver implements CacheResolver
{
  @Autowired
  private CacheManager cacheManager;
  
  public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context)
  {
    List<Cache> caches = new ArrayList();
    for (String cacheName : context.getOperation().getCacheNames()) {
      Cache cache = this.cacheManager.getCache(cacheName);
      
      caches.add(cache);
    }
    
    return caches;
  }
}
