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

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HibernateSearchInitTest {

    /**
     * The Tested Component.
     */
    @InjectMocks
    @Spy
    HibernateSearchInit hibernateSearchInit;

    /**
     * The Entity Manager mock.
     */
    @Mock
    EntityManager entityManager;

    // Test Variables
    private SearchSession searchSession;
    private MassIndexer massIndexer;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() {
        this.searchSession = mock(SearchSession.class);
        this.massIndexer = mock(MassIndexer.class);
    }

    /**
     * Test that the hibernate search will initialise correctly on the
     * application events.
     */
    @Test
    void testOnApplicationEvent() throws InterruptedException {
        try (MockedStatic<Search> mockedSearch = Mockito.mockStatic(Search.class)) {
            mockedSearch.when(() -> Search.session(this.entityManager)).thenReturn(this.searchSession);

            doReturn(this.massIndexer).when(this.searchSession).massIndexer(any(Class.class));
            doReturn(this.massIndexer).when(this.massIndexer).threadsToLoadObjects(anyInt());
            doNothing().when(this.massIndexer).startAndWait();

            // Perform the component call
            this.hibernateSearchInit.onApplicationEvent(null);
        }

        // Verify the indexing initialisation was performed
        verify(this.massIndexer, times(1)).startAndWait();
    }

    /**
     * Test that when the hibernate search will failed to initialise we can
     * still boot the service without an error.
     */
    @Test
    void testOnApplicationEventFailed() throws InterruptedException {
        try (MockedStatic<Search> mockedSearch = Mockito.mockStatic(Search.class)) {
            mockedSearch.when(() -> Search.session(this.entityManager)).thenReturn(this.searchSession);

            doReturn(this.massIndexer).when(this.searchSession).massIndexer(any(Class.class));
            doReturn(this.massIndexer).when(this.massIndexer).threadsToLoadObjects(anyInt());
            doThrow(InterruptedException.class).when(this.massIndexer).startAndWait();

            // Perform the component call
            this.hibernateSearchInit.onApplicationEvent(null);
        }

        // Verify the indexing initialisation was performed
        verify(massIndexer, times(1)).startAndWait();
    }

}