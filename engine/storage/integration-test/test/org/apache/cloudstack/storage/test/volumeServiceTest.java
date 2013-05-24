/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.type.RootDisk;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;
import org.apache.cloudstack.storage.datastore.VolumeDataFactory;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.provider.DataStoreProvider;
import org.apache.cloudstack.storage.datastore.provider.DataStoreProviderManager;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.image.ImageDataFactory;
import org.apache.cloudstack.storage.image.ImageService;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.db.ImageDataDao;
import org.apache.cloudstack.storage.image.db.ImageDataVO;
import org.apache.cloudstack.storage.volume.VolumeService;
import org.apache.cloudstack.storage.volume.VolumeService.VolumeApiResult;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Managed.ManagedState;
import com.cloud.resource.ResourceState;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.TemplateType;

@ContextConfiguration(locations={"classpath:/storageContext.xml"})
public class volumeServiceTest extends CloudStackTestNGBase {
	//@Inject
	//ImageDataStoreProviderManager imageProviderMgr;
	@Inject
	ImageService imageService;
	@Inject
	VolumeService volumeService;
	@Inject
	ImageDataDao imageDataDao;
	@Inject
	VolumeDao2 volumeDao;
	@Inject 
	HostDao hostDao;
	@Inject
	HostPodDao podDao;
	@Inject
	ClusterDao clusterDao;
	@Inject
	DataCenterDao dcDao;
	@Inject
	PrimaryDataStoreDao primaryStoreDao;
	@Inject
	DataStoreProviderManager dataStoreProviderMgr;
	@Inject
	AgentManager agentMgr;
	@Inject
	EndPointSelector selector;
	@Inject
	ImageDataFactory imageDataFactory;
	@Inject
	VolumeDataFactory volumeFactory;
	Long dcId;
	Long clusterId;
	Long podId;
	HostVO host;
	String primaryName = "my primary data store";
	DataStore primaryStore;
	
