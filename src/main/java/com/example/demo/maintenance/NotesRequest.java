
package com.example.demo.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NotesRequest {

    @NotBlank
    @Size(max = 1000)
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
