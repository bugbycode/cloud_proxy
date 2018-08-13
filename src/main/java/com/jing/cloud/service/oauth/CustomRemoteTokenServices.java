package com.jing.cloud.service.oauth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
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

    private HttpsClient client;
    
	private AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();
	
	public CustomRemoteTokenServices(String checkTokenEndpointUrl, String clientId, String clientSecret,
			String keystorePath,String keystorePassword) {
		this.checkTokenEndpointUrl = checkTokenEndpointUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.client = new HttpsClient(keystorePath, keystorePassword);
	}


	@Override
	public OAuth2Authentication loadAuthentication(String accessToken)
			throws AuthenticationException, InvalidTokenException {
		String jsonStr = client.checkToken(checkTokenEndpointUrl, clientId, clientSecret, accessToken);
		try {
			JSONObject json = new JSONObject(jsonStr);
			@SuppressWarnings("unchecked")
			Iterator<String> it = json.keys();
			Map<String,Object> map = new HashMap<String,Object>(); 
			while(it.hasNext()) {
				String key = it.next();
				if("authorities".equals(key) || "scope".equals(key)) {
					JSONArray arr = json.getJSONArray(key);
					int len =arr.length();
					Collection<String> collection = new ArrayList<String>();
					for(int index = 0;index < len;index++) {
						collection.add(arr.getString(index));
					}
					map.put(key, collection);
				}else {
					map.put(key, json.get(key));
				}
			}
			
			if (map.containsKey("error")) {
				if (logger.isDebugEnabled()) {
					logger.debug("check_token returned error: " + map.get("error"));
				}
				throw new InvalidTokenException(accessToken);
			}
			
			if (!Boolean.TRUE.equals(map.get("active"))) {
				logger.debug("check_token returned active attribute: " + map.get("active"));
				throw new InvalidTokenException(accessToken);
			}
			
			return tokenConverter.extractAuthentication(map);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

}
