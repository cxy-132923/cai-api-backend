package com.cxy.caiapiadmin.service.impl;

import cn.hutool.json.JSONUtil;
import com.cxy.caiapiadmin.exception.BusinessException;
import com.cxy.caiapiadmin.exception.ThrowUtils;
import com.cxy.caiapiadmin.mapper.InterfaceInfoMapper;
import com.cxy.caiapiadmin.service.InterfaceInfoService;
import com.cxy.caiapiadmin.service.UserInterfaceInfoService;
import com.cxy.caiapiadmin.service.UserService;
import com.cxy.caiapiadmin.utils.SqlUtils;
import com.cxy.caiapicommon.common.ErrorCode;
import com.cxy.caiapicommon.constant.CommonConstant;
import com.cxy.caiapicommon.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.cxy.caiapicommon.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.cxy.caiapicommon.model.entity.InterfaceInfo;
import com.cxy.caiapicommon.model.entity.User;
import com.cxy.caiapicommon.model.entity.UserInterfaceInfo;
import com.cxy.caiapicommon.model.vo.InterfaceInfoVO;
import com.cxy.caiapicommon.model.vo.RequestParamsRemarkVO;
import com.cxy.caiapicommon.model.vo.ResponseParamsRemarkVO;
import com.cxy.caiapicommon.model.vo.UserVO;
import com.cxy.caiclientsdk.client.CaiApiClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author cxy
 * @description 针对表【interface_info(接口信息)】的数据库操作Service实现
 * @createDate 2023-06-07 09:37:06
 */
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {

        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String description = interfaceInfo.getDescription();
        String url = interfaceInfo.getUrl();
        String requestParams = interfaceInfo.getRequestParams();
        String host = interfaceInfo.getHost();
        String method = interfaceInfo.getMethod();

        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(name, description, url, host, requestParams, method), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (interfaceInfoQueryRequest == null) {
            return queryWrapper;
        }

        String name = interfaceInfoQueryRequest.getName();
        String description = interfaceInfoQueryRequest.getDescription();
        String method = interfaceInfoQueryRequest.getMethod();
        Integer status = interfaceInfoQueryRequest.getStatus();
        String searchText = interfaceInfoQueryRequest.getSearchText();
        Date createTime = interfaceInfoQueryRequest.getCreateTime();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        Long id = interfaceInfoQueryRequest.getId();
        Long userId = interfaceInfoQueryRequest.getUserId();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.like("name", searchText).or().like("description", searchText);
        }
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(method), "method", method);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.gt(ObjectUtils.isNotEmpty(createTime), "createTime", createTime);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public InterfaceInfoVO getInterfaceInfoVO(InterfaceInfo interfaceInfo, HttpServletRequest request) {
        InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
        // 1. 关联查询用户信息
        Long userId = interfaceInfo.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        interfaceInfoVO.setUser(userVO);
        // 封装请求参数说明 和 响应参数说明
        List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
        List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
        interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
        interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
        return interfaceInfoVO;
    }

    @Override
    public Page<InterfaceInfoVO> getInterfaceInfoVOPage(Page<InterfaceInfo> interfaceInfoPage, HttpServletRequest request) {
        List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords();
        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            return interfaceInfoVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = interfaceInfoList.stream().map(InterfaceInfo::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream()
                .map(interfaceInfo -> {
                    InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
                    // 创建人的用户ID
                    Long userId = interfaceInfo.getUserId();

                    // 判断是否是当前用户拥有的接口
                    boolean isOwnedByCurrentUser = false;

                    // 查询当前登录用户的接口调用次数
                    UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
                            .eq(UserInterfaceInfo::getUserId, loginUser.getId())
                            .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
                            .one();

                    if (userInterfaceInfo != null) {
                        isOwnedByCurrentUser = true;
                        interfaceInfoVO.setTotalNum(userInterfaceInfo.getTotalNum());
                        interfaceInfoVO.setLeftNum(userInterfaceInfo.getLeftNum());
                    }

                    // 获取用户信息
                    User user = userIdUserListMap.getOrDefault(userId, Collections.emptyList()).stream().findFirst().orElse(null);
                    interfaceInfoVO.setUser(userService.getUserVO(user));

                    // 封装请求参数说明和响应参数说明
                    List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
                    List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
                    interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
                    interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);

                    // 设置是否为当前用户拥有的接口
                    interfaceInfoVO.setIsOwnerByCurrentUser(isOwnedByCurrentUser);

                    return interfaceInfoVO;
                })
                .collect(Collectors.toList());

        interfaceInfoVOPage.setRecords(interfaceInfoVOList);
        return interfaceInfoVOPage;
    }

    @Override
    public Page<InterfaceInfoVO> getInterfaceInfoVOByUserIdPage(Page<InterfaceInfo> interfaceInfoPage, HttpServletRequest request) {
        List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords();
        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            return interfaceInfoVOPage;
        }
        // 传入当前用户ID
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 过滤掉不是当前用户的接口，并且填充信息
        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream()
                .map(interfaceInfo -> {
                    InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
                    UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
                            .eq(UserInterfaceInfo::getUserId, userId)
                            .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
                            .one();
                    if (userInterfaceInfo != null) {
                        interfaceInfoVO.setTotalNum(userInterfaceInfo.getTotalNum());
                        interfaceInfoVO.setLeftNum(userInterfaceInfo.getLeftNum());
                        // 封装请求参数说明和响应参数说明
                        List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
                        List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
                        interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
                        interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
                        return interfaceInfoVO;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        interfaceInfoVOPage.setRecords(interfaceInfoVOList);
        return interfaceInfoVOPage;
    }


    @Override
    public CaiApiClient getCaiApiClient(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();

        return new CaiApiClient(accessKey, secretKey);
    }

    @Override
    public boolean updateInterfaceInfo(InterfaceInfoUpdateRequest interfaceInfoUpdateRequest) {
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = this.getById(id);
        ThrowUtils.throwIf(oldInterfaceInfo == null, ErrorCode.NOT_FOUND_ERROR);

        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        interfaceInfo.setRequestParamsRemark(JSONUtil.toJsonStr(interfaceInfoUpdateRequest.getRequestParamsRemark()));
        interfaceInfo.setResponseParamsRemark(JSONUtil.toJsonStr(interfaceInfoUpdateRequest.getResponseParamsRemark()));

        // 参数校验
        this.validInterfaceInfo(interfaceInfo, false);
        return this.updateById(interfaceInfo);
    }

}




