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

package net.maritimeconnectivity.serviceregistry.components;

import net.maritimeconnectivity.serviceregistry.models.dto.datatables.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DomainDtoMapperTest {

    /**
     * The Tested Component.
     */
    @InjectMocks
    @Spy
    private DomainDtoMapper<TestDomainClass, TestDtoClass> domainDtoMapper;

    // Class Variables
    private TestDomainClass testDomainClass;
    private TestDtoClass testDtoClass;

    private List<TestDomainClass> testDomainClassList;
    private Page<TestDomainClass> testDomainClassPage;
    private DtPagingRequest dtPagingRequest;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.domainDtoMapper.modelMapper = new ModelMapper();

        // Create the test domain class
        this.testDomainClass = new TestDomainClass();
        this.testDomainClass.setI(1);
        this.testDomainClass.setString("string");
        this.testDomainClass.setBytes("bytes".getBytes());
        this.testDomainClass.setList(Arrays.asList(new Integer[]{1, 2, 3}));

        // Create the test Dto class
        this.testDtoClass = new TestDtoClass();
        this.testDtoClass.setI(1);
        this.testDtoClass.setString("string");
        this.testDtoClass.setBytes("bytes".getBytes());
        this.testDtoClass.setList(Arrays.asList(new Integer[]{1, 2, 3}));

        // Create a test domain class list
        this.testDomainClassList = Collections.singletonList(this.testDomainClass);

        // Create a test domain class page
        this.testDomainClassPage = new PageImpl<>(this.testDomainClassList);

        // Create a test datatables paging request
        DtColumn dtColumn = new DtColumn("id");
        dtColumn.setName("ID");
        dtColumn.setOrderable(true);
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        this.dtPagingRequest = new DtPagingRequest();
        this.dtPagingRequest.setStart(0);
        this.dtPagingRequest.setLength(this.testDomainClassList.size());
        this.dtPagingRequest.setDraw(1);
        this.dtPagingRequest.setSearch(new DtSearch());
        this.dtPagingRequest.setOrder(Collections.singletonList(dtOrder));
        this.dtPagingRequest.setColumns(Collections.singletonList(dtColumn));
    }

    /**
     * Test that we can correctly convert between domain and DTO objects.
     */
    @Test
    void testConvertTo() {
        TestDtoClass result = this.domainDtoMapper.convertTo(this.testDomainClass, TestDtoClass.class);
        assertNotNull(result);
        assertEquals(this.testDomainClass.getI(), result.getI());
        assertEquals(this.testDomainClass.getString(), result.getString());
        assertTrue(Arrays.equals(this.testDomainClass.getBytes(), result.getBytes()));
        assertTrue(Arrays.equals(this.testDomainClass.getList().toArray(), result.getList().toArray()));
    }

    /**
     * Test that we can correctly convert lists between domain and DTO objects.
     */
    @Test
    void testConvertToList() {
        List<TestDtoClass> result = this.domainDtoMapper.convertToList(this.testDomainClassList, TestDtoClass.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(this.testDomainClassList.get(0).getI(), result.get(0).getI());
        assertEquals(this.testDomainClassList.get(0).getString(), result.get(0).getString());
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getBytes(), result.get(0).getBytes()));
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getList().toArray(), result.get(0).getList().toArray()));
    }

    /**
     * Test that we can correctly convert pages between domain and DTO objects.
     */
    @Test
    void testConvertToPage() {
        Page<TestDtoClass> result = this.domainDtoMapper.convertToPage(this.testDomainClassPage, TestDtoClass.class);
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(this.testDomainClassList.get(0).getI(), result.getContent().get(0).getI());
        assertEquals(this.testDomainClassList.get(0).getString(), result.getContent().get(0).getString());
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getBytes(), result.getContent().get(0).getBytes()));
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getList().toArray(), result.getContent().get(0).getList().toArray()));
    }

    /**
     * Test that we can correctly convert datatables pages between domain and
     * DTO objects.
     */
    @Test
    void testConvertToDtPage() {
        DtPage<TestDtoClass> result = this.domainDtoMapper.convertToDtPage(this.testDomainClassPage, this.dtPagingRequest, TestDtoClass.class);
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(this.testDomainClassList.get(0).getI(), result.getData().get(0).getI());
        assertEquals(this.testDomainClassList.get(0).getString(), result.getData().get(0).getString());
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getBytes(), result.getData().get(0).getBytes()));
        assertTrue(Arrays.equals(this.testDomainClassList.get(0).getList().toArray(), result.getData().get(0).getList().toArray()));
    }

}

/**
 * A test domain object class.
 */
class TestDomainClass {
    private int i;
    private String string;
    private byte[] bytes;
    private List<Integer> list;

    public TestDomainClass() {

    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }
};

/**
 * A test DTO object class.
 */
class TestDtoClass {
    private int i;
    private String string;
    private byte[] bytes;
    private List<Integer> list;

    public TestDtoClass() {

    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }
};