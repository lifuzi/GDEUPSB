package com.bocom.bbip.gdeupsb.action.trsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.bocom.bbip.comp.BBIPPublicService;
import com.bocom.bbip.eups.action.BaseAction;
import com.bocom.bbip.eups.action.common.OperateFTPAction;
import com.bocom.bbip.eups.action.common.OperateFileAction;
import com.bocom.bbip.eups.adaptor.ThirdPartyAdaptor;
import com.bocom.bbip.eups.common.Constants;
import com.bocom.bbip.eups.common.ErrorCodes;
import com.bocom.bbip.eups.common.ParamKeys;
import com.bocom.bbip.eups.entity.EupsThdFtpConfig;
import com.bocom.bbip.eups.repository.EupsThdFtpConfigRepository;
import com.bocom.bbip.file.reporting.impl.VelocityTemplatedReportRender;
import com.bocom.bbip.gdeupsb.common.GDParamKeys;
import com.bocom.bbip.gdeupsb.entity.GDEupsbTrspFeeInfo;
import com.bocom.bbip.gdeupsb.entity.TrspCheckTmp;
import com.bocom.bbip.gdeupsb.repository.GDEupsbTrspFeeInfoRepository;
import com.bocom.bbip.gdeupsb.repository.TrspCheckTmpRepository;
import com.bocom.bbip.service.Result;
import com.bocom.bbip.thd.org.apache.commons.collections.CollectionUtils;
import com.bocom.bbip.utils.BeanUtils;
import com.bocom.bbip.utils.DateUtils;
import com.bocom.jump.bp.core.Context;
import com.bocom.jump.bp.core.CoreException;
import com.bocom.jump.bp.core.CoreRuntimeException;

