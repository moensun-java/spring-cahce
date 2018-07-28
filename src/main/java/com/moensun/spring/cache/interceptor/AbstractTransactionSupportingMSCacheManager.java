package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:29
 */
public abstract class AbstractTransactionSupportingMSCacheManager extends AbstractMSCacheManager {

    private boolean transactionAware = false;


    /**
     * Set whether this CacheManager should expose transaction-aware Cache objects.
     * <p>Default is "false". Set this to "true" to synchronize cache put/evict
     * operations with ongoing Spring-managed transactions, performing the actual cache
     * put/evict operation only in the after-commit phase of a successful transaction.
     */
    public void setTransactionAware(boolean transactionAware) {
        this.transactionAware = transactionAware;
    }

    /**
     * Return whether this CacheManager has been configured to be transaction-aware.
     */
    public boolean isTransactionAware() {
        return this.transactionAware;
    }


    @Override
    protected MSCache decorateCache(MSCache cache) {
        return (isTransactionAware() ? new TransactionAwareMSCacheDecorator(cache) : cache);
    }

}
