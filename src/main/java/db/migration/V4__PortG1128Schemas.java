package db.migration;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.eNav.utils.G1128PortingUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
public class V4__PortG1128Schemas extends BaseJavaMigration {

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

}
