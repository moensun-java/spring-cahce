package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;
import com.moensun.spring.cache.annotation.DataType;
import com.moensun.spring.cache.operation.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.UsesJava8;
import org.springframework.util.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午5:00
 */
public class MSCacheAspectSupport extends AbstractMSCacheInvoker implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    private static Class<?> javaUtilOptionalClass = null;

    static {
        try {
            javaUtilOptionalClass =
                    ClassUtils.forName("java.util.Optional", CacheAspectSupport.class.getClassLoader());
        }
        catch (ClassNotFoundException ex) {
            // Java 8 not available - Optional references simply not supported then.
        }
    }

    protected final Log logger = LogFactory.getLog(getClass());

    private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache =
            new ConcurrentHashMap<CacheOperationCacheKey, CacheOperationMetadata>(1024);

    private final MSCacheOperationExpressionEvaluator evaluator = new MSCacheOperationExpressionEvaluator();

    private MSCacheOperationSource cacheOperationSource;

    private KeyGenerator keyGenerator = new SimpleKeyGenerator();

    private KeyGenerator hashKeyGenerator = new SimpleKeyGenerator();

    private MSCacheResolver cacheResolver;

    private BeanFactory beanFactory;

    private boolean initialized = false;


    /**
     * Set one or more cache operation sources which are used to find the cache
     * attributes. If more than one source is provided, they will be aggregated
     * using a {@link CompositeCacheOperationSource}.
     */
    public void setCacheOperationSources(MSCacheOperationSource... cacheOperationSources) {
        Assert.notEmpty(cacheOperationSources, "At least 1 CacheOperationSource needs to be specified");
        this.cacheOperationSource = (cacheOperationSources.length > 1 ?
                new CompositeMSCacheOperationSource(cacheOperationSources) : cacheOperationSources[0]);
    }

    /**
     * Return the CacheOperationSource for this cache aspect.
     */
    public MSCacheOperationSource getCacheOperationSource() {
        return this.cacheOperationSource;
    }

    /**
     * Set the default {@link KeyGenerator} that this cache aspect should delegate to
     * if no specific key generator has been set for the operation.
     * <p>The default is a {@link SimpleKeyGenerator}
     */
    public void setKeyGenerator(KeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    /**
     * Return the default {@link KeyGenerator} that this cache aspect delegates to.
     */
    public KeyGenerator getKeyGenerator() {
        return this.keyGenerator;
    }

    public KeyGenerator getHashKeyGenerator() {
        return hashKeyGenerator;
    }

    public void setHashKeyGenerator(KeyGenerator hashKeyGenerator) {
        this.hashKeyGenerator = hashKeyGenerator;
    }


    public void setCacheManager(RedisCacheManager cacheManager) {
        this.cacheResolver = new SimpleMSCacheResolver(cacheManager);
    }


    public void setCacheResolver(MSCacheResolver cacheResolver) {
        this.cacheResolver = cacheResolver;
    }

    /**
     * Return the default {@link CacheResolver} that this cache aspect delegates to.
     */
    public MSCacheResolver getCacheResolver() {
        return this.cacheResolver;
    }

    /**
     * Set the containing {@link BeanFactory} for {@link CacheManager} and other
     * service lookups.
     * @since 4.3
     */
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * @deprecated as of 4.3, in favor of {@link #setBeanFactory}
     */
    @Deprecated
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.beanFactory = applicationContext;
    }


    public void afterPropertiesSet() {
        Assert.state(getCacheOperationSource() != null, "The 'cacheOperationSources' property is required: " +
                "If there are no cacheable methods, then don't use a cache aspect.");
        Assert.state(getErrorHandler() != null, "The 'errorHandler' property is required");
    }

    public void afterSingletonsInstantiated() {
        if (getCacheResolver() == null) {
            // Lazily initialize cache resolver via default cache manager...
            try {
                setCacheManager(this.beanFactory.getBean(RedisCacheManager.class));
            }
            catch (NoUniqueBeanDefinitionException ex) {
                throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
                        "RedisCacheManager found. Mark one as primary (or give it the name 'cacheManager') or " +
                        "declare a specific CacheManager to use, that serves as the default one.");
            }
            catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
                        "Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
            }
        }
        this.initialized = true;
    }


    /**
     * Convenience method to return a String representation of this Method
     * for use in logging. Can be overridden in subclasses to provide a
     * different identifier for the given method.
     * @param method the method we're interested in
     * @param targetClass class the method is on
     * @return log message identifying this method
     * @see ClassUtils#getQualifiedMethodName
     */
    protected String methodIdentification(Method method, Class<?> targetClass) {
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        return ClassUtils.getQualifiedMethodName(specificMethod);
    }

    protected Collection<? extends MSCache> getCaches(
            CacheOperationInvocationContext<MSCacheOperation> context, MSCacheResolver cacheResolver) {

        Collection<? extends MSCache> caches = cacheResolver.resolveCaches(context);
        if (caches.isEmpty()) {
            throw new IllegalStateException("No cache could be resolved for '" +
                    context.getOperation() + "' using resolver '" + cacheResolver +
                    "'. At least one cache should be provided per cache operation.");
        }
        return caches;
    }

    protected MSCacheAspectSupport.CacheOperationContext getOperationContext(
            MSCacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {

        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new CacheOperationContext(metadata, args, target);
    }

    /**
     * Return the {@link CacheAspectSupport.CacheOperationMetadata} for the specified operation.
     * <p>Resolve the {@link CacheResolver} and the {@link KeyGenerator} to be
     * used for the operation.
     * @param operation the operation
     * @param method the method on which the operation is invoked
     * @param targetClass the target type
     * @return the resolved metadata for the operation
     */
    protected CacheOperationMetadata getCacheOperationMetadata(
            MSCacheOperation operation, Method method, Class<?> targetClass) {

        CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
        MSCacheAspectSupport.CacheOperationMetadata metadata = this.metadataCache.get(cacheKey);
        if (metadata == null) {
            KeyGenerator operationKeyGenerator;
            if (StringUtils.hasText(operation.getKeyGenerator())) {
                operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
            }
            else {
                operationKeyGenerator = getKeyGenerator();
            }

            KeyGenerator operationHashKeyGenerator;
            if (StringUtils.hasText(operation.getHashKeyGenerator())) {
                operationHashKeyGenerator = getBean(operation.getHashKeyGenerator(), KeyGenerator.class);
            }
            else {
                operationHashKeyGenerator = getHashKeyGenerator();
            }

            MSCacheResolver operationCacheResolver;
            if (StringUtils.hasText(operation.getCacheResolver())) {
                operationCacheResolver = getBean(operation.getCacheResolver(), MSCacheResolver.class);
            }
            else if (StringUtils.hasText(operation.getCacheManager())) {
                RedisCacheManager cacheManager = getBean(operation.getCacheManager(), RedisCacheManager.class);
                operationCacheResolver = new SimpleMSCacheResolver(cacheManager);
            }
            else {
                operationCacheResolver = getCacheResolver();
            }
            metadata = new CacheOperationMetadata(operation, method, targetClass, operationKeyGenerator,operationHashKeyGenerator, operationCacheResolver);
            this.metadataCache.put(cacheKey, metadata);
        }
        return metadata;
    }

    /**
     * Return a bean with the specified name and type. Used to resolve services that
     * are referenced by name in a {@link CacheOperation}.
     * @param beanName the name of the bean, as defined by the operation
     * @param expectedType type for the bean
     * @return the bean matching that name
     * @throws NoSuchBeanDefinitionException if such bean does not exist
     * @see CacheOperation#keyGenerator
     * @see CacheOperation#cacheManager
     * @see CacheOperation#cacheResolver
     */
    protected <T> T getBean(String beanName, Class<T> expectedType) {
        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
    }

    /**
     * Clear the cached metadata.
     */
    protected void clearMetadataCache() {
        this.metadataCache.clear();
        this.evaluator.clear();
    }

    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
        if (this.initialized) {
            Class<?> targetClass = getTargetClass(target);
            Collection<MSCacheOperation> operations = getCacheOperationSource().getCacheOperations(method, targetClass);
            if (!CollectionUtils.isEmpty(operations)) {
                return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
            }
        }

        return invoker.invoke();
    }

    /**
     * Execute the underlying operation (typically in case of cache miss) and return
     * the result of the invocation. If an exception occurs it will be wrapped in
     * a {@link CacheOperationInvoker.ThrowableWrapper}: the exception can be handled
     * or modified but it <em>must</em> be wrapped in a
     * {@link CacheOperationInvoker.ThrowableWrapper} as well.
     * @param invoker the invoker handling the operation being cached
     * @return the result of the invocation
     * @see CacheOperationInvoker#invoke()
     */
    protected Object invokeOperation(CacheOperationInvoker invoker) {
        return invoker.invoke();
    }

    private Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null && target != null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheOperationContext context = contexts.get(MSCacheableOperation.class).iterator().next();
            if (isConditionPassing(context, MSCacheOperationExpressionEvaluator.NO_RESULT)) {
                Object key = generateKey(context, MSCacheOperationExpressionEvaluator.NO_RESULT);
                Cache cache = context.getCaches().iterator().next();
                try {
                    return wrapCacheValue(method, cache.get(key, new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            return unwrapReturnValue(invokeOperation(invoker));
                        }
                    }));
                }
                catch (Cache.ValueRetrievalException ex) {
                    // The invoker wraps any Throwable in a ThrowableWrapper instance so we
                    // can just make sure that one bubbles up the stack.
                    throw (CacheOperationInvoker.ThrowableWrapper) ex.getCause();
                }
            }
            else {
                // No caching required, only call the underlying method
                return invokeOperation(invoker);
            }
        }


        // Process any early evictions
        processCacheEvicts(contexts.get(MSCacheEvictOperation.class), true,
                MSCacheOperationExpressionEvaluator.NO_RESULT);

        // Check if we have a cached item matching the conditions
        MSCache.ValueWrapper cacheHit = findCachedItem(contexts.get(MSCacheableOperation.class));

        // Collect puts from any @MSCacheableOperation miss, if no cached item is found
        List<CachePutRequest> cachePutRequests = new LinkedList<CachePutRequest>();
        if (cacheHit == null) {
            collectPutRequests(contexts.get(MSCacheableOperation.class),
                    MSCacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
        }

        Object cacheValue;
        Object returnValue;

        if (cacheHit != null && cachePutRequests.isEmpty() && !hasCachePut(contexts)) {
            // If there are no put requests, just use the cache hit
            cacheValue = cacheHit.get();
            returnValue = wrapCacheValue(method, cacheValue);
        }
        else {
            // Invoke the method if we don't have a cache hit
            returnValue = invokeOperation(invoker);
            cacheValue = unwrapReturnValue(returnValue);
        }

        // Collect any explicit @CachePuts
        collectPutRequests(contexts.get(MSCachePutOperation.class), cacheValue, cachePutRequests);

        // Process any collected put requests, either from @CachePut or a @Cacheable miss
        for (CachePutRequest cachePutRequest : cachePutRequests) {
            cachePutRequest.apply(cacheValue);
        }

        // Process any late evictions
        processCacheEvicts(contexts.get(MSCacheEvictOperation.class), false, cacheValue);

        return returnValue;
    }

    private Object wrapCacheValue(Method method, Object cacheValue) {
        if (method.getReturnType() == javaUtilOptionalClass &&
                (cacheValue == null || cacheValue.getClass() != javaUtilOptionalClass)) {
            return MSCacheAspectSupport.OptionalUnwrapper.wrap(cacheValue);
        }
        return cacheValue;
    }

    private Object unwrapReturnValue(Object returnValue) {
        if (returnValue != null && returnValue.getClass() == javaUtilOptionalClass) {
            return MSCacheAspectSupport.OptionalUnwrapper.unwrap(returnValue);
        }
        return returnValue;
    }

    private boolean hasCachePut(MSCacheAspectSupport.CacheOperationContexts contexts) {
        // Evaluate the conditions *without* the result object because we don't have it yet...
        Collection<CacheOperationContext> cachePutContexts = contexts.get(CachePutOperation.class);
        Collection<CacheOperationContext> excluded = new ArrayList<CacheOperationContext>();
        for (MSCacheAspectSupport.CacheOperationContext context : cachePutContexts) {
            try {
                if (!context.isConditionPassing(MSCacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
                    excluded.add(context);
                }
            }
            catch (MSVariableNotAvailableException ex) {
                // Ignoring failure due to missing result, consider the cache put has to proceed
            }
        }
        // Check if all puts have been excluded by condition
        return (cachePutContexts.size() != excluded.size());
    }

    private void processCacheEvicts(Collection<CacheOperationContext> contexts, boolean beforeInvocation, Object result) {
        for (MSCacheAspectSupport.CacheOperationContext context : contexts) {
            MSCacheEvictOperation operation = (MSCacheEvictOperation) context.metadata.operation;
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    /**
     * 清除缓存
     * @param context
     * @param operation
     * @param result
     */
    private void performCacheEvict(CacheOperationContext context, MSCacheEvictOperation operation, Object result) {
        Object key = null;
        for (MSCache cache : context.getCaches()) {
            if (operation.isCacheWide()) {
                logInvalidating(context, operation, null);
                doClear(cache);
            }
            else {
                if (key == null) {
                    key = context.generateKey(result);
                }
                logInvalidating(context, operation, key);
                DataType dataType = context.generateDataType();
                switch (dataType){
                    case STRING:
                        doEvict(cache, key);
                     break;
                    case HASH:
                        Object hashKey = context.generateHashKey(result);
                        doHEvict(cache,key,hashKey);
                        break;
                    default:
                        doEvict(cache, key);
                        break;
                }

            }
        }
    }

    private void logInvalidating(CacheOperationContext context, MSCacheEvictOperation operation, Object key) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
                    " for operation " + operation + " on method " + context.metadata.method);
        }
    }

    /**
     * Find a cached item only for {@link CacheableOperation} that passes the condition.
     * @param contexts the cacheable operations
     * @return a {@link Cache.ValueWrapper} holding the cached item,
     * or {@code null} if none is found
     */
    private MSCache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
        Object result = MSCacheOperationExpressionEvaluator.NO_RESULT;
        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                MSCache.ValueWrapper cached = null;
                DataType dataType = generateDataType(context);
                Object key = generateKey(context, result);
                switch (dataType){
                    case STRING:
                        cached = findInCaches(context, key);
                        break;
                    case HASH:
                        Object hashKey = generateHashKey(context,result);
                        cached = findInCaches(context,key,hashKey);
                    break;
                    default:
                        cached = findInCaches(context, key);
                        break;
                }

                if (cached != null) {
                    return cached;
                }
                else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Collect the {@link CacheAspectSupport.CachePutRequest} for all {@link CacheOperation} using
     * the specified result item.
     * @param contexts the contexts to handle
     * @param result the result item (never {@code null})
     * @param putRequests the collection to update
     */
    private void collectPutRequests(Collection<CacheOperationContext> contexts,
                                    Object result, Collection<CachePutRequest> putRequests) {

        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                Object hashKey = generateHashKey(context,result);
                DataType dataType = generateDataType(context);
                putRequests.add(new CachePutRequest(context, key,dataType,hashKey));
            }
        }
    }

    private MSCache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
        for (MSCache cache : context.getCaches()) {
            MSCache.ValueWrapper wrapper = doGet(cache, key);
            if (wrapper != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
                }
                return wrapper;
            }
        }
        return null;
    }

    private MSCache.ValueWrapper findInCaches(CacheOperationContext context, Object key, Object hashKey) {
        for (MSCache cache : context.getCaches()) {
            MSCache.ValueWrapper wrapper = doHGet(cache, key,hashKey);
            if (wrapper != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
                }
                return wrapper;
            }
        }
        return null;
    }

    private boolean isConditionPassing(CacheOperationContext context, Object result) {
        boolean passing = context.isConditionPassing(result);
        if (!passing && logger.isTraceEnabled()) {
            logger.trace("Cache condition failed on method " + context.metadata.method +
                    " for operation " + context.metadata.operation);
        }
        return passing;
    }

    private Object generateKey(CacheOperationContext context, Object result) {
        Object key = context.generateKey(result);
        if (key == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
                    "using named params on classes without debug info?) " + context.metadata.operation);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache key '" + key + "' for operation " + context.metadata.operation);
        }
        return key;
    }


    /**
     * 获取 DataType
     * @param context
     * @return
     */
    private DataType generateDataType(CacheOperationContext context){
        return context.generateDataType();
    }


    /**
     * 获取hashKey
     * @param context
     * @param result
     * @return
     */
    private Object generateHashKey(CacheOperationContext context, Object result){
        Object hashKey = context.generateHashKey(result);
        if (hashKey == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " +
                    "using named params on classes without debug info?) " + context.metadata.operation);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache hashKey '" + hashKey + "' for operation " + context.metadata.operation);
        }
        return hashKey;
    }


    private class CacheOperationContexts {

        private final MultiValueMap<Class<? extends MSCacheOperation>, CacheOperationContext> contexts =
                new LinkedMultiValueMap<Class<? extends MSCacheOperation>, CacheOperationContext>();

        private final boolean sync;

        public CacheOperationContexts(Collection<? extends MSCacheOperation> operations, Method method,
                                      Object[] args, Object target, Class<?> targetClass) {

            for (MSCacheOperation operation : operations) {
                this.contexts.add(operation.getClass(), getOperationContext(operation, method, args, target, targetClass));
            }
            this.sync = determineSyncFlag(method);
        }

        public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
            Collection<CacheOperationContext> result = this.contexts.get(operationClass);
            return (result != null ? result : Collections.<MSCacheAspectSupport.CacheOperationContext>emptyList());
        }

        public boolean isSynchronized() {
            return this.sync;
        }

        private boolean determineSyncFlag(Method method) {
            List<CacheOperationContext> cacheOperationContexts = this.contexts.get(MSCacheableOperation.class);
            if (cacheOperationContexts == null) {  // no @Cacheable operation at all
                return false;
            }
            boolean syncEnabled = false;
            for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
                if (((MSCacheableOperation) cacheOperationContext.getOperation()).isSync()) {
                    syncEnabled = true;
                    break;
                }
            }
            if (syncEnabled) {
                if (this.contexts.size() > 1) {
                    throw new IllegalStateException("@MSCacheableOperation(sync=true) cannot be combined with other cache operations on '" + method + "'");
                }
                if (cacheOperationContexts.size() > 1) {
                    throw new IllegalStateException("Only one @MSCacheableOperation(sync=true) entry is allowed on '" + method + "'");
                }
                CacheOperationContext cacheOperationContext = cacheOperationContexts.iterator().next();
                MSCacheableOperation operation = (MSCacheableOperation) cacheOperationContext.getOperation();
                if (cacheOperationContext.getCaches().size() > 1) {
                    throw new IllegalStateException("@MSCacheableOperation(sync=true) only allows a single cache on '" + operation + "'");
                }
                if (StringUtils.hasText(operation.getUnless())) {
                    throw new IllegalStateException("@MSCacheableOperation(sync=true) does not support unless attribute on '" + operation + "'");
                }
                return true;
            }
            return false;
        }
    }


    /**
     * Metadata of a cache operation that does not depend on a particular invocation
     * which makes it a good candidate for caching.
     */
    protected static class CacheOperationMetadata {

        private final MSCacheOperation operation;

        private final Method method;

        private final Class<?> targetClass;

        private final KeyGenerator keyGenerator;

        private final KeyGenerator hashKeyGenerator;

        private final MSCacheResolver cacheResolver;

        public CacheOperationMetadata(MSCacheOperation operation, Method method, Class<?> targetClass,
                                      KeyGenerator keyGenerator, KeyGenerator hashKeyGenerator, MSCacheResolver cacheResolver) {

            this.operation = operation;
            this.method = method;
            this.targetClass = targetClass;
            this.keyGenerator = keyGenerator;
            this.hashKeyGenerator = hashKeyGenerator;
            this.cacheResolver = cacheResolver;
        }
    }


    protected class CacheOperationContext implements CacheOperationInvocationContext<MSCacheOperation> {

        private final CacheOperationMetadata metadata;

        private final Object[] args;

        private final Object target;

        private final Collection<? extends MSCache> caches;

        private final Collection<String> cacheNames;

        private final AnnotatedElementKey methodCacheKey;

        public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            this.metadata = metadata;
            this.args = extractArgs(metadata.method, args);
            this.target = target;
            this.caches = MSCacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
            this.cacheNames = createCacheNames(this.caches);
            this.methodCacheKey = new AnnotatedElementKey(metadata.method, metadata.targetClass);
        }

        @Override
        public MSCacheOperation getOperation() {
            return this.metadata.operation;
        }

        @Override
        public Object getTarget() {
            return this.target;
        }

        @Override
        public Method getMethod() {
            return this.metadata.method;
        }

        @Override
        public Object[] getArgs() {
            return this.args;
        }

        private Object[] extractArgs(Method method, Object[] args) {
            if (!method.isVarArgs()) {
                return args;
            }
            Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
            Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
            System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
            System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
            return combinedArgs;
        }

        protected boolean isConditionPassing(Object result) {
            if (StringUtils.hasText(this.metadata.operation.getCondition())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                return evaluator.condition(this.metadata.operation.getCondition(),
                        this.methodCacheKey, evaluationContext);
            }
            return true;
        }

        protected boolean canPutToCache(Object value) {
            String unless = "";
            if (this.metadata.operation instanceof MSCacheableOperation) {
                unless = ((MSCacheableOperation) this.metadata.operation).getUnless();
            }
            else if (this.metadata.operation instanceof MSCachePutOperation) {
                unless = ((MSCachePutOperation) this.metadata.operation).getUnless();
            }
            if (StringUtils.hasText(unless)) {
                EvaluationContext evaluationContext = createEvaluationContext(value);
                return !evaluator.unless(unless, this.methodCacheKey, evaluationContext);
            }
            return true;
        }

        /**
         * Compute the key for the given caching operation.
         * @return the generated key, or {@code null} if none can be generated
         */
        protected Object generateKey(Object result) {
            if (StringUtils.hasText(this.metadata.operation.getKey())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                return evaluator.key(this.metadata.operation.getKey(), this.methodCacheKey, evaluationContext);
            }
            return this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args);
        }

        protected DataType generateDataType(){
            return this.metadata.operation.getDataType();
        }

        protected Object generateHashKey( Object result ){
            String hashKey = "";
            if( StringUtils.hasText( this.metadata.operation.getHashKey()) ){
                EvaluationContext evaluationContext = createEvaluationContext(result);
                hashKey += evaluator.hashKey(this.metadata.operation.getHashKey(),this.methodCacheKey, evaluationContext);
            }
            if( StringUtils.hasText( this.metadata.operation.getHashKeyGenerator())  ){
                if(  StringUtils.hasText( this.metadata.operation.getHashKey())  ){
                    hashKey += "_";
                }
                hashKey += this.metadata.hashKeyGenerator.generate(this.target, this.metadata.method, this.args);
            }

            return hashKey;
        }



        private EvaluationContext createEvaluationContext(Object result) {
            return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
                    this.target, this.metadata.targetClass, result, beanFactory);
        }

        protected Collection<? extends MSCache> getCaches() {
            return this.caches;
        }

        protected Collection<String> getCacheNames() {
            return this.cacheNames;
        }

        private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
            Collection<String> names = new ArrayList<String>();
            for (Cache cache : caches) {
                names.add(cache.getName());
            }
            return names;
        }
    }


    private class CachePutRequest {

        private final CacheOperationContext context;

        private final Object key;

        private final DataType dataType;

        private final Object hashKey;

        public CachePutRequest(CacheOperationContext context, Object key, DataType dataType, Object hashKey) {
            this.context = context;
            this.key = key;
            this.dataType = dataType;
            this.hashKey = hashKey;
        }

        public void apply(Object result) {
            if (this.context.canPutToCache(result)) {
                for (MSCache cache : this.context.getCaches()) {

                    switch (dataType){
                        case STRING:
                            doPut(cache, this.key, result);
                            break;
                        case HASH:
                            doHSet(cache,this.key,this.hashKey,result);
                            break;
                        default:
                            doPut(cache, this.key, result);
                            break;
                    }


                }
            }
        }
    }


    private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

        private final CacheOperation cacheOperation;

        private final AnnotatedElementKey methodCacheKey;

        private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
            this.cacheOperation = cacheOperation;
            this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MSCacheAspectSupport.CacheOperationCacheKey)) {
                return false;
            }
            MSCacheAspectSupport.CacheOperationCacheKey otherKey = (MSCacheAspectSupport.CacheOperationCacheKey) other;
            return (this.cacheOperation.equals(otherKey.cacheOperation) &&
                    this.methodCacheKey.equals(otherKey.methodCacheKey));
        }

        @Override
        public int hashCode() {
            return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
        }

        @Override
        public String toString() {
            return this.cacheOperation + " on " + this.methodCacheKey;
        }

        @Override
        public int compareTo(MSCacheAspectSupport.CacheOperationCacheKey other) {
            int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
            if (result == 0) {
                result = this.methodCacheKey.compareTo(other.methodCacheKey);
            }
            return result;
        }
    }


    /**
     * Inner class to avoid a hard dependency on Java 8.
     */
    @UsesJava8
    private static class OptionalUnwrapper {

        public static Object unwrap(Object optionalObject) {
            Optional<?> optional = (Optional<?>) optionalObject;
            if (!optional.isPresent()) {
                return null;
            }
            Object result = optional.get();
            Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
            return result;
        }

        public static Object wrap(Object value) {
            return Optional.ofNullable(value);
        }
    }
}
