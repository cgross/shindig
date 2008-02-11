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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Validates and wraps a JSON input object into a
 *
 */
public class JsonRpcRequest {
  private final JsonRpcContext context;
  private final List<JsonRpcGadget> gadgets;

  /**
   * Processes the request and returns a JSON object
   * That can be emitted as output.
   */
  public JSONObject process(CrossServletState servletState)
      throws RpcException {
    GadgetServer server = servletState.getGadgetServer();

    JSONObject out = new JSONObject();

    // Dispatch a separate thread for each gadget that we wish to render.
    CompletionService<Gadget> processor =
      new ExecutorCompletionService<Gadget>(server.getConfig().getExecutor());

    for (JsonRpcGadget gadget : gadgets) {
      processor.submit(new JsonRpcGadgetJob(server, context, gadget));
    }

    int numJobs = gadgets.size();
    do {
      try {
        Gadget outGadget = processor.take().get();
        JSONObject gadgetJson = new JSONObject();

        if (outGadget.getTitleURI() != null) {
          gadgetJson.put("titleUrl", outGadget.getTitleURI().toString());
        }
        gadgetJson.put("url", outGadget.getId().getURI().toString())
                  .put("moduleId", outGadget.getId().getModuleId())
                  .put("title", outGadget.getTitle())
                  .put("contentType",
                      outGadget.getContentType().toString().toLowerCase());

        // Features.
        gadgetJson.put("features", new JSONArray());
        for (String feature : outGadget.getRequires().keySet()) {
          gadgetJson.append("features", feature);
        }

        JSONObject prefs = new JSONObject();

        // User pref specs
        for (GadgetSpec.UserPref pref : outGadget.getUserPrefs()) {
          JSONObject up = new JSONObject()
              .put("displayName", pref.getDisplayName())
              .put("type", pref.getDataType().toString().toLowerCase())
              .put("default", pref.getDefaultValue())
              .put("enumValues", pref.getEnumValues());
        }
        gadgetJson.put("userPrefs", prefs);

        // Content
        String iframeUrl = servletState.getIframeUrl(outGadget, null);
        gadgetJson.put("content", iframeUrl);
        out.append("gadgets", gadgetJson);
      } catch (InterruptedException e) {
        throw new RpcException("Incomplete processing", e);
      } catch (ExecutionException e) {
        throw new RpcException("Incomplete processing", e);
      } catch (RuntimeException rte) {
        if (!(rte.getCause() instanceof RpcException)) {
          throw rte;
        }
        RpcException e = (RpcException)rte.getCause();
        // Just one gadget failed; mark it as such.
        try {
          JSONObject errorObj = new JSONObject();
          errorObj.put("url", e.getGadget().getUrl())
                  .put("moduleId", e.getGadget().getModuleId());
          if (e.getCause() instanceof GadgetServer.GadgetProcessException) {
            GadgetServer.GadgetProcessException gpe
                = (GadgetServer.GadgetProcessException)e.getCause();
            for (GadgetException ge : gpe.getComponents()) {
              errorObj.append("errors", e.getMessage());
            }
          } else {
            errorObj.append("errors", e.getMessage());
          }
          out.append("gadgets", errorObj);
        } catch (JSONException je) {
          throw new RpcException("Unable to write JSON", je);
        }
      } catch (JSONException e) {
        throw new RpcException("Unable to write JSON", e);
      } finally {
        numJobs--;
      }
    } while (numJobs > 0);

    return out;
  }

  public JsonRpcRequest(String content) throws RpcException {
    try {
      JSONObject json = new JSONObject(content);
      JSONObject context = json.getJSONObject("context");
      JSONArray gadgets = json.getJSONArray("gadgets");
      if (gadgets.length() == 0) {
        throw new RpcException("No gadgets requested.");
      }
      this.context = new JsonRpcContext(context);

      List<JsonRpcGadget> gadgetList = new LinkedList<JsonRpcGadget>();
      for (int i = 0, j = gadgets.length(); i < j; ++i) {
        gadgetList.add(new JsonRpcGadget(gadgets.getJSONObject(i)));
      }
      this.gadgets = Collections.unmodifiableList(gadgetList);
    } catch (JSONException e) {
      throw new RpcException("Malformed JSON input.", e);
    }
  }
}