<?php
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

class DataServiceServlet extends ApiServlet {
	
	protected static $FORMAT_PARAM = "format";
	protected static $ATOM_FORMAT = "atom";
	protected static $XML_FORMAT = "atom";
	
	public static $PEOPLE_ROUTE = "people";
	public static $ACTIVITY_ROUTE = "activities";
	public static $APPDATA_ROUTE = "appdata";
	public static $MESSAGE_ROUTE = "message";

	public function doGet()
	{
		$this->doPost();
	}

	public function doPut()
	{
		$this->doPost();
	}

	public function doDelete()
	{
		$this->doPost();
	}

	public function doPost()
	{
		try {
			$token = $this->getSecurityToken();
			if ($token == null) {
				$this->sendSecurityError();
				return;
			}
			$inputConverter = $this->getInputConverterForRequest();
			$outputConverter = $this->getOutputConverterForRequest();
			$this->handleSingleRequest($token, $inputConverter, $outputConverter);
		} catch (Exception $e) {
			$code = '500 Internal Server Error';
			header("HTTP/1.0 $code", true);
			echo "<h1>$code - Internal Server Error</h1>\n". $e->getMessage();
			if (Config::get('debug')) {
				echo "\n\n<br>\nDebug backtrace:\n<br>\n<pre>\n";
				echo $e->getTraceAsString();
				echo "\n</pre>\n";
			}
		}
	}

	public function sendError(ResponseItem $responseItem)
	{
		$errorMessage = $responseItem->getErrorMessage();
		switch ($responseItem->getError()) {
			case ResponseError::$BAD_REQUEST:
				$code = '400 Bad Request';
				break;
			case ResponseError::$UNAUTHORIZED:
				$code = '401 Unauthorized';
				break;
			case ResponseError::$FORBIDDEN:
				$code = '403 Forbidden';
				break;
			case ResponseError::$FORBIDDEN:
				$code = '404 Not Found';
				break;
			case ResponseError::$NOT_IMPLEMENTED:
				$code = '501 Not Implemented';
				break;
			case ResponseError::$INTERNAL_ERROR:
			default:
				$code = '500 Internal Server Error';
				break;
		}
		header("HTTP/1.0 $code", true);
		echo "$code - $errorMessage";
		die();
	}

	/**
	 * Handler for non-batch requests (REST only has non-batch requests)
	 */
	private function handleSingleRequest(SecurityToken $token, $inputConverter, $outputConverter)
	{
		$servletRequest = array(
				'url' => substr($_SERVER["REQUEST_URI"], strlen(Config::get('web_prefix') . '/social/rest'))
		);
		if (isset($GLOBALS['HTTP_RAW_POST_DATA'])) {
			$servletRequest['postData'] = $GLOBALS['HTTP_RAW_POST_DATA'];
			if (get_magic_quotes_gpc()) {
				$servletRequest['postData'] = stripslashes($servletRequest['postData']);
			}
		}
		$requestItem = RestRequestItem::createWithRequest($servletRequest, $token, $inputConverter, $outputConverter);
		$responseItem = $this->getResponseItem($this->handleRequestItem($requestItem));
		if ($responseItem->getError() == null) {
			$outputConverter->outputResponse($responseItem, $requestItem);
		} else {
			$this->sendError($responseItem);
		}
	}

	/**
	 * Returns the output converter to use
	 *
	 * @return OutputConverter
	 */
	private function getOutputConverterForRequest()
	{
		$outputFormat = strtolower(trim(! empty($_POST[self::$FORMAT_PARAM]) ? $_POST[self::$FORMAT_PARAM] : (! empty($_GET[self::$FORMAT_PARAM]) ? $_GET[self::$FORMAT_PARAM] : 'json')));
		switch ($outputFormat) {
			case 'xml':
				return new OutputXmlConverter();
			case 'atom':
				return new OutputAtomConverter();
			case 'json':
				return new OutputJsonConverter();
			default:
				// if no output format is set, see if we can match an input format header
				// if not, default to json
				if (isset($_SERVER['CONTENT_TYPE'])) {
					switch ($_SERVER['CONTENT_TYPE']) {
						case 'application/atom+xml':
							return new OutputAtomConverter();
						case 'application/xml':
							return new OutputXmlConverter();
						default:
						case 'application/json':
							return new OutputJsonConverter();
					}
				}
				break;
		}
	}
	
	/**
	 * Returns the input converter to use
	 *
	 * @return InputConverter
	 */
	private function getInputConverterForRequest()
	{
		$inputFormat = $this->getInputRequestFormat();
		switch ($inputFormat) {
			case 'xml':
				return new InputXmlConverter();
			case 'atom':
				return new InputAtomConverter();
			case 'json':
				return new InputJsonConverter();
			default:
				throw new Exception("Unknown format param: $inputFormat");
		}
	}

	/**
	 * Tries to guess the input format based on the Content-Type
	 * header, of if none is set, the format query param
	 *
	 * @return string request format to use
	 */
	private function getInputRequestFormat()
	{
		// input format is defined by the Content-Type header
		// if that isn't set we use the &format= param
		// if that isn't set, we default to json
		if (isset($_SERVER['CONTENT_TYPE'])) {
			switch ($_SERVER['CONTENT_TYPE']) {
				case 'application/atom+xml':
					return 'atom';
				case 'application/xml':
					return 'xml';
				case 'application/json':
				default:
					return 'json';
			}
		} else {
			// if no Content-Type header is set, we assume the input format will be the same as the &format=<foo> param
			// if that isn't set either, we assume json
			return strtolower(trim(! empty($_POST[self::$FORMAT_PARAM]) ? $_POST[self::$FORMAT_PARAM] : (! empty($_GET[self::$FORMAT_PARAM]) ? $_GET[self::$FORMAT_PARAM] : 'json')));
		}
	}

	/**
	 * Returns the route to use (activities, people, appdata, messages)
	 *
	 * @param string $pathInfo
	 * @return string the route name
	 */
	private function getRouteFromParameter($pathInfo)
	{
		$pathInfo = substr($pathInfo, 1);
		$indexOfNextPathSeparator = strpos($pathInfo, "/");
		return $indexOfNextPathSeparator !== false ? substr($pathInfo, 0, $indexOfNextPathSeparator) : $pathInfo;
	}
}