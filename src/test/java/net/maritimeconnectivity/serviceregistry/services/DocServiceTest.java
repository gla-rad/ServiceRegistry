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

package net.maritimeconnectivity.serviceregistry.services;

import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Doc;
import net.maritimeconnectivity.serviceregistry.repos.DocRepo;
import net.maritimeconnectivity.serviceregistry.repos.InstanceRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    private DocService docService;

    /**
     * The Doc Repository Mock.
     */
    @Mock
    private DocRepo docRepo;

    /**
     * The Instance Repository Mock.
     */
    @Mock
    private InstanceRepo instanceRepo;

    // Test Variables
    private List<Doc> docs;
    private Pageable pageable;
    private Doc newDoc;
    private Doc existingDoc;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the xmls list
        this.docs = new ArrayList<>();
        for(long i=0; i<10; i++) {
            Doc doc = new Doc();
            doc.setId(i);
            doc.setName(String.format("Test Doc {}", i));
            doc.setComment("No comment");
            doc.setFilecontentContentType("application/pdf");
            doc.setFilecontent(new byte[]{0b00});
            this.docs.add(doc);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new doc
        this.newDoc = new Doc();
        this.newDoc.setName("Doc Name");
        this.newDoc.setComment("No comment");
        this.newDoc.setMimetype("application/pdf");
        this.newDoc.setFilecontentContentType("application/pdf");
        this.newDoc.setFilecontent(new byte[]{0b00});

        // Create an existing doc
        this.existingDoc = new Doc();
        this.existingDoc.setId(100L);
        this.newDoc.setName("Doc Name");
        this.newDoc.setComment("No comment");
        this.newDoc.setMimetype("application/pdf");
        this.newDoc.setFilecontentContentType("application/pdf");
        this.newDoc.setFilecontent(new byte[]{0b00});
    }

    /**
     * Test that we can retrieve all the docs currently present in the
     * database through a paged call.
     */
    @Test
    public void testFindAll() {
        // Created a result page to be returned by the mocked repository
        Page<Doc> page = new PageImpl<>(this.docs.subList(0, 5), this.pageable, this.docs.size());
        doReturn(page).when(this.docRepo).findAll(this.pageable);

        // Perform the service call
        Page<Doc> result = this.docService.findAll(pageable);

        // Test the result
        assertEquals(page.getSize(), result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getSize(); i++){
            assertEquals(result.getContent().get(i), this.docs.get(i));
        }
    }

    /**
     * Test that we can retrieve a single doc entry based on the xml
     * ID and all the eager relationships are loaded.
     */
    @Test
    public void testFindOne() throws DataNotFoundException {
        doReturn(Optional.of(this.existingDoc)).when(this.docRepo).findById(this.existingDoc.getId());

        // Perform the service call
        Doc result = this.docService.findOne(this.existingDoc.getId());

        // Make sure the eager relationships repo call was called
        verify(this.docRepo, times(1)).findById(this.existingDoc.getId());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingDoc.getId(), result.getId());
        assertEquals(this.existingDoc.getName(), result.getName());
        assertEquals(this.existingDoc.getComment(), result.getComment());
        assertEquals(this.existingDoc.getMimetype(), result.getMimetype());
        assertEquals(this.existingDoc.getFilecontentContentType(), result.getFilecontentContentType());
        assertEquals(this.existingDoc.getFilecontent(), result.getFilecontent());
    }

    /**
     * Test that if we do not find the doc we are looking for, a DataNotFound
     * exception will be thrown.
     */
    @Test
    public void testFindOneNotFound() {
        doReturn(Optional.empty()).when(this.docRepo).findById(this.existingDoc.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.docService.findOne(this.existingDoc.getId())
        );
    }

    /**
     * Test that we can save successfully a valid doc.
     */
    @Test
    public void testSave() {
        doReturn(this.existingDoc).when(this.docRepo).save(this.newDoc);

        //Perform the service call
        Doc result = this.docService.save(this.newDoc);

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingDoc.getId(), result.getId());
        assertEquals(this.existingDoc.getName(), result.getName());
        assertEquals(this.existingDoc.getComment(), result.getComment());
        assertEquals(this.existingDoc.getMimetype(), result.getMimetype());
        assertEquals(this.existingDoc.getFilecontentContentType(), result.getFilecontentContentType());
        assertEquals(this.existingDoc.getFilecontent(), result.getFilecontent());
    }

    /**
     * Test that we can successfully delete an existing doc.
     */
    @Test
    public void testDelete() throws DataNotFoundException {
        doReturn(Boolean.TRUE).when(this.docRepo).existsById(this.existingDoc.getId());
        doNothing().when(this.docRepo).deleteById(this.existingDoc.getId());

        // Perform the service call
        this.docService.delete(this.existingDoc.getId());

        // Verify that a deletion call took place in the repository
        verify(this.docRepo, times(1)).deleteById(this.existingDoc.getId());
    }

    /**
     * Test that if we try to delete a non-existing doc then a DataNotFound
     * exception will be thrown.
     */
    @Test
    public void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.docRepo).existsById(this.existingDoc.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.docService.delete(this.existingDoc.getId())
        );
    }
}