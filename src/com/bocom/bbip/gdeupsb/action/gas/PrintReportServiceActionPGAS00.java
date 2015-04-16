package com.bocom.bbip.gdeupsb.action.gas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.bocom.bbip.comp.BBIPPublicService;
import com.bocom.bbip.eups.action.BaseAction;
import com.bocom.bbip.eups.common.ErrorCodes;
import com.bocom.bbip.eups.common.ParamKeys;
import com.bocom.bbip.eups.repository.EupsThdBaseInfoRepository;
import com.bocom.bbip.file.reporting.impl.VelocityTemplatedReportRender;
import com.bocom.bbip.gdeupsb.action.common.GdPrintReportAction;
import com.bocom.bbip.gdeupsb.repository.GDEupsBatchConsoleInfoRepository;
import com.bocom.bbip.gdeupsb.repository.GdEupsTransJournalRepository;
import com.bocom.bbip.gdeupsb.utils.GdFileUtils;
import com.bocom.bbip.gdeupsb.utils.GdReportUtils;
import com.bocom.bbip.utils.DateUtils;
import com.bocom.jump.bp.SystemConfig;
import com.bocom.jump.bp.core.Context;
import com.bocom.jump.bp.core.CoreException;
import com.bocom.jump.bp.core.CoreRuntimeException;

public class PrintReportServiceActionPGAS00 extends BaseAction {

	
	@Autowired
	private BBIPPublicService bbipPublicService;
	@Autowired
	private SystemConfig systemConfig;
	
	private static Logger logger = LoggerFactory
			.getLogger(PrintReportServiceActionPGAS00.class);

