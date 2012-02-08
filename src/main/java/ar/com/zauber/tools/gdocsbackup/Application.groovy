/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import java.net.URL

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
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
public class Application {
    private static DocsService client;

    public static void main(final String[] args) throws Exception {
        //        def cli = new CliBuilder(usage:'ls')
        //        cli.a('display all files')
        //        cli.l(X'use a long listing format')
        //        cli.t('sort by modification time')
        //        def options = cli.parse(args)
        final DocsService client = new DocsService("yourCo-yourAppName-v1")
        client.setUserCredentials("zaubertest.test@gmail.com", "zaubertesteando")

        final DocumentQuery query = new DocumentQuery(new URL("https://docs.google.com/feeds/default/private/full"))

        // Get Everything
        DocumentListFeed allEntries = new DocumentListFeed()
        DocumentListFeed tempFeed = client.getFeed(query, DocumentListFeed.class)
        while (true) {
            allEntries.getEntries().addAll(tempFeed.getEntries())
            Link nextLink = tempFeed.getNextLink()
            if ((nextLink == null) || (tempFeed.getEntries().size() == 0)) {
                break
            }
            tempFeed = client.getFeed(new URL(nextLink.getHref()), DocumentListFeed.class)
        }

        System.out.println("User has " + allEntries.getEntries().size() + " total entries")
        for (DocumentListEntry entry : allEntries.getEntries()) {
            println entry.title.text + " " + entry.type
            println "Downloading ..."

            downloadDocument(entry.resourceId, "local.doc", client)
        }
    }

    public static void downloadDocument(String resourceId, String filepath, DocsService client) {
        String docId = resourceId.substring(resourceId.lastIndexOf(":") + 1)
        String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1)
        String exportUrl = "https://docs.google.com/feeds/download/documents/Export?docId=" +
                docId + "&exportFormat=" + fileExtension
        downloadFile(exportUrl, filepath, client)
    }

    private static HttpTransport transport = new NetHttpTransport()

    static downloadFile(String exportUrl, String filepath, DocsService client) {
        System.out.println("Exporting document from: " + exportUrl)

        MediaContent mc = new MediaContent()
        mc.setUri(exportUrl)
        MediaSource ms = client.getMedia(mc)

        InputStream inStream = null
        FileOutputStream outStream = null

        try {
            inStream = ms.getInputStream()
            outStream = new FileOutputStream(filepath)

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