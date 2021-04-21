/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

/**
 * The SpringFoxConfig Class.
 * <p>
 * This configuration controls the swagger behaviour.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class SpringFoxConfig {

    @Value("${swagger.title:Maritime Connectivity Platform Service Registry API}" )
    private String swaggerTitle;

    @Value("${swagger.description:Maritime Connectivity Platform Service Registry}" )
    private String swaggerDescription;

    @Value("${swagger.version:0.0}" )
    private String swaggerVersion;

    @Value("${swagger.termsOfServiceUrl:null}" )
    private String swaggerTermsOfServiceUrl;

    @Value("${swagger.contactName:}" )
    private String swaggerContactName;

    @Value("${swagger.contactUrl:}" )
    private String swaggerContactUrl;

    @Value("${swagger.contactEmail:}" )
    private String swaggerContactEmail;

    @Value("${swagger.licence:Apache-2.0}" )
    private String swaggerLicence;

    @Value("${swagger.licenceUrl:http://www.apache.org/licenses/LICENSE-2.0}" )
    private String swaggerLicenceUrl;

    /**
     * Api docket.
     *
     * @return the docket
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(this.apiInfo());
    }

    /**
     * Returns the main API Information.
     *
     * @return the API information object
     */
    private ApiInfo apiInfo() {
        return new ApiInfo(
            this.swaggerTitle,
            this.swaggerDescription,
            this.swaggerVersion,
            this.swaggerTermsOfServiceUrl,
            new Contact(this.swaggerContactName, this.swaggerContactUrl, this.swaggerContactEmail),
            this.swaggerLicence,
            this.swaggerLicenceUrl,
            Collections.emptyList()
        );
    }

}
