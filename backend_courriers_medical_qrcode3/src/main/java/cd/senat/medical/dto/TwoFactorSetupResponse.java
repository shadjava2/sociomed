package cd.senat.medical.dto;

public class TwoFactorSetupResponse {

    private String secret;
    private String otpauthUrl;
    private String qrCodeDataUrl;

    public TwoFactorSetupResponse() {}

    public TwoFactorSetupResponse(String secret, String otpauthUrl, String qrCodeDataUrl) {
        this.secret = secret;
        this.otpauthUrl = otpauthUrl;
        this.qrCodeDataUrl = qrCodeDataUrl;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getOtpauthUrl() { return otpauthUrl; }
    public void setOtpauthUrl(String otpauthUrl) { this.otpauthUrl = otpauthUrl; }

    public String getQrCodeDataUrl() { return qrCodeDataUrl; }
    public void setQrCodeDataUrl(String qrCodeDataUrl) { this.qrCodeDataUrl = qrCodeDataUrl; }
}
