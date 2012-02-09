/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import ar.com.zauber.tools.gdocsbackup.auth.ClientLoginResolver




/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
public class Application {
    public static void main(final String[] args) throws Exception {
        def resolver = new ClientLoginResolver("zaubertest.test@gmail.com", "zaubertesteando", "")

        def backup = new GoogleDocsUserBackup(false, resolver, "./zaubertest.test")

        backup.sync()
    }
}