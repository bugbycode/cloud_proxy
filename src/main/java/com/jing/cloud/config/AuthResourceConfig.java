package com.jing.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import com.jing.cloud.service.oauth.CustomRemoteTokenServices;

@Configuration
public class AuthResourceConfig extends ResourceServerConfigurerAdapter {
	
	@Value("${spring.oauth.clientId}")
	private String clientId;
	
	@Value("${spring.oauth.secret}")
	private String secret;
	
	@Value("${spring.oauth.checkTokenUri}")
	private String url;

	@Value("${server.keystorePath}")
	private String keystorePath;
	
	@Value("${server.keystorePassword}")
	private String keystorePassword;
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.tokenServices(getRemoteTokenServices());
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/api/*").hasRole("ADMIN");
	}
	
	public RemoteTokenServices getRemoteTokenServices() {
		return new CustomRemoteTokenServices(url, clientId, secret, keystorePath, keystorePassword);
	}
	
}
