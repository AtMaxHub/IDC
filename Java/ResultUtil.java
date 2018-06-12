package com.wbhl.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class ResultUtil {

	private static Logger logger = LoggerFactory.getLogger(ResultUtil.class);

	public static final String AUTH_FAIL = "AuthFail";
	public static final String Error = "Error";
	public static final String PwdErr = "PwdErr";
	public static final String OK = "OK";
	public static final String Info = "Info";
	public static final String ParamError = "ParamError";
	public static final String ParaMap = "ParaMap";
	public static final String LOG_ERR = "ErrorResult={}";

	public static final String Code = "code";
	public static final String CODE_OK = "0";
	public static final String CODE_ERR = "1";

	public static String paramError(HttpServletRequest request) {
		Map<String, Object> resMap = new HashMap<>();
		resMap.put(Code, CODE_ERR);
		resMap.put(Info, ParamError);
		resMap.put(ParaMap, request.getParameterMap());
		resMap.put("URL", request.getRequestURL() + request.getRequestURI());
		String result = JSON.toJSONString(resMap);
		logger.error(LOG_ERR, result);
		return result;
	}

	public static String errResult() {
		return errResult(Error);
	}

	public static String errResult(Object data) {
		Map<String, Object> resMap = new HashMap<>();
		resMap.put(Code, CODE_ERR);
		resMap.put(Info, data);
		String result = JSON.toJSONString(resMap);
		logger.error(LOG_ERR, result);
		return result;
	}

	public static String errResult(Map<String, Object> resMap, HttpServletRequest request) {
		resMap.put(Code, CODE_ERR);
		resMap.put(Info, ParamError);
		resMap.put(ParaMap, request.getParameterMap());
		String result = JSON.toJSONString(resMap);
		logger.error(LOG_ERR, result);
		return result;
	}

	//////////////////
	public static String buildOKResult(Object data) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(Code, CODE_OK);
		resultMap.put(Info, data);
		return JSON.toJSONString(resultMap);
	}

	public static String buildOKResult() {
		return buildOKResult(OK);
	}

}
