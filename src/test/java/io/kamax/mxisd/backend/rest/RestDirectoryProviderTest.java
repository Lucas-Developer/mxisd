/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.backend.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RestDirectoryProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(65000);

    private RestDirectoryProvider p;

    private String domain = "example.org";
    private String endpoint = "/directory/search";
    private String byNameSearch = "doe";
    private String byNameAvatar = "http://domain.tld/path/to/avatar.png";
    private String byNameDisplay = "John Doe";
    private String byNameId = "john.doe";
    private String byNameRequest = "{\"by\":\"name\",\"search_term\":\"" + byNameSearch + "\"}";
    private String byNameResponse = "{\"limited\":false,\"results\":[{\"avatar_url\":\"" + byNameAvatar +
            "\",\"display_name\":\"" + byNameDisplay + "\",\"user_id\":\"" + byNameId + "\"}]}";
    private String byNameEmptyResponse = "{\"limited\":false,\"results\":[]}";

    private String byThreepidSearch = "jane";
    private String byThreepidAvatar = "http://domain.tld/path/to/avatar.png";
    private String byThreepidDisplay = "John Doe";
    private String byThreepidId = "john.doe";
    private String byThreepidRequest = "{\"by\":\"threepid\",\"search_term\":\"" + byThreepidSearch + "\"}";
    private String byThreepidResponse = "{\"limited\":false,\"results\":[{\"avatar_url\":\"" + byThreepidAvatar +
            "\",\"display_name\":\"" + byThreepidDisplay + "\",\"user_id\":\"" + byThreepidId + "\"}]}";
    private String byThreepidEmptyResponse = "{\"limited\":false,\"results\":[]}";

    @Before
    public void before() {
        MatrixConfig mxCfg = new MatrixConfig();
        mxCfg.setDomain(domain);
        mxCfg.build();

        RestBackendConfig cfg = new RestBackendConfig();
        cfg.setEnabled(true);
        cfg.setHost("http://localhost:65000");
        cfg.getEndpoints().setDirectory(endpoint);
        cfg.build();

        p = new RestDirectoryProvider(cfg, mxCfg);
    }

    @Test
    public void byNameFound() {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(byNameResponse)
                )
        );

        UserDirectorySearchResult result = p.searchByDisplayName(byNameSearch);
        assertTrue(!result.isLimited());
        assertTrue(result.getResults().size() == 1);
        UserDirectorySearchResult.Result entry = result.getResults().get(0);
        assertNotNull(entry);
        assertTrue(StringUtils.equals(byNameAvatar, entry.getAvatarUrl()));
        assertTrue(StringUtils.equals(byNameDisplay, entry.getDisplayName()));
        assertTrue(StringUtils.equals(new MatrixID(byNameId, domain).getId(), entry.getUserId()));

        verify(postRequestedFor(urlMatching(endpoint))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalTo(byNameRequest))
        );
    }

    @Test
    public void byNameNotFound() {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(byNameEmptyResponse)
                )
        );

        UserDirectorySearchResult result = p.searchByDisplayName(byNameSearch);
        assertTrue(!result.isLimited());
        assertTrue(result.getResults().isEmpty());

        verify(postRequestedFor(urlMatching(endpoint))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalTo(byNameRequest))
        );
    }

    @Test
    public void byThreepidFound() {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(new String(byThreepidResponse.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))
                )
        );

        UserDirectorySearchResult result = p.searchBy3pid(byThreepidSearch);
        assertTrue(!result.isLimited());
        assertTrue(result.getResults().size() == 1);
        UserDirectorySearchResult.Result entry = result.getResults().get(0);
        assertNotNull(entry);
        assertTrue(StringUtils.equals(byThreepidAvatar, entry.getAvatarUrl()));
        assertTrue(StringUtils.equals(byThreepidDisplay, entry.getDisplayName()));
        assertTrue(StringUtils.equals(new MatrixID(byThreepidId, domain).getId(), entry.getUserId()));

        verify(postRequestedFor(urlMatching(endpoint))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalTo(byThreepidRequest))
        );
    }

    @Test
    public void byThreepidNotFound() {
        stubFor(post(urlEqualTo(endpoint))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(byThreepidEmptyResponse)
                )
        );

        UserDirectorySearchResult result = p.searchBy3pid(byThreepidSearch);
        assertTrue(!result.isLimited());
        assertTrue(result.getResults().isEmpty());

        verify(postRequestedFor(urlMatching(endpoint))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalTo(byThreepidRequest))
        );
    }

}
