package webwx.lwl;

import java.awt.EventQueue;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.UIManager;

import blade.kit.DateKit;
import blade.kit.StringKit;
import blade.kit.http.HttpRequest;
import blade.kit.json.JSON;
import blade.kit.json.JSONArray;
import blade.kit.json.JSONObject;
import blade.kit.logging.Logger;
import blade.kit.logging.LoggerFactory;
import webwx.util.CookieUtil;
import webwx.util.JSUtil;
import webwx.util.Matchers;
import java.util.Map;

/**
 * Hello world!
 *
 */
/**
 * @author Administrator
 *
 */
/**
 * @author Administrator
 *
 */
/**
 * @author Administrator
 * 
 */
public class App {

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	private String uuid;
	private int tip = 0;
	private int iAction = 0;
	private String base_uri, redirect_uri,
			webpush_url = "https://webpush2.weixin.qq.com/cgi-bin/mmwebwx-bin";

	private String skey, synckey, wxsid, wxuin, pass_ticket, deviceId = "e"
			+ DateKit.getCurrentUnixTime();

	private String cookie;
	private QRCodeFrame qrCodeFrame;

	private JSONObject SyncKey, User, BaseRequest;

	// 微信联系人列表，可聊天的联系人列表
	private JSONArray MemberList, ContactList;

	// 微信特殊账号
	private List<String> SpecialUsers = Arrays.asList("newsapp", "fmessage",
			"filehelper", "weibo", "qqmail", "fmessage", "tmessage",
			"qmessage", "qqsync", "floatbottle", "lbsapp", "shakeapp",
			"medianote", "qqfriend", "readerapp", "blogapp", "facebookapp",
			"masssendapp", "meishiapp", "feedsapp", "voip", "blogappweixin",
			"weixin", "brandsessionholder", "weixinreminder",
			"wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c", "officialaccounts",
			"notification_messages", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c",
			"wxitil", "userexperience_alarm", "notification_messages");
	private Map<String, WxUser> UserLists = new HashMap<String, WxUser>();
	private Map<String, String> DebManMapList = new HashMap<String, String>();
	private Map<String, MemberChat> MemberChatList;
	private String meUserName, meNickName;
	private String qiangQunName, qiangQunNickName;
	private String zhuangMan ="";
	public App() {
		System.setProperty("jsse.enableSNIExtension", "false");
		MemberChatList = new HashMap<String, MemberChat>();
	}

	/**
	 * 获取UUID
	 * 
	 * @return
	 */
	public String getUUID() {
		String url = "https://login.weixin.qq.com/jslogin";
		HttpRequest request = HttpRequest.get(url, true, "appid",
				"wx782c26e4c19acffb", "fun", "new", "lang", "zh_CN", "_",
				DateKit.getCurrentUnixTime());

		LOGGER.info("[*] " + request);
	 
		String res = request.body();
		request.disconnect();

		if (StringKit.isNotBlank(res)) {
			String code = Matchers.match("window.QRLogin.code = (\\d+);", res);
			if (null != code) {
				if (code.equals("200")) {
					this.uuid = Matchers.match(
							"window.QRLogin.uuid = \"(.*)\";", res);
					return this.uuid;
				} else {
					LOGGER.info("[*] 错误的状态码: %s", code);
				}
			}
		}
		return null;
	}

