package com.xuehu365.aspect;

import com.alibaba.fastjson.JSON;
import com.xuehu365.domain.base.BaseEntity;
import com.xuehu365.util.exception.FieldError;
import com.xuehu365.util.exception.ParamValidException;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/5/17.
 */
@Aspect
@Component
public class LogAspect {
    private Logger logger = LoggerFactory.getLogger(LogAspect.class);
    ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    @Before(value = "execution(* com.xuehu365.controller*..*(..))")
    public void before(JoinPoint point) throws NoSuchMethodException, SecurityException, ParamValidException {
        //  获得切入目标对象
        Object point_target = point.getThis();
        // 获得切入方法参数
        Object[] args = point.getArgs();
        // 获得切入的方法
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        // 执行校验，获得校验结果
        Set<ConstraintViolation<Object>> validResult = validMethodParams(point_target, method, args);

        if (!validResult.isEmpty()) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method); // 获得方法的参数名称
            List<FieldError> errors = new ArrayList<>();
            for (ConstraintViolation constraintViolation : validResult) {
                FieldError error = new FieldError();  // 将需要的信息包装成简单的对象，方便后面处理
                if (constraintViolation.getLeafBean() instanceof BaseEntity) {
                    String[] aa = constraintViolation.getPropertyPath().toString().split("\\.");
                    error.setName(constraintViolation.getLeafBean().getClass().getSimpleName() + "." + aa[aa.length - 1]);
                    error.setMessage(constraintViolation.getMessage());
                } else {
                    PathImpl pathImpl = (PathImpl) constraintViolation.getPropertyPath();  // 获得校验的参数路径信息
                    int paramIndex = pathImpl.getLeafNode().getParameterIndex(); // 获得校验的参数位置
                    String paramName = parameterNames[paramIndex];  // 获得校验的参数名称
                    error.setName(paramName);  // 参数名称（校验错误的参数名称）
                    error.setMessage(constraintViolation.getMessage()); // 校验的错误信息
                }
                errors.add(error);
            }
            throw new ParamValidException(errors);  //抛出异常，交给上层处理
        }
    }

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final ExecutableValidator validator = factory.getValidator().forExecutables();

    private <T> Set<ConstraintViolation<T>> validMethodParams(T obj, Method method, Object[] params) {
        return validator.validateParameters(obj, method, params);
    }


    @SuppressWarnings("unchecked")
    @AfterReturning(value = "execution(* com.xuehu365.controller*..*(..))&&@annotation(log)", returning = "rvt")
    public void after(JoinPoint point, RequestMapping log, Object rvt) {
        try {
            StringBuffer buffer = new StringBuffer();
            buffer.append("请求【" + log.value()[0] + "】");
            buffer.append("调用方法" +
                    point.getSignature().getDeclaringTypeName() +
                    "." + point.getSignature().getName());
            String classType = point.getTarget().getClass().getName();
            Class<?> clazz = Class.forName(classType);
            String clazzName = clazz.getName();
            String methodName = point.getSignature().getName();
            String[] params = getFieldsName(this.getClass(), clazzName, methodName);
            Object[] values = point.getArgs();
            StringBuffer paramStr = new StringBuffer();
            int length = params.length;
            if (length > 0) {
                for (int i = length - 1; i > 0; i--) {
                    if (isLogClass(values[i]))
                        paramStr.append(params[i] + "=" + JSON.toJSONString(values[i])).append(";");
                }
                if (isLogClass(values[0])) {
                    paramStr.append(params[0] + "=" + JSON.toJSONString(values[0])).append(";");
                }
            }
            String a=new String();
            buffer.append("参数为：【 " + paramStr.toString() + " 】");
            buffer.append("返回值为：【 " + JSON.toJSONString(rvt) + " 】");
            logger.info(buffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private String[] getFieldsName(Class cls, String clazzName, String methodName) throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        ClassClassPath classPath = new ClassClassPath(cls);
        pool.insertClassPath(classPath);
        CtClass cc = pool.get(clazzName);
        CtMethod cm = cc.getDeclaredMethod(methodName);
        MethodInfo methodInfo = cm.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
        String[] paramNames = new String[cm.getParameterTypes().length];
        int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
        for (int i = 0; i < paramNames.length; i++) {
            paramNames[i] = attr.variableName(i + pos); //paramNames即参数名
        }
        return paramNames;
    }

    private boolean isLogClass(Object object) {
        if (object == null) return false;
        Class c = object.getClass();
        return (c != null && c.getClassLoader() == null) || BaseEntity.class.isAssignableFrom(c);
    }
}
