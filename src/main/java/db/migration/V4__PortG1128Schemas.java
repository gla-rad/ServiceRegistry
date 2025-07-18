/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package db.migration;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.eNav.utils.G1128PortingUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.CRC32;

/**
 * The G1128 Schema Migration
 * <p/>
 * Implements the G1128 migration which is 4th in line to be executed. This
 * migration will port the old G1128 v1.3 existing service registrations to
 * the latest v1.7.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Slf4j
public class V4__PortG1128Schemas extends BaseJavaMigration implements Serializable {

    @Serial
    private static final long serialVersionUID = 4569511869080087442L;

    /**
     * Implements the migration process using Java to take advantage of
     * the G1128  library facilities that can translate the old G1128
     * version 1.3 to  the latest version 1.7.
     * <p/>
     * The process is as follows:
     * <ul>
     *    <li>1. The migration will read all existing G1128 XML-base registrations, including the XML content.</li>
     *    <li>2. The migration will use the G1128PortingUtils utility to upgrade the XML content.</li>
     *    <li>3. The generated results will then be stored to the database.</li>
     * </ul>
     * Any errors that occur will not stop the migration, and will just get logged.
     *
     * @param context the Flyway Context
     * @throws Exception for any exceptions thrown during the operation
     */
    @Override
    public void migrate(Context context) throws Exception {
        final Connection connection = context.getConnection();
        //Create a new statement to retrieve the current G1128 XML
        final Statement st = connection.createStatement();
        final ResultSet rs = st.executeQuery("SELECT id, content FROM xml;");
        // Create a statement tp update the XML
        final PreparedStatement psXML = connection.prepareStatement("UPDATE xml SET content = (?) WHERE id = (?);");
        //For each entry
        while (rs.next()) {
            final Long xmlId = rs.getLong("id");
            final String xml_v1_3 = rs.getString("content");

            // Try to port the XML version and update the database
            try {
                final String xml_v1_7 = G1128PortingUtils.portXMLVersion_1_3_to_1_7(xml_v1_3);

                // Now perform the update statement
                psXML.setString(1, xml_v1_7);
                psXML.setLong(2, xmlId);
                psXML.execute();
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
        psXML.close();
        rs.close();
        st.close();
    }

    /**
     * Returns a checksum for this migration.
     * <p/>
     * CAREFULL!!!
     * The checksum fixed based on the serialization UID of the class. You will
     * need to manually change it if anything is altered in the code.
     *
     * @return the migration checksum
     */
    @Override
    public Integer getChecksum() {
        return (int) serialVersionUID % Integer.MAX_VALUE + 1;
    }

}
