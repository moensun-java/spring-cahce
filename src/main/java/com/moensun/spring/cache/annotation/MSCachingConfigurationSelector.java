package com.moensun.spring.cache.annotation;

import com.moensun.spring.cache.EnableMSCaching;
import com.moensun.spring.cache.config.ProxyMSCachingConfiguration;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bane.Shi.
 * Copyright MoenSun
 * User: Bane.Shi
 * Date: 2018/7/23
 * Time: 下午2:00
 * @see org.springframework.cache.annotation.CachingConfigurationSelector
 */
public class MSCachingConfigurationSelector extends AdviceModeImportSelector<EnableMSCaching> {
    @Override
    protected String[] selectImports(AdviceMode adviceMode) {
        switch (adviceMode) {
            case PROXY:
                return getProxyImports();
            case ASPECTJ:
                return null;
            default:
                return null;
        }
    }


    private String[] getProxyImports() {
        List<String> result = new ArrayList<String>();
        result.add(AutoProxyRegistrar.class.getName());
        result.add(ProxyMSCachingConfiguration.class.getName());
        return result.toArray(new String[result.size()]);
    }

}
