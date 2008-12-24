package org.apache.shindig.gadgets.stax.model;

/*
 *
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
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.shindig.gadgets.spec.SpecParserException;

public class StaxGadgetSpec extends SpecElement {

  public static final String ELEMENT_NAME = "Module";

  private ModulePrefs modulePrefs = null;

  private List<UserPref> userPrefs = new ArrayList<UserPref>();

  private List<Content> contents = new ArrayList<Content>();

  public StaxGadgetSpec(final QName name) {
    super(name);
  }

  public ModulePrefs getModulePrefs() {
    return modulePrefs;
  }

  public List<UserPref> getUserPrefs() {
    return Collections.unmodifiableList(userPrefs);
  }

  public List<Content> getContents() {
    return Collections.unmodifiableList(contents);
  }

  private void setModulePrefs(final ModulePrefs modulePrefs) {
    this.modulePrefs = modulePrefs;
  }

  private void addUserPref(final UserPref userPref) {
    this.userPrefs.add(userPref);
  }

  protected void addContent(final Content content) throws SpecParserException {
    contents.add(content);
  }

  @Override
  protected void writeChildren(final XMLStreamWriter writer)
      throws XMLStreamException {
    if (modulePrefs != null) {
      modulePrefs.toXml(writer);
    }
    for (UserPref pref : userPrefs) {
      pref.toXml(writer);
    }
    for(Content content: contents) {
      content.toXml(writer);
    }
  }

  @Override
  public void validate() throws SpecParserException {
    // TODO - according to the spec, this is actually wrong.
    if (modulePrefs == null) {
      throw new SpecParserException(name().getLocalPart()
          + " needs a ModulePrefs section!");
    }
    if (contents.size() == 0) {
      throw new SpecParserException(name().getLocalPart()
          + " needs a Content section!");
    }
  }

  public static class Parser<T extends StaxGadgetSpec> extends SpecElement.Parser<StaxGadgetSpec> {
    public Parser() {
      this(new QName(ELEMENT_NAME));
    }

    public Parser(final QName name) {
      super(name);
      register(new ModulePrefs.Parser());
      register(new UserPref.Parser());
      register(new Content.Parser());
    }

    @Override
    protected StaxGadgetSpec newElement() {
      return new StaxGadgetSpec(getName());
    }

    @Override
    protected void addChild(final XMLStreamReader reader,
        final StaxGadgetSpec spec, final SpecElement child) throws SpecParserException {
      if (child instanceof ModulePrefs) {
        spec.setModulePrefs((ModulePrefs) child);
      } else if (child instanceof UserPref) {
        spec.addUserPref((UserPref) child);
      } else if (child instanceof Content) {
        spec.addContent((Content) child);
      } else {
        super.addChild(reader, spec, child);
      }
    }
  }
}
