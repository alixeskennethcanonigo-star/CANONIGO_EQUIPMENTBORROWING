package com.scc.model;

public record BorrowRequest(
        int requestId,
        int borrowerId,
        String borrowerName,
        int equipmentId,
        String equipmentName,
        int quantity,
        String purpose,
        String status,
        String remarks,
        String requestDate
) {
}