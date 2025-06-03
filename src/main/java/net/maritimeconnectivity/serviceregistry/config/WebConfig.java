/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.config;

import net.maritimeconnectivity.serviceregistry.components.GeoJsonStringToGeometryConverter;
import net.maritimeconnectivity.serviceregistry.components.StringToG1128SchemaConverter;
import net.maritimeconnectivity.serviceregistry.components.StringToServiceStatusConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The WebConfig Class
 *
 * This is the main configuration class for the Web MVC operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * The String to G1128 Schema Converter.
     */
    @Autowired
    StringToG1128SchemaConverter stringToG1128SchemaConverter;

    /**
     * The String to Service Status Converter.
     */
    @Autowired
    StringToServiceStatusConverter stringToServiceStatusConverter;

    /**
     * The GeoJSON string to Geometry Converter.
     */
    @Autowired
    GeoJsonStringToGeometryConverter geoJsonStringToGeometryConverter;

    /**
     * Add the static resources and webjars to the web resources.
     *
     * @param registry the resource handler registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**",
                        "/static/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/",
                        "classpath:/static/")
                .resourceChain(false);
        registry.setOrder(1);
    }

    /**
     * Make the index.html our main page so that it's being picked up by
     * Thymeleaf.
     *
     * @param registry The View Controller Registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index");
    }

    /**
     * Add the converters between strings and the G1128 Service Instance status
     * and the G1128 Schemas enumerations.
     *
     * @param registry the Formatter Registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToG1128SchemaConverter);
        registry.addConverter(stringToServiceStatusConverter);
        registry.addConverter(geoJsonStringToGeometryConverter);
    }

}
