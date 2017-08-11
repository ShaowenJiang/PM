package com.xuehu365.aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Liuxx-Bear on 2016/6/6.
 */
public class LogPrint {
    private static final Logger standardLog = LoggerFactory.getLogger("standardLog");
    private static final Logger longtimeLog = LoggerFactory.getLogger("longtimeLog");

    public static void print(String methodName,String stbParam,long endTimeMillis,long startTimeMillis,StringBuffer returnParam){
        String ret = "";

        if(returnParam!=null){
            ret = returnParam.toString();
        }
        print(methodName, stbParam, endTimeMillis, startTimeMillis, ret);

    }

    public static void print(String methodName,String stbParam,long endTimeMillis,long startTimeMillis,String returnParam){
        StringBuffer logStrBuff = new StringBuffer();
        long cost = endTimeMillis - startTimeMillis;

        logStrBuff.append("method:["+methodName+"]	request:["+stbParam+"]	time：["+(endTimeMillis - startTimeMillis)+"ms ;]" );
        logStrBuff.append("	return-:["+returnParam+"]");
        standardLog.info(logStrBuff.toString());
        if(cost>1000*60*2){
            longtimeLog.info(logStrBuff.toString());
        }

    }
}
