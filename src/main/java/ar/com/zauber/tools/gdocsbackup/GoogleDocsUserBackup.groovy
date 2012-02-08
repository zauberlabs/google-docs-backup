/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import ar.com.zauber.tools.gdocsbackup.auth.GDataAuthResolver

import com.google.api.client.testing.json.AbstractJsonParserTest.E
import com.google.gdata.client.docs.DocsService





/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
class GoogleDocsUserBackup {

    enum    BackupStrategy { UPDATE, REPLACE };

    private String savePath

    private boolean update

    private Map mediaTypes = [
        documents: "doc",
        spreadsheets: "xls",
        presentations: "ppt"
    ]

    private GDataAuthResolver resolver

    private DocsService client

    // Defines the type of authorization to use
    private BackupStrategy strategy

    public GoogleDocsUserBackup(boolean update, GDataAuthResolver resolver, String savePath) {
        this(update, resolver, savePath)
    }

    public GoogleDocsUserBackup(boolean update, GDataAuthResolver resolver, Map mediaTypes, String savePath) {
        this.update = update
        this.resolver = resolver
        this.client = resolver.docsService
        this.mediaTypes = mediaTypes
        this.savePath = savePath
    }





}
