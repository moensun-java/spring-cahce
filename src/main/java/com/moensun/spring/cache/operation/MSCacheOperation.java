package com.moensun.spring.cache.operation;

import com.moensun.spring.cache.annotation.DataType;
import org.springframework.cache.interceptor.CacheOperation;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 上午11:15
 */
public class MSCacheOperation extends CacheOperation {

    private final DataType dataType;

    private final String hashKey;

    private final String hashKeyGenerator;

    public MSCacheOperation(Builder b) {
        super(b);
        this.dataType = b.dataType;
        this.hashKey = b.hashKey;
        this.hashKeyGenerator = b.hashKeyGenerator;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getHashKey() {
        return hashKey;
    }

    public String getHashKeyGenerator() {
        return hashKeyGenerator;
    }

    public static class Builder extends CacheOperation.Builder {

        private DataType dataType;

        private String hashKey;

        private String hashKeyGenerator;

        public void setHashKey(String hashKey){ this.hashKey = hashKey; }


        public void setDataType(DataType dataType){ this.dataType = dataType; }

        public void setHashKeyGenerator(String hashKeyGenerator) {
            this.hashKeyGenerator = hashKeyGenerator;
        }

        public DataType getDataType() {
            return dataType;
        }

        public String getHashKey() {
            return hashKey;
        }

        public String getHashKeyGenerator() {
            return hashKeyGenerator;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | dataType=' ");
            sb.append(this.dataType);
            sb.append("'");
            sb.append(" | hashKey=' ");
            sb.append(this.hashKey);
            sb.append("'");
            sb.append(" | hashKeyGenerator=' ");
            sb.append(this.hashKeyGenerator);
            sb.append("'");
            return sb;
        }

        @Override
        public MSCacheOperation build() {
            return new MSCacheOperation(this);
        }
    }

}
