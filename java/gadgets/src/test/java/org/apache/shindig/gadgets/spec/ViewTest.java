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

package org.apache.shindig.gadgets.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.StaxTestUtils;
import org.apache.shindig.gadgets.variables.Substitutions;
import org.apache.shindig.gadgets.variables.Substitutions.Type;
import org.junit.Test;

public class ViewTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");

  @Test
  public void testSimpleView() throws Exception {
    String viewName = "VIEW NAME";
    String content = "This is the content";

    String xml = "<Content" +
                 " type=\"html\"" +
                 " view=\"" + viewName + '\"' +
                 " quirks=\"false\"><![CDATA[" +
                    content +
                 "]]></Content>";

    View view = new View(viewName, Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);

    assertEquals(viewName, view.getName());
    assertEquals(false, view.isQuirks());
    assertEquals(Content.Type.HTML, view.getType());
    assertEquals("html", view.getRawType());
    assertEquals(content, view.getContent());
    assertTrue("Default value for sign_owner should be true.", view.isSignOwner());
    assertTrue("Default value for sign_viewer should be true.", view.isSignViewer());
  }

  @Test
  public void testConcatenation() throws Exception {
    String body1 = "Hello, ";
    String body2 = "World!";
    String content1 = "<Content type=\"html\">" + body1 + "</Content>";
    String content2 = "<Content type=\"html\">" + body2 + "</Content>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    View view = new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
    assertEquals(body1 + body2, view.getContent());
  }

  @Test
  public void testNonStandardContentType() throws Exception {
    String contentType = "html-inline";
    String xml = "<Content" +
                 " type=\"" + contentType + '\"' +
                 " quirks=\"false\"><![CDATA[blah]]></Content>";
    View view = new View("default", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);

    assertEquals(Content.Type.HTML, view.getType());
    assertEquals(contentType, view.getRawType());
  }

  @Test(expected = SpecParserException.class)
  public void testContentTypeConflict() throws Exception {
    String content1 = "<Content type=\"html\"/>";
    String content2 = "<Content type=\"url\" href=\"http://example.org/\"/>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void testHrefOnTypeUrl() throws Exception {
    String xml = "<Content type=\"url\"/>";
    new View("default", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);

  }

  @Test(expected = SpecParserException.class)
  public void testHrefMalformed() throws Exception {
    // Unfortunately, this actually does URI validation rather than URL, so
    // most anything will pass. urn:isbn:0321146530 is valid here.
    String xml = "<Content type=\"url\" href=\"fobad@$%!fdf\"/>";
    new View("default", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);
  }

  @Test
  public void testQuirksCascade() throws Exception {
    String content1 = "<Content type=\"html\" quirks=\"true\"/>";
    String content2 = "<Content type=\"html\" quirks=\"false\"/>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    View view = new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
    assertEquals(false, view.isQuirks());
  }

  @Test
  public void testQuirksCascadeReverse() throws Exception {
    String content1 = "<Content type=\"html\" quirks=\"false\"/>";
    String content2 = "<Content type=\"html\" quirks=\"true\"/>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    View view = new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
    assertEquals(true, view.isQuirks());
  }

  @Test
  public void testPreferredHeight() throws Exception {
    String content1 = "<Content type=\"html\" preferred_height=\"100\"/>";
    String content2 = "<Content type=\"html\" preferred_height=\"300\"/>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    View view = new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
    assertEquals(300, view.getPreferredHeight());
  }

  @Test
  public void testPreferredWidth() throws Exception {
    String content1 = "<Content type=\"html\" preferred_width=\"300\"/>";
    String content2 = "<Content type=\"html\" preferred_width=\"172\"/>";
    Content.Parser parser = new Content.Parser(SPEC_URL);
    View view = new View("test", Arrays.asList(StaxTestUtils.parseElement(content1, parser),
        StaxTestUtils.parseElement(content2, parser)), SPEC_URL);
    assertEquals(172, view.getPreferredWidth());
  }

  @Test
  public void testContentSubstitution() throws Exception {
    String xml
        = "<Content type=\"html\">Hello, __MSG_world__ __MODULE_ID__</Content>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "world", "foo __UP_planet____BIDI_START_EDGE__");
    substituter.addSubstitution(Type.USER_PREF, "planet", "Earth");
    substituter.addSubstitution(Type.BIDI, "START_EDGE", "right");
    substituter.addSubstitution(Type.MODULE, "ID", "3");

    View view = new View("default", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL).substitute(substituter);
    assertEquals("Hello, foo Earthright 3", view.getContent());
  }

  @Test
  public void testHrefSubstitution() throws Exception {
    String href = "http://__MSG_domain__/__MODULE_ID__?dir=__BIDI_DIR__";
    String xml = "<Content type=\"url\" href=\"" + href + "\"/>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "domain", "__UP_subDomain__.example.org");
    substituter.addSubstitution(Type.USER_PREF, "subDomain", "up");
    substituter.addSubstitution(Type.BIDI, "DIR", "rtl");
    substituter.addSubstitution(Type.MODULE, "ID", "123");

    View view = new View("default", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL).substitute(substituter);
    assertEquals("http://up.example.org/123?dir=rtl",
                 view.getHref().toString());
  }

  @Test
  public void testHrefRelativeSubstitution() throws Exception {
    String href = "__MSG_foo__";
    String xml = "<Content type=\"url\" href=\"" + href + "\"/>";

    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Type.MESSAGE, "foo", "/bar");

    View view = new View("test", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);
    view = view.substitute(substituter);
    assertEquals(SPEC_URL.resolve(Uri.parse("/bar")), view.getHref());
  }

  @Test
  public void authAttributes() throws Exception {
    String xml = "<Content type='html' sign_owner='false' sign_viewer='false' foo='bar' " +
                 "yo='momma' sub='__MSG_view__'/>";

    View view = new View("test", Collections.singleton(StaxTestUtils.parseElement(xml, new Content.Parser(SPEC_URL))), SPEC_URL);
    Substitutions substituter = new Substitutions();
    substituter.addSubstitution(Substitutions.Type.MESSAGE, "view", "stuff");
    View substituted = view.substitute(substituter);
    assertEquals("bar", substituted.getAttributes().get("foo"));
    assertEquals("momma", substituted.getAttributes().get("yo"));
    assertEquals("stuff", substituted.getAttributes().get("sub"));
    assertFalse("sign_owner parsed incorrectly.", view.isSignOwner());
    assertFalse("sign_viewer parsed incorrectly.", view.isSignViewer());
  }
}
