/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup.auth;

import com.google.gdata.client.docs.DocsService
import com.google.gdata.client.spreadsheet.SpreadsheetService




/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
abstract class GDataAuthResolver {

    DocsService docsService
    SpreadsheetService spService

    GDataAuthResolver(appName) {
        docsService = new DocsService(appName)
        spService = new SpreadsheetService(appName)
    }

    abstract def loadCredentials()

    def beforeSpreadsheetRequest() {}

    def afterSpreadsheetRequest() {}
}
