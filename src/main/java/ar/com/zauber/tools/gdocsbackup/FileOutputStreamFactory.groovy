/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 9, 2012
 */
class FileOutputStreamFactory {
    FileOutputStream create(String filename) {
        new FileOutputStream(filename)
    }
}
