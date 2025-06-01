cpackage org.example.loan;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import com.owlike.jackson.databind.ObjectMapper;
import com.owlike.jackson.databind.node.ObjectNode;
import java.util.Arrays;

@Contract(name = "LoanContract")
public class LoanContract {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String checkStudentLoanStatus(final Context ctx, final String studentID, final String requesterRole) {
        ChaincodeStub stub = ctx.getStub();

        try {
            // Step 1: Input validation
            if (studentID == null || studentID.trim().isEmpty()) {
                throw new ChaincodeException("Student ID is required");
            }

            if (!Arrays.asList("Board", "Employer", "Institution").contains(requesterRole)) {
                throw new ChaincodeException("Unauthorized requester role: " + requesterRole);
            }

            // Step 2: Retrieve loan record
            String loanKey = "LOAN_" + studentID;
            String loanRecordJSON = stub.getStringState(loanKey);
            if (loanRecordJSON == null || loanRecordJSON.isEmpty()) {
                throw new ChaincodeException("No loan record found for student ID: " + studentID);
            }

            ObjectNode loanRecord = (ObjectNode) objectMapper.readTree(loanRecordJSON);

            // Step 3: Filter data based on requesterRole
            ObjectNode response = objectMapper.createObjectNode();

            switch (requesterRole) {
                case "Employer":
                    response.put("studentID", studentID);
                    response.set("outstandingBalance", loanRecord.get("balance"));
                    response.set("status", loanRecord.get("status"));
                    break;

                case "Institution":
                    response.put("studentID", studentID);
                    response.set("loanApprovalStatus", loanRecord.get("approvalStatus"));
                    break;

                case "Board":
                    return loanRecord.toString(); // Full loan record
            }

            return response.toString();

        } catch (Exception e) {
            // Emit log event (in real implementat stub.setEvent may be used)
            String errorLog = String.format("{\"studentID\": \"%s\", \"requesterRole\": \"%s\", \"errorMessage\": \"%s\", \"timestamp\": \"%s\"}",
                    studentID, requesterRole, e.getMessage(), java.time.Instant.now().toString());

            stub.setEvent("LoanStatusCheckFailed", errorLog.getBytes());
            throw new ChaincodeException("Failed to retrieve loan status: " + e.getMessage());
        }
    }
}
