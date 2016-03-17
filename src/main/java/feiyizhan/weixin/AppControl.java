package feiyizhan.weixin;

import java.util.ArrayList;
import java.util.List;

import blade.kit.StringKit;
import blade.kit.json.JSONArray;
import blade.kit.json.JSONObject;
import blade.kit.json.JSONValue;
import blade.kit.logging.Logger;
import blade.kit.logging.LoggerFactory;
import feiyizhan.api.tuling.TulingUtil;
import feiyizhan.weixin.msg.handle.MessageHandleImpl;
import feiyizhan.weixin.msg.handle.text.CmdTextMessageHandle;
import feiyizhan.weixin.msg.handle.text.NormalTextMessageHandle;
import feiyizhan.weixin.msg.handle.text.TextMesageHandle;
import feiyizhan.weixin.util.JSONUtil;
import feiyizhan.weixin.util.MessageUtil;
import feiyizhan.weixin.util.UserUtil;

public class AppControl {
	private static final Logger LOGGER = LoggerFactory.getLogger(AppControl.class);
	
	private UserSession userSession ;
	
	/**
	 * 工具群，用于转发系统消息
	 */
	public JSONObject ToolsGroup = null;
	
	/**
	 * 自动答复用户清单（包括群）
	 */
	public JSONArray AutoReceiveUserList = new JSONArray();
	/**
	 * 管理员帐号清单
	 */
	public JSONArray MangerUsrList = new JSONArray();
	
	/**
	 * 关键字消息提醒-指定的群列表（
	 */
	public JSONArray RemindGroupList = new JSONArray();
	
	/**
	 * 关键字消息列表
	 */
	public List<String> keyWordList = new ArrayList<String>();
	
	/**
	 * 自动消息回复标志
	 */
	public boolean autoReceiveFlag=false;
	
	/**
	 * 消息自动转发标志
	 */
	public boolean forwordFlag =false;
	
	/**
	 * 关键字内容提醒
	 */
	public boolean keyWorkFlag =false;
	
	/**
	 * 消息处理器列表
	 */
	public List<MessageHandleImpl> handleList;
	
	
	/**
	 * 批处理处理线程
	 */
	public boolean batchFlag = false;
	
	public boolean isBatchFlag() {
		return batchFlag;
	}

	public void setBatchFlag(boolean batchFlag) {
		this.batchFlag = batchFlag;
	}

	public AppControl(UserSession userSession) {
		// TODO 自动生成的构造函数存根
		this.userSession=userSession;
		this.init();
	}

	/**
	 * 控制器初始化
	 */
	private void init(){
		this.handleList = new ArrayList<MessageHandleImpl>();
		//增加命令文本消息处理器
		this.handleList.add(new CmdTextMessageHandle(this.userSession, this));
		//增加普通文本消息处理器
		this.handleList.add(new NormalTextMessageHandle(this.userSession, this));
	}




