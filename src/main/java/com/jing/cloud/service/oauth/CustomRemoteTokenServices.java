package com.jing.cloud.service.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import com.bugbycode.https.HttpsClient;

public class CustomRemoteTokenServices extends RemoteTokenServices {

	private String checkTokenEndpointUrl;

	private String clientId;

	private String clientSecret;

    private String keystorePath;
    
    private String keystorePassword;

	private AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();
	
	public CustomRemoteTokenServices(String checkTokenEndpointUrl, String clientId, String clientSecret,
			String keystorePath,String keystorePassword) {
		this.checkTokenEndpointUrl = checkTokenEndpointUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;
	}


	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {
		HttpsClient client = new HttpsClient(keystorePath, keystorePassword);
		String jsonStr = client.checkToken(checkTokenEndpointUrl, clientId, clientSecret, accessToken);
		try {
			JSONObject json = new JSONObject(jsonStr);
			json.get("");
			return super.loadAuthentication(accessToken);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

}
