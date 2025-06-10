// chaincode/javascript/lib/loanPayment.js
'use strict';

const { Contract } = require('fabric-contract-api');

class LoanPaymentContract extends Contract {
    async InitLedger(ctx) {
        console.info('============= START : Initialize Ledger ===========');
        // Initialization if needed
        console.info('============= END : Initialize Ledger ===========');
    }

    // Record a payment transaction
    async recordPayment(ctx, loanId, paymentId, amount, paymentDate, payer) {
        const payment = {
            docType: 'payment',
            loanId,
            paymentId,
            amount,
            paymentDate,
            payer,
            timestamp: ctx.stub.getTxTimestamp(),
            txId: ctx.stub.getTxID()
        };

        await ctx.stub.putState(paymentId, Buffer.from(JSON.stringify(payment)));
        return JSON.stringify(payment);
    }

    // Get payment by ID
    async getPayment(ctx, paymentId) {
        const paymentAsBytes = await ctx.stub.getState(paymentId);
        if (!paymentAsBytes || paymentAsBytes.length === 0) {
            throw new Error(`${paymentId} does not exist`);
        }
        return paymentAsBytes.toString();
    }

    // Get all payments for a loan (for auditing)
    async getPaymentsByLoan(ctx, loanId) {
        const queryString = {
            selector: {
                docType: 'payment',
                loanId: loanId
            }
        };

        const iterator = await ctx.stub.getQueryResult(JSON.stringify(queryString));
        const allResults = [];
        
        while (true) {
            const res = await iterator.next();
            if (res.value && res.value.value.toString()) {
                const record = JSON.parse(res.value.value.toString('utf8'));
                allResults.push(record);
            }
            if (res.done) {
                await iterator.close();
                return JSON.stringify(allResults);
            }
        }
    }
}

module.exports = LoanPaymentContract;