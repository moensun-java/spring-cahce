package com.fclassroom.ssp.spring.cache.operation;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 上午11:43
 */
public class MSCachePutOperation extends MSCacheOperation {


    private final String unless;


    /**
     * @since 4.3
     */
    public MSCachePutOperation(MSCachePutOperation.Builder b) {
        super(b);
        this.unless = b.unless;
    }


    public String getUnless() {
        return this.unless;
    }


    /**
     * @since 4.3
     */
    public static class Builder extends MSCacheOperation.Builder {

        private String unless;

        public void setUnless(String unless) {
            this.unless = unless;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | unless='");
            sb.append(this.unless);
            sb.append("'");
            return sb;
        }

        public MSCacheOperation build() {
            return new MSCacheOperation(this);
        }
    }

}
