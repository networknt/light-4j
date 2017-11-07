package com.networknt.status;

/**
 * Test implementation for the Light framework serialized status
 *   format:
 *   “error”: {
 *          “statusCode”: “500",
 *          “code”: “ERR10010",
 *          “message”: “RUNTIME_EXCEPTION”,
 *          “description”: “Unexpected runtime exception”
 *      }
 * @author Dan Dobrin
 **/
public class ErrorRootStatusSerializer implements StatusSerializer {

	@Override
	public String serializeStatus(Status status) {
		return "{ error : {\"statusCode\":" + status.getStatusCode()
        + ",\"code\":\"" + status.getCode()
        + "\",\"message\":\""
        + status.getMessage() + "\",\"description\":\""
        + status.getDescription() + "\"} }";
	}

}
