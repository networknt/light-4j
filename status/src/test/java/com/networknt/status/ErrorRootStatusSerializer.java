/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
		return "{ \"error\" : {\"statusCode\":" + status.getStatusCode()
        + ",\"code\":\"" + status.getCode()
        + "\",\"message\":\""
        + status.getMessage() + "\",\"description\":\""
        + status.getDescription() + "\"} }";
	}

}