	/**
	 * 等待登录
	 */
	public String waitForLogin() {
		this.tip = 1;
		String url = "https://login.weixin.qq.com/cgi-bin/mmwebwx-bin/login";
		HttpRequest request = HttpRequest.get(url, true, "tip", this.tip,
				"uuid", this.uuid, "_", DateKit.getCurrentUnixTime());

		LOGGER.info("[*] " + request.toString());

		String res = request.body();
		request.disconnect();

		if (null == res) {
			LOGGER.info("[*] 扫描二维码验证失败");
			return "";
		}

		String code = Matchers.match("window.code=(\\d+);", res);
		if (null == code) {
			LOGGER.info("[*] 扫描二维码验证失败");
			return "";
		} else {
			if (code.equals("201")) {
				LOGGER.info("[*] 成功扫描,请在手机上点击确认以登录");
				tip = 0;
			} else if (code.equals("200")) {
				LOGGER.info("[*] 正在登录...");
				String pm = Matchers.match("window.redirect_uri=\"(\\S+?)\";",
						res);

				String redirectHost = "wx.qq.com";
				try {
					URL pmURL = new URL(pm);
					redirectHost = pmURL.getHost();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				String pushServer = JSUtil.getPushServer(redirectHost);
				webpush_url = "https://" + pushServer + "/cgi-bin/mmwebwx-bin";

				this.redirect_uri = pm + "&fun=new";
				LOGGER.info("[*] redirect_uri=%s", this.redirect_uri);
				this.base_uri = this.redirect_uri.substring(0,
						this.redirect_uri.lastIndexOf("/"));
				LOGGER.info("[*] base_uri=%s", this.base_uri);
			} else if (code.equals("408")) {
				LOGGER.info("[*] 登录超时");
			} else {
				LOGGER.info("[*] 扫描code=%s", code);
			}
		}
		return code;
	}

	private void closeQrWindow() {
		qrCodeFrame.dispose();
	}

	/**
	 * 登录
	 */
	public boolean login() {

		HttpRequest request = HttpRequest.get(this.redirect_uri);

		LOGGER.info("[*] " + request);

		String res = request.body();
		this.cookie = CookieUtil.getCookie(request);

		request.disconnect();

		if (StringKit.isBlank(res)) {
			return false;
		}

		this.skey = Matchers.match("<skey>(\\S+)</skey>", res);
		this.wxsid = Matchers.match("<wxsid>(\\S+)</wxsid>", res);
		this.wxuin = Matchers.match("<wxuin>(\\S+)</wxuin>", res);
		this.pass_ticket = Matchers.match("<pass_ticket>(\\S+)</pass_ticket>",
				res);

		LOGGER.info("[*] skey[%s]", this.skey);
		LOGGER.info("[*] wxsid[%s]", this.wxsid);
		LOGGER.info("[*] wxuin[%s]", this.wxuin);
		LOGGER.info("[*] pass_ticket[%s]", this.pass_ticket);

		this.BaseRequest = new JSONObject();
		BaseRequest.put("Uin", this.wxuin);
		BaseRequest.put("Sid", this.wxsid);
		BaseRequest.put("Skey", this.skey);
		BaseRequest.put("DeviceID", this.deviceId);

		return true;
	}

	/* 获取聊天群信息 */
	public void getMemberList(JSONArray memberObj) {
		//MemberChatList.clear();
		if (memberObj != null) {
			for (int k = 0; k < memberObj.size(); k++) {
				JSONObject item = memberObj.getJSONObject(k);
				int contackFlag = item.getInt("ContactFlag", 0);
				if ( 2 == 2) {
					String userName = item.getString("UserName");
					if ( userName.indexOf("@@") == -1) {
						continue;
					}
					String nickName = item.getString("NickName");
					JSONArray jsonArr = item.getJSONArray("MemberList");
					MemberChat memInfo = new MemberChat();
					for (int i = 0; i < jsonArr.size(); i++) {
						JSONObject jobj = jsonArr.getJSONObject(i);
						String mUserName = jobj.getString("UserName");
						memInfo.MemberList.add(mUserName);
						LOGGER.info("群成员%s", mUserName);
					}
					memInfo.NickName = nickName;
					memInfo.UserName = userName;
					LOGGER.info("群NickName%s", nickName);
					LOGGER.info("群UserName%s", userName);
					MemberChatList.put(userName, memInfo);

				}
			}
		}
	}

	/**
	 * 微信初始化
	 */
	public boolean wxInit() {

		String url = this.base_uri + "/webwxinit?r="
				+ DateKit.getCurrentUnixTime() + "&pass_ticket="
				+ this.pass_ticket + "&skey=" + this.skey;

		JSONObject body = new JSONObject();
		body.put("BaseRequest", this.BaseRequest);

		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return false;
		}
		LOGGER.info("初始化信息= " + res.toString());
		try {
			JSONObject jsonObject = JSON.parse(res).asObject();
			if (null != jsonObject) {
				JSONObject BaseResponse = jsonObject
						.getJSONObject("BaseResponse");
				if (null != BaseResponse) {
					int ret = BaseResponse.getInt("Ret", -1);
					if (ret == 0) {
						this.SyncKey = jsonObject.getJSONObject("SyncKey");
						this.User = jsonObject.getJSONObject("User");
						this.meUserName = this.User.getString("UserName");
						this.meNickName = this.User.getString("NickName");
						LOGGER.info("me username=" + this.meUserName
								+ " me nickname = " + this.meNickName);
						JSONArray contactList = jsonObject
								.getJSONArray("ContactList");
						getMemberList(contactList);
						StringBuffer synckey = new StringBuffer();

						JSONArray list = SyncKey.getJSONArray("List");
						for (int i = 0, len = list.size(); i < len; i++) {
							JSONObject item = list.getJSONObject(i);
							synckey.append("|" + item.getInt("Key", 0) + "_"
									+ item.getInt("Val", 0));
						}

						this.synckey = synckey.substring(1);

						return true;
					}
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 微信状态通知
	 */
	public boolean wxStatusNotify() {

		String url = this.base_uri
				+ "/webwxstatusnotify?lang=zh_CN&pass_ticket="
				+ this.pass_ticket;

		JSONObject body = new JSONObject();
		body.put("BaseRequest", BaseRequest);
		body.put("Code", 3);
		body.put("FromUserName", this.User.getString("UserName"));
		body.put("ToUserName", this.User.getString("UserName"));
		body.put("ClientMsgId", DateKit.getCurrentUnixTime());

		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return false;
		}

		try {
			JSONObject jsonObject = JSON.parse(res).asObject();
			JSONObject BaseResponse = jsonObject.getJSONObject("BaseResponse");
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				return ret == 0;
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 
	 * @param string
	 * @return 是否数字的串
	 */
	public static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 获取联系人
	 */
	public boolean getContact() {

		String url = this.base_uri + "/webwxgetcontact?pass_ticket="
				+ this.pass_ticket + "&skey=" + this.skey + "&r="
				+ DateKit.getCurrentUnixTime();

		JSONObject body = new JSONObject();

		body.put("BaseRequest", BaseRequest);

		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return false;
		}

		try {
			JSONObject jsonObject = JSON.parse(res).asObject();
			JSONObject BaseResponse = jsonObject.getJSONObject("BaseResponse");
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				if (ret == 0) {
					this.MemberList = jsonObject.getJSONArray("MemberList");
					this.ContactList = new JSONArray();
					if (null != MemberList) {
						for (int i = 0, len = MemberList.size(); i < len; i++) {
							JSONObject contact = this.MemberList
									.getJSONObject(i);
							// 公众号/服务号
							if (contact.getInt("VerifyFlag", 0) == 8) {
								continue;
							}
							// 特殊联系人
							if (SpecialUsers.contains(contact
									.getString("UserName"))) {
								continue;
							}
							// 群聊
							if (contact.getString("UserName").indexOf("@@") != -1) {
								LOGGER.info("获取群聊联系人=" + contact);
								continue;
							}
							// 自己
							if (contact.getString("UserName").equals(
									this.User.getString("UserName"))) {
								continue;
							}
							ContactList.add(contact);
							WxUser wxUser = new WxUser();
							wxUser.UserName = contact.getString("UserName");
							wxUser.UserNickName = contact.getString("NickName");
							wxUser.UserRemarkName = contact.getString("RemarkName");
							// LOGGER.info("获取联系人=" + contact);
							UserLists.put(contact.getString("UserName"),
									wxUser);
						}
						return true;
					}
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 获取联系人
	 */
	public boolean getContactByUserName(String UserName, String QunUserName) {

		String url = this.base_uri
				+ "/webwxbatchgetcontact?type=ex&pass_ticket="
				+ this.pass_ticket + "&skey=" + this.skey + "&r="
				+ DateKit.getCurrentUnixTime();

		JSONObject body = new JSONObject();

		JSONObject list = new JSONObject();

		JSONArray listInfo = new JSONArray();
		list.put("UserName", UserName);
		list.put("EncryChatRoomId", QunUserName);
		listInfo.add(list);
		body.put("BaseRequest", BaseRequest);
		body.put("Count", "1");
		body.put("List", listInfo);
		LOGGER.info("[*] " + "查询单个用户报文=" + body);
		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return false;
		}

		try {
			JSONObject jsonObject = JSON.parse(res).asObject();
			LOGGER.info("获取个人信息=" + jsonObject.toString());
			JSONObject BaseResponse = jsonObject.getJSONObject("BaseResponse");
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				if (ret == 0) {
					JSONArray contact = jsonObject.getJSONArray("ContactList");
					// this.MemberList = jsonObject.getJSONArray("ContactList");
					// this.ContactList = new JSONArray();
					if (null != contact) {
						for (int i = 0, len = contact.size(); i < len; i++) {
							JSONObject item = contact.getJSONObject(i);
							// 公众号/服务号

							ContactList.add(item);
							LOGGER.info("获取联系人=" + contact);
							WxUser wxUser = new WxUser();
							wxUser.UserName = item.getString("UserName");
							wxUser.UserNickName = item.getString("NickName");
							wxUser.UserRemarkName = item.getString("RemarkName");
							UserLists.put(item.getString("UserName"),
									wxUser);
						}
						return true;
					}
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 显示二维码
	 * 
	 * @return
	 */
	public void showQrCode() {

		String url = "https://login.weixin.qq.com/qrcode/" + this.uuid;

		final File output = new File("temp.jpg");

		HttpRequest.post(url, true, "t", "webwx", "_",
				DateKit.getCurrentUnixTime()).receive(output);

		if (null != output && output.exists() && output.isFile()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						UIManager
								.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
						qrCodeFrame = new QRCodeFrame(output.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	/**
	 * 消息检查
	 */
	public int[] syncCheck() {

		int[] arr = new int[2];

		String url = this.webpush_url + "/synccheck";

		JSONObject body = new JSONObject();
		body.put("BaseRequest", BaseRequest);

		HttpRequest request = HttpRequest.get(url, true, "r",
				DateKit.getCurrentUnixTime() + StringKit.getRandomNumber(5),
				"skey", this.skey, "uin", this.wxuin, "sid", this.wxsid,
				"deviceid", this.deviceId, "synckey", this.synckey, "_",
				System.currentTimeMillis()).header("Cookie", this.cookie);

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return arr;
		}

		String retcode = Matchers.match("retcode:\"(\\d+)\",", res);
		String selector = Matchers.match("selector:\"(\\d+)\"}", res);
		if (null != retcode && null != selector) {
			arr[0] = Integer.parseInt(retcode);
			arr[1] = Integer.parseInt(selector);
			return arr;
		}
		return arr;
	}

	private void webwxsendmsg(String content, String to) {

		String url = this.base_uri + "/webwxsendmsg?lang=zh_CN&pass_ticket="
				+ this.pass_ticket;

		JSONObject body = new JSONObject();

		String clientMsgId = DateKit.getCurrentUnixTime()
				+ StringKit.getRandomNumber(5);
		JSONObject Msg = new JSONObject();
		Msg.put("Type", 1);
		Msg.put("Content", content);
		Msg.put("FromUserName", User.getString("UserName"));
		Msg.put("ToUserName", to);
		Msg.put("LocalID", clientMsgId);
		Msg.put("ClientMsgId", clientMsgId);

		body.put("BaseRequest", this.BaseRequest);
		body.put("Msg", Msg);

		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		request.body();
		request.disconnect();
	}

	/**
	 * 获取最新消息
	 */
	public JSONObject webwxsync() {

		String url = this.base_uri + "/webwxsync?lang=zh_CN&pass_ticket="
				+ this.pass_ticket + "&skey=" + this.skey + "&sid="
				+ this.wxsid + "&r=" + DateKit.getCurrentUnixTime();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", BaseRequest);
		body.put("SyncKey", this.SyncKey);
		body.put("rr", DateKit.getCurrentUnixTime());

		HttpRequest request = HttpRequest.post(url)
				.header("Content-Type", "application/json;charset=utf-8")
				.header("Cookie", this.cookie).send(body.toString());

		LOGGER.info("[*] " + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			return null;
		}

		JSONObject jsonObject = JSON.parse(res).asObject();
		JSONObject BaseResponse = jsonObject.getJSONObject("BaseResponse");
		if (null != BaseResponse) {
			int ret = BaseResponse.getInt("Ret", -1);
			if (ret == 0) {
				this.SyncKey = jsonObject.getJSONObject("SyncKey");

				StringBuffer synckey = new StringBuffer();
				JSONArray list = SyncKey.getJSONArray("List");
				for (int i = 0, len = list.size(); i < len; i++) {
					JSONObject item = list.getJSONObject(i);
					synckey.append("|" + item.getInt("Key", 0) + "_"
							+ item.getInt("Val", 0));
				}
				this.synckey = synckey.substring(1);
			}
		}
		return jsonObject;
	}

	/**
	 * 获取最新消息
	 */
	public void handleMsg(JSONObject data) {
		if (null == data) {
			return;
		}

		JSONArray AddMsgList = data.getJSONArray("AddMsgList");

		for (int i = 0, len = AddMsgList.size(); i < len; i++) {
			LOGGER.info("[*] 你有新的消息，请注意查收");
			JSONObject msg = AddMsgList.getJSONObject(i);
			LOGGER.info("msg obj=" + msg.toString());
			int msgType = msg.getInt("MsgType", 0);
			String name = getUserRemarkName(msg.getString("FromUserName"));
			String content = msg.getString("Content");
			String nickName = "";
			String ans = "";
			String fromUserName = msg.getString("FromUserName");
			String toUserName = msg.getString("ToUserName");
			if (msgType == 51) {
				LOGGER.info("[*] 成功截获微信初始化消息");
			} else if (msgType == 1) {
				LOGGER.info("[*] fromUserName =" + fromUserName
						+ "| toUserName=" + toUserName);
				LOGGER.info("[*] meUserName =" + this.meUserName
						+ "| meNickName=" + this.meNickName);
				if (fromUserName.indexOf("@@") != -1) {
					// 群里的信息
					MemberChat memChat = MemberChatList.get(fromUserName);
					LOGGER.info("[1] 群消息 名称=" + memChat.NickName);
					String[] sayText = content.split(":<br/>");
					String orgsayUserName = sayText[0];
					zhuangMan = orgsayUserName;
					String sayUserName = getNickName(orgsayUserName);
					if (sayUserName == null) {
						LOGGER.info("[1]  UserName=" + orgsayUserName
								+ " 新加入查询信息");
						getContactByUserName(orgsayUserName, fromUserName);
						sayUserName = getNickName(orgsayUserName);
					}
					String sayInfo = sayText[1];
					LOGGER.info("[1] 群消息 成员" + sayUserName + "说:" + sayInfo
							+ "|");
					LOGGER.info("[1122] " + " DebManMapList " + DebManMapList
							+ "fromUserName [" + fromUserName
							+ "] qiangQunName=[" + qiangQunName + "] content="
							+ content + "iAction=" + iAction);
					if (iAction == 1 && fromUserName.equals(qiangQunName)) {
						// 开始抢字
						if (sayInfo.equals("抢")) {
							LOGGER.info("[1] 群消息 成员" + sayUserName + "是第一个说:"
									+ sayInfo);
							iAction = 2;
							ans = "开始上分吧小伙伴们  庄为[" + sayUserName + "]";
							webwxsendmsg(ans, msg.getString("FromUserName"));
							continue;
						}

					}
					if (iAction == 2 && fromUserName.equals(qiangQunName)) {
						// 上分了
						LOGGER.info("[1123]" + "fromUserName [" + fromUserName
								+ "] qiangQunName=[" + qiangQunName
								+ "] content=" + content + "iAction=" + iAction);
						if (isInteger(sayInfo)) {
							DebManMapList.put(orgsayUserName, sayInfo);
						} else {
							String wrongAns = "@" + sayUserName
									+ "你说的不是数字请输入数字";
							LOGGER.info(" 非数字回给用户信息=[" + wrongAns + "]");
							webwxsendmsg(wrongAns,
									msg.getString("FromUserName"));
						}
						continue;
					}

				} else if (fromUserName.equals(this.meUserName)) {
					// 自己说的
					LOGGER.info("[2] 群" + qiangQunNickName + " 消息 我说" + content);
					if (content.equals("开始") && iAction == 0) {
						//
						LOGGER.info("[1] 群" + qiangQunNickName + " 消息 我说匹配"
								+ content);
						String toUser = msg.getString("ToUserName");
						//MemberChat memChat = MemberChatList.get(toUser);
						//LOGGER.info(memChat.toString());
						qiangQunName = toUser;
						//qiangQunNickName = memChat.NickName;
						LOGGER.info("[1] 群" + qiangQunNickName + " 消息 我说"
								+ content);
						iAction = 2;
						continue;
					}
					if (content.equals("庄设为") && iAction == 0) {
						//
						LOGGER.info("[1] 群" + qiangQunNickName + " 消息匹配"
								+ content);
						String toUser = msg.getString("ToUserName");
						//MemberChat memChat = MemberChatList.get(toUser);
						//LOGGER.info(memChat.toString());
						qiangQunName = toUser;
						//qiangQunNickName = memChat.NickName;
						String strInfo [] = content.split("@");
						String tempZhuangMan = strInfo[1];									
						LOGGER.info("[1] 群" + qiangQunNickName + " 匹配到庄家="
								+ tempZhuangMan);
						zhuangMan = getUserNameByRemarkName(tempZhuangMan);
						if (zhuangMan != "") {
							LOGGER.info("[1] 群" + qiangQunNickName + " 找到专家="
									+ zhuangMan);	
						} else {
							LOGGER.info("[1] 群" + qiangQunNickName + "通过备注没有找到专家="
									+ tempZhuangMan);	
							continue;
						}
						
						
						iAction = 1;
						continue;
					}					
					if (content.equals("结束封盘")) {
						//
						ans = "";
						String toUser = msg.getString("ToUserName");
						LOGGER.info("[22]匹配结束封盘" + "ToUserName " + toUser
								+ "iAction=" + iAction);
						if (toUser.equals(qiangQunName) && iAction == 2) {
							Iterator iter = DebManMapList.entrySet().iterator();

							LOGGER.info("[333]匹配结束封盘" + " DebManMapList size="
									+ DebManMapList + "ToUserName " + toUser
									+ "iAction=" + iAction);
							while (iter.hasNext()) {
								Map.Entry entry = (Map.Entry) iter.next();
								Object key = entry.getKey();
								LOGGER.info("[4442]匹配结束封盘 key" + key.toString());
								nickName = UserLists.get(key.toString()).UserNickName;
								Object val = entry.getValue();
								ans = ans + nickName + "上分=" + val.toString() + "\n";
								LOGGER.info("[444]匹配结束封盘 ans" + ans);
							}
							webwxsendmsg(ans, toUser);
							iAction = 3;
							continue;
						}

					}
					String beginStr = "++++红包详情++++";
					if (content.contains(beginStr)) {
						//
						LOGGER.info("[33]匹配++++红包详情++++");
						if ( !content.substring(0, beginStr.length()).equals(beginStr)) {
							LOGGER.info("[33]非开头匹配++++红包详情++++"    );
							continue;
						}
						String toUser = msg.getString("ToUserName");
			 
						ans = "";
						String hbDetail = content.substring(beginStr.length(), content.length());
						String hbList[] = hbDetail.split("<br/>");
						hbDetail = hbList[1];
						String newList[] = hbDetail.split(";");
						for (int a=0;a<newList.length;a++) {
							String oneList[] =  newList[a].split("	");
							LOGGER.info("===============");
							String theUser=oneList[0];
							if (UserLists.get(theUser) != null) {
								LOGGER.info("===find user inlist======="+theUser);
							} else {
								LOGGER.info("==not=find user inlist======="+theUser);
							}
						/*	for(int b=0;b<oneList.length;b++) {
								LOGGER.info(oneList[b]);
							}*/
							LOGGER.info("===============");
						}
						LOGGER.info("[33]匹配++++红包详情++++" + "hbDetail " + hbDetail );
						toUser = msg.getString("ToUserName");
						LOGGER.info("[]匹配结束封盘" + "ToUserName " + toUser
								+ "iAction=" + iAction);
					 

					}					
				}
				if (SpecialUsers.contains(msg.getString("ToUserName"))) {
					continue;

				}
			} else if (msgType == 3) {

				LOGGER.info("=========================");
			} else if (msgType == 34) {

				LOGGER.info("=========================");
			} else if (msgType == 42) {
				LOGGER.info(name + " 给你发送了一张名片:");
				LOGGER.info("=========================");
			}
		}
	}

	private final String ITPK_API = "http://i.itpk.cn/api.php";

	// 这里的api_key和api_secret可以自己申请一个
	private final String KEY = "?api_key=你的api_key&api_secret=你的api_secret";

	private String xiaodoubi(String msg) {
		String url = ITPK_API + KEY + "&question=" + msg;
		String result = HttpRequest.get(url).body();
		return result;
	}

	private String getNickName(String userName) {
		try {
			return UserLists.get(userName).UserNickName;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
			
		}
	}
	private String getUserNameByRemarkName(String remarkName) {
		String userName ="";
		for(WxUser wxUser: UserLists.values()) {
			if (wxUser.UserRemarkName == remarkName) {
				return wxUser.UserName;
			}
		}
		return userName;
	}
	private String getRemarkName(String userName) {
		try {
			return UserLists.get(userName).UserRemarkName;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
			
		}
	}
	private String getUserRemarkName(String id) {
		String name = "这个人物名字未知";
		for (int i = 0, len = MemberList.size(); i < len; i++) {
			JSONObject member = this.MemberList.getJSONObject(i);
			if (member.getString("UserName").equals(id)) {
				if (StringKit.isNotBlank(member.getString("RemarkName"))) {
					name = member.getString("RemarkName");
				} else {
					name = member.getString("NickName");
				}
				return name;
			}
		}
		return name;
	}

	public void listenMsgMode() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				LOGGER.info("[*] 进入消息监听模式 ...");
				int playWeChat = 0;
				while (true) {

					int[] arr = syncCheck();

					LOGGER.info("[*] retcode=%s,selector=%s", arr[0], arr[1]);

					if (arr[0] == 1100) {
						// LOGGER.info("[*] 你在手机上登出了微信，债见");
						// break;
						arr = syncCheck();
					}

					if (arr[0] == 0) {
						if (arr[1] == 2) {
							JSONObject data = webwxsync();
							handleMsg(data);
						} else if (arr[1] == 6) {
							JSONObject data = webwxsync();
							handleMsg(data);
						} else if (arr[1] == 7) {
							playWeChat += 1;
							LOGGER.info("[*] 你在手机上玩微信被我发现了 %d 次", playWeChat);
							webwxsync();
						} else if (arr[1] == 3) {
							LOGGER.info("[arr[1] == 3]arr[1] == 3");
							JSONObject data = webwxsync();
							LOGGER.info("[arr[1] == 3]arr[1] == 3");
						} else if (arr[1] == 4) {
							LOGGER.info("[!]有friend cycle info");
							JSONObject data = webwxsync();
							LOGGER.info("[!]有friend cycle info end ");
						} else if (arr[1] == 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					} else {
						return ;
				/*		try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}*/
					}
				}
			}
		}, "listenMsgMode").start();
	}

	public static void main(String[] args) throws InterruptedException {

		System.out.println(JSUtil.getPushServer("wx.qq.com"));
		String clientName = "";
		 
		if ( args.length < 1 ) {
			System.out.println("没有指定用户运行 args" + args.toString());
		} else {			
			clientName= args[0];
			System.out.println("指定用户运行=" + clientName);	
		}

		App app = new App();
		String uuid = app.getUUID();
		if (null == uuid) {
			LOGGER.info("["+clientName+"] uuid获取失败");
		} else {
			String sql =" insert into uuid_tab(uuid,run_flag,run_user) values(?,?,?)";
			String []params ={uuid,"0",clientName};
			SqlHelper.executeUpdate(sql, params);
			LOGGER.info("["+clientName+"]  获取到uuid为 [%s] 插入数据库", app.uuid);
			//app.showQrCode();
			while (!app.waitForLogin().equals("200")) {
				Thread.sleep(2000);
			}
		//	app.closeQrWindow();

			if (!app.login()) {
				LOGGER.info("微信登录失败");
				return;
			}

			LOGGER.info("["+clientName+"] 微信登录成功");

			if (!app.wxInit()) {
				LOGGER.info("["+clientName+"]  微信初始化失败");
				return;
			}

			LOGGER.info("["+clientName+"]  微信初始化成功");

			if (!app.wxStatusNotify()) {
				LOGGER.info("["+clientName+"]  开启状态通知失败");
				return;
			}

			LOGGER.info("["+clientName+"] 开启状态通知成功");

			if (!app.getContact()) {
				LOGGER.info("["+clientName+"]  获取联系人失败");
				return;
			}

			LOGGER.info("["+clientName+"]  获取联系人成功");
			LOGGER.info("["+clientName+"]  共有 %d 位联系人", app.ContactList.size());

			// 监听消息
			app.listenMsgMode();

			// mvn exec:java -Dexec.mainClass="me.biezhi.weixin.App"
		}
	}

}