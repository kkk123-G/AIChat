package xyz.qvtrmx.ai.chat.model;

public class HourlyUsage {

    private Long userId;
    private String hourKey;
    private Long requestCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHourKey() {
        return hourKey;
    }

    public void setHourKey(String hourKey) {
        this.hourKey = hourKey;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }
}
