package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义AOP    实现公共字段填充
 */
@Component
@Aspect
@Slf4j
public class AutoFillAspect {

    /**
     * 指定切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 进行前置通知
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段填充...");

        //获取当前拦截的方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        //获取当前方法的参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        Object objects = args[0];
        //根据操作类型为属性赋值
        LocalDateTime now = LocalDateTime.now();
        Long id = BaseContext.getCurrentId();
        if (operationType == OperationType.INSERT) {

            try {
                Method setCreateTime = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(objects, now);
                setUpdateTime.invoke(objects, now);
                setCreateUser.invoke(objects, id);
                setUpdateUser.invoke(objects, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = objects.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateUser.invoke(objects, id);
                setUpdateTime.invoke(objects, now);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }


}
