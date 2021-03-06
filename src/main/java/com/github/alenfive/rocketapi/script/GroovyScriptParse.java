package com.github.alenfive.rocketapi.script;

/**
 * Groovy脚本执行器
 */

import com.github.alenfive.rocketapi.entity.ApiInfo;
import com.github.alenfive.rocketapi.entity.ApiParams;
import com.github.alenfive.rocketapi.extend.ApiInfoContent;
import com.github.alenfive.rocketapi.extend.IApiPager;
import com.github.alenfive.rocketapi.function.IFunction;
import com.github.alenfive.rocketapi.service.ScriptParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.util.Collection;

@Component
public class GroovyScriptParse implements IScriptParse{

    @Autowired
    private ApiInfoContent apiInfoContent;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ScriptParseService parseService;

    @Autowired
    private IApiPager apiPager;

    private Collection<IFunction> functionList;

    private ScriptEngineManager factory = new ScriptEngineManager();

    private ScriptEngine engine = null;

    @PostConstruct
    public void init(){
        //初始化引擎
        engine = factory.getEngineByName("groovy");

        //加载函数
        functionList = context.getBeansOfType(IFunction.class).values();
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    public Object runScript(String script, ApiInfo apiInfo, ApiParams apiParams) throws Throwable {

        Integer pageNo = buildPagerNo(apiParams);
        Integer pageSize = buildPagerSize(apiParams);
        apiParams.putParam(apiPager.getPageNoVarName(),pageNo);
        apiParams.putParam(apiPager.getPageSizeVarName(),pageSize);
        apiParams.putParam(apiPager.getIndexVarName(),apiPager.getIndexVarValue(pageSize,pageNo));

        try {

            //注入变量
            apiInfoContent.setApiInfo(apiInfo);
            apiInfoContent.setApiParams(apiParams);

            Bindings bindings = new SimpleBindings();

            apiInfoContent.setEngineBindings(bindings);
            for(IFunction function : functionList){
                bindings.put(function.getVarName(),function);
            }

            //注入属性变量
            buildScriptParams(bindings,apiParams);
            Object result = engine.eval(script,bindings);
            return result;
        }catch (Exception e){
            if (e.getCause() != null && e.getCause().getCause() != null){
                throw e.getCause().getCause();
            }else{
                throw e;
            }
        }

    }

    private Integer buildPagerNo(ApiParams apiParams) {
        Object value = parseService.buildParamItem(apiParams,null,apiPager.getPageNoVarName());
        if (StringUtils.isEmpty(value)){
            apiParams.putParam(apiPager.getPageNoVarName(),apiPager.getPageNoDefaultValue());
            return apiPager.getPageNoDefaultValue();
        }
        return Integer.valueOf(value.toString());
    }

    private Integer buildPagerSize(ApiParams apiParams) {
        Object value = parseService.buildParamItem(apiParams,null,apiPager.getPageSizeVarName());
        if (StringUtils.isEmpty(value)){
            apiParams.putParam(apiPager.getPageSizeVarName(),apiPager.getPageSizeDefaultValue());
            return apiPager.getPageSizeDefaultValue();
        }
        return Integer.valueOf(value.toString());
    }

    private void buildScriptParams(Bindings bindings, ApiParams apiParams) {
        bindings.put("pathVar",apiParams.getPathVar());
        bindings.put("param",apiParams.getParam());
        bindings.put("body",apiParams.getBody());
        bindings.put("header",apiParams.getHeader());
        bindings.put("cookie",apiParams.getCookie());
        bindings.put("session",apiParams.getSession());

        if (!CollectionUtils.isEmpty(apiParams.getSession())){
            apiParams.getSession().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

        if (!CollectionUtils.isEmpty(apiParams.getCookie())){
            apiParams.getCookie().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

        if (!CollectionUtils.isEmpty(apiParams.getHeader())){
            apiParams.getHeader().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

        if (!CollectionUtils.isEmpty(apiParams.getBody())){
            apiParams.getBody().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

        if (!CollectionUtils.isEmpty(apiParams.getParam())){
            apiParams.getParam().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

        if (!CollectionUtils.isEmpty(apiParams.getPathVar())){
            apiParams.getPathVar().forEach((key,value)->{
                if (StringUtils.isEmpty(key))return;
                bindings.put(key,value);
            });
        }

    }
}
