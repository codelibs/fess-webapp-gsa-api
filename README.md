GSA-compatible Search API Plugin for Fess
[![Java CI with Maven](https://github.com/codelibs/fess-webapp-gsa-api/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-webapp-gsa-api/actions/workflows/maven.yml)
==========================

## Overview

This is GSA Search API plugin for Fess webapp.

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-webapp-gsa-api/).

## Installation

See [Plugin](https://fess.codelibs.org/13.9/admin/plugin-guide.html) of Administration guide.

## Features

This plugin provides a GSA (Google Search Appliance) compatible XML API for Fess, allowing existing GSA clients to seamlessly integrate with Fess search engine.

### Supported GSA Features

- **Core Search Parameters**: q, start, num, offset, sort, site, fields, etc.
- **Advanced Search**: requiredfields, ex_q, as_* parameters
- **Output Control**: xml, xml_no_dtd, getfields, oe/ie encoding
- **Language Support**: ulang, inlang with Accept-Language fallback
- **Response Elements**:
  - Standard GSA XML structure (GSP, TM, Q, RES, R, etc.)
  - Metadata fields via MT elements
  - CRAWLDATE for last modified dates
  - Spelling suggestions
  - Facet navigation (Context, Facet elements)
  - Cache links (configurable)
  - Pagination with NB/NU/PU elements

### Configuration Options

The following configuration properties can be set in `fess_config.properties` to customize GSA API behavior:

#### Response Parameters (Optional)

```properties
# Access level for results (default: p = public)
# Values: p (public), s (secure), a (all)
api.gsa.response.access=p

# Query expansion recursion level (default: 3)
api.gsa.response.entqr=3

# Query expansion return mode (default: 0)
api.gsa.response.entqrm=0

# Word context size for snippets (default: 200)
api.gsa.response.wc=200

# Word context max count (default: 1)
api.gsa.response.wc_mc=1
```

#### Feature Toggles (Optional)

```properties
# Include spelling suggestions in response (default: true)
api.gsa.response.include_spelling=true

# Include facet navigation in response (default: true)
api.gsa.response.include_facets=true

# Include CRAWLDATE element in response (default: true)
api.gsa.response.include_crawldate=true

# Generate cache links in response (default: false for backward compatibility)
# When enabled, generates links like: http://host:port/context/cache?docId=xxx
api.gsa.response.include_cache_link=false
```

### Request Parameters

#### Basic Search Parameters

- `q` - Search query string (required)
- `start` - Starting position for results (default: 0)
- `num` - Number of results per page (default: 10, max: configurable)
- `output` - Output format: `xml` (with DTD) or `xml_no_dtd` (default)
- `getfields` - Dot-separated list of metadata fields to return (e.g., `author.date.category`)

#### Advanced Parameters

- `filter` - Duplicate filtering: `0` (include duplicates) or `1` (exclude duplicates, default)
- `sort` - Sort order:
  - `date:D` or `date:A` - Sort by date (D=descending, A=ascending)
  - `meta:fieldname:D` or `meta:fieldname:A` - Sort by metadata field
- `site` - Site/label filtering
- `requiredfields` - Required metadata fields (`.` for AND, `|` for OR)
- `ex_q` - Extra query terms (multiple values supported)
- `as_*` - Advanced search conditions (e.g., `as_sitesearch`, `as_filetype`)
- `ulang` - User language preference
- `sdh` - Similar document hash for related document search

### Response Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<GSP VER="3.2">
  <TM>0.123</TM>
  <Q>search query</Q>
  <Spelling>
    <Suggestion q="corrected+query">corrected query</Suggestion>
  </Spelling>
  <PARAM name="..." value="..." original_value="..."/>
  <!-- Multiple PARAM elements -->
  <RES SN="1" EN="10">
    <M>1000</M>
    <NB>
      <NU>next_page_url</NU>
    </NB>
    <R N="1">
      <U>http://example.com/page1</U>
      <UE>http://example.com/page1</UE>
      <T>Page Title</T>
      <RK>8</RK>
      <S>Page description snippet...</S>
      <LANG>en</LANG>
      <CRAWLDATE>2025-01-01</CRAWLDATE>
      <MT N="author" V="John Doe"/>
      <ENT_SOURCE>FESS</ENT_SOURCE>
      <FS NAME="date" VALUE="2025-01-01"/>
      <HAS>
        <L>http://host/cache?docId=xxx</L>
        <C SZ="15k" CID="doc_id" ENC="UTF-8"/>
      </HAS>
    </R>
    <!-- More result elements -->
  </RES>
  <Context>
    <Facet>
      <FacetItem>
        <label>Category</label>
        <anchor_text>Technology (50)</anchor_text>
        <anchor_text>Science (30)</anchor_text>
      </FacetItem>
    </Facet>
  </Context>
</GSP>
```
