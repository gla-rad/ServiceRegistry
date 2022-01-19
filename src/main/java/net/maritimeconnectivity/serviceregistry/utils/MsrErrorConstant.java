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
    public static final String LEDGER_NOT_CONNECTED = "Unable to connect to the MSR global ledger.";
    public static final String LEDGER_REQUEST_STATUS_NOT_FULFILLED = "Registration to the global MSR ledger requires the request status to be set to 'VETTED'.";
    public static final String LEDGER_REQUEST_NOT_FOUND = "No requested ledger request was not found.";
    public static final String LEDGER_REGISTRATION_FAILED = "Instance registration to the MSR ledger has failed.";
}
