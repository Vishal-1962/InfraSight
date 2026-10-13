package com.infrasight.dto;

import jakarta.validation.constraints.Size;

public class ResolveForm {

    @Size(max = 1000, message = "Use 1000 characters or fewer.")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
