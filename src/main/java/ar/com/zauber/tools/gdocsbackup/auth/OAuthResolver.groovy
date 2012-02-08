/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup.auth

import com.google.gdata.client.authn.oauth.GoogleOAuthParameters
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer

/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
class OAuthResolver extends GDataAuthResolver {
    String consumer_key, consumer_secret

    public OAuthResolver(String consumer_key, String consumer_secret, String appName) {
        super(appName)
        this.consumer_key    = consumer_key
        this.consumer_secret = consumer_secret
    }


    def loadCredentials() {
        GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters()
        oauthParameters.setOAuthConsumerKey    consumer_key
        oauthParameters.setOAuthConsumerSecret consumer_secret

        docsService.setOAuthCredentials(oauthParameters, new OAuthHmacSha1Signer())
    }
}
