package io.simpleframework.crud.util;

import io.simpleframework.crud.DomainEventPublisher;
import io.simpleframework.crud.Domains;
import io.simpleframework.crud.ModelDatasourceProvider;
import io.simpleframework.crud.Models;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@Configuration
public class SimpleCrudAutoConfiguration implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        setModelDatasourceProvider(applicationContext);
        setDomainEventPublisher(applicationContext);
    }

    private void setModelDatasourceProvider(ApplicationContext applicationContext) {
        ModelDatasourceProvider datasourceProvider = new ModelDatasourceProvider() {
            @Override
            public <T> T getBean(String name, Class<T> clazz) {
                T result;
                if (SimpleCrudUtils.hasText(name)) {
                    result = applicationContext.getBean(name, clazz);
                } else {
                    result = applicationContext.getBean(clazz);
                }
                return result;
            }
        };
        Models.datasourceProvider(datasourceProvider);
    }

    private void setDomainEventPublisher(ApplicationContext applicationContext) {
        DomainEventPublisher publisher = applicationContext::publishEvent;
        Domains.eventPublisher(publisher);
    }

}
