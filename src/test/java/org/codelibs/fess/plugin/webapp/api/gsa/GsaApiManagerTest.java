/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.plugin.webapp.api.gsa;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.api.WebApiManagerFactory;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;
import org.dbflute.utflute.mocklet.MockletHttpServletRequest;
import org.dbflute.utflute.mocklet.MockletServletContextImpl;

public class GsaApiManagerTest extends LastaFluteTestCase {

    private WebApiManagerFactory webApiManagerFactory;
    private MockletServletContextImpl mockServletContext;

    // Test-specific FessConfig implementation that doesn't rely on properties
    private static class TestFessConfig extends FessConfig.SimpleImpl {
        private static final long serialVersionUID = 1L;

        @Override
        public String getQueryGsaDefaultSort() {
            return "score.desc";
        }

        @Override
        public Integer getPagingSearchPageStartAsInteger() {
            return 0;
        }

        @Override
        public Integer getPagingSearchPageSizeAsInteger() {
            return 10;
        }

        @Override
        public Integer getPagingSearchPageMaxSizeAsInteger() {
            return 100;
        }

        @Override
        public String getIndexFieldTimestamp() {
            return "timestamp";
        }

        @Override
        public String getQueryGsaMetaPrefix() {
            return "meta_";
        }

        @Override
        public String getQueryGsaDefaultLang() {
            return "ja";
        }
    }

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        ComponentUtil.setFessConfig(new TestFessConfig());
        webApiManagerFactory = new WebApiManagerFactory();
        ComponentUtil.register(webApiManagerFactory, "webApiManagerFactory");
        mockServletContext = new MockletServletContextImpl("/");
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_pathPrefix() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        assertEquals("/gsa", gsaApiManager.getPathPrefix());
    }

    public void test_register() {
        GsaApiManager gsaApiManager = new GsaApiManager();
        gsaApiManager.register();

        // Verify that the manager is registered
        assertNotNull(gsaApiManager);
        assertEquals("/gsa", gsaApiManager.getPathPrefix());
    }

    public void test_matches_validGsaPath() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        MockletHttpServletRequest request = mockServletContext.createRequest("/gsa/search");

        // Mock FessConfig to enable GSA API
        ComponentUtil.setFessConfig(new TestFessConfig() {
            @Override
            public boolean isWebApiGsa() {
                return true;
            }
        });

        assertTrue(gsaApiManager.matches(request));
    }

    public void test_matches_invalidPath() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        MockletHttpServletRequest request = mockServletContext.createRequest("/api/search");

        // Mock FessConfig to enable GSA API
        ComponentUtil.setFessConfig(new TestFessConfig() {
            @Override
            public boolean isWebApiGsa() {
                return true;
            }
        });

        assertFalse(gsaApiManager.matches(request));
    }

    public void test_matches_gsaApiDisabled() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        MockletHttpServletRequest request = mockServletContext.createRequest("/gsa/search");

        // Mock FessConfig to disable GSA API
        ComponentUtil.setFessConfig(new TestFessConfig() {
            @Override
            public boolean isWebApiGsa() {
                return false;
            }
        });

        assertFalse(gsaApiManager.matches(request));
    }

    public void test_appendParam_withEncoding() throws UnsupportedEncodingException {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        StringBuilder buf = new StringBuilder();

        gsaApiManager.appendParam(buf, "query", "test search");

        String result = buf.toString();
        assertTrue(result.contains("<PARAM name=\"query\""));
        assertTrue(result.contains("value=\"test search\""));
        assertTrue(result.contains("original_value=\"test+search\""));
        assertTrue(result.endsWith("/>"));
    }

    public void test_appendParam_withOriginal() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        StringBuilder buf = new StringBuilder();

        gsaApiManager.appendParam(buf, "query", "test search", "test+search");

        String result = buf.toString();
        assertTrue(result.contains("<PARAM name=\"query\""));
        assertTrue(result.contains("value=\"test search\""));
        assertTrue(result.contains("original_value=\"test+search\""));
        assertTrue(result.endsWith("/>"));
    }

    public void test_escapeXml_string() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        String result = gsaApiManager.escapeXml("<test>&data\"");

        assertEquals("&lt;test&gt;&amp;data&quot;", result);
    }

    public void test_escapeXml_list() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        List<String> testList = Arrays.asList("item1", "item2", "<item3>");
        String result = gsaApiManager.escapeXml(testList);

        assertTrue(result.contains("<list>"));
        assertTrue(result.contains("<item>item1</item>"));
        assertTrue(result.contains("<item>item2</item>"));
        assertTrue(result.contains("<item>&lt;item3&gt;</item>"));
        assertTrue(result.contains("</list>"));
    }

    public void test_escapeXml_map() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        Map<String, String> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("<key2>", "<value2>");

        String result = gsaApiManager.escapeXml(testMap);

        assertTrue(result.contains("<data>"));
        assertTrue(result.contains("<name>key1</name><value>value1</value>"));
        assertTrue(result.contains("<name>&lt;key2&gt;</name><value>&lt;value2&gt;</value>"));
        assertTrue(result.contains("</data>"));
    }

    public void test_escapeXml_date() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        Date testDate = new Date(1640995200000L); // 2022-01-01 00:00:00 UTC
        String result = gsaApiManager.escapeXml(testDate);

        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    public void test_escapeXml_null() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        String result = gsaApiManager.escapeXml(null);

        assertEquals("", result);
    }

    // Note: writeXmlResponse tests are skipped because they require servlet response context
    // which is not available in unit test environment. These methods are tested through integration tests.

    public void test_gsaRequestParams_construction() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("q", "test query");
        request.setParameter("start", "10");
        request.setParameter("num", "20");
        request.setParameter("sort", "date:D");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals("test query", params.getQuery());
        assertEquals(10, params.getStartPosition());
        assertEquals(20, params.getPageSize());
        assertEquals("date:D", params.getSortParam());
    }

    public void test_gsaRequestParams_defaultValues() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals(0, params.getStartPosition());
        assertEquals(10, params.getPageSize());
        assertEquals("score.desc", params.getSortParam());
    }

    public void test_gsaRequestParams_sortParsing_date() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "date:A");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String sortResult = params.getSort();
        assertTrue(sortResult.contains("timestamp.asc"));
        assertTrue(sortResult.contains("score.desc"));
    }

    public void test_gsaRequestParams_sortParsing_meta() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "meta:title:D");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String sortResult = params.getSort();
        assertTrue(sortResult.contains("meta_title.desc"));
        assertTrue(sortResult.contains("score.desc"));
    }

    public void test_gsaRequestParams_fields() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("fields.title", "Test Title");
        request.setParameter("fields.content", "Test Content");
        request.setParameter("site", "example.com");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> fields = params.getFields();

        assertTrue(fields.containsKey("title"));
        assertTrue(fields.containsKey("content"));
        assertTrue(fields.containsKey("label"));
        assertEquals("Test Title", fields.get("title")[0]);
        assertEquals("Test Content", fields.get("content")[0]);
        assertEquals("example.com", fields.get("label")[0]);
    }

    public void test_gsaRequestParams_extraQueries() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("ex_q", "extra query 1");
        request.setParameter("requiredfields", "title.author");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String[] extraQueries = params.getExtraQueries();

        assertTrue(extraQueries.length >= 1);
        assertEquals("extra query 1", extraQueries[0]);

        // Check that requiredfields is converted to meta query
        boolean foundRequiredFields = false;
        for (String query : extraQueries) {
            if (query.contains("meta_title") && query.contains("meta_author")) {
                foundRequiredFields = true;
                break;
            }
        }
        assertTrue(foundRequiredFields);
    }

    public void test_gsaRequestParams_invalidNumbers() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("start", "invalid");
        request.setParameter("num", "invalid");
        request.setParameter("offset", "invalid");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Should fallback to defaults for invalid numbers
        assertEquals(0, params.getStartPosition());
        assertEquals(10, params.getPageSize());
        assertEquals(0, params.getOffset());
    }

    public void test_gsaRequestParams_pageSizeLimit() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("num", "1000"); // Very large number

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Should be limited to max size
        assertEquals(100, params.getPageSize());
    }

    public void test_gsaRequestParams_languages_fromParam() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("ulang", "en");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String[] languages = params.getLanguages();
        assertEquals(1, languages.length);
        assertEquals("en", languages[0]);
    }

    public void test_gsaRequestParams_languages_default() {
        // Create a mock request that doesn't return Accept-Language header
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        // Note: MockletHttpServletRequest doesn't set Accept-Language by default,
        // so it should fall back to the default language from config

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String[] languages = params.getLanguages();
        assertEquals(1, languages.length);
        assertEquals("ja", languages[0]);
    }

    public void test_gsaRequestParams_conditions() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("as_sitesearch", "example.com");
        request.setParameter("as_filetype", "pdf");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> conditions = params.getConditions();

        assertTrue(conditions.containsKey("sitesearch"));
        assertTrue(conditions.containsKey("filetype"));
        assertEquals("example.com", conditions.get("sitesearch")[0]);
        assertEquals("pdf", conditions.get("filetype")[0]);
    }

    public void test_gsaRequestParams_similarDocHash() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sdh", "abc123");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals("abc123", params.getSimilarDocHash());
    }

    public void test_gsaRequestParams_trackTotalHits() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("track_total_hits", "true");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals("true", params.getTrackTotalHits());
    }
}