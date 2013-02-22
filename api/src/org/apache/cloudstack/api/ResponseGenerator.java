// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api;

import com.cloud.async.AsyncJob;
import com.cloud.capacity.Capacity;
import com.cloud.configuration.Configuration;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.dc.Vlan;
import com.cloud.domain.Domain;
import com.cloud.event.Event;
import com.cloud.host.Host;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.network.*;
import com.cloud.network.Network.Service;
import com.cloud.network.as.*;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.*;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRoute;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.server.ResourceTag;
import com.cloud.storage.*;
import com.cloud.storage.snapshot.SnapshotPolicy;
import com.cloud.storage.snapshot.SnapshotSchedule;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.uservm.UserVm;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.command.user.job.QueryAsyncJobResultCmd;
import org.apache.cloudstack.api.response.*;
import org.apache.cloudstack.region.Region;

import java.text.DecimalFormat;
import java.util.EnumSet;
import java.util.List;

public interface ResponseGenerator {
    UserResponse createUserResponse(UserAccount user);

    AccountResponse createAccountResponse(Account account);

    DomainResponse createDomainResponse(Domain domain);

    DiskOfferingResponse createDiskOfferingResponse(DiskOffering offering);

    ResourceLimitResponse createResourceLimitResponse(ResourceLimit limit);

    ResourceCountResponse createResourceCountResponse(ResourceCount resourceCount);

    ServiceOfferingResponse createServiceOfferingResponse(ServiceOffering offering);

    ConfigurationResponse createConfigurationResponse(Configuration cfg);

    SnapshotResponse createSnapshotResponse(Snapshot snapshot);

    SnapshotPolicyResponse createSnapshotPolicyResponse(SnapshotPolicy policy);

    List<UserVmResponse> createUserVmResponse(String objectName, UserVm... userVms);

    List<UserVmResponse> createUserVmResponse(String objectName, EnumSet<VMDetails> details, UserVm... userVms);

    SystemVmResponse createSystemVmResponse(VirtualMachine systemVM);

    DomainRouterResponse createDomainRouterResponse(VirtualRouter router);

    HostResponse createHostResponse(Host host, EnumSet<HostDetails> details);

    HostResponse createHostResponse(Host host);

    VlanIpRangeResponse createVlanIpRangeResponse(Vlan vlan);

    IPAddressResponse createIPAddressResponse(IpAddress ipAddress);

    GlobalLoadBalancerResponse createGlobalLoadBalancerResponse(GlobalLoadBalancerRule globalLoadBalancerRule);

    LoadBalancerResponse createLoadBalancerResponse(LoadBalancer loadBalancer);

    LBStickinessResponse createLBStickinessPolicyResponse(List<? extends StickinessPolicy> stickinessPolicies, LoadBalancer lb);

    LBStickinessResponse createLBStickinessPolicyResponse(StickinessPolicy stickinessPolicy, LoadBalancer lb);

    PodResponse createPodResponse(Pod pod, Boolean showCapacities);

    ZoneResponse createZoneResponse(DataCenter dataCenter, Boolean showCapacities);

    VolumeResponse createVolumeResponse(Volume volume);

    InstanceGroupResponse createInstanceGroupResponse(InstanceGroup group);

    StoragePoolResponse createStoragePoolResponse(StoragePool pool);

    ClusterResponse createClusterResponse(Cluster cluster, Boolean showCapacities);

    FirewallRuleResponse createPortForwardingRuleResponse(PortForwardingRule fwRule);

    IpForwardingRuleResponse createIpForwardingRuleResponse(StaticNatRule fwRule);

    User findUserById(Long userId);

    UserVm findUserVmById(Long vmId);

    Volume findVolumeById(Long volumeId);

    Account findAccountByNameDomain(String accountName, Long domainId);

    VirtualMachineTemplate findTemplateById(Long templateId);

    Host findHostById(Long hostId);

    List<TemplateResponse> createTemplateResponses(long templateId, long zoneId, boolean readyOnly);

    VpnUsersResponse createVpnUserResponse(VpnUser user);

    RemoteAccessVpnResponse createRemoteAccessVpnResponse(RemoteAccessVpn vpn);

