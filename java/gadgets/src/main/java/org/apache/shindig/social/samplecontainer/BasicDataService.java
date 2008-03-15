/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.DataService;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicDataService implements DataService {

  public ResponseItem<Map<String, Map<String, String>>> getPersonData(
      List<String> ids) {

    Map<String, Map<String, String>> allData
        = XmlStateFileFetcher.get().getAppData();

    // TODO: Use the opensource Collections library
    Map<String, Map<String, String>> data =
        new HashMap<String, Map<String, String>>();

    for (String id : ids) {
      data.put(id, allData.get(id));
    }

    return new ResponseItem<Map<String, Map<String, String>>>(data);
  }

  public ResponseItem updatePersonData(String id, String key, String value) {
    if (!isValidKey(key)) {
      return new ResponseItem<Object>(ResponseError.BAD_REQUEST,
          "The person data key had invalid characters",
          new JSONObject());
    }

    XmlStateFileFetcher.get().setAppData(id, key, value);
    return new ResponseItem<JSONObject>(new JSONObject());
  }

  /**
   * Determines whether the input is a valid key. Valid keys match the regular
   * expression [\w\-\.]+. The logic is not done using java.util.regex.* as
   * that is 20X slower.
   *
   * @param key the key to validate.
   * @return true if the key is a valid appdata key, false otherwise.
   */
  public static boolean isValidKey(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    for (int i = 0; i < key.length(); ++i) {
      char c = key.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          (c == '-') ||
          (c == '_') ||
          (c == '.')) {
        continue;
      }
      return false;
    }
    return true;
  }

}