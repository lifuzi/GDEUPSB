package com.bocom.bbip.gdeupsb.action.common;

import com.bocom.bbip.comp.BBIPPublicService;
import com.bocom.bbip.eups.action.BaseAction;
import com.bocom.bbip.eups.common.ParamKeys;
import com.bocom.jump.bp.core.Context;
import com.bocom.jump.bp.core.CoreException;
import com.bocom.jump.bp.core.CoreRuntimeException;

public class BatchFileChangeAction extends BaseAction{
	@Override
	public void execute(Context context) throws CoreException,
			CoreRuntimeException {
		BBIPPublicService bbipPublicService=get(BBIPPublicService.class);
		log.info("====================Start   BatchFileChangeAction");
		String eupsBusTyp=context.getData(ParamKeys.EUPS_BUSS_TYPE).toString();
		if(eupsBusTyp.equals("FSAG00")){
			bbipPublicService.synExecute("eups.fileBatchPayCreateDataProcess",context);
		}else if(eupsBusTyp.equals("ZHAG00") || eupsBusTyp.equals("ZHAG01") || eupsBusTyp.equals("ZHAG02")){
			System.out.println(111);
			bbipPublicService.asynExecute("eups.fileBatchPayCreateDataProcess",context);
			System.out.println(222);
		}else{
			throw new CoreException("不支持该业务类型的交易");
		}
		log.info("====================End   BatchFileChangeAction");
	}
}
