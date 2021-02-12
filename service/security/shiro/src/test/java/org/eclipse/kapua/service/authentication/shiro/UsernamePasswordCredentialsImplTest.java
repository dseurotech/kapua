/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.authentication.shiro;

import org.eclipse.kapua.qa.markers.junit.JUnitTests;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JUnitTests.class)
public class UsernamePasswordCredentialsImplTest extends Assert {

    String[] usernames, passwords, newUsernames, newPasswords, trustKeys, authenticationCodes;
    UsernamePasswordCredentialsImpl usernamePasswordCredentialsImpl;

    @Before
    public void initialize() {
        usernames = new String[]{null, "", "user_name123!!", "user#999username", "USERNAME_9", "user,,,,name", "... us_er%%67na*(me"};
        passwords = new String[]{null, "", "pass-word0000@!!,,,#", "!@#00PaSSwOrD.", " password ---44<>", "pA_ss0###woE**9()", "    pass0wo-rd  12344*&^%"};
        newUsernames = new String[]{null, "", "NEW---user_name123!!", "user#999username,.,@#NEW", "1111USERNAME_9", "   new--^%4user,,,,name", "... us_er%%67na*(me   NEW  "};
        newPasswords = new String[]{null, "", "pass-word0000@!!,new password,,#", "!@#00PaSSwOrD._@#new", "new    password ---44<>", "   new#@$pA_ss0###woE**9()", "    pass0wo-rd  12344*NEW&^%"};
        trustKeys = new String[]{null, "", "!!trust key-1", "#1(TRUST KEY.,/trust key)9--99", "!$$ 1-2 KEY//", "trust 99key(....)<00>"};
        authenticationCodes = new String[]{null, "", "  authentication@#$%Code=t110.,<> code", "(!!)432j&^authenti)(&%cation-Code$#3t", "##<>/.CODE    ", "__J!#W(-8T    ", "authenticatioN&* 99code0t  ", "jwt987)_=;'''     .", "jwt CODE-123"};
        usernamePasswordCredentialsImpl = new UsernamePasswordCredentialsImpl("username", "password");
    }

    @Test
    public void usernamePasswordCredentialsImplTest() {
        for (String username : usernames) {
            for (String password : passwords) {
                UsernamePasswordCredentialsImpl usernamePasswordCredentialsImpl = new UsernamePasswordCredentialsImpl(username, password);
                assertEquals("Expected and actual values should be the same.", username, usernamePasswordCredentialsImpl.getUsername());
                assertEquals("Expected and actual values should be the same.", username, usernamePasswordCredentialsImpl.getPrincipal());
                assertEquals("Expected and actual values should be the same.", password, usernamePasswordCredentialsImpl.getPassword());
                assertEquals("Expected and actual values should be the same.", password, usernamePasswordCredentialsImpl.getCredentials());
                assertNull("Null expected.", usernamePasswordCredentialsImpl.getAuthenticationCode());
                assertNull("Null expected.", usernamePasswordCredentialsImpl.getTrustKey());
            }
        }
    }

    @Test
    public void setAndGetUsernameAndPrincipalTest() {
        for (String newUsername : newUsernames) {
            usernamePasswordCredentialsImpl.setUsername(newUsername);
            assertEquals("Expected and actual values should be the same.", newUsername, usernamePasswordCredentialsImpl.getUsername());
            assertEquals("Expected and actual values should be the same.", newUsername, usernamePasswordCredentialsImpl.getPrincipal());
        }
    }

    @Test
    public void setAndGetPasswordAndCredentialsTest() {
        for (String newPassword : newPasswords) {
            usernamePasswordCredentialsImpl.setPassword(newPassword);
            assertEquals("Expected and actual values should be the same.", newPassword, usernamePasswordCredentialsImpl.getPassword());
            assertEquals("Expected and actual values should be the same.", newPassword, usernamePasswordCredentialsImpl.getCredentials());
        }
    }

    @Test
    public void setAndGetAuthenticationCodeTest() {
        for (String authenticationCode : authenticationCodes) {
            usernamePasswordCredentialsImpl.setAuthenticationCode(authenticationCode);
            assertEquals("Expected and actual values should be the same.", authenticationCode, usernamePasswordCredentialsImpl.getAuthenticationCode());
        }
    }

    @Test
    public void setAndGetTrustKeyTest() {
        for (String trustKey : trustKeys) {
            usernamePasswordCredentialsImpl.setTrustKey(trustKey);
            assertEquals("Expected and actual values should be the same.", trustKey, usernamePasswordCredentialsImpl.getTrustKey());
        }
    }
}