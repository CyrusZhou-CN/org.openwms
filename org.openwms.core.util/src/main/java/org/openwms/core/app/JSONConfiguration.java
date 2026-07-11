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
package org.openwms.core.app;

import org.ameba.annotation.ExcludeFromScan;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.openwms.core.time.TimeProvider.DATE_FORMAT;
import static org.openwms.core.time.TimeProvider.DATE_TIME_MILLIS_FORMAT;

/**
 * A JSONConfiguration customizes the Jackson JSON mapping with the date and date-time formats used across all OpenWMS.org services.
 *
 * @author Heiko Scherrer
 */
@ExcludeFromScan
@Configuration
public class JSONConfiguration {

    /**
     * Customize the Jackson {@code JsonMapper} to serialize and deserialize {@code LocalDate} and {@code LocalDateTime} types with the
     * formats defined in {@link org.openwms.core.time.TimeProvider}.
     *
     * @return The customizer instance applied by Spring Boot's Jackson auto-configuration
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            var module = new SimpleModule();
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_MILLIS_FORMAT)));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_MILLIS_FORMAT)));
            builder.addModule(module);
        };
    }
}
