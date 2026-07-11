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

/**
 * An EventBroker manages subscriptions of {@link EventListener}s to types of {@link RootApplicationEvent}s. Listeners can be
 * subscribed and unsubscribed either directly by instance or indirectly by their Spring bean name.
 *
 * @author Heiko Scherrer
 */
public interface EventBroker {

    /**
     * Subscribe the Spring managed bean with the name {@code listenerBeanName} to events of type {@code event}.
     *
     * @param event The type of event to subscribe to
     * @param listenerBeanName The Spring bean name of the listener, the bean must be of type {@link EventListener}
     */
    void subscribe(Class<? extends RootApplicationEvent> event, String listenerBeanName);

    /**
     * Subscribe the given {@code listener} to events of type {@code event}.
     *
     * @param event The type of event to subscribe to
     * @param listener The listener instance to notify when events occur
     */
    void subscribe(Class<? extends RootApplicationEvent> event, EventListener listener);

    /**
     * Unsubscribe the Spring managed bean with the name {@code listenerBeanName} from events of type {@code event}.
     *
     * @param event The type of event to unsubscribe from
     * @param listenerBeanName The Spring bean name of the listener, the bean must be of type {@link EventListener}
     */
    void unsubscribe(Class<? extends RootApplicationEvent> event, String listenerBeanName);

    /**
     * Unsubscribe the given {@code listener} from events of type {@code event}.
     *
     * @param event The type of event to unsubscribe from
     * @param listener The listener instance to remove from the subscription
     */
    void unsubscribe(Class<? extends RootApplicationEvent> event, EventListener listener);
}