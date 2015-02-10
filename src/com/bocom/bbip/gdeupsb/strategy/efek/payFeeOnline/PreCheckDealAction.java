package com.bocom.bbip.gdeupsb.strategy.efek.payFeeOnline;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bocom.bbip.comp.BBIPPublicService;
import com.bocom.bbip.eups.common.ParamKeys;
import com.bocom.bbip.eups.entity.EupsThdTranCtlInfo;
import com.bocom.bbip.eups.repository.EupsThdTranCtlInfoRepository;
import com.bocom.bbip.gdeupsb.common.GDParamKeys;
import com.bocom.bbip.utils.DateUtils;
import com.bocom.jump.bp.core.Context;
import com.bocom.jump.bp.core.CoreException;
import com.bocom.jump.bp.core.CoreRuntimeException;
import com.bocom.jump.bp.core.Executable;

public class PreCheckDealAction implements Executable{
	private final static Log logger=LogFactory.getLog(PreCheckDealAction.class);
	@Autowired
	EupsThdTranCtlInfoRepository eupsThdTranCtlInfoRepository;
	@Autowired
	BBIPPublicService bbipPublicService;
	/**
	 * 交易前策略处理
	 */
	@Override
	public void execute(Context context) throws CoreException,
			CoreRuntimeException {
		logger.info("============Start  PreCheckDealAction");
		
			context.setData(ParamKeys.THD_CUS_NO, context.getData(GDParamKeys.PAY_NO));
			String comNo=context.getData(ParamKeys.COMPANY_NO).toString();

//			comNo=comNo.substring(1,5);         
			
			EupsThdTranCtlInfo eupsThdTranCtlInfo=new EupsThdTranCtlInfo();
			eupsThdTranCtlInfo.setComNo(comNo);
			eupsThdTranCtlInfo=eupsThdTranCtlInfoRepository.findOne(comNo);
				if(null == eupsThdTranCtlInfo){
								context.setData(GDParamKeys.MSGTYP, "E");                  //  Contants常量   
								context.setData(ParamKeys.RSP_CDE,"EFE999");        //  Contants常量   
								context.setData(ParamKeys.RSP_MSG, "获取单位编码【"+comNo+"】交易参数错");
								throw new CoreRuntimeException("获取单位编码【"+comNo+"】交易参数错");
				}
				context.setData(GDParamKeys.TOTNUM, "1");
				
				//日期时间格式修改
				context.setData(ParamKeys.CCY, "RMB");
				context.setData("thdTxnDte", DateUtils.parse(DateUtils.format(new Date(),DateUtils.STYLE_SIMPLE_DATE)));
				context.setData("thdTxnTme", DateUtils.parse(DateUtils.formatAsTranstime(new Date())));
				
				context.setData(ParamKeys.TXN_DATE, DateUtils.parse(DateUtils.format(new Date(),DateUtils.STYLE_SIMPLE_DATE)));
				context.setData(ParamKeys.TXN_TIME,DateUtils.parse(DateUtils.format(new Date(),DateUtils.STYLE_TRANS_TIME)));
				
				if(null != context.getData(GDParamKeys.NET_NAME)){
						context.setData(ParamKeys.BR, context.getData(GDParamKeys.NET_NAME));
				}
				//TODO
				context.setData(ParamKeys.BUS_TYP,context.getData(GDParamKeys.BUS_TYPE));
				logger.info("~~~~~~~~~~~交易日期："+context.getData(ParamKeys.TXN_DATE)
						+"~~~~~~~~~~~交易时间："+context.getData(ParamKeys.TXN_TIME));
				context.setData(ParamKeys.REQ_JRN_NO, context.getData(ParamKeys.SEQUENCE));
	}
}