    @Test(priority = -1)
	public void setUp() {
        try {
            dataStoreProviderMgr.configure(null, new HashMap<String, Object>());
        } catch (ConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        host = hostDao.findByGuid(this.getHostGuid());
        if (host != null) {
            dcId = host.getDataCenterId();
            clusterId = host.getClusterId();
            podId = host.getPodId();
            return;
        }
		//create data center
		DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24", 
				null, null, NetworkType.Basic, null, null, true,  true, null, null);
		dc = dcDao.persist(dc);
		dcId = dc.getId();
		//create pod

		HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), dc.getId(), this.getHostGateway(), this.getHostCidr(), 8, "test");
		pod = podDao.persist(pod);
		podId = pod.getId();
		//create xen cluster
		ClusterVO cluster = new ClusterVO(dc.getId(), pod.getId(), "devcloud cluster");
		cluster.setHypervisorType(HypervisorType.XenServer.toString());
		cluster.setClusterType(ClusterType.CloudManaged);
		cluster.setManagedState(ManagedState.Managed);
		cluster = clusterDao.persist(cluster);
		clusterId = cluster.getId();
		//create xen host

		host = new HostVO(this.getHostGuid());
		host.setName("devcloud xen host");
		host.setType(Host.Type.Routing);
		host.setPrivateIpAddress(this.getHostIp());
		host.setDataCenterId(dc.getId());
		host.setVersion("6.0.1");
		host.setAvailable(true);
		host.setSetup(true);
		host.setPodId(podId);
		host.setLastPinged(0);
		host.setResourceState(ResourceState.Enabled);
		host.setHypervisorType(HypervisorType.XenServer);
		host.setClusterId(cluster.getId());

		host = hostDao.persist(host);

		//primaryStore = createPrimaryDataStore();
	
		//CreateVolumeAnswer createVolumeFromImageAnswer = new CreateVolumeAnswer(UUID.randomUUID().toString());

		/*try {
			Mockito.when(agentMgr.send(Mockito.anyLong(), Mockito.any(CreateVolumeFromBaseImageCommand.class))).thenReturn(createVolumeFromImageAnswer);
		} catch (AgentUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationTimedoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/


		//Mockito.when(primaryStoreDao.findById(Mockito.anyLong())).thenReturn(primaryStore);
	}
    
    @Override
    protected void injectMockito() {
        if (host == null) {
            return;
        }
        List<HostVO> results = new ArrayList<HostVO>();
        results.add(host);
        Mockito.when(hostDao.listAll()).thenReturn(results);
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.when(hostDao.findHypervisorHostInCluster(Mockito.anyLong())).thenReturn(results);
        List<EndPoint> eps = new ArrayList<EndPoint>();
        eps.add(HypervisorHostEndPoint.getHypervisorHostEndPoint(host.getId(),
                host.getPrivateIpAddress()));
        Mockito.when(selector.selectAll(Mockito.any(DataStore.class))).thenReturn(eps);
        Mockito.when(selector.select(Mockito.any(DataObject.class))).thenReturn(eps.get(0));
        Mockito.when(selector.select(Mockito.any(DataObject.class), Mockito.any(DataObject.class))).thenReturn(eps.get(0));
    }

	private ImageDataVO createImageData() {
		ImageDataVO image = new ImageDataVO();
		image.setTemplateType(TemplateType.USER);
		image.setUrl(this.getTemplateUrl());
		image.setUniqueName(UUID.randomUUID().toString());
		image.setName(UUID.randomUUID().toString());
		image.setPublicTemplate(true);
		image.setFeatured(true);
		image.setRequireHvm(true);
		image.setBits(64);
		image.setFormat(Storage.ImageFormat.VHD.toString());
		image.setAccountId(1);
		image.setEnablePassword(true);
		image.setEnableSshKey(true);
		image.setGuestOSId(1);
		image.setBootable(true);
		image.setPrepopulate(true);
		image.setCrossZones(true);
		image.setExtractable(true);
		
		//image.setImageDataStoreId(storeId);
		image = imageDataDao.persist(image);
		
		return image;
	}

	private TemplateInfo createTemplate() {
		try {
		    DataStore store = createImageStore();
		    ImageDataVO image = createImageData();
		    TemplateInfo template = imageDataFactory.getTemplate(image.getId(), store);
		    AsyncCallFuture<CommandResult> future = imageService.createTemplateAsync(template, store);
		    future.get();
		    template = imageDataFactory.getTemplate(image.getId(), store);
			/*imageProviderMgr.configure("image Provider", new HashMap<String, Object>());
			ImageDataVO image = createImageData();
			ImageDataStoreProvider defaultProvider = imageProviderMgr.getProvider("DefaultProvider");
			ImageDataStoreLifeCycle lifeCycle = defaultProvider.getLifeCycle();
			ImageDataStore store = lifeCycle.registerDataStore("defaultHttpStore", new HashMap<String, String>());
			imageService.registerTemplate(image.getId(), store.getImageDataStoreId());
			TemplateEntity te = imageService.getTemplateEntity(image.getId());
			return te;*/
		    return template;
		} catch (Exception e) {
		    Assert.fail("failed", e);
		    return null;
		}
	}

	//@Test
	public void createTemplateTest() {
		createTemplate();
	}
	
	@Test
	public void testCreatePrimaryStorage() {
	    DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider("default primary data store provider");
        Map<String, String> params = new HashMap<String, String>();
        URI uri = null;
        try {
            uri = new URI(this.getPrimaryStorageUrl());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        params.put("url", this.getPrimaryStorageUrl());
        params.put("server", uri.getHost());
        params.put("path", uri.getPath());
        params.put("protocol", uri.getScheme());
        params.put("dcId", dcId.toString());
        params.put("clusterId", clusterId.toString());
        params.put("name", this.primaryName);
        params.put("port", "1");
        params.put("roles", DataStoreRole.Primary.toString());
        params.put("uuid", UUID.nameUUIDFromBytes(this.getPrimaryStorageUrl().getBytes()).toString());
        params.put("providerId", String.valueOf(provider.getId()));
        
        DataStoreLifeCycle lifeCycle = provider.getLifeCycle();
        this.primaryStore  = lifeCycle.initialize(params);
        ClusterScope scope = new ClusterScope(clusterId, podId, dcId);
        lifeCycle.attachCluster(this.primaryStore, scope);
	}
	
	private DataStore createImageStore() {
	    DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider("default image data store");
        Map<String, String> params = new HashMap<String, String>();
        String name = UUID.randomUUID().toString();
        params.put("name", name);
        params.put("uuid", name);
        params.put("protocol", "http");
        params.put("scope", ScopeType.GLOBAL.toString());
        params.put("provider", Long.toString(provider.getId()));
        DataStoreLifeCycle lifeCycle = provider.getLifeCycle();
        DataStore store = lifeCycle.initialize(params);
        return store;
	}
	//@Test
	public void testcreateImageStore() {
	    createImageStore();
	}
	

	public DataStore createPrimaryDataStore() {
		try {
		    DataStoreProvider provider = dataStoreProviderMgr.getDataStoreProvider("default primary data store provider");
		    Map<String, String> params = new HashMap<String, String>();
		    URI uri = new URI(this.getPrimaryStorageUrl());
		    params.put("url", this.getPrimaryStorageUrl());
            params.put("server", uri.getHost());
            params.put("path", uri.getPath());
            params.put("protocol", uri.getScheme());
            params.put("dcId", dcId.toString());
            params.put("clusterId", clusterId.toString());
            params.put("name", this.primaryName);
            params.put("port", "1");
            params.put("roles", DataStoreRole.Primary.toString());
            params.put("uuid", UUID.nameUUIDFromBytes(this.getPrimaryStorageUrl().getBytes()).toString());
            params.put("providerId", String.valueOf(provider.getId()));
		    
		    DataStoreLifeCycle lifeCycle = provider.getLifeCycle();
		    DataStore store = lifeCycle.initialize(params);
		    ClusterScope scope = new ClusterScope(clusterId, podId, dcId);
		    lifeCycle.attachCluster(store, scope);
		    
		    /*
		    PrimaryDataStoreProvider provider = primaryDataStoreProviderMgr.getDataStoreProvider("default primary data store provider");
		    primaryDataStoreProviderMgr.configure("primary data store mgr", new HashMap<String, Object>());
            
		    List<PrimaryDataStoreVO> ds = primaryStoreDao.findPoolByName(this.primaryName);
		    if (ds.size() >= 1) {
		        PrimaryDataStoreVO store = ds.get(0);
		        if (store.getRemoved() == null) {
		            return provider.getDataStore(store.getId());
		        }
		    }
		    
		
			Map<String, String> params = new HashMap<String, String>();
			params.put("url", this.getPrimaryStorageUrl());
			params.put("dcId", dcId.toString());
			params.put("clusterId", clusterId.toString());
			params.put("name", this.primaryName);
			PrimaryDataStoreInfo primaryDataStoreInfo = provider.registerDataStore(params);
			PrimaryDataStoreLifeCycle lc = primaryDataStoreInfo.getLifeCycle();
			ClusterScope scope = new ClusterScope(clusterId, podId, dcId);
			lc.attachCluster(scope);
			return primaryDataStoreInfo;
			*/
		    return store;
		} catch (Exception e) {
			return null;
		}
	}

	private VolumeVO createVolume(Long templateId, long dataStoreId) {
		VolumeVO volume = new VolumeVO(1000, new RootDisk().toString(), UUID.randomUUID().toString(), templateId);
		volume.setPoolId(dataStoreId);
		volume = volumeDao.persist(volume);
		return volume;
	}

	@Test(priority=2)
	public void createVolumeFromTemplate() {
	    DataStore primaryStore = this.primaryStore;
		TemplateInfo te = createTemplate();
		VolumeVO volume = createVolume(te.getId(), primaryStore.getId());
		VolumeInfo vol = volumeFactory.getVolume(volume.getId(), primaryStore);
		//ve.createVolumeFromTemplate(primaryStore.getId(), new VHD(), te);
		AsyncCallFuture<VolumeApiResult> future = volumeService.createVolumeFromTemplateAsync(vol, primaryStore.getId(), te);
		try {
            future.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	//@Test(priority=3) 
	public void createDataDisk() {
	    DataStore primaryStore = this.primaryStore;
	    VolumeVO volume = createVolume(null, primaryStore.getId());
	    VolumeInfo vol = volumeFactory.getVolume(volume.getId(), primaryStore);
	    AsyncCallFuture<VolumeApiResult> future = volumeService.createVolumeAsync(vol, primaryStore.getId());
	    try {
            future.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	//@Test(priority=3) 
	public void createAndDeleteDataDisk() {
	    DataStore primaryStore = this.primaryStore;
	    VolumeVO volume = createVolume(null, primaryStore.getId());
	    VolumeInfo vol = volumeFactory.getVolume(volume.getId(), primaryStore);
	    AsyncCallFuture<VolumeApiResult> future = volumeService.createVolumeAsync(vol, primaryStore.getId());
	    try {
	        future.get();
	    } catch (InterruptedException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (ExecutionException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	    
	    //delete the volume
	    vol = volumeFactory.getVolume(volume.getId(), primaryStore);
	    future = volumeService.deleteVolumeAsync(vol);
	    try {
            future.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

	//@Test(priority=3)
	public void tearDown() {
	    List<PrimaryDataStoreVO> ds = primaryStoreDao.findPoolByName(this.primaryName);
	    for (int i = 0; i < ds.size(); i++) {
	        PrimaryDataStoreVO store = ds.get(i);
	        store.setUuid(null);
	        primaryStoreDao.remove(ds.get(i).getId());
	        primaryStoreDao.expunge(ds.get(i).getId());
	    }
	}

	//@Test
	//@Test
    public void test1() {
		/*System.out.println(VolumeTypeHelper.getType("Root"));
		System.out.println(VolumeDiskTypeHelper.getDiskType("vmdk"));
		System.out.println(ImageFormatHelper.getFormat("ova"));
		AssertJUnit.assertFalse(new VMDK().equals(new VHD()));
		VMDK vmdk = new VMDK();
		AssertJUnit.assertTrue(vmdk.equals(vmdk));
		VMDK newvmdk = new VMDK();
		AssertJUnit.assertTrue(vmdk.equals(newvmdk));

		ImageFormat ova = new OVA();
		ImageFormat iso = new ISO();
		AssertJUnit.assertTrue(ova.equals(new OVA()));
		AssertJUnit.assertFalse(ova.equals(iso));
		AssertJUnit.assertTrue(ImageFormatHelper.getFormat("test").equals(new Unknown()));

		VolumeDiskType qcow2 = new QCOW2();
		ImageFormat qcow2format = new org.apache.cloudstack.storage.image.format.QCOW2();
		AssertJUnit.assertFalse(qcow2.equals(qcow2format));
*/
	}

}