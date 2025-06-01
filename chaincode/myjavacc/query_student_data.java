import com.owlike.jackson.databind.ObjectMapper;
import com.owlike.jackson.databind.node.ObjectNode;
import org.hyperledger.fabric.gateway.Contract;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

public class StudentDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Contract contract;
    private final Connection mysqlConnection;

    public StudentDataService(Contract contract, Connection mysqlConnection) {
        this.contract = contract;
        this.mysqlConnection = mysqlConnection;
    }

    public String queryStudentData(String studentID, String queryType) throws Exception {
        if (studentID == null || studentID.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }

        if (!Arrays.asList("onchain", "offchain", "both").contains(queryType)) {
            throw new IllegalArgumentException("Invalid query type");
        }

        ObjectNode studentData = objectMapper.createObjectNode();

        // Step 1: Query on-chain data from Fabric
        if (queryType.equals("onchain") || queryType.equals("both")) {
            byte[] result = contract.evaluateTransaction("queryOnchainData", studentID);
            if (result != null && result.length > 0) {
                studentData.set("onchain", objectMapper.readTree(new String(result)));
            } else {
                studentData.put("onchain", "No on-chain data found");
            }
        }

        // Step 2: Query off-chain data from MySQL
        if (queryType.equals("offchain") || queryType.equals("both")) {
            String sql = "SELECT transcript, scanned_docs FROM student_documents WHERE student_id = ?";
            try (PreparedStatement stmt = mysqlConnection.prepareStatement(sql)) {
                stmt.setString(1, studentID);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    ObjectNode offchainData = objectMapper.createObjectNode();
                    offchainData.put("transcript", rs.getString("transcript"));
                    offchainData.put("scanned_docs", rs.getString("scanned_docs"));
                    studentData.set("offchain", offchainData);
                } else {
                    studentData.put("offchain", "No off-chain documents found");
                }
            }
        }

        return studentData.toString();
    }
}