	public void execute(Context context) throws CoreException,
			CoreRuntimeException {
		logger.info("Enter in PrintReportServiceActionPGAS00....");
		logger.info("===============context:" + context);
		
		String prtFlg = context.getData(ParamKeys.PRT_FLG);
		
		////////////////////FOR  TEST       //////////////
		context.setData("pNodNo1", "CNJT");
		context.setData("pTlrId1", "CNJT0001");
		context.setData("txnDte1", DateUtils.format((Date)context.getData(ParamKeys.TXN_DTE), DateUtils.STYLE_SIMPLE_DATE));
		//////////////////////////////////////////////////
		
		//配VM文件
		StringBuffer fileName = null;
		String br = context.getData(ParamKeys.BR);
		String beginDate = context.getData("beginDate");
		String endDate = context.getData("endDate");
		
		context.setData("beginDateY", beginDate.substring(0, 4));
		context.setData("beginDateM", beginDate.substring(5, 7));
		context.setData("beginDateD", beginDate.substring(8));
		
		context.setData("endDateY", endDate.substring(0, 4));
		context.setData("endDateM", endDate.substring(5, 7));
		context.setData("endDateD", endDate.substring(8));
		
		logger.info("=============context:" + context);

		Map<String, Object> detailMap = new HashMap<String, Object>();
		detailMap.put("comNo", "4910000430");
		detailMap.put("beginDte", DateUtils.parse(beginDate, DateUtils.STYLE_SIMPLE_DATE));
		detailMap.put("endDte", DateUtils.parse(endDate, DateUtils.STYLE_SIMPLE_DATE));
		
		context.setData("beginDte", DateUtils.parse(beginDate, DateUtils.STYLE_SIMPLE_DATE));
		context.setData("endDte", DateUtils.parse(endDate, DateUtils.STYLE_SIMPLE_DATE));
		
		
		String comNme = get(EupsThdBaseInfoRepository.class).findOne("4910000430").getComNme();
		context.setData("comNme", comNme);
		
		List<Map<String, Object>> prtList = new ArrayList<Map<String, Object>>();

		if("1".equals(prtFlg)){
			prtList = get(GdEupsTransJournalRepository.class).findGasAllJnlInfo(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("TOTCNT", prtList.get(0).get("TOTCNT"));
			context.setData("TOTAMT", prtList.get(0).get("TOTAMT"));
			
			context.setData("SUCCCNT", prtList.get(0).get("SUCCCNT"));
			context.setData("TOTSUCCAMT", prtList.get(0).get("TOTSUCCAMT"));
			
			context.setData("FAILCNT", prtList.get(0).get("FAILCNT"));
			context.setData("TOTFAILAMT", prtList.get(0).get("TOTFAILAMT"));
			
			context.setData("DOUBTCNT", prtList.get(0).get("DOUBATCNT"));
			context.setData("TOTDOUBTAMT", prtList.get(0).get("TOTDOUBATAMT"));
			
			context.setData("OTHERCNT", prtList.get(0).get("OTHERCNT"));
			context.setData("TOTOTHERAMT", prtList.get(0).get("TOTOTHERAMT"));
			
			//TODO .vm
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气单笔代扣汇总报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		if("2".equals(prtFlg)){
			prtList = get(GdEupsTransJournalRepository.class).findGasSucJnlInfo(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("sumCnt", prtList.get(0).get("SUCCCNT"));
			context.setData("sumAmt", prtList.get(0).get("TOTSUCCAMT"));
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气单笔代扣成功报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		if("3".equals(prtFlg)){
			prtList = get(GdEupsTransJournalRepository.class).findGasFalJnlInfo(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("sumCnt", prtList.get(0).get("FAILCNT"));
			context.setData("sumAmt", prtList.get(0).get("TOTFAILAMT"));
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气单笔代扣失败报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		if("4".equals(prtFlg)){
			prtList = get(GDEupsBatchConsoleInfoRepository.class).findGasBatAllRecord(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("sumCnt", prtList.get(0).get("SUM_TOT_CNT"));
			context.setData("sumAmt", prtList.get(0).get("SUM_TOT_AMT"));
			context.setData("sumSucCnt", prtList.get(0).get("SUM_SUC_TOT_CNT"));
			context.setData("sumSucAmt", prtList.get(0).get("SUM_SUC_TOT_AMT"));
			context.setData("sumFalCnt", prtList.get(0).get("SUM_FAL_TOT_CNT"));
			context.setData("sumFalAmt", prtList.get(0).get("SUM_FAL_TOT_AMT"));
			
			//TODO .vm
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气批量代扣汇总报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		if("5".equals(prtFlg)){
			prtList = get(GDEupsBatchConsoleInfoRepository.class).findGasBatSucRecord(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("sumCnt", prtList.get(0).get("SUM_SUC_TOT_CNT"));
			context.setData("sumAmt", prtList.get(0).get("SUM_SUC_TOT_AMT"));
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气批量代扣成功报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		if("6".equals(prtFlg)){
			prtList = get(GDEupsBatchConsoleInfoRepository.class).findGasBatFalRecord(detailMap);
			if (null == prtList || CollectionUtils.isEmpty(prtList)) {
				logger.info("There are no records for select check trans journal ");
				throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
			}
			context.setData("sumCnt", prtList.get(0).get("SUM_FAL_TOT_CNT"));
			context.setData("sumAmt", prtList.get(0).get("SUM_FAL_TOT_AMT"));
			fileName = new StringBuffer((new StringBuilder("(惠州分行)燃气批量代扣失败报表_"+ br + "_" + beginDate + "_" + endDate).append(".txt").toString()));
		}
		
		if (null == prtList || CollectionUtils.isEmpty(prtList)) {
			logger.info("There are no records for select check trans journal ");
			throw new CoreException(ErrorCodes.EUPS_QUERY_NO_DATA);
		}
		logger.info("================prtList.size:" + prtList.size());
		

//		EupsThdFtpConfigRepository eupsThdFtpConfigRepository = get(EupsThdFtpConfigRepository.class);
//		ReportHelper reportHelper = get(ReportHelper.class);
//		MFTPConfigInfo mftpConfigInfo = reportHelper
//				.getMFTPConfigInfo(eupsThdFtpConfigRepository);
//		logger.info((new StringBuilder("mftpConfigInfo:>>>>").append(BeanUtils
//				.toMap(mftpConfigInfo))).toString());

		String reportPath = GdReportUtils.reportPath(bbipPublicService,
				systemConfig);
		VelocityTemplatedReportRender render = new VelocityTemplatedReportRender();
		
		
		try {
			render.afterPropertiesSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String result = null;
		Map<String, String> map = new HashMap<String, String>();
		if("1".equals(prtFlg)){
			context.setData("prtTtl", "(惠州分行)燃气单笔代扣汇总报表");
			map.put("gasAllJnlRpt", "config/report/pgas/PrintgasAllReport.vm");
			render.setReportNameTemplateLocationMapping(map);
			context.setData("eles", prtList);
			result = render.renderAsString("gasAllJnlRpt", context);
			logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>> result >>>>>>>>>>>>>>>>>>>>>>>>>>");
			logger.info(result);
		}
		if("2".equals(prtFlg) || "3".equals(prtFlg)){
			map.put("gasJnlRpt", "config/report/pgas/printTransJournal.vm");
			render.setReportNameTemplateLocationMapping(map);
			context.setData("eles", prtList);
			result = render.renderAsString("gasJnlRpt", context);
			logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>> result >>>>>>>>>>>>>>>>>>>>>>>>>>");
			logger.info(result);
		}
		
		if("4".equals(prtFlg) ){
			map.put("gasBatAllRpt", "config/report/pgas/printBatInfoAll.vm");
			render.setReportNameTemplateLocationMapping(map);
			context.setData("eles", prtList);
			result = render.renderAsString("gasBatAllRpt", context);
			logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>> result >>>>>>>>>>>>>>>>>>>>>>>>>>");
			logger.info(result);
		}
		if("5".equals(prtFlg) || "6".equals(prtFlg)){
			map.put("gasBatRpt", "config/report/pgas/printBatInfo.vm");
			render.setReportNameTemplateLocationMapping(map);
			context.setData("eles", prtList);
			result = render.renderAsString("gasBatRpt", context);
			logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>> result >>>>>>>>>>>>>>>>>>>>>>>>>>");
			logger.info(result);
		}
		
		
		
		logger.info("=============ready to print report list=============");
		
		try {
			GdFileUtils.write(
					new File((new StringBuffer(String.valueOf(reportPath)))
							.append(fileName).toString()), result, "GBK");
			
			context.setVariable("reportDir", reportPath);
			context.setVariable("reportName", fileName);
			
			get(GdPrintReportAction.class).execute(context);
			
			logger.info("放到前端完成");
			context.setData("filePath", reportPath);
			String printReportName = context.getData("printReportName");
			context.setData("rspMsg", printReportName);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		
//		PrintWriter printWriter = null;
//		
//		// TODO 拼装本地路径
//		StringBuffer sbLocDir = new StringBuffer();
//		sbLocDir.append("D:/testGash/checkFilTest/");
//		try {
//			File file = new File(sbLocDir.toString());
//			if (!file.exists()) {
//				file.mkdirs();
//			}
//			printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
//							new FileOutputStream(sbLocDir.append(fileName).toString()), "GBK")));
//			printWriter.write(result);
//			
//		} catch (IOException e) {
//			throw new CoreException(ErrorCodes.EUPS_FILE_CREATE_FAIL);
//		} finally {
//			if (null != printWriter) {
//				try {
//					printWriter.close();
//				} catch (Exception e) {
//					throw new CoreException(ErrorCodes.EUPS_FILE_CREATE_FAIL);
//				}
//			}
//		}
    
//    bbipPublicService.sendFileToBBOS(new File(TransferUtils.resolveFilePath(mftploca, reportFileName)), reportFileName, MftpTransfer.FTYPE_NORMAL);
	
//		reportHelper.createFileAndSendMFTP(context, result, fileName, mftpConfigInfo);
//		context.setData("filName", fileName);
		
		
		
		logger.info("PrintReportServiceActionPGAS00 execute end ... ...");

	}

}
