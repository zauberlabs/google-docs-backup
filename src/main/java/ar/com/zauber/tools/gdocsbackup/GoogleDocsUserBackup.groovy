/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import groovyx.net.http.URIBuilder

import org.apache.commons.codec.digest.DigestUtils

import ar.com.zauber.tools.gdocsbackup.auth.GDataAuthResolver

import com.google.api.client.testing.json.AbstractJsonParserTest.E
import com.google.common.io.Files
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

    private static final def folderizeEmail = { String email ->
        email.replaceAll("@","_at_")
    }

    private static final def shortenedHash = { String hash ->
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(char c in hash) {
            if (i % 8 == 0) {
                builder.append(c)
            }
            i++;
        }
        builder.toString();
    }

    private static final def safeFileName = { String path,
        String resourceName, String hashedId, String resourceFormat ->
        "${path}/${resourceName.replaceAll("/","")}-${hashedId}.${resourceFormat}"
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
                        presentation: "ppt",
                        pdf: "pdf"
                    ]
        }
        this.savePath = savePath

        File path = new File("${this.savePath}/data/")

        if (!path.exists()) {
            path.mkdirs()
        }
    }

    void doBackup() {
        this.resolver.loadCredentials()


        URIBuilder builder = new URIBuilder("https://docs.google.com/feeds/default/private/full")
                                .addQueryParams(resolver.extraURLParams)
        final DocumentQuery query = new DocumentQuery(new URL(builder.toString()))


        println builder.toString()
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
            String file = backupEntry entry

            File tmp = new File(file);

            if (tmp.exists()) {

                File folder = new File("${savePath}/${folderizeEmail(resolver.owner)}")
                if (!folder.exists()) {
                    folder.mkdirs()
                }

                String hashedId = DigestUtils.shaHex(entry.resourceId)

                File target = new File("${savePath}/data/${hashedId[0..1]}/${hashedId[2..-1]}")

                Files.copy(tmp, target)
                Runtime.runtime.exec("ln -s ${target.absolutePath} ${savePath}/${folderizeEmail(resolver.owner)}/${tmp.name}")

            }
            tmp.delete();
        }

    }

    private String backupEntry(DocumentListEntry entry) {

        String resourceId    = entry.resourceId
        String resourceType  = resourceId.substring(0, resourceId.lastIndexOf(":"))
        String docId         = resourceId.substring(resourceId.lastIndexOf(":") + 1)
        String fileExtension = mediaTypes[resourceType]

        if (fileExtension == null) {
            if (entry.title.plainText.lastIndexOf(".") != -1) {
                fileExtension = entry.title.plainText.substring(entry.title.plainText.lastIndexOf(".") + 1)
            }
            if (!fileExtension && resourceType == "drawing") {
                fileExtension = "png"
            }
            if (!fileExtension) {
                fileExtension = entry.content.mimeType.subType
            }
        }


        String hashedId = DigestUtils.shaHex(entry.resourceId)
        String hashedIdShorted = shortenedHash(hashedId)


        String fileName = safeFileName(savePath, entry.title.plainText, hashedIdShorted, fileExtension)

        DateTime remoteDate = entry.edited

        File dir = new File("${savePath}/data/${hashedId[0..1]}/")

        if (!dir.exists()) {
            dir.mkdir();
        }

        File localFile = new File("${savePath}/data/${hashedId[0..1]}/${hashedId[2..-1]}")

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
                    exportUrl = new URIBuilder(googleDocsURL(resourceType)).addQueryParams([key: docId, exportFormat: fileExtension] + resolver.extraURLParams).toString()
                    try {
                        downloadFile(exportUrl, fileName)
                    } catch (Exception) {
                        downloadFile(entry.content.uri, fileName)
                    }
                    resolver.afterSpreadsheetRequest()
                    break;
                default:
                    exportUrl = new URIBuilder(googleDocsURL(resourceType)).addQueryParams([docId: docId, exportFormat: fileExtension] + resolver.extraURLParams).toString()
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
        fileName
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
