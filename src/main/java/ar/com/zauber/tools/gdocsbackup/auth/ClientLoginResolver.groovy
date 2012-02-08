/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup.auth;

import com.google.gdata.client.GoogleAuthTokenFactory.UserToken


/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
class ClientLoginResolver extends GDataAuthResolver {
    String username
    String password

    UserToken docsToken, spreadsToken

    public ClientLoginResolver(String username, String password, String appName) {
        super(appName)
        this.username = username
        this.password = password
    }


    def loadCredentials() {
        docsService.setUserCredentials(username, password)
        spService.setUserCredentials(username, password)

        docsToken    = docsService.authTokenFactory.authToken
        spreadsToken = spService.authTokenFactory.authToken
    }

    @Override
    def afterSpreadsheetRequest() {
        docsService.userToken = spreadsToken
    }

    @Override
    def beforeSpreadsheetRequest() {
        docsService.userToken = docsToken
    }
}
