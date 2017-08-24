package com.yunosauto.testsuites.homeshell;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import com.aliyun.framework.log.SysLog;
import com.yaf.framework.Assert;
import com.yaf.framework.env.ConfigParser;
import com.yaf.framework.interfaces.wrapper.DeviceBox;
import com.yaf.framework.interfaces.wrapper.SystemInfoBox;

public class Utils{
	
	
	private static String type = "responsetime";
	private static String caseversion = "V1.4";
//	public static String url = "http://auto11.yunosauto.com/hubble/savedata.php";
	public static String url = "http://hubble.yunos.com/savedata.php";
//	public static String url = "http://10.69.35.108/hubbleSystem/savedata.php";
	CommonOperation co = new CommonOperation();
	
	/**
	 * 获取系统当前时间 格式为hh:mm:ss
	 * @return
	 */
	static String getTimeStamp() {
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = sdFormatter.format(nowTime);
		return timestamp;
	}
	
	public static String getHostip() {
		String hostip = null;
		try {
			InetAddress ia = InetAddress.getLocalHost();
			hostip = ia.getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hostip;
	}
	
	public static String getHostname() {
		String hostname = null;
		try {
			InetAddress ia = InetAddress.getLocalHost();
			hostname = ia.getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hostname;
	}
	
	public static String getDevice() {
		return SystemInfoBox.getDeviceModel();
	}
	
	public static String getVersion() {
		return SystemInfoBox.getFirmwareVersion();
	}
	
	public static String getType() {
		return type;
	}
	
	public static String getCaseversion() {
		return caseversion;
	}
	
	public static String getImei() {
		return SystemInfoBox.getIMEI();
	}
	
	public static String getCaseName(){
		return Thread.currentThread().getStackTrace()[1].getMethodName();
	}
	
	/**
	 * 排序list,然后截取list
	 * @param list
	 * @param startIndex 截取的起始值(包含)
	 * @param endIndex 截取的截止值(不包含)
	 * @return
	 */
	public static List<Long> getResponsetime(List<Long> list, int startIndex, int endIndex) {
		Object[] obj = list.toArray();
		Arrays.sort(obj);
		List<Long> li = new ArrayList<Long>();
		List<Long> listResult = new ArrayList<Long>();
		for(Object o : obj){
			li.add((Long) o);
		}
		listResult = li.subList(startIndex, endIndex);
		return listResult;
	}
	
	
	public static long getAvgesponsetime(List<Long> li, int startIndex, int endIndex, int avgreCount) {
		long avgresponsetime = 0;
		List<Long> list = new ArrayList<Long>();
		list = getResponsetime(li, startIndex, endIndex);
		int size = list.size();
		for (int i = 0; i < size; i++) {
			avgresponsetime += list.get(i);
		}
		return avgresponsetime / avgreCount;
	}
	
	
	public static long getVariance(List<Long> li,int startIndex, int endIndex, int avgreCount) {
		SysLog.info("接收到的集合是" + li);
		SysLog.info("最终计算的集合是" + getResponsetime(li, startIndex, endIndex));
		long variance = 0;
		for (int i = 0; i < getResponsetime(li, startIndex, endIndex).size(); i++) {
			variance += ((getAvgesponsetime(li, startIndex, endIndex, avgreCount) - getResponsetime(li, startIndex, endIndex).get(i)) 
					* (getAvgesponsetime(li, startIndex, endIndex, avgreCount) - getResponsetime(li, startIndex, endIndex).get(i)));
		}
		SysLog.info("标准差是----" + Math.round(Math.sqrt(variance / avgreCount)));
		SysLog.info("平均数是----" + getAvgesponsetime(li, startIndex, endIndex, avgreCount));
		return Math.round(Math.sqrt(variance / avgreCount));
	}

	/**
	 * 冷启动
	 * @param callable
	 * @param model
	 * @param casename
	 */
	public void coldRun(Callable<Long> callable ,String model, String casename) {
		Run run = new Utils.Run(1, 6, 5, 0, 7, 15);
		run.run(callable, model, casename);
	}
	
	
	/**
	 * 热启动
	 * @param callable
	 * @param model
	 * @param casename
	 */
	public void hotRun(Callable<Long> callable ,String model, String casename) {
		Run run = new Run(1, 11, 10, 1, 12, 30);
		run.run(callable, model, casename);
	}
	
	/**
	 * 应用冷启动
	 * @param callable
	 * @param model
	 * @param casename
	 */
	public void appColdStartRun(Callable<Long> callable ,String model, String casename){
		Run run = new Utils.Run(1, 6, 5, 2, 7, 10);
		run.run(callable, model, casename);
	}
	
	/**
	 * 联网应用热启动
	 * @param callable
	 * @param model
	 * @param casename
	 */
	public void accessNetWorkHotRun(Callable<Long> callable ,String model, String casename){
		Run run = new Utils.Run(1, 11, 10, 3, 12, 20);
		run.run(callable, model, casename);
	}
	
	
	public class Run {
		int total = 40;
		int expectSuccesscount  = 20;
		int startIndex;
		int endIndex;
		int avgreCount;
		int runMode;
		
		/**
		 * 
		 * @param startIndex    测试结果排序后取值的起点
		 * @param endIndex      测试结果排序后取值的结束点
		 * @param avgreCount    测试结果排序后取值
		 * @param runMode    测试模块表示
		 *                   0 普通启动
		 *                   1 普通热启动
		 *                   2 应用冷启动
		 *                   3 联网应用热启动
		 * @param expectSuccesscount    期望的测试成功的个数
		 * @param total   期望的测试的总测试，当达到expectSuccesscount时，测试停止
		 */
		Run(int startIndex, int endIndex, int avgreCount,int runMode, int expectSuccesscount, int total){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.avgreCount = avgreCount;
			this.runMode = runMode;
			this.expectSuccesscount = expectSuccesscount;
			this.total = total;
		}
		
		public void run(Callable<Long> callable ,String model, String casename){
			String runModeString = "";
			if(runMode == 0){
				runModeString = "【冷启动】";
			}else if(runMode == 1){
				runModeString = "【热启动】";
			} else if(runMode == 2){
				runModeString = "【应用冷启动】";
			}else if(runMode == 3){
				runModeString = "【联网应用热启动】";
			}
			int actualCount = 1;
			int successcount = 0;
			String startTime = "";
			String endTime = "";
			startTime = getTimeStamp();
			ArrayList<Long> li = new ArrayList<Long>();
			// 所有的WaitForPix方法都可能会返回-1或-2至，-1是超时，-2是出错，所以需要取有效的15次响应时间
			// 同时需要加上次数限制，超过一定次数直接报错
			while (actualCount <= total) {
				// 定义总total数次为40
				Long time = -2l;
				try {
					if(runMode == 0){
						Assert.assertTrue("手机重启",DeviceBox.reboot());
                     	co.cancleFailDialog();
						SysLog.info("关闭重启后的提示框");
						co.cancleDialog();
						co.goback(4);
					}else if(runMode == 2){
						co.goback(4);
					}
					time = callable.call();
					SysLog.info("time:" + time);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// 判断取值是否异常，如果存在异常则认为此次响应时间取值无效，不记录ArrayList
				if (time < 0) {
					SysLog.info("返回time异常：" + time);
				} else {
					li.add(time);
					SysLog.info(">>>"+casename+model+" 第"+actualCount+"次"+runModeString+"响应时间："+time + "\n");
					// 每成功取值一次，成功的次数加1
					successcount = successcount + 1;
					// 如果成功次数等于20，跳出循环
					if (successcount == expectSuccesscount) {
						break;
					}
				}
				actualCount = actualCount + 1;
			}
			
			endTime = getTimeStamp();
			// 如果实际执行次数大于30，报错
			if (actualCount > total) {
				Assert.assertFalse("实际运行次数:"+actualCount+"，出错次数:"+(actualCount-successcount) + ",成功次数：" + successcount, true);
			} else {
				SysLog.info(model+"用例执行成功");
//				UploadData upload = new Utils.UploadData(li, startIndex, endIndex, avgreCount);
//				upload.uploadDataMethod(li, startTime, endTime, casename, model);
			}
		}
	}
	
	
	public enum Model{
		hotRun("普通应用热启动"),
		accessNetWorkHotRun("联网应用热启动"),
		appInsideRun("应用内浏览"),
		outOfAppRun("退出应用"),
		homeShell("桌面"),
		screenLockAndRestart("锁屏&开关机"),
		appColdRun("应用冷启动");
		
		private String name;
		
		public String getName(){
			return name;
		}
		
		Model(String name){
			this.name = name;
		}
	}
	
}


abstract class TestCaseModel {
	
	Utils utils = new Utils();
	
	public void BeforeRun(){
		
	}
	
	public abstract long CaseStep();
	
	public void AfterRun(){
		
	}

	public  void testCaseHot(String model, String casename){
		BeforeRun();
		try{
			utils.hotRun(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					// TODO Auto-generated method stub
					return CaseStep();
				}
			}, model, casename);
		}finally{
			AfterRun();
			SysLog.info(">>>"+casename + " 测试结束");
		}
	}
	
	public  void testCaseCold(String model, String casename){
		BeforeRun();
		try{
			utils.coldRun(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					// TODO Auto-generated method stub
					return CaseStep();
				}
			}, model, casename);
		}finally{
			AfterRun();
			SysLog.info(">>>"+casename + " 测试结束");
		}
		
	}
	
	public  void testAppColdStart(String model, String casename){
		BeforeRun();
		try{
			utils.appColdStartRun(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					// TODO Auto-generated method stub
					return CaseStep();
				}
			}, model, casename);
		}finally{
			AfterRun();
			SysLog.info(">>>"+casename + " 测试结束");
		}
	}
	
	public  void testAccessNetWorkHotRun(String model, String casename){
		BeforeRun();
		try{
			utils.accessNetWorkHotRun(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					// TODO Auto-generated method stub
					return CaseStep();
				}
			}, model, casename);
		}finally{
			AfterRun();
			SysLog.info(">>>"+casename + " 测试结束");
		}
		
	}
	
}

