/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.graphalytics.configuration;

/**
 * Wrapper class for exceptions that occur during the initialization phase of Graphalytics.
 *
 * @author Tim Hegeman
 */
public class GraphalyticsLoaderException extends RuntimeException {

	public GraphalyticsLoaderException(String message) {
		super(message);
	}

	public GraphalyticsLoaderException(String message, Throwable throwable) {
		super(message, throwable);
	}

}