public class CheckTrspFileAction extends BaseAction{
			private final static Log logger=LogFactory.getLog(CheckTrspFileAction.class);
			@Autowired
			TrspCheckTmpRepository trspCheckTmpRepository;
			@Autowired
			GDEupsbTrspFeeInfoRepository gdEupsbTrspFeeInfoRepository;
			@Autowired
			OperateFTPAction operateFTPAction;
			@Autowired
			@Qualifier("callThdTradeManager")
			ThirdPartyAdaptor callThdTradeManager;
			public void execute(Context context)throws CoreException,CoreRuntimeException{
				logger.info("=================Start  CheckTrspFile");
				
				String tChkNo=DateUtils.format(new Date(),DateUtils.STYLE_yyyyMMddHHmmss);
				context.setData(ParamKeys.WS_TRANS_CODE,"GetChk");
				//交易上锁
		        Result ret = get(BBIPPublicService.class).tryLock( tChkNo, (long) 0,(long) 600);
		        int status = ret.getStatus();
		        if (status != 0) {
		            log.info("交易并发，请稍后在做");
		            context.setData(ParamKeys.RSP_CDE,"329999");
		            context.setData(ParamKeys.RSP_MSG,"交易并发，请稍后在做 !!");
		            throw new CoreException("交易并发，请稍后在做 ");
		        }
		       if(null == context.getData(GDParamKeys.END_DATE)){
		    	   		context.setData(GDParamKeys.END_DATE, DateUtils.format(new Date(), DateUtils.STYLE_yyyyMMdd));
		       }
		        //外发第三方
		        callThd(context);
		        //清除对账表中的信息
		        get(TrspCheckTmpRepository.class).deleteAll("1");
		        //文件内容添加到对账表
		        fileInsertTrspCheckTmp(context,tChkNo);
		        //更改表内所有对账状态
		        trspCheckTmpRepository.updateAll(tChkNo);
		        
		        //错误数量 
		        int numErr=0;
		        //错误金额 
		        BigDecimal AmtErr=new BigDecimal("0.00");
		        String chkErr=""; //错误信息
		        //TODO 文件名
		        String fileName=context.getData("fileName").toString();
		        List<TrspCheckTmp> list=trspCheckTmpRepository.findAll();
		        int chkFlg=-1;
		        //轮询对账
		    	List<GDEupsbTrspFeeInfo> detailList=new ArrayList<GDEupsbTrspFeeInfo>();
		        for (TrspCheckTmp trspCheckTmp : list) {
						 	//根据流水得到每条数据
			        	GDEupsbTrspFeeInfo  gdEupsbTrspFeeInfo=gdEupsbTrspFeeInfoRepository.findOne(trspCheckTmp.getSqn());
			        	if(gdEupsbTrspFeeInfo == null){
			        			chkErr="企业多账";
			        			chkFlg=3;
			        	}else{
					        		if(!gdEupsbTrspFeeInfo.getCarNo().equals(trspCheckTmp.getCarNo()) 
					        				||  !gdEupsbTrspFeeInfo.getCarTyp().equals(trspCheckTmp.getCarTyp())){
						        			chkErr="企业车牌号或车辆类型不符";
						        			chkFlg=2;
							        	}else if(!gdEupsbTrspFeeInfo.getInvNo().equals(trspCheckTmp.getInvNo())){
							        		chkErr="交易状态不符";
						        			chkFlg=2;
							        	}else if(gdEupsbTrspFeeInfo.getTxnAmt().compareTo(trspCheckTmp.getTxnAmt()) !=0
							        			|| !gdEupsbTrspFeeInfo.getPayMon().equals(trspCheckTmp.getPayMon()) ){
							        		chkErr="金额或月数不符";
						        			chkFlg=2;
							        	}else{
							        		chkErr="核对成功";
						        			chkFlg=1;
							        	}
					        		//临时使用实体类  定义类型和数据 使其生成文件使用
					        		GDEupsbTrspFeeInfo  gdEupsbTrspFeeInfoNew=new GDEupsbTrspFeeInfo();
					        		gdEupsbTrspFeeInfoNew.setTcusNm(chkErr);
					        		gdEupsbTrspFeeInfoNew.setThdKey(trspCheckTmp.getSqn());
					        		gdEupsbTrspFeeInfoNew.setTxnAmt(gdEupsbTrspFeeInfo.getTxnAmt());
					        		gdEupsbTrspFeeInfoNew.setPayMon(gdEupsbTrspFeeInfo.getPayMon());
					        		gdEupsbTrspFeeInfoNew.setCarTyp(gdEupsbTrspFeeInfo.getCarTyp());
					        		gdEupsbTrspFeeInfoNew.setCarNo(gdEupsbTrspFeeInfo.getCarNo());
					        		gdEupsbTrspFeeInfoNew.setStatus(gdEupsbTrspFeeInfo.getStatus());
					        		gdEupsbTrspFeeInfoNew.setPayDat(gdEupsbTrspFeeInfo.getPayDat());
					        		gdEupsbTrspFeeInfoNew.setInvNo(gdEupsbTrspFeeInfo.getInvNo());
					        		
					        	if(1 !=chkFlg){
						        		numErr=numErr+1;
						        		AmtErr=AmtErr.add(trspCheckTmp.getTxnAmt());
//			                           银行交易日期|银行流水号|发生额|发票号|缴费月数|车辆类型|车牌号|状态|
					        			detailList.add(gdEupsbTrspFeeInfoNew);
					        	}else if(3 !=chkFlg){
					        			//跟新对账标志 
						        		String checkFlg=chkFlg+"";
						        		gdEupsbTrspFeeInfo.setChkFlg(checkFlg);
						        		gdEupsbTrspFeeInfo.setTchkNo(tChkNo);
						        		gdEupsbTrspFeeInfoRepository.update(gdEupsbTrspFeeInfo);
					        	}
					        	//跟新表中对账状态 
					        	//更新银行单边账   设置3 = chkFlg
					        	gdEupsbTrspFeeInfoRepository.updateChkFlg(tChkNo);
				       }
		        }
		        context.setData(ParamKeys.TXN_AMT, AmtErr);
		        //拼装文件
		        EupsThdFtpConfig eupsThdFtpConfig=get(EupsThdFtpConfigRepository.class).findOne("trspCheckFile");
		        Map<String, Object> map =  new HashMap<String, Object>();
    	        map.put(ParamKeys.EUPS_FILE_DETAIL, BeanUtils.toMaps(detailList));
    			get(OperateFileAction.class).createCheckFile(eupsThdFtpConfig, "trspCheckFile", fileName, map);
		        
    			//判断是否全部对账
		        List<GDEupsbTrspFeeInfo> gdEupsbTrspFeeInfoList=gdEupsbTrspFeeInfoRepository.findNotCheck(tChkNo);
		        if(CollectionUtils.isNotEmpty(gdEupsbTrspFeeInfoList)){
		        	context.setData(ParamKeys.RSP_CDE, "329999");
		        	context.setData(ParamKeys.RSP_MSG, "系统错误");
		        	//上传文件
		        	sendFile(context,fileName);
		        	throw new CoreException("系统错误");
		        }
		        
		        context.setData(GDParamKeys.MSGTYP, "N");
		        context.setData(ParamKeys.RSP_CDE, "000000");
		        context.setData(ParamKeys.RSP_MSG, "交易成功");
		        
		        ret=get(BBIPPublicService.class).unlock(tChkNo);
		        
				//放到指定位置
		        
		      //清除对账表中的信息
		        get(TrspCheckTmpRepository.class).deleteAll("1");
		        logger.info("============对账结束");
		   }
	/**
	 * 外发第三方
	 */
	public void callThd(Context context){
			logger.info("~~~~~~~~~~~Start  CheckTrspFile   callThd");
			try {
				Map<String, Object> rspMap=callThdTradeManager.trade(context);
				String responseCode=rspMap.get(ParamKeys.THIRD_RETURN_CODE).toString();
	             context.setData(GDParamKeys.RETCOD, responseCode);
	             context.setData("fileName", rspMap.get("fileName"));
	             
	             if(!Constants.RESPONSE_CODE_SUCC.equals(responseCode)){
						context.setData(GDParamKeys.MSGTYP, "E");
						context.setData(ParamKeys.RSP_CDE, "329999");
						context.setData(ParamKeys.RSP_MSG, "调用路桥方时:系统错误或超时或未发送,请检查后重做");
						throw new CoreException("调用路桥方时:系统错误或超时或未发送,请检查后重做");
				}
			} catch (CoreException e) {
					logger.info("CheckTrspFile   callThd  error="+e);
			}
	}
	/**
	 * 将文件拆分入对账表 更改交易表中对账状态
	 */
	public void fileInsertTrspCheckTmp(Context context,String tChkNo){
		logger.info("~~~~~~~~~~~Start  CheckTrspFile   fileInsertTrspCheckTmp");
		//io 每行读  在写入文件
		String ftpNo="trspCheckFile";
		EupsThdFtpConfig eupsThdFtpConfig=get(EupsThdFtpConfigRepository.class).findOne(ftpNo);
		String path=eupsThdFtpConfig.getLocDir();
		String fileName=eupsThdFtpConfig.getLocFleNme();
		try {
			FileReader fileReader=new FileReader(path+"//"+fileName);
			BufferedReader bufferedReader=new BufferedReader(fileReader);
			String string=null;
			GDEupsbTrspFeeInfo gdEupsbTrspFeeInfo =new GDEupsbTrspFeeInfo();
			gdEupsbTrspFeeInfo.setChkFlg("0");
			//读行
			while((string=bufferedReader.readLine())!=null){
					String[] str=string.split("\\|");
//					0银行交易日期|1银行流水号|2发生额|3发票号|4缴费月数|5车辆类型|6车牌号|7状态|				
					TrspCheckTmp trspCheckTmp=new TrspCheckTmp();
					trspCheckTmp.setTxnDat(DateUtils.parse(str[0],DateUtils.STYLE_SIMPLE_DATE));
					trspCheckTmp.setSqn(str[1]);		
					
					double i=Double.parseDouble(str[2].toString());
					double d=i/100;
					DecimalFormat df=new DecimalFormat("#.00");
					BigDecimal txnAmt=new BigDecimal(df.format(d));
					trspCheckTmp.setTxnAmt(txnAmt);
					
					trspCheckTmp.setInvNo(str[3]);
					trspCheckTmp.setPayMon(str[4]);
					trspCheckTmp.setCarTyp(str[5]);
					trspCheckTmp.setCarNo(str[6]);
					trspCheckTmp.setStatue(str[7]);
					
					gdEupsbTrspFeeInfo.setThdKey(str[1]);
					//更改对账状态
					gdEupsbTrspFeeInfoRepository.update(gdEupsbTrspFeeInfo);
					//插入对账表
					trspCheckTmpRepository.insert(trspCheckTmp);
			}
			bufferedReader.close();
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("~~~~~~~~~~~End  CheckTrspFile   fileInsertTrspCheckTmp");
	}
	/**
	 * 发送文件
	 */
	public void sendFile(Context context,String fileName)throws CoreException{
		logger.info("~~~~~~~~~~~Start  CheckTrspFile   printFile");
		
		//获取FTP信息,发送文件到指定路径
        EupsThdFtpConfig eupsThdFtpConfig = context.getData(ParamKeys.CONSOLE_THD_FTP_CONFIG_LIST);
        eupsThdFtpConfig.setLocFleNme(fileName);
        operateFTPAction.putCheckFile(eupsThdFtpConfig);
	}
	/**
	 * 打印清单
	 */
	public void printDetail(Context context,String tChkNo)throws CoreException{
		logger.info("~~~~~~~~~~~Start  CheckTrspFile   printDetail");
		//报表模式
		int i=Integer.parseInt(context.getData(GDParamKeys.JOURNAL_MODEL).toString());
		context.setData("RptFil", "Car"+context.getData(GDParamKeys.START_DATE).toString().substring(5)+".dat");
		if(0 == i){		//~~~~~~~~汇总方式
				List<Map<String, String>> list=gdEupsbTrspFeeInfoRepository.findSumForTxnAmt(tChkNo);
				if(CollectionUtils.isEmpty(list)){
					context.setData(ParamKeys.RSP_CDE, "329999");
				    context.setData(ParamKeys.RSP_MSG, "查询统计信息失败");
			        throw new  CoreException("查询统计信息失败");
				}else{
						int count=Integer.parseInt(list.get(0).get("COUNT"));
						BigDecimal sumTxnAmt=new BigDecimal(list.get(0).get("SUMTXNAMT"));
						context.setData("RptFmt", "etc/RBFBCHKSUM_RPT.XML");
						context.setData("QryNod", "A");
				}
		}else if(1 == i){//~~~~~~~~~~~~~清单方式
				context.setData("RptFmt", "etc/RBFBCHKBil_RPT.XML");
				context.setData("QryNod", "A");
		}else if(2 == i){//~~~~~~~~~~~~~更改发票清单
				context.setData("RptFmt", "etc/RBFBINVCHG_RPT.XML");
		}else if(3 == i){//~~~~~~~~~~~~~未打印发票清单
				context.setData("RptFmt", "etc/RBFBNOPRT_RPT.XML");
		}else{
				context.setData(GDParamKeys.MSGTYP, "E");
				context.setData(ParamKeys.RSP_CDE, "329999");
				context.setData(ParamKeys.RSP_MSG, "统计模式错误");
				throw new CoreException("统计模式错误");
		}
		String RptFil="Car"+context.getData("TlrId")+context.getData(GDParamKeys.START_DATE).toString().substring(4);
		context.setData("RptFil", RptFil);
		//TODO 报表生成
		VelocityTemplatedReportRender render = new VelocityTemplatedReportRender();
		try {
			render.afterPropertiesSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, String> mapping = new HashMap<String, String>();
		mapping.put("sample", context.getData("RptFil").toString());
		render.setReportNameTemplateLocationMapping(mapping);
		String result = render.renderAsString("sample", context);
		
		EupsThdFtpConfig eupsThdFtpConfig = context.getData(ParamKeys.CONSOLE_THD_FTP_CONFIG_LIST);
		String sbLocDir=eupsThdFtpConfig.getLocDir();
		String localFileName=eupsThdFtpConfig.getLocFleNme();
        // 生成本地报表文件
        PrintWriter printWriter = null;
		        try {
		            File file = new File(sbLocDir.toString());
		            if (!file.exists()) {
		                file.mkdirs();
		            }
		            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream((sbLocDir+localFileName).toString()),"GBK")));
		            printWriter.write(result);
		        } catch (IOException e) {
		            throw new CoreException(ErrorCodes.EUPS_FILE_CREATE_FAIL);
		        } finally {
		            if (null != printWriter) {
		                try {
		                    printWriter.close();
		                } catch (Exception e) {
		                    throw new CoreException(ErrorCodes.EUPS_FILE_CREATE_FAIL);
		                }
		            }
		        }
		        String fileName=context.getData("fileName").toString();
		        sendFile(context,fileName);
	}
}
