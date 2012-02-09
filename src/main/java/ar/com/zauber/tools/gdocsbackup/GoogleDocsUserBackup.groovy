/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import groovyx.net.http.URIBuilder
import ar.com.zauber.tools.gdocsbackup.auth.GDataAuthResolver

import com.google.api.client.testing.json.AbstractJsonParserTest.E
import com.google.api.client.util.Base64
import com.google.gdata.client.DocumentQuery
import com.google.gdata.client.docs.DocsService
import com.google.gdata.data.DateTime
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

    private static final def shortenedHash = { byte[] hash ->
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(char c in hash) {
            if (i % 16 == 0) {
                builder.append(c)
            }
            i++;
        }
        builder.toString();
    }

    private static final def safeFileName = { String path,
        String resourceName, String resourceId, String resourceFormat ->
        "${path}/${resourceName.replaceAll("/","")}-${resourceId}.${resourceFormat}"
    }

    private static final def googleDocsURL = { resourceType ->
        if (resourceType == "spreadsheet") {
            "https://spreadsheets.google.com/feeds/download/spreadsheets/Export"
        } else {
            "https://docs.google.com/feeds/download/${resourceType}s/Export"
        }
    }

    private String savePath

    private boolean download

    private Map mediaTypes

    private GDataAuthResolver resolver

    private DocsService client

    // Defines the type of authorization to use
    private BackupStrategy strategy

    public GoogleDocsUserBackup(boolean download, GDataAuthResolver resolver, String savePath) {
        this(download, resolver, null, savePath)
    }

    public GoogleDocsUserBackup(boolean download, GDataAuthResolver resolver, Map mediaTypes, String savePath) {
        this.download = download
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
        def allEntries = new DocumentListFeed()
        DocumentListFeed tempFeed = client.getFeed(query, DocumentListFeed.class)
        while (true) {

            allEntries.entries.addAll(tempFeed.entries)

            Link nextLink = tempFeed.nextLink
            if ((nextLink == null) || (tempFeed.entries.size() == 0)) {
                break
            }

            tempFeed = client.getFeed(new URL(nextLink.href), DocumentListFeed.class)
        }

        System.out.println("User has " + allEntries.entries.size() + " total entries")
        for (entry in allEntries.entries) {
            updateEntry entry
        }

    }

    private void updateEntry(DocumentListEntry entry) {

        String resourceId    = entry.resourceId
        String resourceType  = resourceId.substring(0, resourceId.lastIndexOf(":"))
        String docId         = resourceId.substring(resourceId.lastIndexOf(":") + 1)
        String fileExtension = mediaTypes[resourceType]
        String hashedId = shortenedHash(Base64.encode(entry.resourceId.getBytes()))
        String fileName = safeFileName(savePath, entry.title.plainText, hashedId, fileExtension)


        if (fileExtension == null) {
            if (entry.title.plainText.lastIndexOf(".") != -1) {
                fileExtension = entry.title.plainText.substring(entry.title.plainText.lastIndexOf(".") + 1)
            }
            if (!fileExtension) {
                fileExtension = entry.content.mimeType.subType
            }
        }

        DateTime remoteDate = entry.edited

        File localFile = new File(fileName)

        boolean exists = localFile.exists()



        long localDate = localFile.lastModified()
        if (!exists || localDate < remoteDate.value || download) {
            println "Downloading ${fileName}"
            String exportUrl
            switch (resourceType) {
                case "file":
                    downloadFile(entry.content.uri, fileName)
                    break;
                case "spreadsheet":
                    resolver.beforeSpreadsheetRequest()
                    exportUrl = new URIBuilder(googleDocsURL(resourceType)).addQueryParams([key: docId, exportFormat: fileExtension]).toString()
                    try {
                        downloadFile(exportUrl, fileName)
                    } catch (Exception) {
                        downloadFile(entry.content.uri, fileName)
                    }
                    resolver.afterSpreadsheetRequest()
                    break;
                default:
                    exportUrl = new URIBuilder(googleDocsURL(resourceType)).addQueryParams([docId: docId, exportFormat: fileExtension]).toString()
                    try {
                        downloadFile(exportUrl, fileName)
                    } catch (Exception) {
                        downloadFile(entry.content.uri, fileName)
                    }
                    break;
            }
        } else {
            println "Skipping already downloaded file ${fileName}"
        }
    }



    private void downloadFile(String exportUrl, String fileName) {
        MediaContent mc = new MediaContent()
        mc.setUri(exportUrl)
        MediaSource ms = client.getMedia(mc)

        InputStream inStream = null
        FileOutputStream outStream = null

        boolean written = false
        try {
            inStream = ms.getInputStream()
            outStream = fileFactory.create(fileName)
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
