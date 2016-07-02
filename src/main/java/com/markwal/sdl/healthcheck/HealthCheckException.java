/*
 * Copyright 2016 Mark van der Wal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markwal.sdl.healthcheck;

@SuppressWarnings("serial")
public class HealthCheckException extends RuntimeException {

	public HealthCheckException(String message) {
		super(message);
	}

	public HealthCheckException(String message, Throwable exception) {
		super(message, exception);
	}

	public HealthCheckException(Throwable exception) {
		super(exception);
	}

}
