package com.jsh.erp.service.supplier;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.SupplierMapper;
import com.jsh.erp.datasource.mappers.SupplierMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.accountHead.AccountHeadService;
import com.jsh.erp.service.depotHead.DepotHeadService;
import com.jsh.erp.service.log.LogService;
import com.jsh.erp.service.user.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

import static com.jsh.erp.utils.Tools.getNow3;

@Service
public class SupplierService {
    private Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SupplierMapperEx supplierMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;
    @Resource
    private DepotHeadService depotHeadService;
    @Resource
    private AccountHeadService accountHeadService;

    public Supplier getSupplier(long id)throws Exception {
        Supplier result=null;
        try{
            result=supplierMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return result;
    }

    public List<Supplier> getSupplier()throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> list=null;
        try{
            list=supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    public List<Supplier> select(String supplier, String type, String phonenum,
                                 String telephone, String description, int offset, int rows) throws Exception{
        List<Supplier> resList = new ArrayList<Supplier>();
        try{
            List<Supplier> list = supplierMapperEx.selectByConditionSupplier(supplier, type, phonenum, telephone, description, offset, rows);
            for(Supplier s : list) {
                Integer supplierId = s.getId().intValue();
                String endTime = getNow3();
                String supType = null;
                if(("??????").equals(s.getType())) {
                    supType = "customer";
                } else if(("?????????").equals(s.getType())) {
                    supType = "vendor";
                }
                BigDecimal sum = BigDecimal.ZERO;
                BigDecimal beginNeedGet = s.getBeginneedget();
                if(beginNeedGet==null) {
                    beginNeedGet = BigDecimal.ZERO;
                }
                BigDecimal beginNeedPay = s.getBeginneedpay();
                if(beginNeedPay==null) {
                    beginNeedPay = BigDecimal.ZERO;
                }
                sum = sum.add(depotHeadService.findTotalPay(supplierId, endTime, supType));
                sum = sum.add(accountHeadService.findTotalPay(supplierId, endTime, supType));
                if(("??????").equals(s.getType())) {
                    sum = sum.add(beginNeedGet).subtract(beginNeedPay);
                    s.setAllneedget(sum);
                    s.setAllneedpay(BigDecimal.ZERO);
                } else if(("?????????").equals(s.getType())) {
                    sum = sum.add(beginNeedPay).subtract(beginNeedGet);
                    s.setAllneedget(BigDecimal.ZERO);
                    s.setAllneedpay(sum);
                }
                resList.add(s);
            }
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return resList;
    }

    public Long countSupplier(String supplier, String type, String phonenum, String telephone, String description) throws Exception{
        Long result=null;
        try{
            result=supplierMapperEx.countsBySupplier(supplier, type, phonenum, telephone, description);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertSupplier(String beanJson, HttpServletRequest request)throws Exception {
        Supplier supplier = JSONObject.parseObject(beanJson, Supplier.class);
        int result=0;
        try{
            result=supplierMapper.insertSelective(supplier);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateSupplier(String beanJson, Long id)throws Exception {
        Supplier supplier = JSONObject.parseObject(beanJson, Supplier.class);
        if(supplier.getBeginneedpay() == null) {
            supplier.setBeginneedpay(BigDecimal.ZERO);
        }
        if(supplier.getBeginneedget() == null) {
            supplier.setBeginneedget(BigDecimal.ZERO);
        }
        supplier.setId(id);
        int result=0;
        try{
            result=supplierMapper.updateByPrimaryKeySelective(supplier);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteSupplier(Long id)throws Exception {
        int result=0;
        try{
            result=supplierMapper.deleteByPrimaryKey(id);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return supplierMapper.deleteByPrimaryKey(id);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSupplier(String ids) throws Exception{
        List<Long> idList = StringUtil.strToLongList(ids);
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            result=supplierMapper.deleteByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdNotEqualTo(id).andSupplierEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> list=null;
        try{
            list= supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateAdvanceIn(Long supplierId, BigDecimal advanceIn)throws Exception{
        logService.insertLog(BusinessConstants.LOG_INTERFACE_NAME_SUPPLIER,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(supplierId).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        Supplier supplier=null;
        try{
            supplier = supplierMapper.selectByPrimaryKey(supplierId);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        int result=0;
        try{
            if(supplier!=null){
                supplier.setAdvancein(supplier.getAdvancein().add(advanceIn));  //???????????????????????????????????????????????????
                 result=supplierMapper.updateByPrimaryKeySelective(supplier);
            }
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }

    public List<Supplier> findBySelectCus()throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andTypeLike("??????").andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    public List<Supplier> findBySelectSup()throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andTypeLike("?????????").andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    public List<Supplier> findBySelectRetail()throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andTypeLike("??????").andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    public List<Supplier> findById(Long supplierId)throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdEqualTo(supplierId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetEnable(Boolean enabled, String supplierIDs)throws Exception {
        logService.insertLog(BusinessConstants.LOG_INTERFACE_NAME_SUPPLIER,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(supplierIDs).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        List<Long> ids = StringUtil.strToLongList(supplierIDs);
        Supplier supplier = new Supplier();
        supplier.setEnabled(enabled);
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdIn(ids);
        int result=0;
        try{
            result = supplierMapper.updateByExampleSelective(supplier, example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }

    public List<Supplier> findUserCustomer()throws Exception{
        SupplierExample example = new SupplierExample();
        example.createCriteria().andTypeEqualTo("??????")
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }

    public List<Supplier> findByAll(String supplier, String type, String phonenum,
                                    String telephone, String description) throws Exception{
        List<Supplier> list=null;
        try{
            list = supplierMapperEx.findByAll(supplier, type, phonenum, telephone, description);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        return list;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BaseResponseInfo importExcel(List<Supplier> mList) throws Exception {
        logService.insertLog(BusinessConstants.LOG_INTERFACE_NAME_SUPPLIER,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_IMPORT).append(mList.size()).append(BusinessConstants.LOG_DATA_UNIT).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        BaseResponseInfo info = new BaseResponseInfo();
        Map<String, Object> data = new HashMap<String, Object>();
        try {
            for(Supplier s: mList) {
                supplierMapper.insertSelective(s);
            }
            info.code = 200;
            data.put("message", "??????");
        } catch (Exception e) {
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            e.printStackTrace();
            info.code = 500;
            data.put("message", e.getMessage());
        }
        info.data = data;
        return info;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSupplierByIds(String ids)throws Exception {
        logService.insertLog(BusinessConstants.LOG_INTERFACE_NAME_SUPPLIER,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result=0;
        try{
            result = supplierMapperEx.batchDeleteSupplierByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
        return result;
    }
    /**
     * create by: qiankunpingtai
     * website???https://qiankunpingtai.cn
     * description:
     *???????????????????????????????????????????????????????????????
     * create time: 2019/4/10 14:48
     * @Param: ids
     * @return int
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSupplierByIdsNormal(String ids) throws Exception {
        /**
         * ??????
         * 1???????????????	jsh_accounthead
         * 2???????????????	jsh_depothead
         * ?????????????????????
         * */
        int deleteTotal=0;
        if(StringUtils.isEmpty(ids)){
            return deleteTotal;
        }
        String [] idArray=ids.split(",");
        /**
         * ??????????????????	jsh_accounthead
         * */
        List<AccountHead> accountHeadList=null;
        try{
            accountHeadList = accountHeadMapperEx.getAccountHeadListByOrganIds(idArray);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        if(accountHeadList!=null&&accountHeadList.size()>0){
            logger.error("?????????[{}],????????????[{}],??????,OrganIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        /**
         * ??????????????????	jsh_depothead
         * */
        List<DepotHead> depotHeadList=null;
        try{
            depotHeadList = depotHeadMapperEx.getDepotHeadListByOrganIds(idArray);
        }catch(Exception e){
            logger.error("?????????[{}],????????????[{}],??????[{}]",
                    ExceptionConstants.DATA_READ_FAIL_CODE,ExceptionConstants.DATA_READ_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        if(depotHeadList!=null&&depotHeadList.size()>0){
            logger.error("?????????[{}],????????????[{}],??????,OrganIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        /**
         * ??????????????????????????????
         * */
        deleteTotal= batchDeleteSupplierByIds(ids);
        return deleteTotal;

    }
}
