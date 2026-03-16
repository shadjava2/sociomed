package cd.senat.medical.dto;

import jakarta.validation.constraints.NotBlank;

public class TwoFactorVerifyRequest {

    @NotBlank
    private String code;

    public TwoFactorVerifyRequest() {}

    public TwoFactorVerifyRequest(String code) { this.code = code; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
