/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles open proxy requests.
 */
@Singleton
public class ProxyHandler extends ProxyBase {
  private static final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

  private static final Collection<String> DISALLOWED_RESPONSE_HEADERS = Sets.newHashSet(
      "set-cookie", "content-length", "content-encoding", "etag", "last-modified" ,"accept-ranges",
      "vary", "expires", "date", "pragma", "cache-control"
  );

  private final ContentFetcherFactory fetcher;
  private final LockedDomainService lockedDomainService;
  private final ContentRewriterRegistry contentRewriterRegistry;

  @Inject
  public ProxyHandler(ContentFetcherFactory fetcher,
                      LockedDomainService lockedDomainService,
                      ContentRewriterRegistry contentRewriterRegistry) {
    this.fetcher = fetcher;
    this.lockedDomainService = lockedDomainService;
    this.contentRewriterRegistry = contentRewriterRegistry;
  }

  private boolean getIgnoreCache(HttpServletRequest request) {
    String ignoreCache = request.getParameter(IGNORE_CACHE_PARAM);
    if (ignoreCache == null) {
      return false;
    }
    return !ignoreCache.equals("0");
  }

  /**
   * Generate a remote content request based on the parameters sent from the client.
   */
  private HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    Uri url = validateUrl(request.getParameter(URL_PARAM));

    HttpRequest req = new HttpRequest(url);

    req.setContainer(getContainer(request));
    if (request.getParameter(GADGET_PARAM) != null) {
      req.setGadget(Uri.parse(request.getParameter(GADGET_PARAM)));
    }

    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    req.setRewriteMimeType(request.getParameter(REWRITE_MIME_TYPE_PARAM));

    req.setIgnoreCache(getIgnoreCache(request));

    // If the proxy request specifies a refresh param then we want to force the min TTL for
    // the retrieved entry in the cache regardless of the headers on the content when it
    // is fetched from the original source.
    if (request.getParameter(REFRESH_PARAM) != null) {
      try {
        req.setCacheTtl(Integer.parseInt(request.getParameter(REFRESH_PARAM)));
      } catch (NumberFormatException nfe) {
        // Ignore
      }
    }

    return req;
  }

  @Override
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, GadgetException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String host = request.getHeader("Host");
    if (!lockedDomainService.isSafeForOpenProxy(host)) {
      // Force embedded images and the like to their own domain to avoid XSS
      // in gadget domains.
      String msg = "Embed request for url " + getParameter(request, URL_PARAM, "") +
          " made to wrong domain " + host;
      logger.info(msg);
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, msg);
    }

    HttpRequest rcr = buildHttpRequest(request);
    HttpResponse results = fetcher.fetch(rcr);
    if (contentRewriterRegistry != null) {
      results = contentRewriterRegistry.rewriteHttpResponse(rcr, results);
    }

    setResponseHeaders(request, response, results);

    for (Map.Entry<String, String> entry : results.getHeaders().entries()) {
      String name = entry.getKey();
      if (!DISALLOWED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
          response.addHeader(name, entry.getValue());
      }
    }

    if (rcr.getRewriteMimeType() != null) {
      response.setContentType(rcr.getRewriteMimeType());
    }

    if (results.getHttpStatusCode() != HttpResponse.SC_OK) {
      response.sendError(results.getHttpStatusCode());
    }

    IOUtils.copy(results.getResponse(), response.getOutputStream());
  }
}