    List<TemplateResponse> createTemplateResponses(long templateId, Long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(long templateId, Long snapshotId, Long volumeId, boolean readyOnly);

    //ListResponse<SecurityGroupResponse> createSecurityGroupResponses(List<? extends SecurityGroupRules> networkGroups);

    SecurityGroupResponse createSecurityGroupResponseFromSecurityGroupRule(List<? extends SecurityRule> SecurityRules);

    SecurityGroupResponse createSecurityGroupResponse(SecurityGroup group);

    ExtractResponse createExtractResponse(Long uploadId, Long id, Long zoneId, Long accountId, String mode);

    String toSerializedString(CreateCmdResponse response, String responseType);

    AsyncJobResponse createAsyncJobResponse(AsyncJob job);

    EventResponse createEventResponse(Event event);

    //List<EventResponse> createEventResponse(EventJoinVO... events);

    TemplateResponse createIsoResponse(VirtualMachineTemplate result);

    List<CapacityResponse> createCapacityResponse(List<? extends Capacity> result, DecimalFormat format);

    TemplatePermissionsResponse createTemplatePermissionsResponse(List<String> accountNames, Long id, boolean isAdmin);

    AsyncJobResponse queryJobResult(QueryAsyncJobResultCmd cmd);

    NetworkOfferingResponse createNetworkOfferingResponse(NetworkOffering offering);

    NetworkResponse createNetworkResponse(Network network);

    UserResponse createUserResponse(User user);

    //List<UserResponse> createUserResponse(UserAccountJoinVO... users);

    AccountResponse createUserAccountResponse(UserAccount user);

    Long getSecurityGroupId(String groupName, long accountId);

    List<TemplateResponse> createIsoResponses(long isoId, Long zoneId, boolean readyOnly);

    ProjectResponse createProjectResponse(Project project);


    List<TemplateResponse> createIsoResponses(VirtualMachineTemplate iso, long zoneId, boolean readyOnly);

    List<TemplateResponse> createTemplateResponses(long templateId, Long vmId);

    FirewallResponse createFirewallResponse(FirewallRule fwRule);

    HypervisorCapabilitiesResponse createHypervisorCapabilitiesResponse(HypervisorCapabilities hpvCapabilities);

    ProjectAccountResponse createProjectAccountResponse(ProjectAccount projectAccount);

    ProjectInvitationResponse createProjectInvitationResponse(ProjectInvitation invite);

    SystemVmInstanceResponse createSystemVmInstanceResponse(VirtualMachine systemVM);

    SwiftResponse createSwiftResponse(Swift swift);

    S3Response createS3Response(S3 result);

    PhysicalNetworkResponse createPhysicalNetworkResponse(PhysicalNetwork result);

    ServiceResponse createNetworkServiceResponse(Service service);

    ProviderResponse createNetworkServiceProviderResponse(PhysicalNetworkServiceProvider result);

    TrafficTypeResponse createTrafficTypeResponse(PhysicalNetworkTrafficType result);

    VirtualRouterProviderResponse createVirtualRouterProviderResponse(VirtualRouterProvider result);

    LDAPConfigResponse createLDAPConfigResponse(String hostname, Integer port, Boolean useSSL, String queryFilter, String baseSearch, String dn);

    StorageNetworkIpRangeResponse createStorageNetworkIpRangeResponse(StorageNetworkIpRange result);

    RegionResponse createRegionResponse(Region region);

    /**
     * @param resourceTag
     * @param keyValueOnly TODO
     * @return
     */
    ResourceTagResponse createResourceTagResponse(ResourceTag resourceTag, boolean keyValueOnly);


    Site2SiteVpnGatewayResponse createSite2SiteVpnGatewayResponse(Site2SiteVpnGateway result);


    /**
     * @param offering
     * @return
     */
    VpcOfferingResponse createVpcOfferingResponse(VpcOffering offering);

    /**
     * @param vpc
     * @return
     */
    VpcResponse createVpcResponse(Vpc vpc);

    /**
     * @param networkACL
     * @return
     */
    NetworkACLResponse createNetworkACLResponse(FirewallRule networkACL);

    /**
     * @param result
     * @return
     */
    PrivateGatewayResponse createPrivateGatewayResponse(PrivateGateway result);

    /**
     * @param result
     * @return
     */
    StaticRouteResponse createStaticRouteResponse(StaticRoute result);

    Site2SiteCustomerGatewayResponse createSite2SiteCustomerGatewayResponse(Site2SiteCustomerGateway result);

    Site2SiteVpnConnectionResponse createSite2SiteVpnConnectionResponse(Site2SiteVpnConnection result);

    CounterResponse createCounterResponse(Counter ctr);

    ConditionResponse createConditionResponse(Condition cndn);

    AutoScalePolicyResponse createAutoScalePolicyResponse(AutoScalePolicy policy);

    AutoScaleVmProfileResponse createAutoScaleVmProfileResponse(AutoScaleVmProfile profile);

    AutoScaleVmGroupResponse createAutoScaleVmGroupResponse(AutoScaleVmGroup vmGroup);

    GuestOSResponse createGuestOSResponse(GuestOS os);

    SnapshotScheduleResponse createSnapshotScheduleResponse(SnapshotSchedule sched);
}
