package com.alibaba.spring.boot.rsocket.responder.invocation;

import com.alibaba.rsocket.rpc.LocalReactiveServiceCallerImpl;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import com.alibaba.spring.boot.rsocket.RSocketService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * RSocketService annotation processor
 *
 * @author leijuan
 */
public class RSocketServiceAnnotationProcessor extends LocalReactiveServiceCallerImpl implements BeanPostProcessor {

    private RSocketProperties rSocketProperties;

    public RSocketServiceAnnotationProcessor(RSocketProperties rSocketProperties) {
        this.rSocketProperties = rSocketProperties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        scanRSocketServiceAnnotation(bean, beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected void scanRSocketServiceAnnotation(Object bean, String beanName) {
        Class<?> managedBeanClass = bean.getClass();
        RSocketService reactiveService = AnnotationUtils.findAnnotation(managedBeanClass, RSocketService.class);
        if (reactiveService != null) {
            registerRSocketService(reactiveService, bean);
        }
    }

    private void registerRSocketService(RSocketService rSocketService, Object bean) {
        String serviceName = rSocketService.name();
        if (serviceName.isEmpty()) {
            serviceName = rSocketService.serviceInterface().getCanonicalName();
        }
        String group = rSocketProperties.getGroup();
        String version = rSocketService.version().isEmpty() ? rSocketProperties.getVersion() : rSocketService.version();
        addProvider(group, serviceName, version, rSocketService.serviceInterface(), bean);
    }


}
