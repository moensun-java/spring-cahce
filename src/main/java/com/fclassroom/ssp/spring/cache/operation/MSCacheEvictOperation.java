package com.fclassroom.ssp.spring.cache.operation;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 上午11:45
 */
public class MSCacheEvictOperation extends MSCacheOperation {

    private final boolean cacheWide;

    private final boolean beforeInvocation;


    /**
     * @since 4.3
     */
    public MSCacheEvictOperation(MSCacheEvictOperation.Builder b) {
        super(b);
        this.cacheWide = b.cacheWide;
        this.beforeInvocation = b.beforeInvocation;
    }


    public boolean isCacheWide() {
        return this.cacheWide;
    }

    public boolean isBeforeInvocation() {
        return this.beforeInvocation;
    }


    /**
     * @since 4.3
     */
    public static class Builder extends MSCacheOperation.Builder {

        private boolean cacheWide = false;

        private boolean beforeInvocation = false;

        public void setCacheWide(boolean cacheWide) {
            this.cacheWide = cacheWide;
        }

        public void setBeforeInvocation(boolean beforeInvocation) {
            this.beforeInvocation = beforeInvocation;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(",");
            sb.append(this.cacheWide);
            sb.append(",");
            sb.append(this.beforeInvocation);
            return sb;
        }

        public MSCacheEvictOperation build() {
            return new MSCacheEvictOperation(this);
        }
    }

}
