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
package org.apache.shindig.gadgets;

import java.io.StringReader;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.apache.shindig.gadgets.stax.model.GadgetSpec;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * Create GadgetSpec objects using a StAX parser, thus allowing
 * storage and recreation of namespaces on the Gadget spec object
 */
public class StaxGadgetSpecFactory extends AbstractGadgetSpecFactory implements GadgetSpecFactory {

    private static final Logger LOG = Logger.getLogger(StaxGadgetSpecFactory.class.getName());

    private final GadgetSpec.Parser parser = new GadgetSpec.Parser();

    @Inject
    public StaxGadgetSpecFactory(final HttpFetcher fetcher,
            final CacheProvider cacheProvider,
            final@Named("shindig.cache.xml.refreshInterval") long refresh) {
        super (fetcher, cacheProvider, refresh);
    }

    protected org.apache.shindig.gadgets.spec.GadgetSpec buildGadgetSpec(final Uri uri, final String xml) throws GadgetException {

        GadgetSpec gadgetSpec = null;

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

            loop:
            while (true) {
                final int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.END_DOCUMENT:
                        reader.close();
                        break loop;
                    case XMLStreamConstants.START_ELEMENT:
                        // This is the root element. Open a gadget spec parser and let it loose...
                        gadgetSpec = parser.parse(reader); // TODO, that parser must be injectable.
                        break;
                    default:
                        break;
                }
            }
        } catch (XMLStreamException xse) {
            throw new SpecParserException("Could not parse GadgetSpec: ", xse);
        }

        parser.validate(gadgetSpec);
        LOG.info("Spec is " + gadgetSpec);
        return null;
    }
}