	/**
	 * 获取最新消息
	 */
	public void handleMsg(JSONObject data){
		if(null == data){
			return;
		}
		
		JSONArray AddMsgList = data.getJSONArray("AddMsgList");
		
		for(int i=0,len=AddMsgList.size(); i<len; i++){
			LOGGER.info("[*] 你有新的消息，请注意查收");
			JSONObject msg = AddMsgList.getJSONObject(i);
			int msgType = MessageUtil.getMessageType(msg);
			
			String content = MessageUtil.getContent(msg);
			
			LOGGER.debug("[*] msg:"+msg);
			LOGGER.debug("[*] 消息类型:"+msgType);
			LOGGER.debug("[*] content:"+content);
			
			//检查当前消息的群是否在联系人列表里，不在的更新
			String toUserID = MessageUtil.getToUserID(msg);
			String fromUserID = MessageUtil.getFromUserID(msg);
			updateMemberList(toUserID);
			updateMemberList(fromUserID);
			
			for(MessageHandleImpl mh:this.handleList){
				if(mh.handleMessage(msg)){
					break;
				}
			}
			
//			switch (msgType){
//			case 51:{  //初始化消息
//				
//				break ;
//				}
//			case 1:{  //文本消息
//				doHandleTextMsg(msg);  //文本消息处理
//				break ;
//				}
//			case 3:{  //图片消息
//				//webwxsendmsg("小白还不支持图片呢", msg.getString("FromUserName"));
//				break ;
//				}
//			case 34:{  //语音消息
//				//webwxsendmsg("小白还不支持语音呢", msg.getString("FromUserName"));
//				break ;
//				}
//			case 42:{  //名片消息
////				String name = getUserName(msg.getString("FromUserName"),null);
////				LOGGER.info(name + " 给你发送了一张名片:");
////				LOGGER.info("=========================");
//				break ;
//				}
//			default :{
//				
//				break ;
//				}
//			
//			}
			
		
		}
	}
	

	
	/**
	 * 返回状态
	 * @return
	 */
	public String getStatus(){
		StringBuilder sb = new StringBuilder();
		sb.append("当前登录用户【 "+UserUtil.getUserRemarkName(userSession.User)+"】\n");
		sb.append("当前用户有【"+userSession.MemberList.size()+"】个联系人\n");
		int groupCount =0;
		int otherCount =0;
		for(JSONValue val:userSession.GrouptList){
			JSONObject obj = val.asObject();
			if(obj.getString("UserName").startsWith("@@")){ //群
				groupCount++;
			}else{
				otherCount++;
			}
		}
		sb.append("当前用户有【"+groupCount+"】个群和【"+otherCount+"】个其他联系人\n");
		sb.append("当前提醒关键字清单【"+this.keyWordList+"】\n");
		
		String mangerString =JSONUtil.getStringValues(this.MangerUsrList, UserUtil.NAMES_KEYS);
		sb.append("当前总控用户清单【"+mangerString+"】\n");
		
		String groupString =JSONUtil.getStringValues(this.RemindGroupList, UserUtil.NAMES_KEYS);
		sb.append("当前监控消息的群清单【"+groupString+"】\n");
		
		String autoUserString =JSONUtil.getStringValues(this.AutoReceiveUserList, UserUtil.NAMES_KEYS);
		sb.append("当前自动答复消息的用户清单【"+autoUserString+"】\n");
		
		sb.append(null==this.ToolsGroup?"还未设置工具群\n":"当前工具群【"+UserUtil.getUserRemarkName(this.ToolsGroup)+"】\n");
		sb.append(this.autoReceiveFlag?"开启了自动答复\n":"没有开启自动答复\n");
		sb.append(this.forwordFlag?"开启了消息转发\n":"没有开启消息转发"+"\n");
		sb.append(this.keyWorkFlag?"开启了关键字提醒\n":"没有开启关键字提醒"+"\n");
		
		
		return sb.toString();
	}
	
	
	/**
	 * 更新指定ID的信息到当前联系人列表
	 * @param id
	 */
	public void updateMemberList(String id){
		if(UserUtil.getUserID(this.userSession.User).equals(id)){
			return;
		}
		if(this.userSession.isSpaciaUser(id)){
			return;
		}
		JSONArray list = new JSONArray();
		list.add(UserUtil.transferToGetContactFromat(id,""));
		if(id.startsWith("@@")){
			if(this.userSession.getGroup(id)==null){  //更新群联系人
				UserUtil.combinUserList(this.userSession.GrouptList, this.userSession.webWxBatchGetContact(list));
				JSONObject group = this.userSession.getGroup(id);
				if(group!=null){
					this.userSession.reFlashGroupContactList(group);
				}
			}
			

		}else{

			if(this.userSession.getUserByID(id,null)==null){  //更新个人联系人
				UserUtil.combinUserList(this.userSession.MemberList, this.userSession.webWxBatchGetContact(list));
				this.userSession.reFlashContactist();
				
			}
			
		}
	}

	
}
