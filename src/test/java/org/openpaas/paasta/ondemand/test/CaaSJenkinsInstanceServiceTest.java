package org.openpaas.paasta.ondemand.test;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.kubernetes.client.models.AppsV1beta1RollbackConfig;
import io.kubernetes.client.models.V1PodSpec;
import model.*;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.*;
import org.openpaas.paasta.caas_jenkins.common.CommonService;
import org.openpaas.paasta.caas_jenkins.common.RestTemplateService;
import org.openpaas.paasta.caas_jenkins.config.GsonConfig;
import org.openpaas.paasta.caas_jenkins.exception.CaaSJenkinsServiceException;
import org.openpaas.paasta.caas_jenkins.model.JpaJenkinsInstance;
import org.openpaas.paasta.caas_jenkins.model.JpaServiceInstance;
import org.openpaas.paasta.caas_jenkins.model.caas_custom.*;
import org.openpaas.paasta.caas_jenkins.repo.JpaJenkinsInstanceRepository;
import org.openpaas.paasta.caas_jenkins.repo.JpaServiceInstanceRepository;
import org.openpaas.paasta.caas_jenkins.service.impl.CaaSJenkinsInstanceService;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CaaSJenkinsInstanceServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @InjectMocks
    CaaSJenkinsInstanceService caaSJenkinsInstanceService;

    @Mock
    JpaServiceInstanceRepository jpaServiceInstanceRepository;

    @Mock
    JpaJenkinsInstanceRepository jpaJenkinsInstanceRepository;

    @Mock
    private MockRestServiceServer mockServer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    RestTemplateService restTemplateService;

    @Mock
    private Executor serviceBExecutorService;

    @Spy
    GsonConfig gsonConfig;

    @Mock
    CommonService common;

    // My class in which I want to inject the mocks
    @InjectMocks
    private CompletableFuture service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new CompletableFuture();
        ReflectionTestUtils.setField(caaSJenkinsInstanceService, "namespace", "namespace");
        ReflectionTestUtils.setField(caaSJenkinsInstanceService, "caas_api_uri", "caas_api_uri");
        ReflectionTestUtils.setField(caaSJenkinsInstanceService, "master_api", "master_api");

        print();
    }

    private void print() {
        logger.info(caaSJenkinsInstanceService.namespace);
        logger.info(caaSJenkinsInstanceService.caas_api_uri);
        logger.info(caaSJenkinsInstanceService.master_api);
    }

    @Test
    public void updateServiceInstanceTest() throws Exception {
        UpdateServiceInstanceRequest request = ServiceInstanceRequestModel.getUpdateServiceInstanceRequest();
        assertThatThrownBy(() -> caaSJenkinsInstanceService.updateServiceInstance(request))
                .isInstanceOf(ServiceBrokerException.class).hasMessageContaining("Not Supported");
    }

    @Test
    public void createServiceInstanceTest() throws Exception {
        CreateServiceInstanceRequest request = JpaServiceInstanceModel.getCreateServiceInstanceRequest();
        logger.info(request.getServiceDefinition().getName());
        JpaServiceInstance serviceInstance = ServiceInstanceModel.getJpaServiceInstance();
        JpaJenkinsInstance jpaJenkinsInstance = new JpaJenkinsInstance();
        KubeServiceV1  v1 = DeploymentInstanceModel.getKubeServiceV1();
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.convertValue(v1, Map.class);
        when(jpaJenkinsInstanceRepository.existsByOrganizationGuid(request.getOrganizationGuid())).thenReturn(false);
        when(common.deployment(request.getOrganizationGuid())).thenReturn("deployment");
        when(common.service(request.getOrganizationGuid())).thenReturn("service");
        when(restTemplateService.send("/namespaces/"+"namespace"+"/services/"+"jenkins-"+request.getOrganizationGuid(), HttpMethod.GET, null, Map.class)).thenReturn(map);
        Gson gson = new Gson();
        when(common.getGson()).thenReturn(gson);
        ServiceInstance result = caaSJenkinsInstanceService.createServiceInstance(request);
    }

    @Test
    public void createServiceInstanceTest2() throws Exception {
        CreateServiceInstanceRequest request = JpaServiceInstanceModel.getCreateServiceInstanceRequest();
        when(jpaJenkinsInstanceRepository.existsByOrganizationGuid(request.getOrganizationGuid())).thenReturn(true);
        assertThatThrownBy(() -> caaSJenkinsInstanceService.createServiceInstance(request))
                .isInstanceOf(CaaSJenkinsServiceException.class).hasMessageContaining("Currently, only 1 service instances can be created in this organization.");
    }

    @Test
    public void deleteServiceInstanceTest1() throws Exception {
        DeleteServiceInstanceRequest request = JpaServiceInstanceModel.getDeleteServiceInstanceRequest();
        when(jpaJenkinsInstanceRepository.existsByServiceInstanceId(request.getServiceInstanceId())).thenReturn(true);
        JpaJenkinsInstance jpaJenkinsInstance = JpaJenkinsInstanceModel.getJpaJenkinsInstance("org_id","service_id","namespace");
        when(jpaJenkinsInstanceRepository.findByServiceInstanceId(request.getServiceInstanceId())).thenReturn(jpaJenkinsInstance);
        caaSJenkinsInstanceService.deleteServiceInstance(request);
    }

    @Test
    public void deleteServiceInstanceTest2() throws Exception {
        DeleteServiceInstanceRequest request = JpaServiceInstanceModel.getDeleteServiceInstanceRequest();
        when(jpaJenkinsInstanceRepository.existsByServiceInstanceId(request.getServiceInstanceId())).thenReturn(false);
        caaSJenkinsInstanceService.deleteServiceInstance(request);
    }

    @Test
    public void getServiceInstanceTest1() throws Exception {
        JpaJenkinsInstance jpaJenkinsInstance = JpaJenkinsInstanceModel.getJpaJenkinsInstance("org_id","service_id","namespace");
        when(jpaJenkinsInstanceRepository.findByServiceInstanceId("Instanceid")).thenReturn(jpaJenkinsInstance);
        caaSJenkinsInstanceService.getServiceInstance("Instanceid");
    }

    @Test
    public void getOperationServiceInstanceTest1() throws Exception {
        JpaJenkinsInstance jpaJenkinsInstance = JpaJenkinsInstanceModel.getJpaJenkinsInstance("org_id","service_id","namespace");
        when(jpaJenkinsInstanceRepository.findByServiceInstanceId("Instanceid")).thenReturn(jpaJenkinsInstance);
        KubeDeploymentV1 v1 = DeploymentInstanceModel.getKubeDeploymentV1();
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.convertValue(v1, Map.class);
        Gson gson = new Gson();
        when(common.getGson()).thenReturn(gson);
        when(restTemplateService.send("/namespaces/"+"namespace"+"/deployments/"+"jenkins-"+jpaJenkinsInstance.getOrganizationGuid(), HttpMethod.GET, null, Map.class)).thenReturn(map);
        caaSJenkinsInstanceService.getOperationServiceInstance("Instanceid");
    }

    @Test
    public void getOperationServiceInstanceTest2() throws Exception {
        JpaJenkinsInstance jpaJenkinsInstance = JpaJenkinsInstanceModel.getJpaJenkinsInstance("org_id","service_id","namespace");
        when(jpaJenkinsInstanceRepository.findByServiceInstanceId("Instanceid")).thenReturn(jpaJenkinsInstance);
        KubeDeploymentV1 v1 = DeploymentInstanceModel.getKubeDeploymentV1();
        ObjectMapper mapper = new ObjectMapper();
        v1.getStatus().setUnavailableReplicas(-1);
        Map map = mapper.convertValue(v1, Map.class);
        Gson gson = new Gson();
        when(common.getGson()).thenReturn(gson);
        when(restTemplateService.send("/namespaces/"+"namespace"+"/deployments/"+"jenkins-"+jpaJenkinsInstance.getOrganizationGuid(), HttpMethod.GET, null, Map.class)).thenReturn(map);
        caaSJenkinsInstanceService.getOperationServiceInstance("Instanceid");
    }

    @Test
    public void testAll() throws Exception{
        JpaJenkinsInstance jpaJenkinsInstance = new JpaJenkinsInstance();
        jpaJenkinsInstance.setOrganizationGuid("org_id");
        jpaJenkinsInstance.getOrganizationGuid();
        jpaJenkinsInstance.setServiceInstanceId("service_id");
        jpaJenkinsInstance.setNamespace("namespace");
        jpaJenkinsInstance.getNamespace();
        KubeMetadataV1 v1 = new KubeMetadataV1();
        v1.putAnnotationsItem("key", null);
        v1.setDeletionTimestamp("delete");
        v1.addFinalizersItem(null);
        v1.putLabelsItem("key", null);
        v1.addOwnerReferencesItem(null);
        v1.equals(v1);
        v1.equals(null);
        v1.equals(new KubeMetadataV1());
        v1.hashCode();
        v1.toString();
        KubeDeploymentStatusV1 v2 = new KubeDeploymentStatusV1();
        v2.addConditionsItem(null);
        v2.addConditionsItem(new KubeDeploymentConditionV1());
        v2.equals(v2);
        v2.equals(null);
        v2.equals(new KubeDeploymentStatusV1());
        v2.hashCode();
        v2.toString();

        KubeDeploymentV1 v3  = new KubeDeploymentV1();
        v3.equals(v3);
        v3.equals(null);
        v3.equals(new KubeDeploymentV1());
        v3.hashCode();
        v3.toString();

        KubeDeploymentSpecV1 v4 = new KubeDeploymentSpecV1();
        v4.progressDeadlineSeconds(1);
        v4.setProgressDeadlineSeconds(1);
        v4.rollbackTo(new AppsV1beta1RollbackConfig());
        v4.setRollbackTo((new AppsV1beta1RollbackConfig()));
        v4.equals(v4);
        v4.equals(null);
        v4.equals(new KubeDeploymentSpecV1());
        v4.hashCode();
        v4.toString();

        KubeServiceV1 v5 = new KubeServiceV1();
        v5.equals(v5);
        v5.equals(null);
        v5.equals(new KubeServiceV1());
        v5.hashCode();
        v5.toString();

        JpaServiceInstance jpa = new JpaServiceInstance();
        jpa.setUserId("id");
        jpa.getUserId();
        jpa.setCaasAccountTokenName("token");
        jpa.getCaasAccountTokenName();
        jpa.setCaasAccountName("name");
        jpa.getCaasAccountName();
        jpa.setCaasNamespace("namespace");
        jpa.getCaasNamespace();
        jpa.getDashboardUrl();
        jpa.setPlanId("plan");
        jpa.getPlanId();
        jpa.setServiceDefinitionId("id");
        jpa.getServiceDefinitionId();
        jpa.setSpaceGuid("space");
        jpa.getSpaceGuid();

        KubeTemplateV1 v11 = new KubeTemplateV1();
        v11.metadata(DeploymentInstanceModel.getKubeMetadataV1());
        v11.setMetadata(DeploymentInstanceModel.getKubeMetadataV1());
        v11.spec(new V1PodSpec());
        v11.setSpec(new V1PodSpec());
        v11.equals(v11);
        v11.equals(null);
        v11.equals(new KubeTemplateV1());
        v11.hashCode();
        v11.toString();

        JpaJenkinsInstance jpaJenkinsInstance1 = JpaJenkinsInstanceModel.getJpaJenkinsInstance("org_id","service_id","namespace");
        when(jpaJenkinsInstanceRepository.findByServiceInstanceId("Instanceid")).thenReturn(jpaJenkinsInstance1);
        caaSJenkinsInstanceService.getServiceInstance("Instanceid");


    }
}
