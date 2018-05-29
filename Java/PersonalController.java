package com.wbhl.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.wbhl.entity.ComPageResult;
import com.wbhl.entity.OrderRes;
import com.wbhl.pojo.TbAlbum;
import com.wbhl.pojo.TbCoupon;
import com.wbhl.pojo.TbOrder;
import com.wbhl.pojo.TbRecord;
import com.wbhl.pojo.TbWechatUserWithBLOBs;
import com.wbhl.pojo.TbWxPhoto;
import com.wbhl.pojo.TbWxScreenShot;
import com.wbhl.pojo.TbWxVideo;
import com.wbhl.service.AlbumService;
import com.wbhl.service.CouponService;
import com.wbhl.service.OrderService;
import com.wbhl.service.RecordAlbumService;
import com.wbhl.service.RecordService;
import com.wbhl.service.WeChatService;
import com.wbhl.service.WeChatUserService;
import com.wbhl.service.WxPhotoService;
import com.wbhl.service.WxScreenShotService;
import com.wbhl.service.WxVideoService;
import com.wbhl.util.ComStrUtil;
import com.wbhl.util.JsonUtils;
import com.wbhl.util.MapUtil;
import com.wbhl.util.MyDateUtil;
import com.wbhl.util.PageResult;
import com.wbhl.util.PageUtil;
import com.wbhl.util.ResultUtil;
import com.wbhl.wcpay.msg.resp.Article;
import com.wbhl.wcpay.msg.resp.NewsMessage;
import com.wbhl.wcpay.msg.util.MessageUtil;
import com.wbhl.wcpay.utils.SignUtil;

@Controller
@RequestMapping("/personal")
public class PersonalController {

	private static Logger logger = Logger.getLogger(PersonalController.class);

	@Value("${business_domain}")
	private String business_domain;
	@Value("${wxsvc_url_reord}")
	private String wxsvc_url_reord;
	@Value("${COMMON_URL_FOR_GET_OPENID}")
	private String COMMON_URL_FOR_GET_OPENID;
	@Value("${WXSVC_ROOT_URL}")
	private String WXSVC_ROOT_URL;

	@Autowired
	private WxPhotoService wxPhotoService;
	@Autowired
	private WxVideoService wxVideoService;
	@Autowired
	private WxScreenShotService screenShotService;
	@Autowired
	private WeChatService weChatService;
	@Autowired
	private OrderService orderService;
	@Autowired
	private RecordService recordService;
	@Autowired
	private AlbumService albumService;
	@Autowired
	private WeChatUserService weChatUserService;
	@Autowired
	private CouponService couponService;
	@Autowired
	private RecordAlbumService recordAlbumService;

	@Value("${RECORD_DISPLAY_PAGE_PATH}")
	private String RECORD_DISPLAY_PAGE_PATH;
	@Value("${SCREEN_SHOT_DISPLAY_PAGE_PATH}")
	private String SCREEN_SHOT_DISPLAY_PAGE_PATH;
	@Value("${VIDEO_PHOTO_DISPLAY_PAGE_PATH}")
	private String VIDEO_PHOTO_DISPLAY_PAGE_PATH;

	@Value("${REC_IMG_360}")
	private String REC_IMG_360;
	@Value("${REC_IMG_200}")
	private String REC_IMG_200;
	@Value("${VIDEO_PHOTO_IMG_360}")
	private String VIDEO_PHOTO_IMG_360;
	@Value("${VIDEO_PHOTO_IMG_200}")
	private String VIDEO_PHOTO_IMG_200;
	@Value("${SCREEN_SHOT_IMG_360}")
	private String SCREEN_SHOT_IMG_360;
	@Value("${SCREEN_SHOT_IMG_200}")
	private String SCREEN_SHOT_IMG_200;
	@Value("${SERVER_TOKEN}")
	private String SERVER_TOKEN;

