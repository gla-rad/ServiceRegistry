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

package net.maritimeconnectivity.serviceregistry.utils;

public class MsrErrorConstant {
    public static final String LEDGER_NOT_CONNECTED = "The MSR Ledger is not able to connect";
    public static final String LEDGER_REQUEST_STATUS_NOT_FULFILLED = "MSR ledger request status should be set as 'VETTED'";
    public static final String LEDGER_REQUEST_NOT_FOUND = "No ledger request is found";
    public static final String LEDGER_REGISTRATION_FAILED = "Instance registration to the MSR ledger has failed";
}
