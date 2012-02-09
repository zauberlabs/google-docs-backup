/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import ar.com.zauber.tools.gdocsbackup.auth.GDataAuthResolver

import com.google.api.client.testing.json.AbstractJsonParserTest.E
import com.google.gdata.client.DocumentQuery
import com.google.gdata.client.docs.DocsService
import com.google.gdata.data.Link
import com.google.gdata.data.MediaContent
import com.google.gdata.data.docs.DocumentListEntry
import com.google.gdata.data.docs.DocumentListFeed
import com.google.gdata.data.media.MediaSource






/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
class GoogleDocsUserBackup {

    enum    BackupStrategy {
        UPDATE, REPLACE
    }

    FileOutputStreamFactory fileFactory = new FileOutputStreamFactory()

    private String savePath

    private boolean update

    private Map mediaTypes

    private GDataAuthResolver resolver

    private DocsService client

    // Defines the type of authorization to use
    private BackupStrategy strategy

    public GoogleDocsUserBackup(boolean update, GDataAuthResolver resolver, String savePath) {
        this(update, resolver, null, savePath)
    }

    public GoogleDocsUserBackup(boolean update, GDataAuthResolver resolver, Map mediaTypes, String savePath) {
        this.update = update
        this.resolver = resolver
        this.client = resolver.getDocsService()
        this.mediaTypes = mediaTypes
        if (!this.mediaTypes) {
            this.mediaTypes = [
                document: "doc",
                spreadsheet: "xls",
                presentation: "ppt"
            ]
        }
        this.savePath = savePath
    }

    void sync() {

        this.resolver.loadCredentials()

        final DocumentQuery query = new DocumentQuery(new URL("https://docs.google.com/feeds/default/private/full"))

        // Get Everything
        DocumentListFeed allEntries = new DocumentListFeed()
        DocumentListFeed tempFeed = client.getFeed(query, DocumentListFeed.class)
        while (true) {

            println "Adding ${tempFeed.getEntries().size()} elements"
            for (entry in tempFeed.entries) {
                allEntries.entries.add entry
            }
            Link nextLink = tempFeed.getNextLink()
            if ((nextLink == null) || (tempFeed.getEntries().size() == 0)) {
                break
            }
            tempFeed = client.getFeed(new URL(nextLink.getHref()), DocumentListFeed.class)
        }

        System.out.println("User has " + allEntries.getEntries().size() + " total entries")
        for (DocumentListEntry entry : allEntries.getEntries()) {
            println "filename ${entry.title.plainText}"
            downloadEntry(entry.resourceId, entry.title.plainText)
        }

    }

    private void downloadEntry(String resourceId, String entryTitle) {
        String resourceType = resourceId.substring(0, resourceId.lastIndexOf(":"))
        String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1)
        String fileExtension = mediaTypes[resourceType]

        String exportUrl
        if (resourceType == "spreadsheet") {
            resolver.beforeSpreadsheetRequest()
            exportUrl = "https://spreadsheets.google.com/feeds/download/spreadsheets?docId=${docId}&exportFormat=${fileExtension}"
            resolver.afterSpreadsheetRequest()
        } else {
            exportUrl = "https://docs.google.com/feeds/download/${resourceType}s/Export?docId=${docId}&exportFormat=${fileExtension}"
        }

        downloadFile(exportUrl, entryTitle)
    }



    private void downloadFile(String exportUrl, String fileName) {
        System.out.println("Exporting document from: " + exportUrl)

        MediaContent mc = new MediaContent()
        mc.setUri(exportUrl)
        MediaSource ms = client.getMedia(mc)

        InputStream inStream = null
        FileOutputStream outStream = null

        boolean written = false
        try {
            inStream = ms.getInputStream()
            outStream = fileFactory.create("${savePath}/${fileName}")

            int c
            while ((c = inStream.read()) != -1) {
                outStream.write(c)
            }
        } finally {
            if (inStream != null) {
                inStream.close()
            }
            if (outStream != null) {
                outStream.flush()
                outStream.close()
            }
        }
    }
}
