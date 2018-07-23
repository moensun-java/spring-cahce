package com.fclassroom.ssp.spring.cache.operation;



/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午4:26
 */
public class MSCacheableOperation extends MSCacheOperation {

    private final String unless;

    private final boolean sync;


    /**
     * @param b
     * @since 4.3
     */
    protected MSCacheableOperation(MSCacheableOperation.Builder b) {
        super(b);
        this.unless = b.unless;
        this.sync = b.sync;
    }

    public String getUnless() {
        return this.unless;
    }

    public boolean isSync() {
        return this.sync;
    }

    public static class Builder extends MSCacheOperation.Builder {


        private String unless;

        private boolean sync;

        public void setUnless(String unless) {
            this.unless = unless;
        }


        public void setSync(boolean sync) {
            this.sync = sync;
        }


        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | unless='");
            sb.append(this.unless);
            sb.append("'");
            sb.append(" | sync='");
            sb.append(this.sync);
            sb.append("'");
            return sb;
        }

        @Override
        public MSCacheableOperation build() {
            return new MSCacheableOperation(this);
        }
    }

}
