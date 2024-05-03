
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
 * 实习信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/shixi")
public class ShixiController {
    private static final Logger logger = LoggerFactory.getLogger(ShixiController.class);

    @Autowired
    private ShixiService shixiService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private QiyeService qiyeService;
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
        PageUtils page = shixiService.queryPage(params);

        //字典表数据转换
        List<ShixiView> list =(List<ShixiView>)page.getList();
        for(ShixiView c:list){
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
        ShixiEntity shixi = shixiService.selectById(id);
        if(shixi !=null){
            //entity转view
            ShixiView view = new ShixiView();
            BeanUtils.copyProperties( shixi , view );//把实体数据重构到view中

                //级联表
                QiyeEntity qiye = qiyeService.selectById(shixi.getQiyeId());
                if(qiye != null){
                    BeanUtils.copyProperties( qiye , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setQiyeId(qiye.getId());
                }
                //级联表
                XueshengEntity xuesheng = xueshengService.selectById(shixi.getXueshengId());
                if(xuesheng != null){
                    BeanUtils.copyProperties( xuesheng , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setXueshengId(xuesheng.getId());
                }
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
    public R save(@RequestBody ShixiEntity shixi, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,shixi:{}",this.getClass().getName(),shixi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("学生".equals(role))
            shixi.setXueshengId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        else if("企业".equals(role))
            shixi.setQiyeId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<ShixiEntity> queryWrapper = new EntityWrapper<ShixiEntity>()
            .eq("xuesheng_id", shixi.getXueshengId())
            .eq("qiye_id", shixi.getQiyeId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShixiEntity shixiEntity = shixiService.selectOne(queryWrapper);
        if(shixiEntity==null){
            shixi.setInsertTime(new Date());
            shixi.setCreateTime(new Date());
            shixiService.insert(shixi);
            return R.ok();
        }else {
            return R.error(511,"该学生已有该企业的实习记录");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ShixiEntity shixi, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,shixi:{}",this.getClass().getName(),shixi.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("学生".equals(role))
//            shixi.setXueshengId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
//        else if("企业".equals(role))
//            shixi.setQiyeId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<ShixiEntity> queryWrapper = new EntityWrapper<ShixiEntity>()
            .notIn("id",shixi.getId())
            .andNew()
            .eq("xuesheng_id", shixi.getXueshengId())
            .eq("qiye_id", shixi.getQiyeId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShixiEntity shixiEntity = shixiService.selectOne(queryWrapper);
        if(shixiEntity==null){
            shixiService.updateById(shixi);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"该学生已有该企业的实习记录");
        }
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        shixiService.deleteBatchIds(Arrays.asList(ids));
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
            List<ShixiEntity> shixiList = new ArrayList<>();//上传的东西
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
                            ShixiEntity shixiEntity = new ShixiEntity();
//                            shixiEntity.setXueshengId(Integer.valueOf(data.get(0)));   //学生 要改的
//                            shixiEntity.setQiyeId(Integer.valueOf(data.get(0)));   //企业 要改的
//                            shixiEntity.setShixiName(data.get(0));                    //实习名称 要改的
//                            shixiEntity.setShixiTypes(Integer.valueOf(data.get(0)));   //实习类型 要改的
//                            shixiEntity.setShixiKaishiTime(sdf.parse(data.get(0)));          //实习开始时间 要改的
//                            shixiEntity.setShixiJieshuTime(sdf.parse(data.get(0)));          //实习结束时间 要改的
//                            shixiEntity.setShixiJieguoTypes(Integer.valueOf(data.get(0)));   //实习结果 要改的
//                            shixiEntity.setShixiGangweiName(data.get(0));                    //实习岗位 要改的
//                            shixiEntity.setShixiContent("");//详情和图片
//                            shixiEntity.setInsertTime(date);//时间
//                            shixiEntity.setCreateTime(date);//时间
                            shixiList.add(shixiEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        shixiService.insertBatch(shixiList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }






}
