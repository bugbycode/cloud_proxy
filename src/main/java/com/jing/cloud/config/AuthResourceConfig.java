package com.jing.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
public class AuthResourceConfig extends ResourceServerConfigurerAdapter {
	
	@Value("${spring.oauth.clientId}")
	private String clientId;
	
	@Value("${spring.oauth.secret}")
	private String secret;
	
	@Value("${spring.oauth.checkTokenUri}")
	private String url;

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		RemoteTokenServices tokenService = new RemoteTokenServices();
		tokenService.setClientId(clientId);

        tokenService.setClientSecret(secret);

        tokenService.setCheckTokenEndpointUrl(url);

        resources.tokenServices(tokenService);
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		super.configure(http);
	}
	
	
}
