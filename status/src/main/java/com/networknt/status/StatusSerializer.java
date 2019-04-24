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
 * Interface to allow custom serialization for a Status.
 * 
 * Framework users can define their own format to return an error message to a consumer
 * 
 * @author Dan Dobrin
 */
public interface StatusSerializer {
	/**
	 * Serialize the status and provide a custom format in the iomplementing class
	 * 
	 * @param status The status to be serialized
	 * @return the format Status object, to be serialized and returned to the consumer
	 */
	public String serializeStatus(Status status);
}
