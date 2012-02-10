/*
 * Copyright (c) 2012 Zauber S.A.  -- All rights reserved
 */
package ar.com.zauber.tools.gdocsbackup

import ar.com.zauber.tools.gdocsbackup.auth.ClientLoginResolver
import ar.com.zauber.tools.gdocsbackup.auth.GDataAuthResolver
import ar.com.zauber.tools.gdocsbackup.auth.OAuthResolver

import com.google.gdata.client.appsforyourdomain.UserService
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer
import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType
import com.google.gdata.data.appsforyourdomain.provisioning.UserFeed

/**
 * TODO: Description of the class, Comments in english by default
 *
 *
 * @author Cristian Pereyra
 * @since Feb 8, 2012
 */
public class Application {




    public static void main(String[] args) {

        def app_name = "zauber-gdatabackup"

        def cli = new CliBuilder(usage:'gdocsbackup.jar options', parser: new org.apache.commons.cli.GnuParser())
        cli.with {
            email           args: 1, argName: 'email', 'Email when using clientlogin'
            password        args: 1, argName: 'password', 'Password when using clientlogin, but not required, will be prompted later on'
            target          args: 1, argName: 'path', 'Folder to save the data to'
            consumer_key    args: 1, argName: 'consumer_key', 'Consumer key when using two-legged OAuth'
            consumer_secret args: 1, argName: 'consumer_secret', 'Consumer secret when using two-legged OAuth'

            requestor_id    args: 1, argName: 'requestor_id', 'Email of the account to inspect into'

            domain          args: 1, argName: "domain", 'Domain of the organization to download the assets from'

            download        'Ignores all file timestamps and overwrites all files'
            all_domain      'Take docs from the whole domain when using oauth login'
            oauth           'Enables two-legged OAuth permission, uses clientlogin by default'
            d               'Enables debugigng'
        }
        def options = cli.parse(args)

        def resolvers

        if (!options.oauth) {

            if (!options.email) {
                onParamsError(cli, "Email is required")
                return;
            }

            println "Insert your google password: "
            String password = options.password

            if (password == "false" || password == null) {
                password = System.console().readPassword().toString()
            }

            resolvers = [ new ClientLoginResolver(options.email, password, "") ]
        } else {
            if (options.all_domain) {

                if (!options.consumer_key || !options.consumer_secret || !options.domain) {
                    onParamsError(cli, "Not enough arguments for oauth, key and domain required!")
                    return;
                }

                def emails = getOrganizationEmails(app_name, options.consumer_key, options.consumer_secret, options.domain)

                emails.each {
                    println "Downloading ${it}"
                }
                resolvers = emails.collect { new OAuthResolver(options.consumer_key, options.consumer_secret, app_name, it) }
            } else {

                if (!options.consumer_key || !options.consumer_secret || !options.requestor_id) {
                    onParamsError(cli, "Not enough arguments for oauth, key, secret and requestor_id required!")
                    return;
                }

                resolvers = [ new OAuthResolver(options.consumer_key, options.consumer_secret, app_name, options.requestor_id) ]
            }
        }

        String outputPath = options.target

        if (outputPath == null || outputPath == "false") {
            outputPath = "./backup";
        }


        resolvers.each { GDataAuthResolver resolver ->
            try {
               new GoogleDocsUserBackup(false, resolver, outputPath).doBackup()
            } catch (Exception e) {
                println "Error getting data from ${resolver.owner}"
                if (options.d) {
                    throw e
                }
            }
        }

    }

    private static onParamsError(def cli, def message) {
        println message
        cli.usage()
    }

	private static getOrganizationEmails(String app_name, String consumer_key, String consumer_secret, String domain) {
		UserService service = new UserService(app_name)

		def oauthParameters = new GoogleOAuthParameters()
		oauthParameters.OAuthConsumerKey    = consumer_key
		oauthParameters.OAuthConsumerSecret = consumer_secret
        oauthParameters.setOAuthType OAuthType.TWO_LEGGED_OAUTH

		service.setOAuthCredentials oauthParameters, new OAuthHmacSha1Signer()

		def req = new URL("https://apps-apis.google.com/a/feeds/${domain}/user/2.0");

		def feed = service.getFeed req, UserFeed.class

		feed.entries.collect { "${it.title.text}@${domain}" }
	}
}