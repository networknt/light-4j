package com.networknt.status;

import io.undertow.server.HttpServerExchange;

public class TestStatusWrapper implements StatusWrapper {
    @Override
    public Status wrap(Status status, HttpServerExchange exchange) {
        return new TestStatus(status, exchange);
    }

    private class TestStatus extends Status {
        private String customInfo;

        public TestStatus(Status status, HttpServerExchange exchange) {
            this.setStatusCode(status.getStatusCode());
            this.setCode(status.getCode());
            this.setDescription(status.getDescription());
            this.setMessage(status.getMessage());
            this.setSeverity(status.getSeverity());
            this.setCustomInfo("custom_info");
        }

        public String getCustomInfo() {
            return customInfo;
        }

        public void setCustomInfo(String customInfo) {
            this.customInfo = customInfo;
        }

        @Override
        public String toString() {
            String message = "{\"error\":" + "{\"statusCode\":" + getStatusCode()
                    + ",\"code\":\"" + getCode()
                    + "\",\"message\":\"" + getMessage()
                    + "\",\"description\":\"" + getDescription()
                    + "\",\"customInfo\":\"" + getCustomInfo()
                    + "\",\"severity\":\"" + getSeverity() + "\"}" + "}";
            return message;
        }
    }
}
