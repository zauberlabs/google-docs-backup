/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import ar.com.zauber.tools.gdocsbackup.auth.ClientLoginResolver
import ar.com.zauber.tools.gdocsbackup.auth.OAuthResolver

/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
public class Application {
    public static void main(String[] args) throws Exception {

        def cli = new CliBuilder(usage:'gdocsbackup.jar options', parser: new org.apache.commons.cli.GnuParser())
        cli.with {
            email           args: 1, argName: 'email', 'Email when using clientlogin'
            password        args: 1, argName: 'password', 'Password when using clientlogin'
            target          args: 1, argName: 'path', 'Folder to save the data to'
            consumer_key    args: 1, argName: 'consumer_key', 'Consumer key when using two-legged OAuth'
            consumer_secret args: 1, argName: 'consumer_secret', 'Consumer secret when using two-legged OAuth'

            requestor_id    args: 1, argName: 'requestor_id', 'Email of the account to inspect into'

            all_domain      'Take docs from the whole domain when using oauth login'
            oauth           'Enables two-legged OAuth permission, uses clientlogin by default'
        }
        def options = cli.parse(args)

        println options.email

        def resolver

        if (!options.oauth) {
            println "Insert your google password: "
            String password = options.password

            if (password == "false" || password == null) {
                password = System.console().readPassword().toString()
            }


            println "Login with ${options.email}"
            resolver = new ClientLoginResolver(options.email, password, "")
        } else {
            resolver = new OAuthResolver(options.consumer_key, options.consumer_secret, "")
        }

        String outputPath = options.target

        if (outputPath == null || outputPath == "false") {
            outputPath = "./backup";
        }

        def backup = new GoogleDocsUserBackup(false, resolver, outputPath)

        backup.doBackup()
    }
}