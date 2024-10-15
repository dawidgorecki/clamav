package pl.dawidgorecki.clamav;

public record ScanResult(ScanStatus status, String result, String signature) {
}
