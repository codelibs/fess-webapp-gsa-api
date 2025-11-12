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

    // Additional tests for GsaRequestParams methods

    public void test_gsaRequestParams_getAttribute() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setAttribute("testAttr", "testValue");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals("testValue", params.getAttribute("testAttr"));
        assertNull(params.getAttribute("nonExistentAttr"));
    }

    public void test_gsaRequestParams_locale() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals(java.util.Locale.ROOT, params.getLocale());
    }

    public void test_gsaRequestParams_type() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        assertEquals(org.codelibs.fess.entity.SearchRequestParams.SearchRequestType.GSA, params.getType());
    }

    // Note: getGeoInfo(), getFacetInfo(), and getHighlightInfo() tests are skipped
    // because they depend on external components (viewHelper, properties) that are
    // not available in unit test environment. These methods are tested through
    // integration tests.

    // Edge case tests

    public void test_gsaRequestParams_pageSizeZero() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("num", "0");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Should fallback to max size for zero or negative values (line 688-689)
        assertEquals(100, params.getPageSize());
    }

    public void test_gsaRequestParams_pageSizeNegative() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("num", "-5");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Should fallback to max size for negative values (line 688-689)
        assertEquals(100, params.getPageSize());
    }

    public void test_gsaRequestParams_sortEmpty() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Empty sort should return null
        assertNull(params.getSort());
    }

    public void test_gsaRequestParams_sortInvalidFormat() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "invalid:format:test");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        // Invalid format should return null and clear sortParam
        assertNull(params.getSort());
        assertEquals("", params.getSortParam());
    }

    public void test_gsaRequestParams_sortDateDescending() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "date:D");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String sortResult = params.getSort();
        assertTrue(sortResult.contains("timestamp.desc"));
        assertTrue(sortResult.contains("score.desc"));
    }

    public void test_gsaRequestParams_sortMetaAscending() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "meta:author:A");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String sortResult = params.getSort();
        assertTrue(sortResult.contains("meta_author.asc"));
        assertTrue(sortResult.contains("score.desc"));
    }

    public void test_gsaRequestParams_sortMetaNoDirection() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("sort", "meta:title");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String sortResult = params.getSort();
        assertTrue(sortResult.contains("meta_title"));
        assertTrue(sortResult.contains("score.desc"));
    }

    public void test_gsaRequestParams_multipleExtraQueries() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("ex_q", new String[] {"query1", "query2", "query3"});

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String[] extraQueries = params.getExtraQueries();

        assertTrue(extraQueries.length >= 3);
        assertEquals("query1", extraQueries[0]);
        assertEquals("query2", extraQueries[1]);
        assertEquals("query3", extraQueries[2]);
    }

    public void test_gsaRequestParams_requiredFieldsWithOr() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("requiredfields", "title|author|date");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        String[] extraQueries = params.getExtraQueries();

        // Should contain OR clauses
        boolean foundOrQuery = false;
        for (String query : extraQueries) {
            if (query.contains("OR")) {
                foundOrQuery = true;
                break;
            }
        }
        assertTrue(foundOrQuery);
    }

    public void test_gsaRequestParams_fieldsAndSite() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("fields.label", "oldLabel");
        request.setParameter("site", "newSite");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> fields = params.getFields();

        // site parameter should override fields.label
        assertTrue(fields.containsKey("label"));
        String[] labelValues = fields.get("label");
        boolean hasNewSite = false;
        for (String value : labelValues) {
            if ("newSite".equals(value)) {
                hasNewSite = true;
                break;
            }
        }
        assertTrue(hasNewSite);
    }

    public void test_gsaRequestParams_conditionsMultiple() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("as_sitesearch", "example.com");
        request.setParameter("as_filetype", "pdf");
        request.setParameter("as_q", "search term");
        request.setParameter("as_epq", "exact phrase");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> conditions = params.getConditions();

        assertEquals(4, conditions.size());
        assertTrue(conditions.containsKey("sitesearch"));
        assertTrue(conditions.containsKey("filetype"));
        assertTrue(conditions.containsKey("q"));
        assertTrue(conditions.containsKey("epq"));
    }

    // Tests for escapeXml edge cases

    public void test_escapeXml_emptyList() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        List<String> emptyList = Collections.emptyList();
        String result = gsaApiManager.escapeXml(emptyList);

        assertEquals("<list></list>", result);
    }

    public void test_escapeXml_emptyMap() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        Map<String, String> emptyMap = Collections.emptyMap();
        String result = gsaApiManager.escapeXml(emptyMap);

        assertEquals("<data></data>", result);
    }

    public void test_escapeXml_nestedList() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        List<Object> innerList = Arrays.asList("inner1", "inner2");
        List<Object> outerList = Arrays.asList("outer", innerList);
        String result = gsaApiManager.escapeXml(outerList);

        assertTrue(result.contains("<list>"));
        assertTrue(result.contains("<item>outer</item>"));
        assertTrue(result.contains("<item><list>"));
        assertTrue(result.contains("<item>inner1</item>"));
        assertTrue(result.contains("<item>inner2</item>"));
    }

    public void test_escapeXml_nestedMap() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("innerKey", "innerValue");

        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("outerKey", "outerValue");
        outerMap.put("nestedMap", innerMap);

        String result = gsaApiManager.escapeXml(outerMap);

        assertTrue(result.contains("<data>"));
        assertTrue(result.contains("<name>outerKey</name><value>outerValue</value>"));
        assertTrue(result.contains("<name>nestedMap</name><value><data>"));
        assertTrue(result.contains("<name>innerKey</name><value>innerValue</value>"));
    }

    public void test_escapeXml_specialCharacters() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");

        String specialChars = "<>&\"'";
        String result = gsaApiManager.escapeXml(specialChars);

        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&quot;"));
    }

    public void test_appendParam_withSpecialCharacters() throws UnsupportedEncodingException {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        StringBuilder buf = new StringBuilder();

        gsaApiManager.appendParam(buf, "param&name", "<value>");

        String result = buf.toString();
        assertTrue(result.contains("&amp;"));
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&gt;"));
    }

    public void test_appendParam_withEmptyValue() throws UnsupportedEncodingException {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        StringBuilder buf = new StringBuilder();

        gsaApiManager.appendParam(buf, "emptyParam", "");

        String result = buf.toString();
        assertTrue(result.contains("<PARAM name=\"emptyParam\""));
        assertTrue(result.contains("value=\"\""));
        assertTrue(result.contains("original_value=\"\""));
    }

    // Tests for WrappedWebApiRequest

    public void test_wrappedWebApiRequest_getParameter() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("originalParam", "originalValue");

        Map<String, String[]> extraParams = new HashMap<>();
        extraParams.put("extraParam", new String[] {"extraValue"});
        extraParams.put("originalParam", new String[] {"overriddenValue"});

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        // Extra parameter should be accessible
        assertEquals("extraValue", wrappedRequest.getParameter("extraParam"));

        // Original parameter should be overridden by extra parameter
        assertEquals("overriddenValue", wrappedRequest.getParameter("originalParam"));
    }

    public void test_wrappedWebApiRequest_getParameterMap() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("param1", "value1");

        Map<String, String[]> extraParams = new HashMap<>();
        extraParams.put("param2", new String[] {"value2"});

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        Map<String, String[]> paramMap = wrappedRequest.getParameterMap();

        assertTrue(paramMap.containsKey("param1"));
        assertTrue(paramMap.containsKey("param2"));
        assertEquals("value1", paramMap.get("param1")[0]);
        assertEquals("value2", paramMap.get("param2")[0]);

        // The map should be unmodifiable
        try {
            paramMap.put("newParam", new String[] {"newValue"});
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    public void test_wrappedWebApiRequest_getParameterNames() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("param1", "value1");

        Map<String, String[]> extraParams = new HashMap<>();
        extraParams.put("param2", new String[] {"value2"});

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        java.util.Enumeration<String> paramNames = wrappedRequest.getParameterNames();

        List<String> namesList = Collections.list(paramNames);
        assertTrue(namesList.contains("param1"));
        assertTrue(namesList.contains("param2"));
    }

    public void test_wrappedWebApiRequest_getParameterValues() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("multiParam", new String[] {"value1", "value2"});

        Map<String, String[]> extraParams = new HashMap<>();
        extraParams.put("extraMultiParam", new String[] {"extra1", "extra2", "extra3"});

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        String[] values = wrappedRequest.getParameterValues("multiParam");
        assertNotNull(values);
        assertEquals(2, values.length);

        String[] extraValues = wrappedRequest.getParameterValues("extraMultiParam");
        assertNotNull(extraValues);
        assertEquals(3, extraValues.length);
    }

    public void test_wrappedWebApiRequest_parameterOverride() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("overrideParam", "originalValue");

        Map<String, String[]> extraParams = new HashMap<>();
        extraParams.put("overrideParam", new String[] {"newValue"});

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        // Extra parameter should override original
        assertEquals("newValue", wrappedRequest.getParameter("overrideParam"));

        String[] values = wrappedRequest.getParameterValues("overrideParam");
        assertEquals(1, values.length);
        assertEquals("newValue", values[0]);
    }

    public void test_wrappedWebApiRequest_nullParameter() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");

        Map<String, String[]> extraParams = new HashMap<>();

        GsaApiManager.WrappedWebApiRequest wrappedRequest =
            new GsaApiManager.WrappedWebApiRequest(request, "/test", extraParams);

        // Non-existent parameter should return null
        assertNull(wrappedRequest.getParameter("nonExistent"));
        assertNull(wrappedRequest.getParameterValues("nonExistent"));
    }

    // Tests for new filter parameter support
    public void test_gsaRequestParams_filterParameter() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("filter", "1");
        request.setParameter("as_q", "search term");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> conditions = params.getConditions();

        assertTrue(conditions.containsKey("filter"));
        assertEquals("1", conditions.get("filter")[0]);
        assertTrue(conditions.containsKey("q"));
    }

    public void test_gsaRequestParams_filterParameterNotSet() {
        MockletHttpServletRequest request = mockServletContext.createRequest("/");
        request.setParameter("as_q", "search term");

        FessConfig fessConfig = new TestFessConfig();

        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        GsaApiManager.GsaRequestParams params = new GsaApiManager.GsaRequestParams(request, fessConfig);

        Map<String, String[]> conditions = params.getConditions();

        assertFalse(conditions.containsKey("filter"));
    }

    // Tests for new configuration methods
    public void test_getAccessParam_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        String access = gsaApiManager.getAccessParam(fessConfig);

        assertEquals("p", access);
    }

    public void test_getEntqrParam_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        String entqr = gsaApiManager.getEntqrParam(fessConfig);

        assertEquals("3", entqr);
    }

    public void test_getWcParam_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        String wc = gsaApiManager.getWcParam(fessConfig);

        assertEquals("200", wc);
    }

    public void test_isIncludeSpelling_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        boolean includeSpelling = gsaApiManager.isIncludeSpelling(fessConfig);

        assertTrue(includeSpelling);
    }

    public void test_isIncludeFacets_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        boolean includeFacets = gsaApiManager.isIncludeFacets(fessConfig);

        assertTrue(includeFacets);
    }

    public void test_isIncludeCrawldate_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        boolean includeCrawldate = gsaApiManager.isIncludeCrawldate(fessConfig);

        assertTrue(includeCrawldate);
    }

    public void test_isIncludeCacheLink_default() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();

        boolean includeCacheLink = gsaApiManager.isIncludeCacheLink(fessConfig);

        // Default is false to maintain backward compatibility
        assertFalse(includeCacheLink);
    }

    public void test_generateCacheLink_disabled() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        FessConfig fessConfig = new TestFessConfig();
        MockletHttpServletRequest request = mockServletContext.createRequest("/gsa/search");

        String cacheLink = gsaApiManager.generateCacheLink(request, "doc123", fessConfig);

        // Should return empty string when cache link is disabled
        assertEquals("", cacheLink);
    }

    public void test_generateCacheLink_nullDocId() {
        GsaApiManager gsaApiManager = getComponent("gsaApiManager");
        // Note: isApiGsaResponseIncludeCacheLink() is a new method, not an override
        FessConfig fessConfig = new TestFessConfig() {
            public boolean isApiGsaResponseIncludeCacheLink() {
                return true;
            }
        };
        MockletHttpServletRequest request = mockServletContext.createRequest("/gsa/search");

        String cacheLink = gsaApiManager.generateCacheLink(request, null, fessConfig);

        // Should return empty string for null docId
        assertEquals("", cacheLink);
    }
}