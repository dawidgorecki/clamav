package pl.dawidgorecki.clamav.result;

public class ScanResult {
    private ScanStatus status;
    private String result;
    private String signature;

    public ScanResult(ScanStatus status) {
        this.status = status;
    }

    public ScanResult(ScanStatus status, String result) {
        this.status = status;
        this.result = result;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "status=" + status +
                ", result='" + result + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
