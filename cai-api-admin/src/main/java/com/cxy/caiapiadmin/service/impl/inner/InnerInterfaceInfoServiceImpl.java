package com.cxy.caiapiadmin.service.impl.inner;

import com.cxy.caiapiadmin.service.InterfaceInfoService;
import com.cxy.caiapicommon.model.entity.InterfaceInfo;
import com.cxy.caiapicommon.service.InnerInterfaceInfoService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
* @author cxy
*/
@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public InterfaceInfo getInterfaceInfo(String path, String method) {
        return interfaceInfoService.query()
                .eq("url", path)
                .eq("method", method)
                .one();
    }
}




