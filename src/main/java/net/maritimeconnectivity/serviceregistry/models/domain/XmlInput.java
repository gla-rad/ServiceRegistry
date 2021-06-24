/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.models.domain;

import org.w3c.dom.ls.LSInput;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * The type Xml input.
 *
 * This is a simple implementation of the LSInput interface, to be used for
 * XML resolution and validation.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class XmlInput implements LSInput {

    // Class Variables
    private String publicId;
    private String systemId;
    private BufferedInputStream inputStream;

    /**
     * Instantiates a new Xml input.
     *
     * @param publicId the public id
     * @param sysId    the sys id
     * @param input    the input
     */
    public XmlInput(String publicId, String sysId, InputStream input) {
        this.publicId = publicId;
        this.systemId = sysId;
        this.inputStream = new BufferedInputStream(input);
    }

    /**
     * Gets public ID.
     *
     * @return the public ID
     */
    public String getPublicId() {
        return publicId;
    }

    /**
     * Sets the public ID.
     *
     * @param publicId the public ID
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * ALWAYS NULL.
     *
     * Gets base URI.
     *
     * @return the base URI
     */
    public String getBaseURI() {
        return null;
    }

    /**
     * ALWAYS NULL.
     *
     * Gets byte stream.
     *
     * @return the byte stream
     */
    public InputStream getByteStream() {
        return null;
    }

    /**
     * NOT USED.
     *
     * Sets byte stream.
     *
     * @param byteStream the byte stream
     */
    public void setByteStream(InputStream byteStream) {

    }

    /**
     * NOT USED.
     *
     * Sets base URI,
     *
     * @param baseURI the base URI
     */
    public void setBaseURI(String baseURI) {

    }

    /**
     * ALWAYS FALSE.
     *
     * Gets certified text.
     *
     * @return the certified text
     */
    public boolean getCertifiedText() {
        return false;
    }

    /**
     * NOT USED.
     *
     * Sets certified text.
     *
     * @param certifiedText the certified text
     */
    public void setCertifiedText(boolean certifiedText) {

    }

    /**
     * ALWAYS NULL.
     *
     * Gets the character stream.
     *
     * @return the character stream
     */
    public Reader getCharacterStream() {
        return null;
    }

    /**
     * NOT USED.
     *
     * Sets character stream.
     *
     * @param characterStream the character stream
     */
    public void setCharacterStream(Reader characterStream) {

    }

    /**
     * ALWAYS NULL.
     *
     * Gets encoding.
     *
     * @return the encoding
     */
    public String getEncoding() {
        return null;
    }

    /**
     * NOT USED.
     *
     * Sets enconding.
     *
     * @param encoding the encoding
     */
    public void setEncoding(String encoding) {

    }

    /**
     * Gets the string data.
     *
     * @return the string data
     */
    public String getStringData() {
        synchronized (inputStream) {
            try {
                byte[] input = new byte[inputStream.available()];
                inputStream.read(input);
                String contents = new String(input);
                return contents;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception " + e);
                return null;
            }
        }
    }

    /**
     * NOT USED.
     *
     * Sets string data.
     *
     * @param stringData the string data
     */
    public void setStringData(String stringData) {

    }

    /**
     * Gets system ID.
     *
     * @return the system ID
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets system ID.
     *
     * @param systemId the system ID
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Gets input stream.
     *
     * @return the input stream
     */
    public BufferedInputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets input stream.
     *
     * @param inputStream the input stream
     */
    public void setInputStream(BufferedInputStream inputStream) {
        this.inputStream = inputStream;
    }

}
