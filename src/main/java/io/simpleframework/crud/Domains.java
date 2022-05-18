package io.simpleframework.crud;

import io.simpleframework.crud.domain.DomainRepository;
import io.simpleframework.crud.exception.DomainClassIllegalException;
import io.simpleframework.crud.exception.DomainNotFoundException;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("all")
public final class Domains {
    private static final Map<Class<?>, DomainRepository> REPOS = new ConcurrentHashMap<>();
    private static DomainEventPublisher eventPublisher;

    
    @SneakyThrows
    public static <T> T findById(Class<T> clazz, Serializable id) {
        return findById(clazz, id, true);
    }

    
    @SneakyThrows
    public static <T> T findById(Class<T> clazz, Serializable id, boolean throwException) {
        Constructor<T> constructor = SimpleCrudUtils.getConstructor(clazz, id.getClass());
        if (constructor == null) {
            throw new DomainClassIllegalException(clazz, "Domain only support NoArgsConstructor or OneArgsConstructor");
        }
        T domainModel = constructor.getParameterCount() == 0 ?
                constructor.newInstance() : constructor.newInstance(id);
        return findById(domainModel, id, throwException);
    }

    
    public static <T> T findById(T domainModel, Serializable id) {
        return findById(domainModel, id, true);
    }

    
    public static <T> T findById(T domainModel, Serializable id, boolean throwException) {
        Class<?> clazz = domainModel.getClass();
        DomainRepository repo = repo(clazz);
        T result = (T) repo.findById(domainModel, id);
        if (result == null && throwException) {
            throw new DomainNotFoundException(clazz, id);
        }
        return result;
    }

    
    public static <R extends Serializable> R save(Object domainModel, Object... domainEvents) {
        if (domainModel == null) {
            return null;
        }
        Class modelClass = domainModel.getClass();
        DomainRepository repo = repo(modelClass);
        R modelId = (R) repo.save(domainModel);

        publishEvent(domainEvents);
        return modelId;
    }

    
    public static void deleteById(Class<?> clazz, Serializable id, Object... domainEvents) {
        if (id == null) {
            return;
        }
        DomainRepository repo = repo(clazz);
        repo.deleteById(id);

        publishEvent(domainEvents);
    }

    
    public static void deleteByIds(Class<?> clazz, Collection<? extends Serializable> ids, Object... domainEvents) {
        if (ids.isEmpty()) {
            return;
        }
        DomainRepository repo = repo(clazz);
        for (Serializable id : ids) {
            repo.deleteById(id);
        }
        publishEvent(domainEvents);
    }

    
    public static void eventPublisher(DomainEventPublisher publisher) {
        eventPublisher = publisher;
    }

    private static DomainRepository repo(Class<?> clazz) {
        DomainRepository repo = REPOS.get(clazz);
        if (repo == null) {
            try {
                repo = DomainRepository.of(clazz);
            } catch (DomainClassIllegalException e) {
                throw e;
            } catch (Exception e) {
                throw new DomainClassIllegalException(clazz, e);
            }
            REPOS.put(clazz, repo);
        }
        return repo;
    }

    private static void publishEvent(Object... domainEvents) {
        if (eventPublisher == null || domainEvents == null) {
            return;
        }
        for (Object event : domainEvents) {
            eventPublisher.publish(event);
        }
    }

}
