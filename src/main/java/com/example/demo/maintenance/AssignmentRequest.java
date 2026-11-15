
package com.example.demo.maintenance;

import jakarta.validation.constraints.NotNull;

public class AssignmentRequest {

    @NotNull
    private Long technicianId;

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }
}
