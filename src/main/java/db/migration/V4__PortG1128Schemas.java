package db.migration;

import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V4__PortG1128Schemas extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        final Connection connection = context.getConnection();
        //Create a new statement to retrieve the current G1128 XML
        final Statement st = connection.createStatement();
        final ResultSet rs = st.executeQuery("SELECT id, content FROM xml;");
        //For each entry
//        while (rs.next()) {
//            final Long xmlId = rs.getLong("id");
//            final String xml = rs.getString("xml");
//            G1128Schemas<> (org.iala_aism.g1128.v1_5.serviceinstanceschema.ServiceInstance)
//        }
        st.close();
    }

}
