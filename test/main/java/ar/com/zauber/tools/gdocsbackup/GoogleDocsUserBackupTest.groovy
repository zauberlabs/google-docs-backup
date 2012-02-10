/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import groovy.swing.binding.JListProperties.*
import spock.lang.*
import ar.com.zauber.tools.gdocsbackup.auth.ClientLoginResolver

import com.google.gdata.client.docs.DocsService
import com.google.gdata.client.spreadsheet.SpreadsheetService
import com.google.gdata.data.ITextConstruct
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
class GoogleDocsUserBackupTest extends Specification {

    ClientLoginResolver clientLogin

    InputStream is                      = Mock()
    FileOutputStreamFactory mockFactory = Mock()


    def setup() {

        is.read() >> -1


        clientLogin = Mock()

        clientLogin.getDocsService() >> {
            DocsService service = Mock()
            service.getFeed(_,_) >> {
                DocumentListFeed feed = Mock()
                feed.getEntries() >> {
                    DocumentListEntry entry1 = Mock()
                    entry1.resourceId >> "document:SOMEID"
                    entry1.title >> {
                        ITextConstruct cons = Mock()
                        cons.plainText >> { "entry1" }
                        cons
                    }
                    entry1.hashCode() >> 1
                    entry1.equals() >> false
                    entry1.type >> "document"
                    DocumentListEntry entry2 = Mock()
                    entry2.resourceId >> "spreadsheet:SOMEID"
                    entry2.title >> {
                        ITextConstruct cons = Mock()
                        cons.plainText >> { "entry2" }
                        cons
                    }
                    entry2.type >> "spreadsheet"
                    entry2.hashCode() >> 2
                    entry2.equals() >> false
                    DocumentListEntry entry3 = Mock()
                    entry3.resourceId >> "presentation:SOMEID"
                    entry3.title >> {
                        ITextConstruct cons = Mock()
                        cons.plainText >> { "entry3" }
                        cons
                    }
                    entry3.type >> "presentation"
                    entry3.hashCode() >> 3
                    entry3.equals() >> false
                    [entry1, entry2, entry3]
                }
                feed.getNextLink() >> null
                feed
            }

            service.getMedia(_) >> {
                MediaSource mc = Mock()
                mc.getInputStream() >> {
                    is
                }
                mc
            }
            service
        }
        clientLogin.spService >> {
             def service = Mock(SpreadsheetService)
             service
        }



        clientLogin.loadCredentials()          >> {}
        clientLogin.afterSpreadsheetRequest()  >> {}
        clientLogin.beforeSpreadsheetRequest() >> {}

        mockFactory.create(_) >> { filename ->
             FileOutputStream fos = Mock()
        }

    }

    def "Test"() {

        when:
            def userBackup = new GoogleDocsUserBackup(true, clientLogin, ".")
            userBackup.fileFactory = mockFactory
            userBackup.doBackup()
        then:
            1 * clientLogin.loadCredentials()
            1 * clientLogin.beforeSpreadsheetRequest()
            1 * clientLogin.afterSpreadsheetRequest()
            3 * mockFactory.create(_)

    }


}
