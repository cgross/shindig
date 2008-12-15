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
package org.apache.shindig.gadgets.spec;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.variables.Substitutions;

public interface GadgetSpec {
    String DEFAULT_VIEW = "default";

    Locale DEFAULT_LOCALE = new Locale("all", "ALL");


    Uri getUrl();

    String getChecksum();

    ModulePrefs getModulePrefs();

    List<UserPref> getUserPrefs();

    Map<String, View> getViews();

    View getView(String name);

    Object getAttribute(String key);

    void setAttribute(String key, Object o);

    GadgetSpec substitute(Substitutions substituter);
}
