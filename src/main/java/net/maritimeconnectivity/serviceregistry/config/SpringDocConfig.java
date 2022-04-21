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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SpringFoxConfig Class.
 * <p>
 * This configuration controls the swagger behaviour.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class SpringDocConfig {

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
    public OpenAPI api() {
        return new OpenAPI()
                .info(this.apiInfo());
    }

    /**
     * Returns the main API Information.
     *
     * @return the API information object
     */
    private Info apiInfo() {
        // Create the contact info
        Contact contact = new Contact();
        contact.setName(this.swaggerContactName);
        contact.setUrl(this.swaggerContactUrl);
        contact.setEmail(this.swaggerContactEmail);

        //Create the licence
        License license = new License();
        license.setName(this.swaggerLicence);
        license.setUrl(this.swaggerLicenceUrl);

        // And return the API info
        return new Info()
                .title(this.swaggerTitle)
                .description(this.swaggerDescription)
                .version(this.swaggerVersion)
                .termsOfService(this.swaggerTermsOfServiceUrl)
                .contact(contact)
                .license(license);
    }

}
