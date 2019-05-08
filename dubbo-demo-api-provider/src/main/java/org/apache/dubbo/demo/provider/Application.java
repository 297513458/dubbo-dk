package org.apache.dubbo.demo.provider;

import java.util.concurrent.locks.LockSupport;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.demo.DemoService;
public class Application {
	public static void main(String[] args) throws Exception {
		new Thread() {
			public void run() {
				ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
				service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
				service.setRegistry(new RegistryConfig("zookeeper://192.168.0.167:2181"));
				service.setInterface(DemoService.class);
				service.setRef(new DemoServiceImpl());
				service.export();
			}
		}.start();
		LockSupport.park();
	}
}