	/**
	 * 
	 * {根据openid 查看是否订阅：0（未订阅），1（已订阅）}
	 * 
	 * @param request
	 * @return
	 * @author: WWQ
	 */
	@RequestMapping(value = "/getIsSubscribeByOpeid", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String getIsSubscribeByOpeid(HttpServletRequest request) {
		String openid = request.getParameter("openid");
		if (ComStrUtil.chkParamNotOk(openid)) {
			return ResultUtil.paramError(request);
		}
		if (weChatService.getIsSubscribe(openid)) {
			return ResultUtil.buildOKCodeResult("1");
		}
		return ResultUtil.buildOKCodeResult("0");
	}

	private String checkSignature(HttpServletRequest request) {
		String signature = request.getParameter("signature");
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");
		String echostr = request.getParameter("echostr");
		if (ComStrUtil.chkParamNotOk(signature, timestamp, nonce, echostr)) {
			logger.error(ResultUtil.errorResult(request));
			return ComStrUtil.NullStr;
		}
		ArrayList<String> list = new ArrayList<String>();
		list.add(nonce);
		list.add(timestamp);
		list.add(SERVER_TOKEN);

		Collections.sort(list, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		StringBuilder paramStr = new StringBuilder();
		for (String paramName : list) {// 按顺序取出 String 字符串
			paramStr.append(paramName);
		}

		String sha1Str = SignUtil.getSha1(paramStr.toString());
		return sha1Str;
	}

	/**
	 * 
	 * {公众平台配置的服务器地址}
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @author: WWQ
	 */
	@RequestMapping(value = "/server", method = RequestMethod.GET)
	@ResponseBody
	public void serverGET(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("\nserverGET : " + JSON.toJSONString(request.getParameterMap()));
		// Map<String, String> resMap = new HashMap<String, String>();
		/*
		* 规则描述
		*1. 将token、timestamp、nonce三个参数进行字典序排序
		*2. 将三个参数字符串拼接成一个字符串进行sha1加密
		*3. 开发者获得加密后的字符串可与signature对比，标识该请求来源于微信 
		*/
		try {
			String signature = request.getParameter("signature");
			String echostr = request.getParameter("echostr");
			String sha1Str = checkSignature(request);
			response.setContentType("text/html;charset=UTF-8");
			response.setCharacterEncoding("UTF-8");

			if (sha1Str.equals(signature)) {
				response.getWriter().print(echostr);
			} else {
				response.getWriter().print("Error");
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/server", method = RequestMethod.POST, produces = MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
	@ResponseBody
	public String serverPOST(HttpServletRequest request) {
		logger.debug("\nserverPOST : " + JSON.toJSONString(request.getParameterMap()));
		String respMessage = ComStrUtil.SUCCESS;
		Map<String, String> requestMap = MessageUtil.parseXml(request);
		String fromUserName = requestMap.get("FromUserName");
		String toUserName = requestMap.get("ToUserName");
		/*String msgType = requestMap.get("MsgType");*/
		List<Article> articleList = new ArrayList<>();
		int countRec = recordService.queryCountByOpenid(fromUserName);
		int countPhoto = wxPhotoService.queryCountByOpenid(fromUserName);
		int countVideo = wxVideoService.queryCountByOpenid(fromUserName);
		int countScreenShot = screenShotService.queryCountByOpenid(fromUserName);
		if (countRec > 0) {
			Article article1 = new Article();
			article1.setTitle("您的录音已经上传，点击查看。");
			article1.setDescription("");
			article1.setPicUrl(REC_IMG_360);
			article1.setUrl(RECORD_DISPLAY_PAGE_PATH);
			articleList.add(article1);
		}
		if (countPhoto > 0 || countVideo > 0) {
			Article article1 = new Article();
			article1.setTitle("您的萌拍已经上传，点击查看。");
			article1.setDescription("");
			if (articleList.size() > 0) {
				article1.setPicUrl(VIDEO_PHOTO_IMG_200);
			} else {
				article1.setPicUrl(VIDEO_PHOTO_IMG_360);
			}
			article1.setUrl(VIDEO_PHOTO_DISPLAY_PAGE_PATH);
			articleList.add(article1);
		}
		if (countScreenShot > 0) {
			Article article1 = new Article();
			article1.setTitle("您的截屏已经上传，点击查看。");
			article1.setDescription("");
			if (articleList.size() > 0) {
				article1.setPicUrl(SCREEN_SHOT_IMG_200);
			} else {
				article1.setPicUrl(SCREEN_SHOT_IMG_360);
			}
			article1.setUrl(SCREEN_SHOT_DISPLAY_PAGE_PATH);
			articleList.add(article1);
		}

		if (articleList.size() > 0) {
			NewsMessage newsMessage = new NewsMessage();
			newsMessage.setToUserName(fromUserName);
			newsMessage.setFromUserName(toUserName);
			newsMessage.setCreateTime(MyDateUtil.getRightNowLong());
			newsMessage.setMsgType(MessageUtil.RESP_MESSAGE_TYPE_NEWS);
			newsMessage.setFuncFlag(0);
			newsMessage.setArticleCount(articleList.size());
			newsMessage.setArticles(articleList);
			respMessage = MessageUtil.messageToXml(newsMessage);
		}
		logger.debug("\nrespMessage = " + respMessage);
		return respMessage;
	}

	@RequestMapping("/testUpload")
	public String testUpload() {
		return "TestUpload";
	}

	@RequestMapping(value = "/getBusinessDomain", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String getBusinessDomain(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new HashMap<>();
		result.put("Info", business_domain);
		return JSON.toJSONString(result);
	}

	// 设置 openid 到 session，并重定向到录音页面接口
	@RequestMapping("/openid2Sion")
	public String openid2Sion(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String desUrl = business_domain + wxsvc_url_reord;
		if ("".equals(openid)) {
			return "redirect:" + desUrl;
		} else {
			request.getSession().setAttribute("openid", openid);
			return "redirect:/personal/soundRecord.action";
		}
	}

	// 设置 openid 到 session，并重定向到对应页面接口
	@RequestMapping("/CommonOpenid2Sion")
	public String CommonOpenid2Sion(HttpServletRequest request, HttpServletResponse response) {
		logger.debug(request.getSession().getMaxInactiveInterval());
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String targetUrl = request.getParameter("targetUrl") != null ? request.getParameter("targetUrl").toString() : "";
		String desUrl = COMMON_URL_FOR_GET_OPENID + WXSVC_ROOT_URL + "/personal/CommonOpenid2Sion.action?targetUrl=" + targetUrl;
		if ("".equals(openid)) {
			return "redirect:" + desUrl;
		} else {
			request.getSession().setAttribute("openid", openid);
			return "redirect:" + targetUrl;
		}
	}

	// 展示截屏和视频页面
	@RequestMapping("/jsp4PhotoVideoList")
	public String jsp4PhotoVideoList(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			return "redirect:/personal/CommonOpenid2Sion.action?targetUrl=/personal/jsp4PhotoVideoList.action";
		}
		return "PhotoVideoList";
	}

	// 视频列表页
	@RequestMapping(value = "/getPhotoList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String getPhotoList(HttpServletRequest request, HttpServletResponse response) {
		// logger.debug(request.getSession().getMaxInactiveInterval());
		Map<String, Object> result = new HashMap<>();
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if (ComStrUtil.chkParamNotOk(openid)) {
			result.put("Info", "openidError");
			return JSON.toJSONString(result);
		}
		// String openid = request.getParameter("openid");
		String pageStr = request.getParameter("page");
		String rowsStr = request.getParameter("rows");
		int page = PageUtil.initPage(pageStr);
		int rows = PageUtil.initRows(rowsStr);
		ComPageResult screenShot = wxPhotoService.getVideoList(page, rows, openid);
		result.put("Info", screenShot);
		return JSON.toJSONString(result);
	}

	@RequestMapping("/jsp4PhotoPlay")
	public String jsp4PhotoPlay(HttpServletRequest request, HttpServletResponse response) {
		String photoId = request.getParameter("photoId");
		if (ComStrUtil.chkParamNotOk(photoId)) {
			return "fail4WcUser";
		}
		TbWxPhoto photo = wxPhotoService.queryByPriKey(Integer.parseInt(photoId));
		if (null == photo) {
			return "fail4WcUser";
		}
		request.setAttribute("Photo", photo);
		return "PhotoPlay";
	}

	@RequestMapping(value = "/givePhotoAdmire", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String givePhotoAdmire(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new HashMap<>();
		String photoId = request.getParameter("photoId");
		if (ComStrUtil.chkParamNotOk(photoId)) {
			result.put("Info", ComStrUtil.ParamError);
			return JSON.toJSONString(result);
		}

		String admireNum = request.getSession().getAttribute("VideoAdmireNum") != null ? request.getSession().getAttribute("VideoAdmireNum").toString() : "";
		String numStr = "0";
		if (ComStrUtil.chkParamNotOk(admireNum)) {
			numStr = "1";
		} else if ("1".equals(admireNum)) {
			numStr = "-1";
		} else if ("-1".equals(admireNum)) {
			numStr = "1";
		}
		request.getSession().setAttribute("VideoAdmireNum", numStr);
		int count = wxPhotoService.updateAdmireNum(Integer.parseInt(photoId), Integer.parseInt(numStr));
		result.put("Info", count);
		return JSON.toJSONString(result);
	}

	// 视频列表页
	@RequestMapping(value = "/getVideoList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String getVideoList(HttpServletRequest request, HttpServletResponse response) {
		// logger.debug(request.getSession().getMaxInactiveInterval());
		Map<String, Object> result = new HashMap<>();
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if (ComStrUtil.chkParamNotOk(openid)) {
			result.put("Info", "openidError");
			return JSON.toJSONString(result);
		}
		// String openid = request.getParameter("openid");
		String pageStr = request.getParameter("page");
		String rowsStr = request.getParameter("rows");
		int page = PageUtil.initPage(pageStr);
		int rows = PageUtil.initRows(rowsStr);
		ComPageResult screenShot = wxVideoService.getVideoList(page, rows, openid);
		result.put("Info", screenShot);
		return JSON.toJSONString(result);
	}

	@RequestMapping("/jsp4VideoPlay")
	public String jsp4VideoPlay(HttpServletRequest request, HttpServletResponse response) {
		String videoId = request.getParameter("videoId");
		if (ComStrUtil.chkParamNotOk(videoId)) {
			return "fail4WcUser";
		}
		TbWxVideo video = wxVideoService.queryByPriKey(Integer.parseInt(videoId));
		if (null == video) {
			return "fail4WcUser";
		}
		request.setAttribute("Video", video);
		return "VideoPlay";
	}

	@RequestMapping(value = "/giveVideoAdmire", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String giveVideoAdmire(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new HashMap<>();
		String videoId = request.getParameter("videoId");

		if (ComStrUtil.chkParamNotOk(videoId)) {
			result.put("Info", ComStrUtil.ParamError);
			return JSON.toJSONString(result);
		}

		String admireNum = request.getSession().getAttribute("VideoAdmireNum") != null ? request.getSession().getAttribute("VideoAdmireNum").toString() : "";
		String numStr = "0";
		if (ComStrUtil.chkParamNotOk(admireNum)) {
			numStr = "1";
		} else if ("1".equals(admireNum)) {
			numStr = "-1";
		} else if ("-1".equals(admireNum)) {
			numStr = "1";
		}
		request.getSession().setAttribute("VideoAdmireNum", numStr);
		int count = wxVideoService.updateAdmireNum(Integer.parseInt(videoId), Integer.parseInt(numStr));
		result.put("Info", count);
		return JSON.toJSONString(result);
	}

	// 展示截屏和视频页面
	@RequestMapping("/jsp4ScreenShotList")
	public String jsp4ScreenShot(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			return "redirect:/personal/CommonOpenid2Sion.action?targetUrl=/personal/jsp4ScreenShotList.action";
		}
		return "ScreenShotList";
	}

	@RequestMapping(value = "/getScreenShot", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String getScreenShot(HttpServletRequest request, HttpServletResponse response) {
		// logger.debug(request.getSession().getMaxInactiveInterval());
		Map<String, Object> result = new HashMap<>();
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if (ComStrUtil.chkParamNotOk(openid)) {
			result.put("Info", "openidError");
			return JSON.toJSONString(result);
		}
		// String openid = request.getParameter("openid");
		String pageStr = request.getParameter("page");
		String rowsStr = request.getParameter("rows");
		int page = PageUtil.initPage(pageStr);
		int rows = PageUtil.initRows(rowsStr);
		ComPageResult screenShot = screenShotService.getScreenShot(page, rows, openid);
		result.put("Info", screenShot);
		return JSON.toJSONString(result);
	}

	@RequestMapping("/jsp4PicturePlay")
	public String jsp4ShowScreenShot(HttpServletRequest request, HttpServletResponse response) {
		String screenShotId = request.getParameter("screenShotId");
		if (ComStrUtil.chkParamNotOk(screenShotId)) {
			return "fail4WcUser";
		}
		TbWxScreenShot screenShot = screenShotService.queryByPriKey(Integer.parseInt(screenShotId));
		if (null == screenShot) {
			return "fail4WcUser";
		}
		request.setAttribute("ScreenShot", screenShot);
		return "ScreenShotPlay";
	}

	@RequestMapping(value = "/giveAdmire", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String giveAdmire(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new HashMap<>();
		String screenShotId = request.getParameter("screenShotId");

		if (ComStrUtil.chkParamNotOk(screenShotId)) {
			result.put("Info", ComStrUtil.ParamError);
			return JSON.toJSONString(result);
		}

		String admireNum = request.getSession().getAttribute("admireNum") != null ? request.getSession().getAttribute("admireNum").toString() : "";
		String numStr = "0";
		if (ComStrUtil.chkParamNotOk(admireNum)) {
			numStr = "1";
		} else if ("1".equals(admireNum)) {
			numStr = "-1";
		} else if ("-1".equals(admireNum)) {
			numStr = "1";
		}
		request.getSession().setAttribute("admireNum", numStr);
		int count = screenShotService.updateAdmireNum(Integer.parseInt(screenShotId), Integer.parseInt(numStr));
		result.put("Info", count);
		return JSON.toJSONString(result);
	}

	// 获取微信用户基本信息 : openid
	@RequestMapping("/getSNSUserInfo")
	public String getSNSUserInfo(HttpServletRequest request, HttpServletResponse response) {
		String code = request.getParameter("code") != null ? request.getParameter("code").toString() : "";
		// state : 此处为要跳转的接口地址，可以动态跳转
		String state = request.getParameter("state") != null ? request.getParameter("state").toString() : "";
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		if ("".equals(openid)) {
			openid = weChatService.getOpenid(code, state);
		}
		logger.debug(" 获取微信用户基本信息  openid:" + openid);
		if (null == openid) {// 获取用户信息失败时，就直接返回没有订单
			logger.debug("getSNSUserInfo 获取微信用户基本信息失败   openid：" + openid);
			request.setAttribute("errorinfo", "获取微信用户基本信息失败");
			return "fail_user";
		}
		return "redirect:/personal/" + state + "?openid=" + openid;
	}

	// 微信用户所有信息：根据 openid 获取所有信息
	@RequestMapping(value = "/getWcUserInfo", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String getWcUserInfo(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		Map<String, Object> resultMap = new HashMap<String, Object>();
		if ("".equals(openid)) {
			resultMap.put("wcUserInfo", "error");
			return JSON.toJSONString(resultMap);
		}
		resultMap.put("wcUserInfo", weChatUserService.queryByOpenid(openid));
		return JSON.toJSONString(resultMap);
	}

	// 订单：根据 openid 获取个人全部订单信息
	@RequestMapping("/getOrder")
	public String getOrder(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		logger.debug("page rows :" + page + ", " + rows);
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			String code = request.getParameter("code") != null ? request.getParameter("code").toString() : "";
			String state = request.getParameter("state") != null ? request.getParameter("state").toString() : "";
			openid = weChatService.getOpenid(code, state);
		}
		logger.debug(" 获取个人订单信息== openid" + openid);
		if (null == openid) {// 获取用户信息失败时，就直接返回没有订单
			logger.debug("订单 - 获取用户信息失败 openid：" + openid);
			request.setAttribute("result", null);
			return "personal_order";
		}
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "8";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}
		Map<String, Object> paraMap = new ConcurrentHashMap<String, Object>();
		paraMap.put("openid", openid);
		paraMap.put("page", pageInt * rowsInt);
		paraMap.put("pageNow", pageInt);
		paraMap.put("rows", rowsInt);

		PageResult result = orderService.queryOrderByOpenid(paraMap);
		request.setAttribute("result", result);
		return "personal_order";
	}

	/**
	 * 
	 * {订单：根据订单号, 查询订单}
	 * 
	 * @param orderID
	 * @return
	 * @author: WangWanQuan
	 */
	@RequestMapping(value = "/queryByOrderId", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String queryByOrderId(String orderID) {
		TbOrder orderEntity = orderService.queryByID(orderID);
		return JsonUtils.objectToJson(orderEntity);
	}

	@RequestMapping(value = "/showOrderInfoByPriKey")
	public String showOrderInfoByPriKey(String orderId, HttpServletRequest request) {
		OrderRes orderRes = orderService.queryOrderInfoByPriKey(orderId);
		request.setAttribute("order", orderRes);
		return "OrderInfo";
	}

	@RequestMapping(value = "/showOrderEval")
	public String showOrderEval(String orderId, HttpServletRequest request) {
		OrderRes orderRes = orderService.queryOrderInfoByPriKey(orderId);
		request.setAttribute("order", orderRes);
		return "OrderEval";
	}

	@RequestMapping(value = "/addOrderEval", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String addOrderEval(String orderId, int overallEval) {
		Map<String, Object> resMap = MapUtil.getHashMap();
		resMap.put("Info", orderService.addOrderEval(orderId, overallEval));
		return JSON.toJSONString(resMap);
	}

	// 录音：根据 openid 获取个人录音
	@RequestMapping("/soundRecord")
	public String soundRecord(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		/*String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";*/
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			return "redirect:" + business_domain + wxsvc_url_reord;
		}
		logger.debug("获取个人录音 openid：" + openid);
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "8";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}

		request.setAttribute("openid", openid);
		Map<String, Object> argMap = new ConcurrentHashMap<String, Object>();
		argMap.put("openid", openid);
		argMap.put("page", pageInt * rowsInt);
		argMap.put("pageNow", pageInt);
		argMap.put("rows", rowsInt);

		PageResult result = recordService.queryByOpenid(argMap);
		request.setAttribute("result", result);
		return "personal_record";
	}

	// 录音：根据 openid 获取个人录音，（page）
	@RequestMapping(value = "/recordList", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String recordList(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "8";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}

		request.setAttribute("openid", openid);
		Map<String, Object> argMap = new ConcurrentHashMap<String, Object>();
		argMap.put("openid", openid);
		argMap.put("page", pageInt * rowsInt);
		argMap.put("pageNow", pageInt);
		argMap.put("rows", rowsInt);

		PageResult result = recordService.queryByOpenid(argMap);
		request.setAttribute("result", result);
		return JsonUtils.objectToJson(result);
	}

	// 录音：根据录音 id 删除录音记录 和对应文件
	@RequestMapping(value = "/del_record", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String delRecord(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		String recordid = request.getParameter("recordid") != null ? request.getParameter("recordid").toString() : "";
		/*String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";*/
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if ("".equals(openid)) {
			result.put("data", "fail");
			return JsonUtils.mapToJson(result);
		}
		TbRecord record = recordService.getByRecordid(Integer.parseInt(recordid));
		// 此 recordid 录音存在且 此时操作 openid 为录音本人的
		if (null != record && openid.equals(record.getPlayername())) {
			int flag = recordAlbumService.delRecordUpdateAlbumList(Integer.parseInt(recordid));
			if (1 <= flag) {
				result.put("data", "success");
			} else {
				result.put("data", "fail");
			}

		} else {
			result.put("data", "fail");
		}
		return JsonUtils.mapToJson(result);
	}

	/**
	 * 
	 * {更新点击次数}
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @author: WWQ
	 */
	@RequestMapping(value = "/recordBrowse", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String recordBrowse(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		String recordId = request.getParameter("recordId") != null ? request.getParameter("recordId").toString() : "";
		String keyName = "recordBrowse" + recordId;
		if (ComStrUtil.chkParamNotOk(recordId)) {
			return ResultUtil.errorResult(result, request);
		}
		String recordBrowse = request.getSession().getAttribute(keyName) != null ? request.getSession().getAttribute(keyName).toString() : "";
		logger.debug("\n\nsessionIsNew[" + recordBrowse + "]" + request.getSession().getId());
		if (ComStrUtil.chkParamNotOk(recordBrowse)) {
			result.put("Info", recordService.updateRecordBrowse(Integer.parseInt(recordId)));
			request.getSession().setAttribute(keyName, request.getSession().getId());
		} else {
			result.put("Info", recordService.queryRecordBrowse(Integer.parseInt(recordId)));
		}
		return JsonUtils.mapToJson(result);
	}

	@RequestMapping(value = "/queryAdmireNum", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String queryAdmireNum(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		String recordId = request.getParameter("recordId") != null ? request.getParameter("recordId").toString() : "";

		if (ComStrUtil.chkParamNotOk(recordId)) {
			return ResultUtil.errorResult(result, request);
		}
		result.put("Info", recordService.queryAdmireNum(Integer.parseInt(recordId)));
		return JsonUtils.mapToJson(result);
	}

	@RequestMapping(value = "/giveRecordAdmire", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String giveRecordAdmire(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new HashMap<>();
		String recordId = request.getParameter("recordId");

		if (ComStrUtil.chkParamNotOk(recordId)) {
			result.put("Info", ComStrUtil.ParamError);
			return JSON.toJSONString(result);
		}
		String numStr = giveAdmireByKey(request, "RecordAdmireNum");
		int count = recordService.updateAdmireNum(Integer.parseInt(recordId), Integer.parseInt(numStr));
		result.put("Info", count);
		return JSON.toJSONString(result);
	}

	private String giveAdmireByKey(HttpServletRequest request, String keyName) {
		String admireNum = request.getSession().getAttribute(keyName) != null ? request.getSession().getAttribute(keyName).toString() : "";
		String numStr = "0";
		if (ComStrUtil.chkParamNotOk(admireNum)) {
			numStr = "1";
		} else if ("1".equals(admireNum)) {
			numStr = "-1";
		} else if ("-1".equals(admireNum)) {
			numStr = "1";
		}
		request.getSession().setAttribute(keyName, numStr);
		return numStr;
	}

	/************************ 专辑 ******************************/

	// 专辑：根据微信用户 openid 查询专辑信息
	@RequestMapping("/my_album")
	public String myAlbum(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			return "redirect:" + business_domain + wxsvc_url_reord;
		}
		/*String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";*/
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		request.setAttribute("openid", openid);
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "8";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}
		Map<String, Object> argMap = new ConcurrentHashMap<String, Object>();
		argMap.put("openid", openid);
		argMap.put("page", pageInt * rowsInt);
		argMap.put("pageNow", pageInt);
		argMap.put("rows", rowsInt);

		PageResult result = albumService.queryByOpenid(argMap);
		request.setAttribute("result", result);
		return "personal_album";
	}

	// 专辑：查询, 根据微信用户 openid，（page）
	@RequestMapping(value = "/albumList", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String albumList(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		request.setAttribute("openid", openid);
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "8";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}
		Map<String, Object> argMap = new ConcurrentHashMap<String, Object>();
		argMap.put("openid", openid);
		argMap.put("page", pageInt * rowsInt);
		argMap.put("pageNow", pageInt);
		argMap.put("rows", rowsInt);

		PageResult result = albumService.queryByOpenid(argMap);
		request.setAttribute("result", result);
		return JsonUtils.objectToJson(result);
	}

	// 专辑：跳转制作专辑页面
	@RequestMapping("make_album")
	public String makeAlbum(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		String page = request.getParameter("page") != null ? request.getParameter("page").toString() : "";
		String rows = request.getParameter("rows") != null ? request.getParameter("rows").toString() : "";
		request.setAttribute("openid", openid);
		if ("".equals(page) || "".equals(rows)) {
			page = "0";
			rows = "10";
		}
		Integer pageInt = Integer.parseInt(page);
		Integer rowsInt = Integer.parseInt(rows);
		if (pageInt < 0) {
			pageInt = 0;
		}
		Map<String, Object> argMap = new ConcurrentHashMap<String, Object>();
		argMap.put("openid", openid);
		argMap.put("page", pageInt * rowsInt);
		argMap.put("pageNow", pageInt);
		argMap.put("rows", rowsInt);

		PageResult result = recordService.queryByOpenid(argMap);
		request.setAttribute("result", result);
		return "personal_make_album";
	}

	// 专辑：打开专辑 , 获取录音
	@RequestMapping("/open_album")
	public String openAlbum(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		request.setAttribute("openid", openid);
		TbWechatUserWithBLOBs weChatUser = weChatUserService.queryByOpenid(openid);
		if (null != weChatUser) {
			request.setAttribute("nickName", weChatUser.getNickName());
			request.setAttribute("headImg", weChatUser.getHeadimgurl());
		}

		/************** 获取用户昵称和头像 *******************/
		/*	SNSUserInfo snsUserInfo = (SNSUserInfo) request.getSession().getAttribute("snsUserInfo");// 获取用户头像和昵称
			if (null != snsUserInfo) {
				request.setAttribute("nickName", snsUserInfo.getNickname());
				request.setAttribute("headImg", snsUserInfo.getHeadImgUrl());
			} else {// 如果 session 过期
				String WeChatUserInfoUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appid + "&redirect_uri=" + redirect_uri
						+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				return "redirect:/personal/getuserinfo";
				https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx05910520282fbf12&
				 * redirect_uri=http%3a%2f%2f219.234.5.13%3a8094%2fHPBS%2fwechaturl&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect
			}*/

		/***************************************/

		String albumid = request.getParameter("albumid") != null ? request.getParameter("albumid").toString() : "";
		// 根据专辑id , 取出对应专辑
		TbAlbum album = albumService.queryByAlbumid(Integer.parseInt(albumid));
		if (null != album) {
			String[] recordidArr = album.getRecordid().split(",");
			List<TbRecord> recordList = new ArrayList<TbRecord>();
			if (null != recordidArr && !"".equals(album.getRecordid())) {
				for (int i = 0; i < recordidArr.length; i++) {
					TbRecord record = recordService.getByRecordid(Integer.parseInt(recordidArr[i]));
					if (null != record) {
						recordList.add(record);
					}
				}
			}
			request.setAttribute("recordList", recordList);
		}
		request.setAttribute("album", album);
		return "personal_open_album";
	}

	// 专辑：删除专辑 Byid
	@RequestMapping(value = "/del_album", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String delAlbum(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> result = new ConcurrentHashMap<String, Object>();
		String albumid = request.getParameter("albumid") != null ? request.getParameter("albumid").toString() : "";
		int flag = albumService.delAlbum(Integer.parseInt(albumid));
		if (1 == flag) {
			result.put("data", "success");
		} else {
			result.put("data", "fail");
		}
		return JsonUtils.mapToJson(result);
	}

	/************************ 优惠券 ******************************/

	// 优惠券：扫码后随机生成优惠券
	@RequestMapping(value = "/generateCoupons")
	public String generateCoupons(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		TbCoupon coupon = new TbCoupon();
		coupon.setOpenid(openid);
		couponService.addCou(coupon);
		return "redirect:mycouponList.action?openid=" + openid;
	}

	// 优惠券：获取个人所有优惠券,并删除过期优惠券 view
	@RequestMapping(value = "/mycouponList")
	public String mycouponList(HttpServletRequest request, HttpServletResponse response) {
		/*String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";*/
		String openid = request.getSession().getAttribute("openid") != null ? request.getSession().getAttribute("openid").toString() : "";
		if ("".equals(openid)) {// 刚刚点击微信菜单栏进入，没有用户 openid，即时获取
			return "redirect:" + COMMON_URL_FOR_GET_OPENID + WXSVC_ROOT_URL + "/personal/CommonOpenid2Sion.action?targetUrl=/personal/mycouponList.action";
		}
		List<TbCoupon> result = couponService.queryAndDelInvalidCoupon(openid);
		request.setAttribute("result", result);
		request.setAttribute("today", MyDateUtil.getFmtDate("yyyyMMdd"));
		return "personal_coupon";
	}

	// 优惠券：获取个人所有优惠券,并删除过期优惠券
	@RequestMapping(value = "/mycoupon", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String getCouponByOpenid(HttpServletRequest request, HttpServletResponse response) {
		String openid = request.getParameter("openid") != null ? request.getParameter("openid").toString() : "";
		List<TbCoupon> result = couponService.queryAndDelInvalidCoupon(openid);
		return JsonUtils.listToJson(result);
	}

	/**
	 * 
	 * {获取支付域名}
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @author: WWQ
	 */
	@RequestMapping(value = "/getPayDomain", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
	@ResponseBody
	public String getPayDomain(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> map = new HashMap<>();
		map.put("Info", this.business_domain);
		return JsonUtils.mapToJson(map);
	}
}
