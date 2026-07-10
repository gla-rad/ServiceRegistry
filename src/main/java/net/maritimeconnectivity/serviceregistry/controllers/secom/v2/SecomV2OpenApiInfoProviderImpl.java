package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import org.grad.secomv2.springboot4.openapi.SecomV2OpenApiInfoProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The SECOM OpenApi Provider Implementation
 * <p/>
 * Provides the definition of the service OpenAPI documentation so that it can
 * be used for the description of the SECOM V2 interfaces.
 *
 * @author - Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
public class SecomV2OpenApiInfoProviderImpl implements SecomV2OpenApiInfoProvider {

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

    @Value("${swagger.secomOpenApiConfig:#{null}}" )
    private String secomOpenApiConfig;

    /**
     * Definition of the server URLS to access the API.
     */
    @Value("${swagger.serverUrls:}")
    List<String> serverUrls;

    /**
     * Returns the OpenAPI documentation details.
     *
     * @return The OpenAPI documentation details
     */
    @Override
    public OpenAPI getSecomOpenApiInfo() {
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
