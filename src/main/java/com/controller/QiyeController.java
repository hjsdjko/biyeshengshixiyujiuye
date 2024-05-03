
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 企业
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/qiye")
public class QiyeController {
    private static final Logger logger = LoggerFactory.getLogger(QiyeController.class);

    @Autowired
    private QiyeService qiyeService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service

    @Autowired
    private XueshengService xueshengService;
    @Autowired
    private LaoshiService laoshiService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("学生".equals(role))
            params.put("xueshengId",request.getSession().getAttribute("userId"));
        else if("老师".equals(role))
            params.put("laoshiId",request.getSession().getAttribute("userId"));
        else if("企业".equals(role))
            params.put("qiyeId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = qiyeService.queryPage(params);

        //字典表数据转换
        List<QiyeView> list =(List<QiyeView>)page.getList();
        for(QiyeView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        QiyeEntity qiye = qiyeService.selectById(id);
        if(qiye !=null){
            //entity转view
            QiyeView view = new QiyeView();
            BeanUtils.copyProperties( qiye , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody QiyeEntity qiye, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,qiye:{}",this.getClass().getName(),qiye.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<QiyeEntity> queryWrapper = new EntityWrapper<QiyeEntity>()
            .eq("username", qiye.getUsername())
            .or()
            .eq("qiye_phone", qiye.getQiyePhone())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        QiyeEntity qiyeEntity = qiyeService.selectOne(queryWrapper);
        if(qiyeEntity==null){
            qiye.setCreateTime(new Date());
            qiye.setPassword("123456");
            qiyeService.insert(qiye);
            return R.ok();
        }else {
            return R.error(511,"账户或者企业联系方式已经被使用");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody QiyeEntity qiye, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,qiye:{}",this.getClass().getName(),qiye.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        //根据字段查询是否有相同数据
        Wrapper<QiyeEntity> queryWrapper = new EntityWrapper<QiyeEntity>()
            .notIn("id",qiye.getId())
            .andNew()
            .eq("username", qiye.getUsername())
            .or()
            .eq("qiye_phone", qiye.getQiyePhone())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        QiyeEntity qiyeEntity = qiyeService.selectOne(queryWrapper);
        if("".equals(qiye.getQiyePhoto()) || "null".equals(qiye.getQiyePhoto())){
                qiye.setQiyePhoto(null);
        }
        if(qiyeEntity==null){
            qiyeService.updateById(qiye);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"账户或者企业联系方式已经被使用");
        }
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        qiyeService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<QiyeEntity> qiyeList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            QiyeEntity qiyeEntity = new QiyeEntity();
//                            qiyeEntity.setUsername(data.get(0));                    //账户 要改的
//                            //qiyeEntity.setPassword("123456");//密码
//                            qiyeEntity.setQiyeName(data.get(0));                    //企业名称 要改的
//                            qiyeEntity.setQiyeAddress(data.get(0));                    //企业地址 要改的
//                            qiyeEntity.setQiyePhoto("");//详情和图片
//                            qiyeEntity.setQiyePhone(data.get(0));                    //企业联系方式 要改的
//                            qiyeEntity.setQiyeEmail(data.get(0));                    //企业邮箱 要改的
//                            qiyeEntity.setQiyeTypes(Integer.valueOf(data.get(0)));   //所在行业 要改的
//                            qiyeEntity.setQiyeContent("");//详情和图片
//                            qiyeEntity.setCreateTime(date);//时间
                            qiyeList.add(qiyeEntity);


                            //把要查询是否重复的字段放入map中
                                //账户
                                if(seachFields.containsKey("username")){
                                    List<String> username = seachFields.get("username");
                                    username.add(data.get(0));//要改的
                                }else{
                                    List<String> username = new ArrayList<>();
                                    username.add(data.get(0));//要改的
                                    seachFields.put("username",username);
                                }
                                //企业联系方式
                                if(seachFields.containsKey("qiyePhone")){
                                    List<String> qiyePhone = seachFields.get("qiyePhone");
                                    qiyePhone.add(data.get(0));//要改的
                                }else{
                                    List<String> qiyePhone = new ArrayList<>();
                                    qiyePhone.add(data.get(0));//要改的
                                    seachFields.put("qiyePhone",qiyePhone);
                                }
                        }

                        //查询是否重复
                         //账户
                        List<QiyeEntity> qiyeEntities_username = qiyeService.selectList(new EntityWrapper<QiyeEntity>().in("username", seachFields.get("username")));
                        if(qiyeEntities_username.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(QiyeEntity s:qiyeEntities_username){
                                repeatFields.add(s.getUsername());
                            }
                            return R.error(511,"数据库的该表中的 [账户] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                         //企业联系方式
                        List<QiyeEntity> qiyeEntities_qiyePhone = qiyeService.selectList(new EntityWrapper<QiyeEntity>().in("qiye_phone", seachFields.get("qiyePhone")));
                        if(qiyeEntities_qiyePhone.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(QiyeEntity s:qiyeEntities_qiyePhone){
                                repeatFields.add(s.getQiyePhone());
                            }
                            return R.error(511,"数据库的该表中的 [企业联系方式] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        qiyeService.insertBatch(qiyeList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }


    /**
    * 登录
    */
    @IgnoreAuth
    @RequestMapping(value = "/login")
    public R login(String username, String password, String captcha, HttpServletRequest request) {
        QiyeEntity qiye = qiyeService.selectOne(new EntityWrapper<QiyeEntity>().eq("username", username));
        if(qiye==null || !qiye.getPassword().equals(password))
            return R.error("账号或密码不正确");
        //  // 获取监听器中的字典表
        // ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        // Map<String, Map<Integer, String>> dictionaryMap= (Map<String, Map<Integer, String>>) servletContext.getAttribute("dictionaryMap");
        // Map<Integer, String> role_types = dictionaryMap.get("role_types");
        // role_types.get(.getRoleTypes());
        String token = tokenService.generateToken(qiye.getId(),username, "qiye", "企业");
        R r = R.ok();
        r.put("token", token);
        r.put("role","企业");
        r.put("username",qiye.getQiyeName());
        r.put("tableName","qiye");
        r.put("userId",qiye.getId());
        return r;
    }

    /**
    * 注册
    */
    @IgnoreAuth
    @PostMapping(value = "/register")
    public R register(@RequestBody QiyeEntity qiye){
//    	ValidatorUtils.validateEntity(user);
        Wrapper<QiyeEntity> queryWrapper = new EntityWrapper<QiyeEntity>()
            .eq("username", qiye.getUsername())
            .or()
            .eq("qiye_phone", qiye.getQiyePhone())
            ;
        QiyeEntity qiyeEntity = qiyeService.selectOne(queryWrapper);
        if(qiyeEntity != null)
            return R.error("账户或者企业联系方式已经被使用");
        qiye.setCreateTime(new Date());
        qiyeService.insert(qiye);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @GetMapping(value = "/resetPassword")
    public R resetPassword(Integer  id){
        QiyeEntity qiye = new QiyeEntity();
        qiye.setPassword("123456");
        qiye.setId(id);
        qiyeService.updateById(qiye);
        return R.ok();
    }


    /**
     * 忘记密码
     */
    @IgnoreAuth
    @RequestMapping(value = "/resetPass")
    public R resetPass(String username, HttpServletRequest request) {
        QiyeEntity qiye = qiyeService.selectOne(new EntityWrapper<QiyeEntity>().eq("username", username));
        if(qiye!=null){
            qiye.setPassword("123456");
            boolean b = qiyeService.updateById(qiye);
            if(!b){
               return R.error();
            }
        }else{
           return R.error("账号不存在");
        }
        return R.ok();
    }


    /**
    * 获取用户的session用户信息
    */
    @RequestMapping("/session")
    public R getCurrQiye(HttpServletRequest request){
        Integer id = (Integer)request.getSession().getAttribute("userId");
        QiyeEntity qiye = qiyeService.selectById(id);
        if(qiye !=null){
            //entity转view
            QiyeView view = new QiyeView();
            BeanUtils.copyProperties( qiye , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }
    }


    /**
    * 退出
    */
    @GetMapping(value = "logout")
    public R logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return R.ok("退出成功");
    }





}
