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
package org.openwms.core.startup;

import org.openwms.core.event.MergePropertiesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * A LocalServiceInitializer raises a {@link MergePropertiesEvent} whenever the Spring
 * ApplicationContext is started or refreshed.
 *
 * @author Heiko Scherrer
 */
@Component
public class LocalServiceInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalServiceInitializer.class);
    private final ApplicationContext ctx;

    /**
     * Constructor.
     *
     * @param ctx The Spring ApplicationContext used to publish events
     */
    public LocalServiceInitializer(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("CORE Service bundle started. Publishing properties merge event ...");
        ctx.publishEvent(new MergePropertiesEvent(this));
    }
}