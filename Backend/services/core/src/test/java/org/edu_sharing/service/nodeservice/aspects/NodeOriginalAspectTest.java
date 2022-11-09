package org.edu_sharing.service.nodeservice.aspects;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.annotation.NodeManipulation;
import org.edu_sharing.service.nodeservice.annotation.NodeOriginal;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NodeOriginalAspectTest {

    @Mock
    NodeService nodeService;


    @Test
    @RepeatedTest(value = 5)
    void mapNodeOriginal() {
        String nodeA = UUID.randomUUID().toString();
        String nodeB = UUID.randomUUID().toString();
        String nodeC = UUID.randomUUID().toString();
        TestClass target
                = Mockito.mock(TestClass.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        NodeOriginalAspect aspect = new NodeOriginalAspect(nodeService);
        Mockito.when(nodeService.getOriginalNode(ArgumentMatchers.anyString())).thenAnswer(invocation ->
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, invocation.getArgument(0) + "_ORIGINAL")
        );
        factory.addAspect(aspect);
        TestClass proxy = factory.getProxy();
        proxy.originalTestMethod(nodeA,nodeB, nodeC);
        Mockito.verify(target).originalTestMethod(nodeA + "_ORIGINAL", nodeB, nodeC + "_ORIGINAL");
    }
}

class TestClass {
    @NodeManipulation
    public void originalTestMethod(
            @NodeOriginal() String nodeA,
            String nodeB,
            @NodeOriginal() String nodeC) {
    }
}