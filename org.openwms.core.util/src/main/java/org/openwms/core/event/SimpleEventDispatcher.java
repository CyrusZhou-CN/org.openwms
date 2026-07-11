/*
 * Copyright 2005-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openwms.core.event;

import org.ameba.exception.ServiceLayerException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A SimpleEventDispatcher is a Spring managed component that stores all subscribers in an
 * in-memory key-value store implementation and calls all subscribers sequentially and
 * synchronously.
 *
 * @author Heiko Scherrer
 */
@Component(value = SimpleEventDispatcher.COMPONENT_NAME)
public class SimpleEventDispatcher implements EventDispatcher {

    /** Springs service name. */
    public static final String COMPONENT_NAME = "simpleEventDispatcher";
    private final Map<Class<? extends RootApplicationEvent>, Set<EventListener>> subscriptions = new ConcurrentHashMap<>();
    private final ApplicationContext ctx;

    /**
     * Autowiring constructor.
     *
     * @param ctx ApplicationContext
     */
    public SimpleEventDispatcher(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Class<? extends RootApplicationEvent> event, EventListener listener) {
        subscriptions.computeIfAbsent(event, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Class<? extends RootApplicationEvent> event, String listenerBeanName) {
        Object instance = ctx.getBean(listenerBeanName);
        if (instance instanceof EventListener i) {
            subscribe(event, i);
        } else {
            throw new ServiceLayerException("The bean with name [%s] is not of type EventListener and cannot subscribe to events".formatted(listenerBeanName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(Class<? extends RootApplicationEvent> event, EventListener listener) {
        var listeners = subscriptions.get(event);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe(Class<? extends RootApplicationEvent> event, String listenerBeanName) {
        Object instance = ctx.getBean(listenerBeanName);
        if (instance instanceof EventListener i) {
            unsubscribe(event, i);
        } else {
            throw new ServiceLayerException("The bean with name [%s] is not of type EventListener and cannot unsubscribe to events".formatted(listenerBeanName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends RootApplicationEvent> void dispatch(T event) {
        if (event == null) {
            return;
        }
        var listeners = subscriptions.get(event.getClass());
        if (listeners != null) {
            listeners.forEach(l -> l.onEvent(event));
        }
    }
}