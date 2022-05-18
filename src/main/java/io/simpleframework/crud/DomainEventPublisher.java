package io.simpleframework.crud;


@FunctionalInterface
public interface DomainEventPublisher {

    
    void publish(Object event);

}
