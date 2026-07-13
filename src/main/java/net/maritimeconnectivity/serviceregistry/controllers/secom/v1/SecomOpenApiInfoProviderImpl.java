package net.maritimeconnectivity.serviceregistry.controllers.secom.v1;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.grad.secomv2.core.base.SecomConstants.API_PATH;

/**
 * The SECOM v1.0 OpenApi Provider Implementation
 * <p/>
 * Provides the definition of the service OpenAPI documentation so that it can
 * be used for the description of the SECOM v1.0 interfaces.
 *
 * @author - Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class SecomOpenApiInfoProviderImpl {


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
     * Automatically create a group for the SECOM v2 interfaces
     *
     * @return a grouped open api SECOM group
     */
    @Bean
    public GroupedOpenApi secomApiV1() {
        return GroupedOpenApi.builder()
                .group("SECOM V1 API")
                .pathsToMatch(API_PATH + "/" + "v1" + "/**")
                .addOpenApiCustomizer((openAPI) -> {
                    this.copyOpenApi(getSecomOpenApiInfo(), openAPI);
                })
                .build();
    }

    /**
     * A helper function to copy as many fields as possible from one OpenAPI
     * documentation to another.
     *
     * @param source the source OpenAPI documentations
     * @param dest the destination OpenAPI documentation
     */
    private void copyOpenApi(OpenAPI source, OpenAPI dest) {
        if(source.getInfo() != null) dest.setInfo(source.getInfo());
        if(source.getServers() != null) dest.setServers(source.getServers());
        if(source.getExternalDocs() != null) dest.setExternalDocs(source.getExternalDocs());
        if(source.getTags() != null) dest.getTags().addAll(source.getTags());
        if(source.getSecurity() != null) dest.setSecurity(source.getSecurity());
        if(source.getExtensions() != null) dest.setExtensions(source.getExtensions());
    }

    /**
     * Returns the OpenAPI documentation details.
     *
     * @return The OpenAPI documentation details
     */
    private OpenAPI getSecomOpenApiInfo() {
        return new OpenAPI().schema("secom-v1", new Schema<>().$schema("openapi.json"))
                .info(this.apiInfo())
                //.servers(serverUrls.stream().map(url -> new Server().url(url)).toList())
                .externalDocs(new ExternalDocumentation()
                        .description("SpringShop Wiki Documentation")
                        .url("https://springshop.wiki.github.org/docs"));
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
